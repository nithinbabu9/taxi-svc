package uber.taxi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uber.taxi.repository.ChatParticipantRepository;
import uber.taxi.repository.ChatRepository;
import uber.taxi.repository.EmailVerificationTokenRepository;
import uber.taxi.repository.MessageRepository;
import uber.taxi.repository.RideMatchRepository;
import uber.taxi.repository.RideRequestRepository;
import uber.taxi.repository.UserRepository;
import uber.taxi.service.LocalEmailVerificationSender;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthenticatedWorkflowIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MessageRepository messages;
    @Autowired private ChatParticipantRepository participants;
    @Autowired private ChatRepository chats;
    @Autowired private RideMatchRepository matches;
    @Autowired private RideRequestRepository rides;
    @Autowired private UserRepository users;
    @Autowired private EmailVerificationTokenRepository verificationTokens;
    @Autowired private LocalEmailVerificationSender localEmailSender;

    @BeforeEach
    void clearDatabase() {
        messages.deleteAll();
        participants.deleteAll();
        chats.deleteAll();
        matches.deleteAll();
        rides.deleteAll();
        verificationTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void authenticatedTravelersCanMatchAcceptAndUseTheirPrivateChat() throws Exception {
        AuthenticatedUser travelerA = register("traveler.a@gmail.com", "Traveler", "A");
        AuthenticatedUser travelerB = register("traveler.b@gmail.com", "Traveler", "B");
        AuthenticatedUser outsider = register("outsider@gmail.com", "Outside", "User");
        Instant departure = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        UUID rideA = createRide(travelerA.token(), "Schaumburg, IL", "Millennium Park, Chicago, IL", departure);
        UUID rideB = createRide(travelerB.token(), "Hoffman Estates, IL", "Millennium Park, Chicago, IL",
                departure.plus(10, ChronoUnit.MINUTES));

        MvcResult matchResult = mockMvc.perform(get("/api/rides/{rideId}/matches", rideA)
                        .header("Authorization", bearer(travelerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        UUID matchId = UUID.fromString(json(matchResult).at("/0/id").asText());

        mockMvc.perform(get("/api/matches/mine").header("Authorization", bearer(travelerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(matchId.toString()));

        mockMvc.perform(get("/api/rides/{rideId}", rideA).header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/matches/{matchId}/accept", matchId)
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/matches/{matchId}/accept", matchId)
                        .header("Authorization", bearer(travelerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.chatId").doesNotExist());

        MvcResult secondAcceptance = mockMvc.perform(post("/api/matches/{matchId}/accept", matchId)
                        .header("Authorization", bearer(travelerB.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.chatId").isNotEmpty())
                .andReturn();
        UUID chatId = UUID.fromString(json(secondAcceptance).at("/chatId").asText());

        mockMvc.perform(get("/api/chats/{chatId}", chatId).header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/chats/mine").header("Authorization", bearer(travelerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatId.toString()));

        mockMvc.perform(post("/api/chats/{chatId}/messages", chatId)
                        .header("Authorization", bearer(travelerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"See you at the pickup point\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId").value(travelerA.id().toString()))
                .andExpect(jsonPath("$.message").value("See you at the pickup point"));
        mockMvc.perform(get("/api/chats/{chatId}/messages", chatId)
                        .header("Authorization", bearer(travelerB.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].senderId").value(travelerA.id().toString()));

        mockMvc.perform(delete("/api/rides/{rideId}", rideB).header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointsRejectMissingBearerTokens() throws Exception {
        mockMvc.perform(get("/api/rides/mine")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidationReturnsHelpfulFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void registrationAndLoginRequireGmailAddresses() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"Traveler\",\"email\":\"test@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("must be a valid @gmail.com address"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("must be a valid @gmail.com address"));
    }

    private AuthenticatedUser register(String email, String firstName, String lastName) throws Exception {
        String request = objectMapper.writeValueAsString(new Registration(firstName, lastName, email, PASSWORD));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isAccepted());
        String code = localEmailSender.latestCodeFor(email);
        MvcResult result = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json(result);
        return new AuthenticatedUser(UUID.fromString(body.at("/user/id").asText()), body.at("/accessToken").asText());
    }

    private UUID createRide(String token, String pickup, String destination, Instant departure) throws Exception {
        String request = objectMapper.writeValueAsString(new RideRequest(pickup, destination, departure));
        MvcResult result = mockMvc.perform(post("/api/rides").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).at("/id").asText());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String firstName, String lastName, String email, String password) { }
    private record RideRequest(String pickupAddress, String destinationAddress, Instant departureTime) { }
    private record AuthenticatedUser(UUID id, String token) { }
}
