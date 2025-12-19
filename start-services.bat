@echo off
echo Restaurant Food Ordering System - Local Development
echo =====================================================
echo.

echo Building all services...
call mvn clean install "-Dmaven.test.skip=true"
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo.
echo Starting services...
echo.
echo Services will start on the following ports:
echo   User Service:       http://localhost:8081
echo   Restaurant Service: http://localhost:8082
echo   Cart Service:       http://localhost:8083
echo   Order Service:      http://localhost:8084
echo   Payment Service:    http://localhost:8085
echo.
echo Press Ctrl+C to stop all services
echo.

echo Starting User Service...
start "User Service" cmd /k "cd user-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081"

timeout /t 5 /nobreak >nul

echo Starting Restaurant Service...
start "Restaurant Service" cmd /k "cd restaurant-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082"

timeout /t 5 /nobreak >nul

echo Starting Cart Service...
start "Cart Service" cmd /k "cd cart-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083"

timeout /t 5 /nobreak >nul

echo Starting Order Service...
start "Order Service" cmd /k "cd order-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084"

timeout /t 5 /nobreak >nul

echo Starting Payment Service...
start "Payment Service" cmd /k "cd payment-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8085"

echo.
echo All services are starting...
echo Each service will open in its own command window.
echo.
pause