package com.smartbackup.exception;

public class RestoreInProgressException extends RuntimeException {

    public RestoreInProgressException(String message) {
        super(message);
    }
}
