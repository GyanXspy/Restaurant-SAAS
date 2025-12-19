package com.restaurant.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/test_user_service",
    "spring.kafka.bootstrap-servers=localhost:9092"
})
class UserServiceApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
        // with all configurations in place
    }
}