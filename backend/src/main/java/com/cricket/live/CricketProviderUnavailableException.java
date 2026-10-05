package com.cricket.live;

public class CricketProviderUnavailableException extends RuntimeException {
    public CricketProviderUnavailableException(String message) {
        super(message);
    }

    public CricketProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}