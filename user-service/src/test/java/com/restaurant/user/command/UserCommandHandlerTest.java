package com.restaurant.user.command;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurant.events.UserCreatedEvent;
import com.restaurant.events.UserDeactivatedEvent;
import com.restaurant.events.UserUpdatedEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserProfile;
import com.restaurant.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    private UserCommandHandler commandHandler;

    private UserProfile validProfile;

    @BeforeEach
    void setUp() {
        commandHandler = new UserCommandHandler(userRepository, eventPublisher);
        validProfile = new UserProfile("John", "Doe", "+1234567890");
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserCommand command = new CreateUserCommand("user123", "john.doe@example.com", validProfile);
        User savedUser = new User("user123", "john.doe@example.com", validProfile);

        when(userRepository.existsByUserId("user123")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        String result = commandHandler.handle(command);

        // Then
        assertEquals("user123", result);
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        UserCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("user123", publishedEvent.getAggregateId());
        assertEquals("john.doe@example.com", publishedEvent.getEmail());
        assertEquals("John", publishedEvent.getFirstName());
        assertEquals("Doe", publishedEvent.getLastName());
    }

    @Test
    void shouldThrowExceptionWhenUserIdAlreadyExists() {
        // Given
        CreateUserCommand command = new CreateUserCommand("user123", "john.doe@example.com", validProfile);

        when(userRepository.existsByUserId("user123")).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commandHandler.handle(command);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        CreateUserCommand command = new CreateUserCommand("user123", "john.doe@example.com", validProfile);

        when(userRepository.existsByUserId("user123")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commandHandler.handle(command);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldUpdateUserProfileSuccessfully() {
        // Given
        UserProfile newProfile = new UserProfile("Jane", "Smith", "+0987654321");
        UpdateUserProfileCommand command = new UpdateUserProfileCommand("user123", newProfile);
        User existingUser = new User("user123", "john.doe@example.com", validProfile);

        when(userRepository.findByUserId("user123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        commandHandler.handle(command);

        // Then
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<UserUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(UserUpdatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        UserUpdatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("user123", publishedEvent.getAggregateId());
        assertEquals("john.doe@example.com", publishedEvent.getEmail());
        assertEquals("Jane", publishedEvent.getFirstName());
        assertEquals("Smith", publishedEvent.getLastName());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given
        UserProfile newProfile = new UserProfile("Jane", "Smith", "+0987654321");
        UpdateUserProfileCommand command = new UpdateUserProfileCommand("user123", newProfile);

        when(userRepository.findByUserId("user123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commandHandler.handle(command);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldDeactivateUserSuccessfully() {
        // Given
        DeactivateUserCommand command = new DeactivateUserCommand("user123", "User requested");
        User existingUser = new User("user123", "john.doe@example.com", validProfile);

        when(userRepository.findByUserId("user123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        commandHandler.handle(command);

        // Then
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<UserDeactivatedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeactivatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        UserDeactivatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("user123", publishedEvent.getAggregateId());
        assertEquals("john.doe@example.com", publishedEvent.getEmail());
        assertEquals("User requested", publishedEvent.getReason());
    }

    @Test
    void shouldThrowExceptionWhenDeactivatingNonExistentUser() {
        // Given
        DeactivateUserCommand command = new DeactivateUserCommand("user123", "User requested");

        when(userRepository.findByUserId("user123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commandHandler.handle(command);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }
}
