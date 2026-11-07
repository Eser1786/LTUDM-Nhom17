package com.tkb.frontend.user.ui.admin;

import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import com.tkb.frontend.user.net.NetworkClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

final class AdminRequestClient {
    private final NetworkClient networkClient;
    private final AtomicInteger nextRequestId = new AtomicInteger(1);

    AdminRequestClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    boolean isAvailable() {
        return networkClient != null;
    }

    AutoCloseable addPushListener(java.util.function.Consumer<Response> listener) {
        if (networkClient == null) {
            return () -> { };
        }
        return networkClient.addPushListener(listener);
    }

    CompletableFuture<Map<String, Object>> send(Command command, Map<String, Object> data) {
        if (networkClient == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Không có kết nối máy chủ."));
        }
        int requestId = nextRequestId.getAndIncrement();
        try {
            return networkClient.send(new Request(requestId, command, data))
                    .thenApply(response -> responseData(requestId, command, response));
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private Map<String, Object> responseData(int requestId, Command command, Response response) {
        if (response == null
                || !Integer.valueOf(requestId).equals(response.getId())
                || response.isPush()
                || response.getOk() == null) {
            throw new IllegalStateException("Máy chủ trả về phản hồi không hợp lệ cho " + command.name() + ".");
        }
        if (!response.getOk()) {
            Response.ErrorPayload error = response.getError();
            if (error != null && "SEMESTER_EXISTS".equals(error.getCode())) {
                throw new IllegalStateException("Mã học kỳ này đã tồn tại. Hãy nhập mã khác.");
            }
            throw new IllegalStateException(error == null || error.getMessage() == null
                    || error.getMessage().isBlank()
                    ? "Máy chủ từ chối yêu cầu " + command.name() + "."
                    : error.getMessage());
        }
        if (response.getData() == null) {
            throw new IllegalStateException("Phản hồi " + command.name() + " thiếu dữ liệu.");
        }
        return response.getData();
    }

    static String messageOf(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }
}
