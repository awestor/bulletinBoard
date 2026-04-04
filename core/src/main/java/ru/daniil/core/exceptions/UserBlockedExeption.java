package ru.daniil.core.exceptions;

public class UserBlockedExeption extends RuntimeException {
    public UserBlockedExeption(String message) {
        super(message);
    }
}
