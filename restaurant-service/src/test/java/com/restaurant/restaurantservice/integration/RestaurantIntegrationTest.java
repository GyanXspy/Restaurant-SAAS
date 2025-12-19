package com.restaurant.restaurantservice.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.restaurantservice.application.dto.AddressDto;
import com.restaurant.restaurantservice.application.dto.CreateRestaurantRequest;
import com.restaurant.restaurantservice.application.dto.MenuItemDto;
import com.restaurant.restaurantservice.application.dto.RestaurantDto;
import com.restaurant.restaurantservice.infrastructure.repository.MongoRestaurantRepository;

/**
 * Integration tests for Restaurant Service operations.
 * Uses Testcontainers for MongoDB integration testing.
 */
@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
class RestaurantIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoRestaurantRepository mongoRestaurantRepository;

    @BeforeEach
    void setUp() {
        mongoRestaurantRepository.deleteAll();
    }

    @Test
    void shouldCreateRestaurant() throws Exception {
        // Given
        CreateRestaurantRequest request = createRestaurantRequest();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Restaurant"))
                .andExpect(jsonPath("$.cuisine").value("Italian"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        RestaurantDto createdRestaurant = objectMapper.readValue(responseBody, RestaurantDto.class);
        
        assertNotNull(createdRestaurant.getRestaurantId());
        assertNotNull(createdRestaurant.getId());
    }

    @Test
    void shouldGetRestaurantById() throws Exception {
        // Given
        CreateRestaurantRequest request = createRestaurantRequest();
        
        MvcResult createResult = mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        RestaurantDto createdRestaurant = objectMapper.readValue(createResponseBody, RestaurantDto.class);

        // When & Then
        mockMvc.perform(get("/api/restaurants/{restaurantId}", createdRestaurant.getRestaurantId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(createdRestaurant.getRestaurantId()))
                .andExpect(jsonPath("$.name").value("Test Restaurant"))
                .andExpect(jsonPath("$.cuisine").value("Italian"));
    }

    @Test
    void shouldGetActiveRestaurants() throws Exception {
        // Given
        CreateRestaurantRequest request1 = createRestaurantRequest();
        CreateRestaurantRequest request2 = new CreateRestaurantRequest(
            "Another Restaurant", 
            "Chinese", 
            new AddressDto("456 Oak St", "New York", "10001", "USA")
        );

        mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].isActive").value(true));
    }

    @Test
    void shouldAddMenuItemToRestaurant() throws Exception {
        // Given
        CreateRestaurantRequest restaurantRequest = createRestaurantRequest();
        
        MvcResult createResult = mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restaurantRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        RestaurantDto createdRestaurant = objectMapper.readValue(createResponseBody, RestaurantDto.class);

        MenuItemDto menuItem = new MenuItemDto(
            null, 
            "Margherita Pizza", 
            "Classic pizza with tomato and mozzarella", 
            new BigDecimal("12.99"), 
            "Pizza", 
            true
        );

        // When & Then
        mockMvc.perform(post("/api/restaurants/{restaurantId}/menu", createdRestaurant.getRestaurantId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menuItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Margherita Pizza"))
                .andExpect(jsonPath("$.price").value(12.99))
                .andExpect(jsonPath("$.category").value("Pizza"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldUpdateMenuItemAvailability() throws Exception {
        // Given
        CreateRestaurantRequest restaurantRequest = createRestaurantRequest();
        
        MvcResult createResult = mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restaurantRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        RestaurantDto createdRestaurant = objectMapper.readValue(createResponseBody, RestaurantDto.class);

        MenuItemDto menuItem = new MenuItemDto(
            null, 
            "Margherita Pizza", 
            "Classic pizza", 
            new BigDecimal("12.99"), 
            "Pizza", 
            true
        );

        MvcResult addItemResult = mockMvc.perform(post("/api/restaurants/{restaurantId}/menu", createdRestaurant.getRestaurantId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menuItem)))
                .andExpect(status().isCreated())
                .andReturn();

        String addItemResponseBody = addItemResult.getResponse().getContentAsString();
        MenuItemDto addedItem = objectMapper.readValue(addItemResponseBody, MenuItemDto.class);

        // When & Then
        mockMvc.perform(put("/api/restaurants/{restaurantId}/menu/{itemId}/availability", 
                createdRestaurant.getRestaurantId(), addedItem.getItemId())
                .param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void shouldActivateAndDeactivateRestaurant() throws Exception {
        // Given
        CreateRestaurantRequest request = createRestaurantRequest();
        
        MvcResult createResult = mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        RestaurantDto createdRestaurant = objectMapper.readValue(createResponseBody, RestaurantDto.class);

        // When & Then - Deactivate
        mockMvc.perform(put("/api/restaurants/{restaurantId}/deactivate", createdRestaurant.getRestaurantId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // When & Then - Activate
        mockMvc.perform(put("/api/restaurants/{restaurantId}/activate", createdRestaurant.getRestaurantId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    private CreateRestaurantRequest createRestaurantRequest() {
        AddressDto address = new AddressDto("123 Main St", "New York", "10001", "USA");
        return new CreateRestaurantRequest("Test Restaurant", "Italian", address);
    }
}