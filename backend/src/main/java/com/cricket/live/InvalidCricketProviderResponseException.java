package com.cricket.live;

public class InvalidCricketProviderResponseException extends RuntimeException {
    public InvalidCricketProviderResponseException(String message) {
        super(message);
    }

    public InvalidCricketProviderResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}