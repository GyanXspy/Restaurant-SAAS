package com.restaurant.integration;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite that runs all integration tests in a specific order.
 * This ensures proper test execution and resource management.
 */
@Suite
@SelectClasses({
    OrderFlowIntegrationTest.class,
    SagaCompensationIntegrationTest.class,
    EventSourcingCQRSIntegrationTest.class,
    PerformanceIntegrationTest.class
})
public class IntegrationTestSuite {
    // Test suite configuration
}