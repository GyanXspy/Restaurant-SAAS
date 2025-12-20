package com.restaurant.user.query;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserQuery {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
}