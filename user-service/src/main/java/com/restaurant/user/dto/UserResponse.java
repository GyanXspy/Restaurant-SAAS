package com.restaurant.user.dto;

import com.restaurant.user.domain.UserProfile;
import com.restaurant.user.domain.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private String id;
    private String userId;
    private String email;
    private UserProfile profile;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
