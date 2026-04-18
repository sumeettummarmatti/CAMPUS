# CAMPUS — Real-Time Campus Auction Platform

> A competitive bidding platform built for college campuses, enabling students to list items and place live bids from any device on the same network.

---

## What This Is

CAMPUS is a full-stack distributed application built around four independent Spring Boot microservices and a JavaFX desktop client. Every registered user can both list items for auction and bid on items listed by others — the mode they are in (browsing vs. selling) is a UI state, not an account property. Auctions progress through a fully automated lifecycle, bids are validated in real time with optimistic locking to handle concurrent submissions, and payments are held in escrow until delivery is confirmed.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   JavaFX Desktop Client                      │
│   Login · Register · Browse Auctions · Bid · Profile        │
└────┬──────────┬───────────┬──────────────┬──────────────────┘
     │ REST     │ REST      │ REST + WS    │ REST
     ▼          ▼           ▼              ▼
┌─────────┐ ┌────────┐ ┌─────────┐ ┌──────────┐
│  User   │ │Auction │ │Bidding  │ │ Payment  │
│ :8081   │ │ :8082  │ │  :8083  │ │  :8084   │
└────┬────┘ └───┬────┘ └────┬────┘ └────┬─────┘
     │          │            │            │
  postgres    postgres    postgres    postgres
   :5436       :5433       :5434       :5435
```

The Auction Service scheduler calls the Bidding Service over REST when an auction ends to determine the winner, then triggers bid resolution automatically. There is no shared database and no shared library between services — all coordination is via HTTP.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17+, Spring Boot 3.2, Spring Security, Spring Data JPA |
| Authentication | JWT (JJWT 0.12.5), shared secret across all services |
| Real-time | Spring WebSocket (raw WebSocket, no STOMP) |
| Persistence | PostgreSQL 15 (one database per service) |
| Containerisation | Docker + Docker Compose |
| Frontend | JavaFX 21, FXML, Jackson for JSON |
| Build | Apache Maven 3.9+ (multi-module parent POM) |

---

## Microservices

### User Service — Port 8081
Handles registration, login, JWT issuance, and profile management. Every user is registered with role `USER` (capable of both buying and selling). The `ADMIN` role is reserved for platform administrators only. Refactored to enforce SRP and DIP through interface-based dependency injection.

**Key classes:** `AuthService`, `UserService`, `ITokenService` → `JwtTokenService`, `IPasswordService` → `BCryptPasswordService`

### Auction Service — Port 8082
Manages the full auction lifecycle from draft to closure. A scheduler runs every 60 seconds to activate scheduled auctions and end expired ones. On auction end, the scheduler calls the Bidding Service to retrieve the highest bid and closes the auction as `CLOSED_SOLD` or `CLOSED_NO_SALE` accordingly.

**Lifecycle:** `DRAFT → SCHEDULED → ACTIVE → ENDED → CLOSED_SOLD / CLOSED_NO_SALE / CLOSED_CANCELLED`

**Key classes:** `AuctionService`, `AuctionSchedulerService`

### Bidding Service — Port 8083
Handles bid placement with optimistic locking (Hibernate `@Version`) to safely resolve concurrent bids. Broadcasts new leading bid amounts to all connected WebSocket clients in real time. Exposes a `/resolve` endpoint called by the Auction Service when an auction ends, which marks the winning bid as `WON` and all others as `LOST`.

**WebSocket endpoint:** `ws://{host}:8083/ws/auction/{auctionId}`

**Key classes:** `BidService`, `BidWebSocketHandler`

### Payment Service — Port 8084
Implements an escrow model: funds are held after payment confirmation and released to the seller only after the buyer confirms delivery (or after 7 days automatically). Disputes freeze the escrow and route through admin resolution. Accepts JWTs issued by the User Service via a shared secret.

**Lifecycle:** `PENDING → PAYMENT_PROCESSING → IN_ESCROW → SHIPPED → DELIVERY_CONFIRMED → COMPLETED`

**Key classes:** `PaymentGatewayService`, `EscrowService`, `DisputeService`, `TransactionQueryService`, `IPaymentGateway` → `SimulatedPaymentGateway`, `INotificationService` → `NoOpNotificationService`

---

## Design Patterns

| Pattern | Type | Location | Purpose |
|---|---|---|---|
| Builder | Creational | `AuctionService`, `Transaction` | Constructs entities with many optional fields without positional constructor chaos |
| Factory | Creational | `PaymentGatewayService` | Abstracts the payment provider behind `IPaymentGateway`; swap Razorpay in with zero logic changes |
| Observer | Behavioural | `BidWebSocketHandler` | Notifies all connected clients the moment a new leading bid is persisted |
| Facade | Structural | `AuctionSchedulerService` | Hides multi-service orchestration behind a single scheduled task |

---

## SOLID Principles Applied

**User Service**
- **SRP** — `AuthService` (auth only), `UserService` (profiles only), `JwtTokenService` (tokens only), `BCryptPasswordService` (hashing only)
- **DIP** — `AuthService` depends on `ITokenService` and `IPasswordService`, not on JJWT or BCrypt directly

**Payment Service**
- **SRP** — `PaymentGatewayService` (lifecycle), `EscrowService` (escrow), `DisputeService` (disputes), `TransactionQueryService` (reads)
- **DIP** — business logic depends on `IPaymentGateway` and `INotificationService`; infrastructure adapters are injected

---

## How to Run

### Prerequisites

```bash
java -version   # 17+
mvn -version    # 3.9+
docker info     # Docker Desktop must be running
```

### One-command demo start

```bash
chmod +x start-demo.sh
./start-demo.sh
```

This script starts all four databases via Docker, compiles all services, launches them in the background, and opens the JavaFX frontend. Press `Ctrl+C` to shut everything down cleanly.

### Manual start (five terminals)

```bash
# Terminal 1 — databases
docker compose up -d

# Terminal 2 — User Service
cd backend/user-service && mvn spring-boot:run

# Terminal 3 — Auction Service
cd backend/auction-service && mvn spring-boot:run

# Terminal 4 — Bidding Service
cd backend/bidding-service && mvn spring-boot:run

# Terminal 5 — Payment Service
cd backend/payment-service && mvn spring-boot:run

# Terminal 6 — Frontend
cd frontend && mvn javafx:run
```

Wait for each service to print `Started [Name]Application` before starting the next.

### Network access (LAN)

Anyone on the same Wi-Fi can connect. Set the `SERVICE_HOST_IP` environment variable to the host machine's local IP before launching the frontend:

```bash
# Mac / Linux
export SERVICE_HOST_IP=$(ipconfig getifaddr en0)

# Windows (PowerShell)
$env:SERVICE_HOST_IP = "192.168.x.x"
```

The `AppConfig` class reads this variable and injects it into all service URLs automatically.

---

## API Quick Reference

### Register and login

```bash
# Register — no role selection, every user can buy and sell
curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Shriya Asija","email":"shriya@pes.edu","password":"pass1234","hostelName":"Block A"}'

# Login — returns JWT
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"shriya@pes.edu","password":"pass1234"}'

# Get your own profile
curl -s http://localhost:8081/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Auctions

```bash
# Browse active auctions
curl -s "http://localhost:8082/api/auctions/browse/active?size=20" \
  -H "Authorization: Bearer $TOKEN"

# Create an auction
curl -s -X POST http://localhost:8082/api/auctions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"MacBook Pro M1","description":"Barely used","price":75000,"reservePrice":70000,"sellerId":1,"startTime":"2025-07-01T10:00:00","endTime":"2025-07-03T18:00:00"}'

# Schedule it (DRAFT → SCHEDULED)
curl -s -X POST http://localhost:8082/api/auctions/1/schedule \
  -H "Authorization: Bearer $TOKEN"
```

### Bidding

```bash
# Place a bid
curl -s -X POST http://localhost:8083/api/bids \
  -H "Content-Type: application/json" \
  -d '{"auctionId":1,"buyerId":2,"amount":76000}'

# Get bid history for an auction
curl -s http://localhost:8083/api/bids/auction/1

# WebSocket — connect via wscat or the JavaFX client
# wscat -c ws://localhost:8083/ws/auction/1
```

### Payments

```bash
TOKEN="paste-jwt-here"

# Initiate payment after winning
curl -s -X POST http://localhost:8084/api/payments/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"auctionId":1,"winnerId":2,"sellerId":1,"amount":76000.00,"paymentMethod":"UPI"}'

# Full happy path
curl -s -X POST http://localhost:8084/api/payments/1/confirm                 -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/hold             -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/ship             -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/confirm-delivery -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8084/api/payments/1/escrow/release          -H "Authorization: Bearer $TOKEN"

# Open a dispute
curl -s -X POST http://localhost:8084/api/disputes/1/open \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"reason":"Item not as described"}'
```

---

## Shutdown

```bash
# Ctrl+C each service terminal, then:
docker compose down
```

---

## Project Structure

```
CAMPUS/
├── backend/
│   ├── user-service/        # Port 8081 — auth, profiles
│   ├── auction-service/     # Port 8082 — auction lifecycle
│   ├── bidding-service/     # Port 8083 — bids + WebSocket
│   └── payment-service/     # Port 8084 — escrow + disputes
├── frontend/
│   └── src/main/
│       ├── java/com/campus/frontend/
│       │   ├── controller/  # LoginController, DashboardController, ProfileController
│       │   ├── service/     # REST + WebSocket clients
│       │   ├── model/       # User (frontend model)
│       │   └── config/      # AppConfig (LAN IP injection)
│       └── resources/
│           ├── fxml/        # Login.fxml, Dashboard.fxml, Register.fxml, Profile.fxml
│           └── config.properties
├── docker-compose.yml       # Four PostgreSQL databases
├── start-demo.sh            # One-command startup script
└── pom.xml                  # Parent POM
```

---

## Group J7

Shriya Asija · Sohamdeep Mandal · Srihari Bharadwaj · Sumeet Tummaramatti 
