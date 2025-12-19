package com.restaurant.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {
    
    @Test
    void createOrderItem_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        String itemId = "item-123";
        String name = "Pizza Margherita";
        BigDecimal price = new BigDecimal("15.99");
        int quantity = 2;
        
        // Act
        OrderItem orderItem = new OrderItem(itemId, name, price, quantity);
        
        // Assert
        assertEquals(itemId, orderItem.getItemId());
        assertEquals(name, orderItem.getName());
        assertEquals(price, orderItem.getPrice());
        assertEquals(quantity, orderItem.getQuantity());
        assertEquals(new BigDecimal("31.98"), orderItem.getTotalPrice());
    }
    
    @Test
    void createOrderItem_WithNullItemId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem(null, "Pizza", new BigDecimal("15.99"), 1));
    }
    
    @Test
    void createOrderItem_WithEmptyItemId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("", "Pizza", new BigDecimal("15.99"), 1));
        
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("   ", "Pizza", new BigDecimal("15.99"), 1));
    }
    
    @Test
    void createOrderItem_WithNullName_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", null, new BigDecimal("15.99"), 1));
    }
    
    @Test
    void createOrderItem_WithEmptyName_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "", new BigDecimal("15.99"), 1));
        
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "   ", new BigDecimal("15.99"), 1));
    }
    
    @Test
    void createOrderItem_WithNullPrice_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "Pizza", null, 1));
    }
    
    @Test
    void createOrderItem_WithZeroPrice_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "Pizza", BigDecimal.ZERO, 1));
    }
    
    @Test
    void createOrderItem_WithNegativePrice_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "Pizza", new BigDecimal("-5.00"), 1));
    }
    
    @Test
    void createOrderItem_WithZeroQuantity_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 0));
    }
    
    @Test
    void createOrderItem_WithNegativeQuantity_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), -1));
    }
    
    @Test
    void getTotalPrice_ShouldCalculateCorrectly() {
        // Test with quantity 1
        OrderItem item1 = new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 1);
        assertEquals(new BigDecimal("15.99"), item1.getTotalPrice());
        
        // Test with quantity > 1
        OrderItem item2 = new OrderItem("item-2", "Soda", new BigDecimal("2.50"), 3);
        assertEquals(new BigDecimal("7.50"), item2.getTotalPrice());
        
        // Test with decimal quantity calculation
        OrderItem item3 = new OrderItem("item-3", "Burger", new BigDecimal("12.75"), 2);
        assertEquals(new BigDecimal("25.50"), item3.getTotalPrice());
    }
    
    @Test
    void equals_ShouldWorkCorrectly() {
        OrderItem item1 = new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 2);
        OrderItem item2 = new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 2);
        OrderItem item3 = new OrderItem("item-2", "Pizza", new BigDecimal("15.99"), 2);
        OrderItem item4 = new OrderItem("item-1", "Burger", new BigDecimal("15.99"), 2);
        OrderItem item5 = new OrderItem("item-1", "Pizza", new BigDecimal("12.99"), 2);
        OrderItem item6 = new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 1);
        
        // Test equality
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        
        // Test inequality
        assertNotEquals(item1, item3); // Different itemId
        assertNotEquals(item1, item4); // Different name
        assertNotEquals(item1, item5); // Different price
        assertNotEquals(item1, item6); // Different quantity
        
        // Test with null and different class
        assertNotEquals(item1, null);
        assertNotEquals(item1, "not an OrderItem");
        
        // Test reflexivity
        assertEquals(item1, item1);
    }
    
    @Test
    void toString_ShouldContainAllFields() {
        OrderItem item = new OrderItem("item-123", "Pizza Margherita", new BigDecimal("15.99"), 2);
        String toString = item.toString();
        
        assertTrue(toString.contains("item-123"));
        assertTrue(toString.contains("Pizza Margherita"));
        assertTrue(toString.contains("15.99"));
        assertTrue(toString.contains("2"));
        assertTrue(toString.contains("31.98")); // Total price
    }
}