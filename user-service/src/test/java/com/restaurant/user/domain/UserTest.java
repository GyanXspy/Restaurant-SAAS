package com.restaurant.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    
    private UserProfile validProfile;
    
    @BeforeEach
    void setUp() {
        validProfile = new UserProfile("John", "Doe", "+1234567890");
    }
    
    @Test
    void shouldCreateValidUser() {
        // Given
        String userId = "user123";
        String email = "john.doe@example.com";
        
        // When
        User user = new User(userId, email, validProfile);
        
        // Then
        assertEquals(userId, user.getUserId());
        assertEquals(email, user.getEmail());
        assertEquals(validProfile, user.getProfile());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.isActive());
    }
    
    @Test
    void shouldThrowExceptionForNullUserId() {
        // Given
        String email = "john.doe@example.com";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(null, email, validProfile);
        });
    }
    
    @Test
    void shouldThrowExceptionForEmptyUserId() {
        // Given
        String email = "john.doe@example.com";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User("", email, validProfile);
        });
    }
    
    @Test
    void shouldThrowExceptionForNullEmail() {
        // Given
        String userId = "user123";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, null, validProfile);
        });
    }
    
    @Test
    void shouldThrowExceptionForInvalidEmail() {
        // Given
        String userId = "user123";
        String invalidEmail = "invalid-email";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, invalidEmail, validProfile);
        });
    }
    
    @Test
    void shouldThrowExceptionForNullProfile() {
        // Given
        String userId = "user123";
        String email = "john.doe@example.com";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, email, null);
        });
    }
    
    @Test
    void shouldUpdateProfile() {
        // Given
        User user = new User("user123", "john.doe@example.com", validProfile);
        UserProfile newProfile = new UserProfile("Jane", "Smith", "+0987654321");
        
        // When
        user.updateProfile(newProfile);
        
        // Then
        assertEquals(newProfile, user.getProfile());
        assertNotNull(user.getUpdatedAt());
    }
    
    @Test
    void shouldThrowExceptionWhenUpdatingWithNullProfile() {
        // Given
        User user = new User("user123", "john.doe@example.com", validProfile);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            user.updateProfile(null);
        });
    }
    
    @Test
    void shouldUpdateEmail() {
        // Given
        User user = new User("user123", "john.doe@example.com", validProfile);
        String newEmail = "jane.smith@example.com";
        
        // When
        user.updateEmail(newEmail);
        
        // Then
        assertEquals(newEmail, user.getEmail());
        assertNotNull(user.getUpdatedAt());
    }
    
    @Test
    void shouldThrowExceptionForInvalidEmailUpdate() {
        // Given
        User user = new User("user123", "john.doe@example.com", validProfile);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            user.updateEmail("invalid-email");
        });
    }
    
    @Test
    void shouldDeactivateUser() {
        // Given
        User user = new User("user123", "john.doe@example.com", validProfile);
        
        // When
        user.deactivate();
        
        // Then
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertFalse(user.isActive());
        assertNotNull(user.getUpdatedAt());
    }
    
    @Test
    void shouldActivateUser() {
        // Given
        User user = new User("user123", "john.doe@example.com", validProfile);
        user.deactivate();
        
        // When
        user.activate();
        
        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
        assertNotNull(user.getUpdatedAt());
    }
    
    @Test
    void shouldImplementEqualsAndHashCodeBasedOnUserId() {
        // Given
        User user1 = new User("user123", "john.doe@example.com", validProfile);
        User user2 = new User("user123", "different.email@example.com", validProfile);
        User user3 = new User("user456", "john.doe@example.com", validProfile);
        
        // Then
        assertEquals(user1, user2); // Same userId
        assertNotEquals(user1, user3); // Different userId
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1.hashCode(), user3.hashCode());
    }
}