package com.tkb.frontend.user.net;

import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface NetworkClient {
    CompletableFuture<Response> send(Request request);

    AutoCloseable addPushListener(Consumer<Response> listener);
}
