package com.restaurant.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UserProfileTest {
    
    @Test
    void shouldCreateValidUserProfile() {
        // Given
        String firstName = "John";
        String lastName = "Doe";
        String phone = "+1234567890";
        
        // When
        UserProfile profile = new UserProfile(firstName, lastName, phone);
        
        // Then
        assertEquals(firstName, profile.getFirstName());
        assertEquals(lastName, profile.getLastName());
        assertEquals(phone, profile.getPhone());
        assertTrue(profile.getAddresses().isEmpty());
        assertEquals("John Doe", profile.getFullName());
    }
    
    @Test
    void shouldThrowExceptionForNullFirstName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new UserProfile(null, "Doe", "+1234567890");
        });
    }
    
    @Test
    void shouldThrowExceptionForEmptyFirstName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new UserProfile("", "Doe", "+1234567890");
        });
    }
    
    @Test
    void shouldThrowExceptionForNullLastName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new UserProfile("John", null, "+1234567890");
        });
    }
    
    @Test
    void shouldThrowExceptionForEmptyLastName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new UserProfile("John", "", "+1234567890");
        });
    }
    
    @Test
    void shouldThrowExceptionForInvalidPhone() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new UserProfile("John", "Doe", "invalid-phone");
        });
    }
    
    @Test
    void shouldAllowNullPhone() {
        // When
        UserProfile profile = new UserProfile("John", "Doe", null);
        
        // Then
        assertNull(profile.getPhone());
    }
    
    @Test
    void shouldAddAddress() {
        // Given
        UserProfile profile = new UserProfile("John", "Doe", "+1234567890");
        Address address = new Address(AddressType.HOME, "123 Main St", "City", "State", "12345", "Country");
        
        // When
        profile.addAddress(address);
        
        // Then
        assertEquals(1, profile.getAddresses().size());
        assertTrue(profile.getAddresses().contains(address));
    }
    
    @Test
    void shouldThrowExceptionWhenAddingNullAddress() {
        // Given
        UserProfile profile = new UserProfile("John", "Doe", "+1234567890");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            profile.addAddress(null);
        });
    }
    
    @Test
    void shouldRemoveAddress() {
        // Given
        UserProfile profile = new UserProfile("John", "Doe", "+1234567890");
        Address address = new Address(AddressType.HOME, "123 Main St", "City", "State", "12345", "Country");
        profile.addAddress(address);
        
        // When
        profile.removeAddress(address);
        
        // Then
        assertTrue(profile.getAddresses().isEmpty());
    }
    
    @Test
    void shouldReturnImmutableAddressList() {
        // Given
        UserProfile profile = new UserProfile("John", "Doe", "+1234567890");
        Address address = new Address(AddressType.HOME, "123 Main St", "City", "State", "12345", "Country");
        profile.addAddress(address);
        
        // When
        var addresses = profile.getAddresses();
        
        // Then
        assertThrows(UnsupportedOperationException.class, () -> {
            addresses.add(new Address(AddressType.WORK, "456 Work St", "City", "State", "67890", "Country"));
        });
    }
}