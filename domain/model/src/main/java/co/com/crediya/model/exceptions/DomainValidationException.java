package co.com.crediya.model.exceptions;

public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String code, String detail) { super(code + ":" + detail); }
}
