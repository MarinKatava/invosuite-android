package com.invoisuite.app.common;

import com.google.gson.Gson;
import com.invoisuite.app.model.ApiError;
import retrofit2.Response;

/** Best-effort extraction of a human-readable message from a failed Retrofit response. */
public final class ApiErrors {

    private static final Gson GSON = new Gson();

    private ApiErrors() {
    }

    public static String message(Response<?> response, String fallback) {
        if (response == null || response.errorBody() == null) {
            return fallback;
        }
        try {
            String body = response.errorBody().string();
            ApiError error = GSON.fromJson(body, ApiError.class);
            if (error != null && error.message() != null && !error.message().isBlank()) {
                return error.message();
            }
        } catch (Exception ignored) {
            // fall through to the generic message below
        }
        return fallback + " (HTTP " + response.code() + ")";
    }
}
