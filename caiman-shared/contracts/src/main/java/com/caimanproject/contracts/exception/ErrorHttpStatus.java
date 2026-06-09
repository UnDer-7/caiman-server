package com.caimanproject.contracts.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorHttpStatus {

    // --- 4xx Client Error ---
    BAD_REQUEST(400, "Bad Request"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    NOT_FOUND(404, "Not Found"),

    // --- 5xx Client Error ---
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int value;
    private final String reasonPhrase;
}
