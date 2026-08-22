package uber.taxi.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uber.taxi.dto.user.CreateUserRequest;
import uber.taxi.dto.user.UserResponse;
import uber.taxi.service.UserService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a user", description = "Creates a user with a unique email address. The returned UUID is used when creating rides and acting in match/chat workflows.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "User created"), @ApiResponse(responseCode = "400", description = "Validation failed"), @ApiResponse(responseCode = "409", description = "Email already exists")})
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user", description = "Returns one user by UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "User found"), @ApiResponse(responseCode = "404", description = "User not found")})
    public UserResponse get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @GetMapping
    @Operation(summary = "Find a user by email", description = "Returns an existing user by email so the same account can create additional ride requests without being registered again.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "User found"), @ApiResponse(responseCode = "404", description = "No user has this email")})
    public UserResponse getByEmail(@RequestParam String email) {
        return userService.getByEmail(email);
    }
}
