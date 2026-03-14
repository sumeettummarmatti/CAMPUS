# CAMPUS
CAMP-us is a real-time auction platform for college campuses, enabling students to buy and sell items through competitive bidding.

## Architecture

The application is built using a strict Model-View-Controller (MVC) Microservices architecture with a JavaFX desktop frontend.

The backend consists of 4 core Spring Boot microservices:
1. **User Service (Port 8081)**: Manages authentication (JWT), user profiles, and seller verification.
2. **Auction Service (Port 8082)**: Manages the lifecycle of auctions (draft, scheduled, active, ended) and implements anti-sniping rules.
3. **Bidding Service (Port 8083)**: Handles real-time WebSockets for live bidding and bid validations.
4. **Payment Service (Port 8084)**: Simulates escrow payments, transaction holding, and dispute resolution logic.

Each microservice has its own dedicated PostgreSQL database containerized via Docker.

---

## How to Run the Application Locally

*Note: Currently, only the **User Service** and the **JavaFX Frontend** are fully completed and wired together.*

### 1. Start the Database (Docker)
Ensure Docker Desktop is running. In the root `CAMPUS` directory, start the PostgreSQL databases:
```bash
docker compose up -d
```
*(The `-d` runs it in the background so it doesn't freeze your terminal).*

### 2. Start the User Service (Backend)
Open a new terminal, navigate to the User Service folder, and run the Spring Boot application:
```bash
cd backend/user-service
mvn spring-boot:run
```
*(Leave this terminal running. The API will be available at `localhost:8081`)*

### 3. Start the JavaFX Application (Frontend)
Open another new terminal, navigate to the frontend folder, and launch the desktop UI:
```bash
cd frontend
mvn javafx:run
```
*(This will open the CAMPUS Desktop App window where you can register and log in).*

---

### Shutting Down
When you are completely done testing:
1. Close the JavaFX window.
2. Stop the Spring Boot backend by pressing `Ctrl + C` in its terminal.
3. Stop the Docker databases by running the following in the project root:
```bash
docker compose down
```
