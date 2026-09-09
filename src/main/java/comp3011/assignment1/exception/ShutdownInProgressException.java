package comp3011.assignment1.exception;

// Represents the specific conflict where a graceful shutdown
// has already been requested and cannot be requested again.
public class ShutdownInProgressException extends RuntimeException {

    public ShutdownInProgressException() {
        super("Graceful shutdown is already in progress.");
    }
}