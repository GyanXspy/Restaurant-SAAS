package com.restaurant.user.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.events.UserCreatedEvent;
import com.restaurant.events.UserDeactivatedEvent;
import com.restaurant.events.UserUpdatedEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.user.domain.User;
import com.restaurant.user.repository.UserRepository;

import jakarta.validation.Valid;

@Component
public class UserCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(UserCommandHandler.class);
    
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    
    @Autowired
    public UserCommandHandler(UserRepository userRepository, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public String handle(@Valid CreateUserCommand command) {
        logger.info("Handling CreateUserCommand for userId: {}", command.getUserId());
        
        // Check if user already exists
        if (userRepository.existsByUserId(command.getUserId())) {
            throw new IllegalArgumentException("User with ID " + command.getUserId() + " already exists");
        }
        
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new IllegalArgumentException("User with email " + command.getEmail() + " already exists");
        }
        
        // Create and save user
        User user = new User(command.getUserId(), command.getEmail(), command.getProfile());
        User savedUser = userRepository.save(user);
        
        // Publish domain event
        UserCreatedEvent event = new UserCreatedEvent(
            savedUser.getUserId(),
            savedUser.getEmail(),
            savedUser.getProfile().getFirstName(),
            savedUser.getProfile().getLastName(),
            1
        );
        eventPublisher.publish(event);
        
        logger.info("User created successfully with ID: {}", savedUser.getUserId());
        return savedUser.getUserId();
    }
    
    @Transactional
    public void handle(@Valid UpdateUserProfileCommand command) {
        logger.info("Handling UpdateUserProfileCommand for userId: {}", command.getUserId());
        
        // Find user
        User user = userRepository.findByUserId(command.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + command.getUserId()));
        
        // Update profile
        user.updateProfile(command.getProfile());
        User savedUser = userRepository.save(user);
        
        // Publish domain event
        UserUpdatedEvent event = new UserUpdatedEvent(
            savedUser.getUserId(),
            savedUser.getEmail(),
            savedUser.getProfile().getFirstName(),
            savedUser.getProfile().getLastName(),
            1
        );
        eventPublisher.publish(event);
        
        logger.info("User profile updated successfully for userId: {}", savedUser.getUserId());
    }
    
    @Transactional
    public void handle(@Valid DeactivateUserCommand command) {
        logger.info("Handling DeactivateUserCommand for userId: {}", command.getUserId());
        
        // Find user
        User user = userRepository.findByUserId(command.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + command.getUserId()));
        
        // Deactivate user
        user.deactivate();
        User savedUser = userRepository.save(user);
        
        // Publish domain event
        UserDeactivatedEvent event = new UserDeactivatedEvent(
            savedUser.getUserId(),
            savedUser.getEmail(),
            command.getReason(),
            1
        );
        eventPublisher.publish(event);
        
        logger.info("User deactivated successfully for userId: {}", savedUser.getUserId());
    }
}