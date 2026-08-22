package uber.taxi.exception;

public class InvalidLocationException extends InvalidRequestException {
    public InvalidLocationException(String message) {
        super(message);
    }
}
