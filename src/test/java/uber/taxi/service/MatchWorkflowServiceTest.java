package uber.taxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uber.taxi.dto.match.MatchActionResponse;
import uber.taxi.entity.Chat;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.RideMatchStatus;
import uber.taxi.entity.RideRequest;
import uber.taxi.entity.RideRequestStatus;
import uber.taxi.entity.User;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.repository.RideMatchRepository;

@ExtendWith(MockitoExtension.class)
class MatchWorkflowServiceTest {

    @Mock
    private RideMatchRepository matchRepository;
    @Mock
    private ChatService chatService;

    private MatchWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new MatchWorkflowService(matchRepository, chatService);
    }

    @Test
    void createsChatOnlyAfterBothUsersAccept() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        RideMatch match = match(matchId, firstUserId, secondUserId);
        Chat chat = new Chat(match);
        setId(chat, UUID.randomUUID());
        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(chatService.getOrCreateForAcceptedMatch(match)).thenReturn(chat);

        MatchActionResponse firstAcceptance = service.accept(matchId, firstUserId);
        MatchActionResponse secondAcceptance = service.accept(matchId, secondUserId);

        assertEquals(RideMatchStatus.PENDING, firstAcceptance.status());
        assertNull(firstAcceptance.chatId());
        assertEquals(RideMatchStatus.ACCEPTED, secondAcceptance.status());
        assertEquals(chat.getId(), secondAcceptance.chatId());
        assertEquals(RideRequestStatus.MATCHED, match.getRideRequest1().getStatus());
        assertEquals(RideRequestStatus.MATCHED, match.getRideRequest2().getStatus());
    }

    @Test
    void nonParticipantCannotAcceptMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        RideMatch match = match(matchId, UUID.randomUUID(), UUID.randomUUID());
        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));

        assertThrows(ForbiddenException.class, () -> service.accept(matchId, UUID.randomUUID()));
    }

    @Test
    void participantCanRejectPendingMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        RideMatch match = match(matchId, firstUserId, UUID.randomUUID());
        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));

        MatchActionResponse response = service.reject(matchId, firstUserId);

        assertEquals(RideMatchStatus.REJECTED, response.status());
        assertNull(response.chatId());
        verify(chatService, never()).getOrCreateForAcceptedMatch(match);
    }

    @Test
    void rejectedMatchCannotBeAccepted() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        RideMatch match = match(matchId, firstUserId, UUID.randomUUID());
        match.reject(firstUserId);
        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));

        assertThrows(uber.taxi.exception.ConflictException.class,
                () -> service.accept(matchId, firstUserId));
        verify(chatService, never()).getOrCreateForAcceptedMatch(match);
    }

    private RideMatch match(UUID matchId, UUID firstUserId, UUID secondUserId) throws Exception {
        User firstUser = user(firstUserId);
        User secondUser = user(secondUserId);
        RideRequest firstRide = ride(UUID.fromString("00000000-0000-0000-0000-000000000001"), firstUser);
        RideRequest secondRide = ride(UUID.fromString("00000000-0000-0000-0000-000000000002"), secondUser);
        RideMatch match = new RideMatch(firstRide, secondRide, 0.8, 100, 100, 0.8, 60);
        setId(match, matchId);
        return match;
    }

    private User user(UUID id) throws Exception {
        User user = new User("Test", "User", id + "@example.com", null);
        setId(user, id);
        return user;
    }

    private RideRequest ride(UUID id, User user) throws Exception {
        RideRequest ride = new RideRequest(user, "Pickup", "Destination", Instant.now().plusSeconds(3600));
        setId(ride, id);
        return ride;
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
