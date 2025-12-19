package com.restaurant.events.versioning;

import com.restaurant.events.DomainEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing event schema versions and evolution.
 * Supports backward compatibility and schema migration.
 */
public class EventSchemaRegistry {
    
    private final Map<String, Map<Integer, Class<? extends DomainEvent>>> eventVersions;
    private final Map<String, Integer> latestVersions;
    
    public EventSchemaRegistry() {
        this.eventVersions = new ConcurrentHashMap<>();
        this.latestVersions = new ConcurrentHashMap<>();
    }
    
    /**
     * Registers an event class with its version information.
     * 
     * @param eventClass the event class to register
     */
    public void registerEventClass(Class<? extends DomainEvent> eventClass) {
        EventVersion versionAnnotation = eventClass.getAnnotation(EventVersion.class);
        if (versionAnnotation == null) {
            throw new IllegalArgumentException("Event class must be annotated with @EventVersion: " + eventClass.getName());
        }
        
        String eventType = getEventTypeName(eventClass);
        int version = versionAnnotation.value();
        
        eventVersions.computeIfAbsent(eventType, k -> new HashMap<>()).put(version, eventClass);
        latestVersions.merge(eventType, version, Integer::max);
    }
    
    /**
     * Gets the event class for a specific event type and version.
     * 
     * @param eventType the event type name
     * @param version the version number
     * @return the event class, or null if not found
     */
    public Class<? extends DomainEvent> getEventClass(String eventType, int version) {
        Map<Integer, Class<? extends DomainEvent>> versions = eventVersions.get(eventType);
        return versions != null ? versions.get(version) : null;
    }
    
    /**
     * Gets the latest version of an event type.
     * 
     * @param eventType the event type name
     * @return the latest version number, or -1 if not found
     */
    public int getLatestVersion(String eventType) {
        return latestVersions.getOrDefault(eventType, -1);
    }
    
    /**
     * Gets the latest event class for an event type.
     * 
     * @param eventType the event type name
     * @return the latest event class, or null if not found
     */
    public Class<? extends DomainEvent> getLatestEventClass(String eventType) {
        int latestVersion = getLatestVersion(eventType);
        return latestVersion >= 0 ? getEventClass(eventType, latestVersion) : null;
    }
    
    /**
     * Checks if a specific version of an event type is supported.
     * 
     * @param eventType the event type name
     * @param version the version number
     * @return true if the version is supported
     */
    public boolean isVersionSupported(String eventType, int version) {
        return getEventClass(eventType, version) != null;
    }
    
    /**
     * Gets all supported versions for an event type.
     * 
     * @param eventType the event type name
     * @return array of supported version numbers
     */
    public int[] getSupportedVersions(String eventType) {
        Map<Integer, Class<? extends DomainEvent>> versions = eventVersions.get(eventType);
        if (versions == null) {
            return new int[0];
        }
        return versions.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
    }
    
    /**
     * Extracts event type name from class name.
     */
    private String getEventTypeName(Class<? extends DomainEvent> eventClass) {
        String className = eventClass.getSimpleName();
        return className.endsWith("Event") ? className.substring(0, className.length() - 5) : className;
    }
}