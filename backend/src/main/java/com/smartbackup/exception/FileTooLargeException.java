package com.smartbackup.exception;

public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(long maxSizeMb) {
        super("Fichier trop volumineux (limite : " + maxSizeMb + " Mo)");
    }
}
