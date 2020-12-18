package Exceptions;

public class InvalidHereOLRException extends Exception {

    /**
     * Is thrown if the given Here OpenLR location is invalid.
     * @param message Exceptions message
     */
    public InvalidHereOLRException(String message) {
        super(message);
    }
}
