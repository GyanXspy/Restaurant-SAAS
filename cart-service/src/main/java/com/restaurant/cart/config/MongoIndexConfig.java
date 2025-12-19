package com.restaurant.cart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.restaurant.cart.domain.Cart;

import jakarta.annotation.PostConstruct;

/**
 * MongoDB index configuration for Cart Service.
 * Creates optimized indexes for all query patterns.
 */
@Configuration
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(Cart.class);

        // Single field indexes
        indexOps.ensureIndex(new Index().on("cartId", Sort.Direction.ASC).unique());
        indexOps.ensureIndex(new Index().on("customerId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("restaurantId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("updatedAt", Sort.Direction.DESC));

        // TTL index for cart expiration
        indexOps.ensureIndex(new Index()
            .on("expiresAt", Sort.Direction.ASC)
            .expire(0)); // Expire documents when expiresAt is reached

        // Compound indexes for common query patterns
        indexOps.ensureIndex(new Index()
            .on("customerId", Sort.Direction.ASC)
            .on("restaurantId", Sort.Direction.ASC));

        indexOps.ensureIndex(new Index()
            .on("customerId", Sort.Direction.ASC)
            .on("updatedAt", Sort.Direction.DESC));

        // Index for cart item queries
        indexOps.ensureIndex(new Index().on("items.itemId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("items.quantity", Sort.Direction.ASC));

        // Compound index for cart validation queries
        indexOps.ensureIndex(new Index()
            .on("customerId", Sort.Direction.ASC)
            .on("restaurantId", Sort.Direction.ASC)
            .on("updatedAt", Sort.Direction.DESC));

        // Index for total amount queries (analytics)
        indexOps.ensureIndex(new Index().on("totalAmount", Sort.Direction.ASC));

        // Sparse index for session-based carts
        indexOps.ensureIndex(new Index()
            .on("sessionId", Sort.Direction.ASC)
            .sparse());
    }
}