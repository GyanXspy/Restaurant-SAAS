@echo off
echo Starting Kafka locally...

REM Start Zookeeper first
echo Starting Zookeeper...
start "Zookeeper" cmd /k "cd /d C:\kafka && bin\windows\zookeeper-server-start.bat config\zookeeper.properties"

REM Wait a bit for Zookeeper to start
timeout /t 10

REM Start Kafka
echo Starting Kafka...
start "Kafka" cmd /k "cd /d C:\kafka && bin\windows\kafka-server-start.bat config\server.properties"

echo Kafka services are starting...
echo Zookeeper: localhost:2181
echo Kafka: localhost:9092
pause