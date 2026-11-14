package com.smartbackup.exception;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException(String mimeType) {
        super("Type de fichier non autorisé : " + mimeType);
    }
}
