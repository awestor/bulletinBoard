package ru.daniil.core.exceptions;

public class DiscountNotApplicableException extends RuntimeException {
    public DiscountNotApplicableException(String message) {
        super(message);
    }
}
