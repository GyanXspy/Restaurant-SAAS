package com.restaurant.user.query;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserStatus;
import com.restaurant.user.repository.UserRepository;

import jakarta.validation.Valid;

@Component
@Transactional(readOnly = true)
public class UserQueryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(UserQueryHandler.class);
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public Optional<User> handle(@Valid GetUserQuery query) {
        logger.debug("Handling GetUserQuery for userId: {}", query.getUserId());
        
        Optional<User> user = userRepository.findByUserId(query.getUserId());
        
        if (user.isPresent()) {
            logger.debug("User found for userId: {}", query.getUserId());
        } else {
            logger.debug("User not found for userId: {}", query.getUserId());
        }
        
        return user;
    }
    
    public Optional<User> handle(@Valid GetUserByEmailQuery query) {
        logger.debug("Handling GetUserByEmailQuery for email: {}", query.getEmail());
        
        Optional<User> user = userRepository.findByEmail(query.getEmail());
        
        if (user.isPresent()) {
            logger.debug("User found for email: {}", query.getEmail());
        } else {
            logger.debug("User not found for email: {}", query.getEmail());
        }
        
        return user;
    }
    
    public List<User> getAllActiveUsers() {
        logger.debug("Handling getAllActiveUsers query");
        
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        
        logger.debug("Found {} active users", activeUsers.size());
        return activeUsers;
    }
    
    public List<User> getAllUsers() {
        logger.debug("Handling getAllUsers query");
        
        List<User> allUsers = userRepository.findAll();
        
        logger.debug("Found {} total users", allUsers.size());
        return allUsers;
    }
    
    public boolean userExists(String userId) {
        logger.debug("Checking if user exists for userId: {}", userId);
        
        boolean exists = userRepository.existsByUserId(userId);
        
        logger.debug("User exists for userId {}: {}", userId, exists);
        return exists;
    }
    
    public boolean emailExists(String email) {
        logger.debug("Checking if email exists: {}", email);
        
        boolean exists = userRepository.existsByEmail(email);
        
        logger.debug("Email exists {}: {}", email, exists);
        return exists;
    }
}