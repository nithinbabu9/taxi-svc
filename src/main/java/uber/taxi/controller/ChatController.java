package uber.taxi.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uber.taxi.dto.chat.ChatResponse;
import uber.taxi.dto.chat.MessageResponse;
import uber.taxi.dto.chat.SendMessageRequest;
import uber.taxi.service.ChatService;

@RestController
@RequestMapping("/api")
@Tag(name = "Chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chats/mine")
    @Operation(summary = "List my chats", description = "Returns chats in which the authenticated user is a participant.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Chat list returned"), @ApiResponse(responseCode = "404", description = "User not found")})
    public List<ChatResponse> getForUser(@AuthenticationPrincipal Jwt jwt) {
        return chatService.getForUser(actorId(jwt));
    }

    @GetMapping("/chats/{chatId}")
    @Operation(summary = "Get a chat", description = "Returns chat metadata and both participant UUIDs.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Chat found"), @ApiResponse(responseCode = "404", description = "Chat not found")})
    public ChatResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId) {
        return chatService.get(actorId(jwt), chatId);
    }

    @GetMapping("/chats/{chatId}/messages")
    @Operation(summary = "List chat messages", description = "Returns a pageable message history. Defaults to 50 messages ordered by sentAt ascending. Use page and size query parameters for pagination.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Message page returned"), @ApiResponse(responseCode = "404", description = "Chat not found")})
    public Page<MessageResponse> getMessages(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId,
            @PageableDefault(size = 50, sort = "sentAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return chatService.getMessages(actorId(jwt), chatId, pageable);
    }

    @PostMapping("/chats/{chatId}/messages")
    @Operation(summary = "Send a chat message", description = "Creates a message from the authenticated participant. Messages must contain 1–4000 non-blank characters.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Message sent"), @ApiResponse(responseCode = "400", description = "Validation failed"), @ApiResponse(responseCode = "403", description = "Authenticated user is not a participant"), @ApiResponse(responseCode = "404", description = "Chat or sender not found")})
    public ResponseEntity<MessageResponse> sendMessage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId,
                                                        @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = chatService.sendMessage(actorId(jwt), chatId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{messageId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    private UUID actorId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
