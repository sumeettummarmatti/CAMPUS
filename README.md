# CAMPUS
CAMP-us is a real-time auction platform for college campuses, enabling students to buy and sell items through competitive bidding.

## Architecture

The application is built using a strict Model-View-Controller (MVC) Microservices architecture with a JavaFX desktop frontend.

The backend consists of 4 core Spring Boot microservices:
1. **User Service (Port 8081)** ✅: Manages authentication (JWT), user profiles, and seller verification.
2. **Auction Service (Port 8082)**: Manages the lifecycle of auctions (draft, scheduled, active, ended) and implements anti-sniping rules.
3. **Bidding Service (Port 8083)**: Handles real-time WebSockets for live bidding and bid validations.
4. **Payment Service (Port 8084)** ✅: Escrow payments, transaction lifecycle, and dispute resolution.

Each microservice has its own dedicated PostgreSQL database containerized via Docker.

### Design Patterns Used
| Pattern | Type | Service | Class |
|---------|------|---------|-------|
| **Builder** | Creational | auction-service | `AuctionBuilder` |
| **Factory** | Creational | payment-service | `PaymentGatewayService` |
| **Observer** | Behavioral | bidding-service | `BidEventPublisher` |
| **Facade** | Structural | auction-service | `AuctionFacade` |

---

## How to Run the Application Locally

### Prerequisites
- Java 17+ (tested on Java 25)
- Apache Maven 3.9+
- Docker Desktop (for PostgreSQL databases)

### 1. Start the Databases (Docker)
Ensure Docker Desktop is running. In the root `CAMPUS` directory:
```bash
docker compose up -d
```

### 2. Start the User Service (Terminal 1)
```bash
cd backend/user-service
mvn spring-boot:run
```
*(API available at `localhost:8081`)*

### 3. Start the Payment Service (Terminal 2)
```bash
cd backend/payment-service
mvn spring-boot:run
```
*(API available at `localhost:8084` — uses the same JWT secret as User Service)*

### 4. Start the JavaFX Frontend (Terminal 3)
```bash
cd frontend
mvn javafx:run
```
*(Opens the CAMPUS Desktop App window for registration and login)*

---

## Testing the API (curl)

### Register & Login
```bash
# Register
curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@pes.edu","password":"pass1234","fullName":"Test User","hostelName":"Block A","role":"BUYER"}'

# Login (returns JWT token)
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@pes.edu","password":"pass1234"}'
```

### Payment Lifecycle (use JWT from login)
```bash
TOKEN="<paste-token-here>"

# Initiate payment
curl -s -X POST http://localhost:8084/api/payments/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"auctionId":1,"winnerId":1,"sellerId":2,"amount":500.00,"paymentMethod":"UPI"}'

# Full lifecycle: confirm → hold → ship → deliver → release
curl -s -X POST http://localhost:8084/api/payments/1/confirm -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/hold -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/ship -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/confirm-delivery -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/release -H "Authorization: Bearer $TOKEN"
```

---

### Shutting Down
1. Close the JavaFX window.
2. Press `Ctrl + C` in each Spring Boot terminal.
3. Stop Docker databases:
```bash
docker compose down
```
