package com.restaurant.user.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.user.domain.User;
import com.restaurant.user.query.GetUserByEmailQuery;
import com.restaurant.user.query.GetUserQuery;
import com.restaurant.user.query.UserQueryHandler;

@RestController
@RequestMapping("/api/users/queries")
public class UserQueryController {

    private static final Logger logger = LoggerFactory.getLogger(UserQueryController.class);

    private final UserQueryHandler queryHandler;

    @Autowired
    public UserQueryController(UserQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        logger.info("Received request to get user by ID: {}", userId);

        try {
            GetUserQuery query = new GetUserQuery(userId);
            Optional<User> user = queryHandler.handle(query);

            if (user.isPresent()) {
                logger.info("User found for ID: {}", userId);
                return ResponseEntity.ok(user.get());
            } else {
                logger.info("User not found for ID: {}", userId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving user by ID: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        logger.info("Received request to get user by email: {}", email);

        try {
            GetUserByEmailQuery query = new GetUserByEmailQuery(email);
            Optional<User> user = queryHandler.handle(query);

            if (user.isPresent()) {
                logger.info("User found for email: {}", email);
                return ResponseEntity.ok(user.get());
            } else {
                logger.info("User not found for email: {}", email);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving user by email: {}", email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        logger.info("Received request to get all active users");

        try {
            List<User> activeUsers = queryHandler.getAllActiveUsers();

            logger.info("Retrieved {} active users", activeUsers.size());
            return ResponseEntity.ok(activeUsers);

        } catch (Exception e) {
            logger.error("Error retrieving active users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("Received request to get all users");

        try {
            List<User> allUsers = queryHandler.getAllUsers();

            logger.info("Retrieved {} total users", allUsers.size());
            return ResponseEntity.ok(allUsers);

        } catch (Exception e) {
            logger.error("Error retrieving all users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<ExistsResponse> checkUserExists(@PathVariable String userId) {
        logger.info("Received request to check if user exists: {}", userId);

        try {
            boolean exists = queryHandler.userExists(userId);

            logger.info("User exists check for ID {}: {}", userId, exists);
            return ResponseEntity.ok(new ExistsResponse(exists));

        } catch (Exception e) {
            logger.error("Error checking if user exists: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/email/{email}/exists")
    public ResponseEntity<ExistsResponse> checkEmailExists(@PathVariable String email) {
        logger.info("Received request to check if email exists: {}", email);

        try {
            boolean exists = queryHandler.emailExists(email);

            logger.info("Email exists check for {}: {}", email, exists);
            return ResponseEntity.ok(new ExistsResponse(exists));

        } catch (Exception e) {
            logger.error("Error checking if email exists: {}", email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Response DTO
    public static class ExistsResponse {

        private boolean exists;

        public ExistsResponse(boolean exists) {
            this.exists = exists;
        }

        public boolean isExists() {
            return exists;
        }
    }
}
