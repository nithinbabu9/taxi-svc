package uber.taxi.exception;

import org.springframework.http.HttpStatus;

public class GoogleMapsServiceException extends RuntimeException {

    private final HttpStatus responseStatus;

    public GoogleMapsServiceException(String message, HttpStatus responseStatus, Throwable cause) {
        super(message, cause);
        this.responseStatus = responseStatus;
    }

    public HttpStatus getResponseStatus() {
        return responseStatus;
    }
}
