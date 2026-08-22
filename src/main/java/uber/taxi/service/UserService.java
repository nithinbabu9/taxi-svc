package uber.taxi.service;

import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.dto.user.CreateUserRequest;
import uber.taxi.dto.user.UserResponse;
import uber.taxi.entity.User;
import uber.taxi.exception.ConflictException;
import uber.taxi.exception.UserNotFoundException;
import uber.taxi.mapper.UserMapper;
import uber.taxi.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists");
        }
        String phone = request.phoneNumber() == null || request.phoneNumber().isBlank()
                ? null : request.phoneNumber().trim();
        User user = userRepository.save(new User(request.firstName().trim(), request.lastName().trim(), email, phone));
        log.info("User created with id {}", user.getId());
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return userMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));
    }

    @Transactional(readOnly = true)
    public User findEntity(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
