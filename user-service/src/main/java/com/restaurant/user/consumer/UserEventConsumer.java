package com.restaurant.user.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.user.domain.UserProfile;
import com.restaurant.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {
    
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "user-creation-events", 
        groupId = "user-service-group",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeUserCreationEvent(String message) {
        log.info("Received user creation event from Kafka: {}", message);
        
        try {
            // Validate JSON format
            if (message == null || message.trim().isEmpty() || !message.trim().startsWith("{")) {
                log.warn("Invalid message format, skipping: {}", message);
                return;
            }
            
            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // Validate required fields
            if (!jsonNode.has("userId") || !jsonNode.has("email") || 
                !jsonNode.has("firstName") || !jsonNode.has("lastName")) {
                log.warn("Missing required fields in message, skipping: {}", message);
                return;
            }
            
            String userId = jsonNode.get("userId").asText();
            String email = jsonNode.get("email").asText();
            String firstName = jsonNode.get("firstName").asText();
            String lastName = jsonNode.get("lastName").asText();
            String phone = jsonNode.has("phone") ? jsonNode.get("phone").asText() : null;
            
            // Create UserProfile
            UserProfile profile = new UserProfile(firstName, lastName, phone);
            
            // Save to database
            userService.saveUserFromEvent(userId, email, profile);
            
            log.info("Successfully processed user creation event for userId: {}", userId);
            
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("Invalid JSON format in message: {}", message, e);
        } catch (Exception e) {
            log.error("Error processing user creation event: {}", e.getMessage(), e);
        }
    }
}
