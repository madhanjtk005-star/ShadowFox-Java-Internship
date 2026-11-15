package com.shadowfox.smartclassroom.repository;

import java.util.function.Consumer;

public interface Callback<T> {
    void onSuccess(T result);

    void onError(String message);

    static <T> Callback<T> of(Consumer<T> ok, Consumer<String> err) {
        return new Callback<T>() {
            @Override public void onSuccess(T result) { ok.accept(result); }
            @Override public void onError(String message) { err.accept(message); }
        };
    }
}
