package uber.taxi.service;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.dto.chat.ChatResponse;
import uber.taxi.dto.chat.MessageResponse;
import uber.taxi.dto.chat.SendMessageRequest;
import uber.taxi.entity.Chat;
import uber.taxi.entity.ChatParticipant;
import uber.taxi.entity.Message;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.User;
import uber.taxi.exception.ChatNotFoundException;
import uber.taxi.exception.InvalidRequestException;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.repository.ChatParticipantRepository;
import uber.taxi.repository.ChatRepository;
import uber.taxi.repository.MessageRepository;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    public ChatService(ChatRepository chatRepository, ChatParticipantRepository participantRepository,
                       MessageRepository messageRepository, UserService userService) {
        this.chatRepository = chatRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @Transactional
    public Chat getOrCreateForAcceptedMatch(RideMatch match) {
        return chatRepository.findByRideMatchId(match.getId()).orElseGet(() -> {
            Chat chat = chatRepository.save(new Chat(match));
            participantRepository.saveAll(List.of(
                    new ChatParticipant(chat, match.getRideRequest1().getUser()),
                    new ChatParticipant(chat, match.getRideRequest2().getUser())));
            log.info("Chat created with id {} for match {}", chat.getId(), match.getId());
            return chat;
        });
    }

    @Transactional(readOnly = true)
    public ChatResponse get(UUID actorId, UUID chatId) {
        return toResponse(requireParticipant(actorId, findChat(chatId)));
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getForUser(UUID actorId) {
        userService.findEntity(actorId);
        return chatRepository.findAllForUser(actorId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID actorId, UUID chatId, Pageable pageable) {
        requireParticipant(actorId, findChat(chatId));
        return messageRepository.findByChatId(chatId, pageable).map(this::toMessageResponse);
    }

    @Transactional
    public MessageResponse sendMessage(UUID actorId, UUID chatId, SendMessageRequest request) {
        Chat chat = requireParticipant(actorId, findChat(chatId));
        User sender = userService.findEntity(actorId);
        Message message = messageRepository.save(new Message(chat, sender, request.message().trim()));
        log.info("Message sent with id {} in chat {}", message.getId(), chatId);
        return toMessageResponse(message);
    }

    private Chat findChat(UUID chatId) {
        return chatRepository.findById(chatId).orElseThrow(() -> new ChatNotFoundException(chatId));
    }

    private Chat requireParticipant(UUID actorId, Chat chat) {
        if (!participantRepository.existsByChatIdAndUserId(chat.getId(), actorId)) {
            throw new ForbiddenException("You are not a participant in this chat");
        }
        return chat;
    }

    private ChatResponse toResponse(Chat chat) {
        List<UUID> participants = participantRepository.findByChatIdOrderByCreatedAtAsc(chat.getId()).stream()
                .map(participant -> participant.getUser().getId())
                .toList();
        return new ChatResponse(chat.getId(), chat.getRideMatch().getId(), participants, chat.getCreatedAt());
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(message.getId(), message.getChat().getId(), message.getSender().getId(),
                message.getText(), message.getSentAt(), message.getReadAt());
    }
}
