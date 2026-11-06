package com.tkb.frontend.user.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.common.crypto.CryptoUtil;
import com.tkb.common.net.FrameCodec;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class TcpNetworkClient implements NetworkClient, AutoCloseable {
    public static final int DEFAULT_PORT = 9000;
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int HANDSHAKE_TIMEOUT_MILLIS = 10_000;
    private static final int AES_FRAME_OVERHEAD = CryptoUtil.GCM_IV_LENGTH + CryptoUtil.GCM_TAG_BYTES;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object stateLock = new Object();
    private final Map<Integer, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Response>> pushListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<NetworkClientException>> disconnectListeners =
            new CopyOnWriteArrayList<>();
    private final ExecutorService connector = Executors.newSingleThreadExecutor(threadFactory("tkb-connect"));
    private final ExecutorService writer = Executors.newSingleThreadExecutor(threadFactory("tkb-write"));
    private final ExecutorService callbacks = Executors.newSingleThreadExecutor(threadFactory("tkb-callback"));

    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile Socket socket;
    private volatile SecretKey sessionKey;
    private volatile boolean closed;

    public CompletableFuture<Void> connect(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535");
        }

        Socket candidate = new Socket();
        synchronized (stateLock) {
            if (closed) {
                return CompletableFuture.failedFuture(
                        new NetworkClientException("TCP client is closed"));
            }
            if (state != ConnectionState.DISCONNECTED) {
                return CompletableFuture.failedFuture(
                        new NetworkClientException("TCP client is already connecting or connected"));
            }
            state = ConnectionState.CONNECTING;
            socket = candidate;
        }

        try {
            return CompletableFuture.runAsync(() -> {
                try {
                    candidate.connect(new InetSocketAddress(host.trim(), port), CONNECT_TIMEOUT_MILLIS);
                    SecretKey key = performHandshake(candidate);
                    candidate.setSoTimeout(0);
                    synchronized (stateLock) {
                        if (socket != candidate || state != ConnectionState.CONNECTING || closed) {
                            throw new IOException("TCP connection was closed during handshake");
                        }
                        sessionKey = key;
                        state = ConnectionState.CONNECTED;
                    }
                    startReader(candidate, key);
                } catch (IOException | RuntimeException ex) {
                    terminateConnection(candidate, ex, true);
                    throw new java.util.concurrent.CompletionException(
                            new NetworkClientException("Could not establish secure TCP connection", ex));
                }
            }, connector);
        } catch (RejectedExecutionException ex) {
            terminateConnection(candidate, ex, false);
            return CompletableFuture.failedFuture(
                    new NetworkClientException("TCP client is closed", ex));
        }
    }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED && socket != null && !closed;
    }

    public AutoCloseable addPushListener(Consumer<Response> listener) {
        Objects.requireNonNull(listener, "listener");
        pushListeners.add(listener);
        return () -> pushListeners.remove(listener);
    }

    public AutoCloseable addDisconnectListener(Consumer<NetworkClientException> listener) {
        Objects.requireNonNull(listener, "listener");
        disconnectListeners.add(listener);
        return () -> disconnectListeners.remove(listener);
    }

    @Override
    public CompletableFuture<Response> send(Request request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("Request must have a positive id"));
        }
        Command command = request.resolveCommand();
        if (command == null || command == Command.JOB_PROGRESS || command == Command.JOB_DONE) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("Request contains an invalid command"));
        }

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("Could not serialize request", ex));
        }
        if (requestJson.getBytes(StandardCharsets.UTF_8).length
                > FrameCodec.MAX_FRAME_SIZE - AES_FRAME_OVERHEAD) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("Encrypted request exceeds the maximum frame size"));
        }

        Socket activeSocket = socket;
        SecretKey activeKey = sessionKey;
        if (!isConnected() || activeSocket == null || activeKey == null) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("TCP client is not connected"));
        }

        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        PendingRequest pending = new PendingRequest(activeSocket, responseFuture);
        if (pendingRequests.putIfAbsent(request.getId(), pending) != null) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("A request with id " + request.getId() + " is already pending"));
        }

        try {
            writer.execute(() -> writeRequest(activeSocket, activeKey, request.getId(), requestJson));
        } catch (RejectedExecutionException ex) {
            pendingRequests.remove(request.getId(), pending);
            responseFuture.completeExceptionally(
                    new NetworkClientException("TCP client is closed", ex));
        }
        return responseFuture;
    }

    public void disconnect() {
        Socket current = socket;
        if (current != null) {
            terminateConnection(current,
                    new NetworkClientException("TCP connection was closed by the client"), false);
        }
    }

    @Override
    public void close() {
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        disconnect();
        connector.shutdownNow();
        writer.shutdownNow();
        callbacks.shutdown();
    }

    private SecretKey performHandshake(Socket connection) throws IOException {
        connection.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
        String helloJson = FrameCodec.readPlainJsonFrame(connection.getInputStream());
        JsonNode hello = objectMapper.readTree(helloJson);
        if (hello == null
                || !hello.path("type").isTextual()
                || !"HELLO".equals(hello.path("type").textValue())
                || !hello.path("protocolVersion").canConvertToInt()
                || hello.path("protocolVersion").intValue() != 1
                || !hello.path("publicKey").isTextual()
                || hello.path("publicKey").textValue().isBlank()) {
            throw new IOException("Server sent an invalid HELLO message");
        }

        var serverPublicKey = CryptoUtil.decodePublicKeyBase64(hello.path("publicKey").textValue());
        SecretKey key = CryptoUtil.generateAesSessionKey();
        String encryptedKey = CryptoUtil.encryptSessionKeyRsaBase64(key, serverPublicKey);
        String keyExchangeJson = objectMapper.writeValueAsString(Map.of(
                "type", "KEY_EXCHANGE",
                "encryptedKey", encryptedKey));
        FrameCodec.writePlainJsonFrame(connection.getOutputStream(), keyExchangeJson);

        String readyJson = FrameCodec.readEncryptedJsonFrame(connection.getInputStream(), key);
        JsonNode ready = objectMapper.readTree(readyJson);
        if (ready == null || !ready.path("type").isTextual()
                || !"READY".equals(ready.path("type").textValue())) {
            throw new IOException("Server did not complete the handshake with READY");
        }
        return key;
    }

    private void startReader(Socket connection, SecretKey key) {
        Thread reader = threadFactory("tkb-read").newThread(() -> readResponses(connection, key));
        reader.start();
    }

    private void readResponses(Socket connection, SecretKey key) {
        try {
            while (socket == connection && !connection.isClosed()) {
                String json = FrameCodec.readEncryptedJsonFrame(connection.getInputStream(), key);
                Response response = objectMapper.readValue(json, Response.class);
                if (response == null) {
                    throw new IOException("Server sent an empty response");
                }
                if (response.isPush()) {
                    dispatchPush(connection, response);
                } else {
                    dispatchResponse(connection, response);
                }
            }
        } catch (SocketTimeoutException ex) {
            terminateConnection(connection, ex, true);
        } catch (IOException | RuntimeException ex) {
            terminateConnection(connection, ex, true);
        }
    }

    private void dispatchResponse(Socket connection, Response response) throws IOException {
        Integer id = response.getId();
        if (id == null || id <= 0 || response.getPush() != null || response.getOk() == null
                || (Boolean.TRUE.equals(response.getOk()) && response.getData() == null)
                || (Boolean.FALSE.equals(response.getOk()) && response.getError() == null)) {
            throw new IOException("Server sent a response with an invalid envelope");
        }

        PendingRequest pending = pendingRequests.get(id);
        if (pending == null || pending.socket() != connection) {
            throw new IOException("Server returned an unknown request id: " + id);
        }
        pendingRequests.remove(id, pending);
        dispatchCallback(() -> pending.future().complete(response),
                () -> pending.future().completeExceptionally(
                        new NetworkClientException("TCP client is closed")));
    }

    private void dispatchPush(Socket connection, Response response) throws IOException {
        Command command = Command.fromString(response.getPush());
        if ((command != Command.JOB_PROGRESS && command != Command.JOB_DONE)
                || !command.name().equals(response.getPush())
                || response.getId() != null
                || response.getOk() != null
                || response.getError() != null
                || response.getData() == null) {
            throw new IOException("Server sent an invalid push message");
        }
        for (Consumer<Response> listener : pushListeners) {
            dispatchCallback(() -> {
                try {
                    listener.accept(objectMapper.convertValue(response, Response.class));
                } catch (RuntimeException ex) {
                    System.getLogger(TcpNetworkClient.class.getName()).log(
                            System.Logger.Level.WARNING, "Push listener failed", ex);
                }
            }, () -> System.getLogger(TcpNetworkClient.class.getName()).log(
                    System.Logger.Level.WARNING, "Could not dispatch push message"));
        }
    }

    private void writeRequest(Socket connection, SecretKey key, int requestId, String requestJson) {
        PendingRequest pending = pendingRequests.get(requestId);
        if (pending == null || pending.socket() != connection) {
            return;
        }
        if (socket != connection || !isConnected()) {
            if (pendingRequests.remove(requestId, pending)) {
                pending.future().completeExceptionally(
                        new NetworkClientException("TCP connection is no longer available"));
            }
            return;
        }
        try {
            FrameCodec.writeEncryptedJsonFrame(connection.getOutputStream(), requestJson, key);
        } catch (IOException | RuntimeException ex) {
            terminateConnection(connection, ex, true);
        }
    }

    private void terminateConnection(Socket connection, Throwable cause, boolean notifyListeners) {
        synchronized (stateLock) {
            if (socket != connection) {
                return;
            }
            socket = null;
            sessionKey = null;
            state = ConnectionState.DISCONNECTED;
            try {
                connection.close();
            } catch (IOException closeError) {
                System.getLogger(TcpNetworkClient.class.getName()).log(
                        System.Logger.Level.WARNING, "Could not close TCP socket cleanly", closeError);
            }
        }

        NetworkClientException failure = cause instanceof NetworkClientException networkError
                ? networkError
                : new NetworkClientException("TCP connection was lost", cause);
        pendingRequests.forEach((id, pending) -> {
            if (pending.socket() == connection && pendingRequests.remove(id, pending)) {
                dispatchCallback(() -> pending.future().completeExceptionally(failure),
                        () -> pending.future().completeExceptionally(failure));
            }
        });
        if (notifyListeners) {
            for (Consumer<NetworkClientException> listener : disconnectListeners) {
                dispatchCallback(() -> {
                    try {
                        listener.accept(failure);
                    } catch (RuntimeException ex) {
                        System.getLogger(TcpNetworkClient.class.getName()).log(
                                System.Logger.Level.WARNING, "Disconnect listener failed", ex);
                    }
                }, () -> System.getLogger(TcpNetworkClient.class.getName()).log(
                        System.Logger.Level.WARNING, "Could not dispatch disconnect event"));
            }
        }
    }

    private void dispatchCallback(Runnable callback, Runnable rejected) {
        try {
            callbacks.execute(callback);
        } catch (RejectedExecutionException ex) {
            rejected.run();
        }
    }

    private static ThreadFactory threadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger(1);
        return task -> {
            Thread thread = new Thread(task, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private record PendingRequest(Socket socket, CompletableFuture<Response> future) {
    }
}
