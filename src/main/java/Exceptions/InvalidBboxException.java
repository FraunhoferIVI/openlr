package Exceptions;

public class InvalidBboxException extends Exception {

    /**
     * Is thrown if the input information for the requested bounding box are not valid.
     */
    public InvalidBboxException() {
        super("Invalid bounding box input.");
    }
}
