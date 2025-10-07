package it.dieti.dietiestatesbackend.infrastructure.storage;

public class LocalStorageException extends RuntimeException {
    public LocalStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
