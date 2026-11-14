package com.smartbackup.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, String message) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(null, message);
    }
}
