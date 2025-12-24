package com.restaurant.user.service;

import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserProfile;
import com.restaurant.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public String publishUserCreationEvent(String userId, String email, UserProfile profile) {
        log.info("Publishing user creation event for ID: {}", userId);
        
        // Create event JSON
        String eventJson = String.format(
            "{\"userId\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\"}",
            userId, email, 
            profile.getFirstName(), 
            profile.getLastName(), 
            profile.getPhone()
        );
        
        // Publish to Kafka
        kafkaTemplate.send("user-creation-events", userId, eventJson);
        
        log.info("User creation event published to Kafka for ID: {}", userId);
        return userId;
    }
    
    public User saveUserFromEvent(String userId, String email, UserProfile profile) {
        log.info("Saving user from Kafka event with ID: {}", userId);
        
        if (userRepository.existsByUserId(userId)) {
            log.warn("User with ID {} already exists, skipping", userId);
            return userRepository.findByUserId(userId).orElseThrow();
        }
        
        if (userRepository.existsByEmail(email)) {
            log.warn("User with email {} already exists, skipping", email);
            return userRepository.findByEmail(email).orElseThrow();
        }
        
        User user = new User(userId, email, profile);
        User savedUser = userRepository.save(user);
        
        log.info("User saved to database from Kafka event with ID: {}", savedUser.getUserId());
        return savedUser;
    }
    
    public Optional<User> getUserById(String userId) {
        return userRepository.findByUserId(userId);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getActiveUsers() {
        return userRepository.findActiveUsers();
    }
    
    public User updateUserProfile(String userId, UserProfile newProfile) {
        log.info("Updating profile for user ID: {}", userId);
        
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.updateProfile(newProfile);
        User savedUser = userRepository.save(user);
        
        log.info("User profile updated successfully for ID: {}", userId);
        return savedUser;
    }
    
    public User deactivateUser(String userId) {
        log.info("Deactivating user ID: {}", userId);
        
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.deactivate();
        User savedUser = userRepository.save(user);
        
        log.info("User deactivated successfully for ID: {}", userId);
        return savedUser;
    }
}
