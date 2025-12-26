@echo off
echo ========================================
echo Starting Cart Service
echo ========================================
echo.

REM Set JVM options for Java 17+ compatibility
set MAVEN_OPTS=--add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED

echo Starting with Java 17+ compatibility options...
echo MAVEN_OPTS: %MAVEN_OPTS%
echo.

mvn spring-boot:run
