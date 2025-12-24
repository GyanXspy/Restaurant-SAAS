package com.restaurant.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restaurant.user.domain.UserProfile;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUserRequest {
    private String userId;
    private String email;
    private UserProfile profile;
}
