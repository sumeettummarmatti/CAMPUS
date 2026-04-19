#!/bin/bash

# Cleanup function to kill all background processes gracefully when script exits
cleanup() {
    echo ""
    echo "Shutting down all CAMPUS services..."
    # kill 0 sends SIGTERM to all processes in the current process group
    kill 0 2>/dev/null
}

# Trap CTRL+C (SIGINT) and script termination (EXIT) to run the cleanup function
trap cleanup SIGINT EXIT

echo "Starting CAMPUS Platform Demo Environment..."

echo "1. Starting PostgreSQL databases via Docker..."
docker compose up -d

echo "2. Compiling backend services..."
mvn clean install -DskipTests

echo "3. Starting microservices in the background..."
export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"
(cd backend/user-service && mvn spring-boot:run) > user-service.log 2>&1 &
(cd backend/auction-service && mvn spring-boot:run) > auction-service.log 2>&1 &
(cd backend/bidding-service && mvn spring-boot:run) > bidding-service.log 2>&1 &
(cd backend/payment-service && mvn spring-boot:run) > payment-service.log 2>&1 &

echo "Waiting 15 seconds for Spring Boot services to initialize..."
sleep 15
echo "Microservices Started."

echo "4. Starting the Frontend UI..."
(cd frontend && mvn clean compile javafx:run) > frontend.log 2>&1 &
FRONTEND_PID=$!

echo ""
echo "=================================================="
echo "    CAMPUS Application is Running!                "
echo "    Frontend GUI will open momentarily.           "
echo "    Backend logs are saved in *.log files.        "
echo "                                                  "
echo "    Press [CTRL+C] or close the frontend window   "
echo "    to stop all services.                         "
echo "=================================================="

# Wait for the frontend application to close
wait $FRONTEND_PID
