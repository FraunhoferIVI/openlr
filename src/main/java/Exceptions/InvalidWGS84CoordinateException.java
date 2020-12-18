package Exceptions;

public class InvalidWGS84CoordinateException extends Exception {

    /**
     * Is thrown if the given WGS84 coordinates are invalid.
     */
    public InvalidWGS84CoordinateException() {
        super("The coordinate is outside of the WGS84 bounds.");
    }
}
