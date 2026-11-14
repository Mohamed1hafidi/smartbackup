package com.smartbackup.exception;

public class BackupInProgressException extends RuntimeException {

    public BackupInProgressException(String message) {
        super(message);
    }
}
