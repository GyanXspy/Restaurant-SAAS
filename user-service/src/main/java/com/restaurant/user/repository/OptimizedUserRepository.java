package com.restaurant.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserStatus;

/**
 * Optimized User Repository with efficient query patterns.
 * Uses proper indexing and aggregation pipelines for better performance.
 */
@Repository
public interface OptimizedUserRepository extends MongoRepository<User, String> {
    
    /**
     * Find user by userId with index hint
     */
    @Query(value = "{'userId': ?0}", fields = "{'password': 0}")
    Optional<User> findByUserIdOptimized(String userId);
    
    /**
     * Find user by email with index hint
     */
    @Query(value = "{'email': ?0}", fields = "{'password': 0}")
    Optional<User> findByEmailOptimized(String email);
    
    /**
     * Find active users with pagination and sorting
     */
    @Query(value = "{'status': 'ACTIVE'}")
    Page<User> findActiveUsersWithPagination(Pageable pageable);
    
    /**
     * Find users by status with efficient projection
     */
    @Query(value = "{'status': ?0}", fields = "{'userId': 1, 'email': 1, 'profile.firstName': 1, 'profile.lastName': 1, 'status': 1, 'createdAt': 1}")
    List<User> findByStatusProjected(UserStatus status);
    
    /**
     * Find users created in date range with index optimization
     */
    @Query("{'createdAt': {'$gte': ?0, '$lte': ?1}}")
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find users by city with compound index usage
     */
    @Query("{'profile.addresses.city': ?0, 'status': 'ACTIVE'}")
    List<User> findActiveUsersByCity(String city);
    
    /**
     * Find users by name pattern with text search optimization
     */
    @Query("{'$or': [{'profile.firstName': {'$regex': ?0, '$options': 'i'}}, {'profile.lastName': {'$regex': ?0, '$options': 'i'}}]}")
    List<User> findByNamePattern(String namePattern);
    
    /**
     * Count users by status using efficient counting
     */
    @Query(value = "{'status': ?0}", count = true)
    long countByStatus(UserStatus status);
    
    /**
     * Find users with aggregation for analytics
     */
    @Aggregation(pipeline = {
        "{'$match': {'status': 'ACTIVE'}}",
        "{'$group': {'_id': '$profile.addresses.city', 'count': {'$sum': 1}}}",
        "{'$sort': {'count': -1}}",
        "{'$limit': 10}"
    })
    List<UserCityCount> getUserCountByCity();
    
    /**
     * Find recent users with efficient sorting
     */
    @Query(value = "{}", sort = "{'createdAt': -1}")
    List<User> findRecentUsers(Pageable pageable);
    
    /**
     * Bulk update user status (for administrative operations)
     */
    @Query("{'userId': {'$in': ?0}}")
    List<User> findByUserIdIn(List<String> userIds);
    
    /**
     * Find users by email domain for analytics
     */
    @Query("{'email': {'$regex': ?0}}")
    List<User> findByEmailDomainPattern(String domainPattern);
    
    /**
     * Check existence efficiently
     */
    @Query(value = "{'userId': ?0}", exists = true)
    boolean existsByUserIdOptimized(String userId);
    
    @Query(value = "{'email': ?0}", exists = true)
    boolean existsByEmailOptimized(String email);
    
    /**
     * Inner class for aggregation results
     */
    interface UserCityCount {
        String getId();
        Long getCount();
    }
}