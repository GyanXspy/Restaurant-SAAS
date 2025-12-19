package com.restaurant.restaurantservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.restaurant.restaurantservice.domain.model.Restaurant;

import jakarta.annotation.PostConstruct;

/**
 * MongoDB index configuration for Restaurant Service.
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
        IndexOperations indexOps = mongoTemplate.indexOps(Restaurant.class);

        // Single field indexes
        indexOps.ensureIndex(new Index().on("restaurantId.value", Sort.Direction.ASC).unique());
        indexOps.ensureIndex(new Index().on("name", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("cuisine", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("isActive", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("updatedAt", Sort.Direction.DESC));

        // Address-based indexes for location queries
        indexOps.ensureIndex(new Index().on("address.city", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("address.zipCode", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("address.street", Sort.Direction.ASC));

        // Compound indexes for common query patterns
        indexOps.ensureIndex(new Index()
            .on("isActive", Sort.Direction.ASC)
            .on("cuisine", Sort.Direction.ASC));

        indexOps.ensureIndex(new Index()
            .on("address.city", Sort.Direction.ASC)
            .on("isActive", Sort.Direction.ASC));

        indexOps.ensureIndex(new Index()
            .on("cuisine", Sort.Direction.ASC)
            .on("address.city", Sort.Direction.ASC)
            .on("isActive", Sort.Direction.ASC));

        // Menu item indexes for menu search
        indexOps.ensureIndex(new Index().on("menu.itemId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("menu.category", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("menu.available", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("menu.price", Sort.Direction.ASC));

        // Compound index for menu queries
        indexOps.ensureIndex(new Index()
            .on("restaurantId.value", Sort.Direction.ASC)
            .on("menu.available", Sort.Direction.ASC)
            .on("menu.category", Sort.Direction.ASC));

        // Text index for restaurant and menu search
        indexOps.ensureIndex(new Index()
            .on("name", Sort.Direction.ASC)
            .on("menu.name", Sort.Direction.ASC));

        // Version index for optimistic locking
        indexOps.ensureIndex(new Index().on("version", Sort.Direction.ASC));

        // Geospatial index for location-based queries (if coordinates are added later)
        // indexOps.ensureIndex(new Index().on("address.coordinates", Sort.Direction.ASC).geo2dsphere());
    }
}