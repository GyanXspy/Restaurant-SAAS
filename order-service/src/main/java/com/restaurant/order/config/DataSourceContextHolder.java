package com.restaurant.order.config;

/**
 * Thread-local context holder for data source routing.
 * Determines whether to use read or write data source.
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();

    public static void setDataSourceType(DataSourceType dataSourceType) {
        contextHolder.set(dataSourceType);
    }

    public static DataSourceType getDataSourceType() {
        DataSourceType type = contextHolder.get();
        return type != null ? type : DataSourceType.WRITE; // Default to write
    }

    public static void clearDataSourceType() {
        contextHolder.remove();
    }
}