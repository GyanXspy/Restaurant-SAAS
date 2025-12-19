package com.restaurant.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserStatus;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    /**
     * Find user by userId (business identifier)
     */
    Optional<User> findByUserId(String userId);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user exists by userId
     */
    boolean existsByUserId(String userId);
    
    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by status
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find users by first name (case insensitive)
     */
    @Query("{'profile.firstName': {$regex: ?0, $options: 'i'}}")
    List<User> findByFirstNameIgnoreCase(String firstName);
    
    /**
     * Find users by last name (case insensitive)
     */
    @Query("{'profile.lastName': {$regex: ?0, $options: 'i'}}")
    List<User> findByLastNameIgnoreCase(String lastName);
    
    /**
     * Find active users
     */
    @Query("{'status': 'ACTIVE'}")
    List<User> findActiveUsers();
    
    /**
     * Find users by email domain
     */
    @Query("{'email': {$regex: ?0}}")
    List<User> findByEmailDomain(String domain);
}