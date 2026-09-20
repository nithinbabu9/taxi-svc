package uber.taxi.service;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.dto.match.MatchActionResponse;
import uber.taxi.entity.Chat;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.RideMatchStatus;
import uber.taxi.entity.RideRequestStatus;
import uber.taxi.exception.ConflictException;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.exception.RideMatchNotFoundException;
import uber.taxi.repository.RideMatchRepository;

@Service
public class MatchWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(MatchWorkflowService.class);
    private final RideMatchRepository matchRepository;
    private final ChatService chatService;

    public MatchWorkflowService(RideMatchRepository matchRepository, ChatService chatService) {
        this.matchRepository = matchRepository;
        this.chatService = chatService;
    }

    @Transactional
    public MatchActionResponse accept(UUID matchId, UUID userId) {
        RideMatch match = findLocked(matchId);
        boolean alreadyAccepted = match.getStatus() == RideMatchStatus.ACCEPTED;
        boolean fullyAccepted;
        try {
            fullyAccepted = match.accept(userId, Instant.now());
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException(exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }

        Chat chat = null;
        if (fullyAccepted) {
            if (!alreadyAccepted) {
                if (match.getRideRequest1().getStatus() != RideRequestStatus.OPEN
                        || match.getRideRequest2().getStatus() != RideRequestStatus.OPEN) {
                    throw new ConflictException("One of the ride requests is no longer available");
                }
                match.getRideRequest1().markMatched();
                match.getRideRequest2().markMatched();
                matchRepository.cancelPendingForRide(match.getRideRequest1().getId(),
                        RideMatchStatus.PENDING, RideMatchStatus.CANCELLED);
                matchRepository.cancelPendingForRide(match.getRideRequest2().getId(),
                        RideMatchStatus.PENDING, RideMatchStatus.CANCELLED);
            }
            chat = chatService.getOrCreateForAcceptedMatch(match);
            log.info("Ride match {} accepted", matchId);
        }
        return new MatchActionResponse(matchId, match.getStatus(), chat == null ? null : chat.getId());
    }

    @Transactional
    public MatchActionResponse reject(UUID matchId, UUID userId) {
        RideMatch match = findLocked(matchId);
        try {
            match.reject(userId);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException(exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        log.info("Ride match {} rejected", matchId);
        return new MatchActionResponse(matchId, match.getStatus(), null);
    }

    private RideMatch findLocked(UUID matchId) {
        return matchRepository.findByIdForUpdate(matchId).orElseThrow(() -> new RideMatchNotFoundException(matchId));
    }
}
