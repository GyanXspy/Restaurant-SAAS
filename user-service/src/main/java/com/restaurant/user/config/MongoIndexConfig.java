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
// @Configuration - Temporarily disabled to avoid index conflicts
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(User.class);

        // Single field indexes with explicit names
        indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique().named("idx_user_id_unique"));
        indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique().named("idx_email_unique"));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_status"));
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_created_at"));
        indexOps.ensureIndex(new Index().on("updatedAt", Sort.Direction.DESC).named("idx_updated_at"));

        // Compound indexes for common query patterns
        indexOps.ensureIndex(new Index()
            .on("status", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC)
            .named("idx_status_created_at"));

        // Text indexes for profile search
        indexOps.ensureIndex(new Index()
            .on("profile.firstName", Sort.Direction.ASC)
            .named("idx_first_name"));
        indexOps.ensureIndex(new Index()
            .on("profile.lastName", Sort.Direction.ASC)
            .named("idx_last_name"));

        // Compound index for full name search
        indexOps.ensureIndex(new Index()
            .on("profile.firstName", Sort.Direction.ASC)
            .on("profile.lastName", Sort.Direction.ASC)
            .named("idx_full_name"));

        // Sparse index for phone numbers (optional field)
        indexOps.ensureIndex(new Index()
            .on("profile.phone", Sort.Direction.ASC)
            .sparse()
            .named("idx_phone_sparse"));

        // Index for user addresses (for location-based queries)
        indexOps.ensureIndex(new Index()
            .on("profile.addresses.city", Sort.Direction.ASC)
            .named("idx_address_city"));
        indexOps.ensureIndex(new Index()
            .on("profile.addresses.zipCode", Sort.Direction.ASC)
            .named("idx_address_zip"));
    }
}