@echo off
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8083
set CUSTOMER_ID=customer-123
set RESTAURANT_ID=restaurant-456

echo ========================================
echo Cart Service API Tests
echo ========================================
echo Base URL: %BASE_URL%
echo Customer ID: %CUSTOMER_ID%
echo Restaurant ID: %RESTAURANT_ID%
echo ========================================
echo.

echo [TEST 1] Get Cart (should be empty/404)
curl -X GET "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%" -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 2] Add First Item - Margherita Pizza
curl -X POST "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%/items" ^
  -H "Content-Type: application/json" ^
  -d "{\"itemId\":\"item-001\",\"name\":\"Margherita Pizza\",\"price\":12.99,\"quantity\":2,\"restaurantId\":\"%RESTAURANT_ID%\"}"
echo.
echo.
pause

echo [TEST 3] Add Second Item - Caesar Salad
curl -X POST "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%/items" ^
  -H "Content-Type: application/json" ^
  -d "{\"itemId\":\"item-002\",\"name\":\"Caesar Salad\",\"price\":8.50,\"quantity\":1,\"restaurantId\":\"%RESTAURANT_ID%\"}"
echo.
echo.
pause

echo [TEST 4] Get Cart (should show 2 items)
curl -X GET "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%" -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 5] Update Item Quantity (item-001 to 5)
curl -X PUT "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%/items/item-001?quantity=5" ^
  -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 6] Get Cart (verify quantity updated)
curl -X GET "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%" -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 7] Remove Item (item-002)
curl -X DELETE "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%/items/item-002" ^
  -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 8] Get Cart (verify item removed)
curl -X GET "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%" -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 9] Clear Cart
curl -X DELETE "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%" ^
  -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 10] Get Cart (should be empty/404)
curl -X GET "%BASE_URL%/api/v1/carts/%CUSTOMER_ID%" -H "Content-Type: application/json"
echo.
echo.
pause

echo [TEST 11] Health Check
curl -X GET "%BASE_URL%/actuator/health" -H "Content-Type: application/json"
echo.
echo.

echo ========================================
echo All Tests Completed!
echo ========================================
pause
