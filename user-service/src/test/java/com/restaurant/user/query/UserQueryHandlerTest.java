package com.restaurant.user.query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserProfile;
import com.restaurant.user.domain.UserStatus;
import com.restaurant.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserQueryHandlerTest {
    
    @Mock
    private UserRepository userRepository;
    
    private UserQueryHandler queryHandler;
    
    private User testUser;
    private UserProfile validProfile;
    
    @BeforeEach
    void setUp() {
        queryHandler = new UserQueryHandler(userRepository);
        validProfile = new UserProfile("John", "Doe", "+1234567890");
        testUser = new User("user123", "john.doe@example.com", validProfile);
    }
    
    @Test
    void shouldReturnUserWhenFound() {
        // Given
        GetUserQuery query = new GetUserQuery("user123");
        when(userRepository.findByUserId("user123")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = queryHandler.handle(query);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByUserId("user123");
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        // Given
        GetUserQuery query = new GetUserQuery("user123");
        when(userRepository.findByUserId("user123")).thenReturn(Optional.empty());
        
        // When
        Optional<User> result = queryHandler.handle(query);
        
        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByUserId("user123");
    }
    
    @Test
    void shouldReturnUserByEmailWhenFound() {
        // Given
        GetUserByEmailQuery query = new GetUserByEmailQuery("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = queryHandler.handle(query);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByEmail("john.doe@example.com");
    }
    
    @Test
    void shouldReturnEmptyWhenUserByEmailNotFound() {
        // Given
        GetUserByEmailQuery query = new GetUserByEmailQuery("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
        
        // When
        Optional<User> result = queryHandler.handle(query);
        
        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail("john.doe@example.com");
    }
    
    @Test
    void shouldReturnAllActiveUsers() {
        // Given
        User user1 = new User("user1", "user1@example.com", validProfile);
        User user2 = new User("user2", "user2@example.com", validProfile);
        List<User> activeUsers = Arrays.asList(user1, user2);
        
        when(userRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(activeUsers);
        
        // When
        List<User> result = queryHandler.getAllActiveUsers();
        
        // Then
        assertEquals(2, result.size());
        assertEquals(activeUsers, result);
        verify(userRepository).findByStatus(UserStatus.ACTIVE);
    }
    
    @Test
    void shouldReturnAllUsers() {
        // Given
        User user1 = new User("user1", "user1@example.com", validProfile);
        User user2 = new User("user2", "user2@example.com", validProfile);
        List<User> allUsers = Arrays.asList(user1, user2);
        
        when(userRepository.findAll()).thenReturn(allUsers);
        
        // When
        List<User> result = queryHandler.getAllUsers();
        
        // Then
        assertEquals(2, result.size());
        assertEquals(allUsers, result);
        verify(userRepository).findAll();
    }
    
    @Test
    void shouldReturnTrueWhenUserExists() {
        // Given
        when(userRepository.existsByUserId("user123")).thenReturn(true);
        
        // When
        boolean result = queryHandler.userExists("user123");
        
        // Then
        assertTrue(result);
        verify(userRepository).existsByUserId("user123");
    }
    
    @Test
    void shouldReturnFalseWhenUserDoesNotExist() {
        // Given
        when(userRepository.existsByUserId("user123")).thenReturn(false);
        
        // When
        boolean result = queryHandler.userExists("user123");
        
        // Then
        assertFalse(result);
        verify(userRepository).existsByUserId("user123");
    }
    
    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Given
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);
        
        // When
        boolean result = queryHandler.emailExists("john.doe@example.com");
        
        // Then
        assertTrue(result);
        verify(userRepository).existsByEmail("john.doe@example.com");
    }
    
    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Given
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        
        // When
        boolean result = queryHandler.emailExists("john.doe@example.com");
        
        // Then
        assertFalse(result);
        verify(userRepository).existsByEmail("john.doe@example.com");
    }
}