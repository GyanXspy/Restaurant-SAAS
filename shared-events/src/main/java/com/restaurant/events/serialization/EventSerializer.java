package com.restaurant.events.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.events.DomainEvent;

/**
 * Utility class for serializing and deserializing domain events.
 * Provides consistent JSON serialization across the system.
 */
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    
    public EventSerializer() {
        this.objectMapper = createObjectMapper();
    }
    
    public EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Serializes a domain event to JSON string.
     * 
     * @param event the domain event to serialize
     * @return JSON string representation of the event
     * @throws EventSerializationException if serialization fails
     */
    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event: " + event.getEventId(), e);
        }
    }
    
    /**
     * Deserializes a JSON string to a domain event.
     * 
     * @param json the JSON string to deserialize
     * @return the deserialized domain event
     * @throws EventSerializationException if deserialization fails
     */
    public DomainEvent deserialize(String json) {
        try {
            return objectMapper.readValue(json, DomainEvent.class);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize event from JSON: " + json, e);
        }
    }
    
    /**
     * Deserializes a JSON string to a specific event type.
     * 
     * @param json the JSON string to deserialize
     * @param eventType the target event class
     * @param <T> the event type
     * @return the deserialized domain event
     * @throws EventSerializationException if deserialization fails
     */
    public <T extends DomainEvent> T deserialize(String json, Class<T> eventType) {
        try {
            return objectMapper.readValue(json, eventType);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize event from JSON to type " + eventType.getSimpleName() + ": " + json, e);
        }
    }
    
    /**
     * Serializes a domain event to byte array for efficient storage/transmission.
     * 
     * @param event the domain event to serialize
     * @return byte array representation of the event
     * @throws EventSerializationException if serialization fails
     */
    public byte[] serializeToBytes(DomainEvent event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event to bytes: " + event.getEventId(), e);
        }
    }
    
    /**
     * Deserializes a byte array to a domain event.
     * 
     * @param bytes the byte array to deserialize
     * @return the deserialized domain event
     * @throws EventSerializationException if deserialization fails
     */
    public DomainEvent deserializeFromBytes(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, DomainEvent.class);
        } catch (IOException e) {
            throw new EventSerializationException("Failed to deserialize event from bytes", e);
        }
    }
    
    /**
     * Creates a configured ObjectMapper for event serialization.
     * 
     * @return configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        return mapper;
    }
    
    /**
     * Gets the underlying ObjectMapper for advanced usage.
     * 
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}