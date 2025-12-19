package com.restaurant.payment.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect for Payment Service read/write routing.
 */
@Aspect
@Component
@Order(0)
public class ReadOnlyTransactionInterceptor {

    @Around("@annotation(transactional)")
    public Object setReadDataSourceType(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        try {
            if (transactional.readOnly()) {
                DataSourceContextHolder.setDataSourceType(DataSourceType.READ);
            } else {
                DataSourceContextHolder.setDataSourceType(DataSourceType.WRITE);
            }
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }

    @Around("execution(* com.restaurant.payment.query..*(..))")
    public Object setReadDataSourceForQueries(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.setDataSourceType(DataSourceType.READ);
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }

    @Around("execution(* com.restaurant.payment.command..*(..))")
    public Object setWriteDataSourceForCommands(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.setDataSourceType(DataSourceType.WRITE);
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}