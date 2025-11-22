package it.dieti.dietiestatesbackend.application.exception;

public class OnboardingException extends RuntimeException {
    public enum Reason {
        USER_NOT_FOUND,
        FIRST_ACCESS_ALREADY_COMPLETED,
        ROLE_NOT_ALLOWED,
        PROFILE_ALREADY_EXISTS,
        AGENCY_NOT_FOUND
    }

    private final Reason reason;

    public OnboardingException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
