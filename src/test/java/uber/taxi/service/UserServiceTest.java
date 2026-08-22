package uber.taxi.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uber.taxi.dto.user.CreateUserRequest;
import uber.taxi.exception.ConflictException;
import uber.taxi.mapper.UserMapper;
import uber.taxi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper());
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        CreateUserRequest request = new CreateUserRequest("Test", "Person", " Person@Example.com ", null);

        assertThrows(ConflictException.class, () -> userService.create(request));
        verify(userRepository).existsByEmailIgnoreCase("person@example.com");
    }
}
