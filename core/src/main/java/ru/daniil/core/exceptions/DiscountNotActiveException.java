package ru.daniil.core.exceptions;

public class DiscountNotActiveException extends RuntimeException {
  public DiscountNotActiveException(String message) {
    super(message);
  }
}
