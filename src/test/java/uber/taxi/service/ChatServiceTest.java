package uber.taxi.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import uber.taxi.dto.chat.SendMessageRequest;
import uber.taxi.entity.Chat;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.RideRequest;
import uber.taxi.entity.ChatParticipant;
import uber.taxi.entity.Message;
import uber.taxi.entity.User;
import uber.taxi.exception.InvalidRequestException;
import uber.taxi.repository.ChatParticipantRepository;
import uber.taxi.repository.ChatRepository;
import uber.taxi.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatParticipantRepository participantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserService userService;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(chatRepository, participantRepository, messageRepository, userService);
    }

    @Test
    void returnsExistingChatInsteadOfCreatingDuplicate() {
        RideMatch match = org.mockito.Mockito.mock(RideMatch.class);
        UUID matchId = UUID.randomUUID();
        when(match.getId()).thenReturn(matchId);
        Chat existing = new Chat(match);
        when(chatRepository.findByRideMatchId(matchId)).thenReturn(Optional.of(existing));

        assertSame(existing, service.getOrCreateForAcceptedMatch(match));
        verify(chatRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsMessageFromNonParticipant() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Chat chat = org.mockito.Mockito.mock(Chat.class);
        User sender = new User("Test", "Sender", "sender@example.com", null);
        setId(sender, senderId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userService.findEntity(senderId)).thenReturn(sender);
        when(participantRepository.existsByChatIdAndUserId(chatId, senderId)).thenReturn(false);

        assertThrows(InvalidRequestException.class,
                () -> service.sendMessage(chatId, new SendMessageRequest(senderId, "Hello")));
        verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsOneChatWithBothRideParticipants() {
        RideMatch match = org.mockito.Mockito.mock(RideMatch.class);
        RideRequest firstRide = org.mockito.Mockito.mock(RideRequest.class);
        RideRequest secondRide = org.mockito.Mockito.mock(RideRequest.class);
        User firstUser = new User("First", "User", "first@example.com", null);
        User secondUser = new User("Second", "User", "second@example.com", null);
        UUID matchId = UUID.randomUUID();
        when(match.getId()).thenReturn(matchId);
        when(match.getRideRequest1()).thenReturn(firstRide);
        when(match.getRideRequest2()).thenReturn(secondRide);
        when(firstRide.getUser()).thenReturn(firstUser);
        when(secondRide.getUser()).thenReturn(secondUser);
        when(chatRepository.findByRideMatchId(matchId)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chat chat = service.getOrCreateForAcceptedMatch(match);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<ChatParticipant>> captor = ArgumentCaptor.forClass(java.util.List.class);
        verify(participantRepository).saveAll(captor.capture());
        java.util.List<ChatParticipant> participants = captor.getValue();
        assertEquals(2, participants.size());
        assertSame(chat, participants.get(0).getChat());
        assertSame(firstUser, participants.get(0).getUser());
        assertSame(secondUser, participants.get(1).getUser());
    }

    @Test
    void trimsMessageBeforeSaving() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Chat chat = org.mockito.Mockito.mock(Chat.class);
        User sender = new User("Test", "Sender", "sender@example.com", null);
        setId(sender, senderId);
        when(chat.getId()).thenReturn(chatId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userService.findEntity(senderId)).thenReturn(sender);
        when(participantRepository.existsByChatIdAndUserId(chatId, senderId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.sendMessage(chatId, new SendMessageRequest(senderId, "  Ready to leave?  "));

        assertEquals("Ready to leave?", response.message());
        assertEquals(chatId, response.chatId());
        assertEquals(senderId, response.senderId());
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
