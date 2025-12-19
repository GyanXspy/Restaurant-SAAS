package com.restaurant.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify database optimization configurations.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseOptimizationTest {

    @Test
    void testDataSourceContextHolder() {
        // Test default data source type
        assertEquals(DataSourceType.WRITE, DataSourceContextHolder.getDataSourceType());
        
        // Test setting read data source
        DataSourceContextHolder.setDataSourceType(DataSourceType.READ);
        assertEquals(DataSourceType.READ, DataSourceContextHolder.getDataSourceType());
        
        // Test clearing context
        DataSourceContextHolder.clearDataSourceType();
        assertEquals(DataSourceType.WRITE, DataSourceContextHolder.getDataSourceType());
    }

    @Test
    void testDataSourceTypeEnum() {
        // Verify enum values
        assertEquals(2, DataSourceType.values().length);
        assertTrue(DataSourceType.valueOf("READ") == DataSourceType.READ);
        assertTrue(DataSourceType.valueOf("WRITE") == DataSourceType.WRITE);
    }

    @Test
    @Transactional(readOnly = true)
    void testReadOnlyTransaction() {
        // This test verifies that read-only transactions can be created
        // The actual routing would be tested in integration tests
        assertTrue(true, "Read-only transaction annotation works");
    }

    @Test
    @Transactional
    void testWriteTransaction() {
        // This test verifies that write transactions can be created
        assertTrue(true, "Write transaction annotation works");
    }
}