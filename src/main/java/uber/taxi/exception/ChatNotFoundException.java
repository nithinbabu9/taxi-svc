package uber.taxi.exception;

import java.util.UUID;

public class ChatNotFoundException extends ResourceNotFoundException {
    public ChatNotFoundException(UUID id) {
        super("Chat not found: " + id);
    }
}
