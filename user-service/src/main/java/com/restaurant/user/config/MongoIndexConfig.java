package com.restaurant.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.restaurant.user.domain.User;

import jakarta.annotation.PostConstruct;

/**
 * MongoDB index configuration for User Service.
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
        IndexOperations indexOps = mongoTemplate.indexOps(User.class);

        // Single field indexes
        indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique());
        indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("updatedAt", Sort.Direction.DESC));

        // Compound indexes for common query patterns
        indexOps.ensureIndex(new Index()
            .on("status", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC));

        // Text indexes for profile search
        indexOps.ensureIndex(new Index()
            .on("profile.firstName", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index()
            .on("profile.lastName", Sort.Direction.ASC));

        // Compound index for full name search
        indexOps.ensureIndex(new Index()
            .on("profile.firstName", Sort.Direction.ASC)
            .on("profile.lastName", Sort.Direction.ASC));

        // Index for email domain queries
        indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC));

        // Sparse index for phone numbers (optional field)
        indexOps.ensureIndex(new Index()
            .on("profile.phone", Sort.Direction.ASC)
            .sparse());

        // Index for user addresses (for location-based queries)
        indexOps.ensureIndex(new Index()
            .on("profile.addresses.city", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index()
            .on("profile.addresses.zipCode", Sort.Direction.ASC));
    }
}