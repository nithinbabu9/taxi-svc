package uber.taxi.mapper;

import org.springframework.stereotype.Component;
import uber.taxi.dto.user.UserResponse;
import uber.taxi.entity.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhoneNumber(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
