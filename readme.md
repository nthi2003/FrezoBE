# Frezo Backend

Enterprise-grade Spring Boot application with modular architecture, RBAC, Spring Cloud Gateway, and modern Java 21 features.

## Tech Stack

- **Java 21** (LTS) with Virtual Threads support
- **Spring Boot 3.2.0**
- **Spring Cloud Gateway** (FrezoGW - API Gateway)
- **PostgreSQL 17**
- **Hibernate 6.3.1.Final**
- **MapStruct 1.5.5.Final** for DTO mapping
- **JWT** Authentication (HS512)
- **Consul** Service Discovery & Config
- **MinIO** Object Storage
- **Docker** + Docker Compose

## Project Structure

```
Freze/
├── FrezoBE/          # Java Spring Boot Backend (this)
│   ├── module-common/        # Shared utilities, exceptions, base entities
│   ├── module-auth-bom/res   # Authentication
│   ├── module-qtht-bom/res   # System Administration
│   ├── module-dmdc-bom/res   # Master Data
│   ├── module-qlns-bom/res   # Human Resource
│   ├── module-qtbv-bom/res   # Content Management
│   ├── module-task-bom/res   # Task Management
│   ├── module-email-bom/res  # Email Service
│   ├── module-customer-bom/res # Customer Management
│   ├── module-product-bom/res  # Product Management
│   ├── module-cms-bom/res    # CMS
│   ├── module-server/        # Main application runner
│   ├── docker-compose.yml    # Full stack orchestration
│   └── Dockerfile
│
├── FrezoGW/          # Spring Cloud Gateway (API Gateway)
│
└── FrezoAI/          # Python FastAPI AI Service
    ├── api/          # Endpoints (scraper, OCR, Zalo)
    ├── core/         # Core logic (EasyOCR, Playwright)
    └── main.py
```

## Module Architecture

```
FrezoBE/
├── module-common/          # Shared utilities, exceptions, base entities
├── module-auth-bom/        # Authentication business logic (User, UserRole)
├── module-auth-res/        # Authentication REST endpoints (Login)
├── module-qtht-bom/        # System Administration (Person, Org, Department, RBAC)
├── module-qtht-res/        # System Administration REST endpoints
├── module-dmdc-bom/        # Master Data/Category business logic
├── module-dmdc-res/        # Master Data REST endpoints
├── module-qlns-bom/        # Human Resource (Contracts) business logic
├── module-qlns-res/        # Human Resource REST endpoints
├── module-qtbv-bom/        # Content Management (Articles, Reactions)
├── module-qtbv-res/        # Content Management REST endpoints
├── module-task-bom/        # Task management business logic
├── module-task-res/        # Task management REST endpoints
├── module-email-bom/       # Email service business logic
├── module-email-res/       # Email service REST endpoints
├── module-customer-bom/    # Customer management business logic
├── module-customer-res/    # Customer management REST endpoints
├── module-product-bom/     # Product management business logic
├── module-product-res/     # Product management REST endpoints
├── module-cms-bom/         # CMS business logic
├── module-cms-res/         # CMS REST endpoints
└── module-server/          # Main application runner + global config
```

## Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose
- Maven 3.9+

### Run full stack (Docker)

```bash
docker compose up -d
```

Services:
| Service | Port | Description |
|---------|------|-------------|
| Consul  | 8500 | Service Discovery & Config |
| MinIO   | 9000 | Object Storage |
| BE      | 8080 | Backend API (internal) |
| AI      | 8001 | AI Service (internal) |
| GW      | 8081 | API Gateway **(FE gọi qua port này)** |

### FE chỉ gọi qua Gateway (port 8081)
```bash
# VD: Login
curl http://localhost:8081/api/auth/login

# VD: Gọi AI Scraper  
curl http://localhost:8081/ai/api/v1/scrape
```

### Run local (dev)
```bash
mvn clean install -DskipTests
mvn spring-boot:run -pl module-server
```

### Access API
- **Swagger UI**: http://localhost:8081/api/swagger-ui.html
- **Health Check**: http://localhost:8081/api/management/health

## Security Features

### JWT Authentication
- **Algorithm**: HS512
- **Expiration**: 24 hours
- **Refresh Token**: 7 days
- **Header**: `Authorization: Bearer <token>`

### RBAC (Role-Based Access Control)
- **Entities**: `User`, `Role`, `Person`, `Menu`, `UserRole`, `RoleMenu`, `Permission`
- **Admin Flag**: `Person.isAdmin` grants full system access
- **Dynamic Roles**: Roles and permissions stored in database

## Gateway (FrezoGW)

Spring Cloud Gateway với các chức năng:
- **Rate Limiting**: Bucket4j - giới hạn request/IP
- **IP Block/Allow**: Block IP tự động khi vượt rate limit
- **Routing**:
  - `/api/**` → Backend (qua Consul load-balance)
  - `/ai/**` → AI Service (Docker DNS)
- **Consul Discovery**: Tự động discover backend service

## AI Service (FrezoAI)

Python FastAPI với các chức năng:
- **Web Scraper**: Google Maps scraping với Playwright
- **OCR**: Nhận dạng hóa đơn/chứng từ với EasyOCR
- **Zalo Automation**: Gửi tin nhắn Zalo tự động
- **Pattern Learning**: Học pattern từ correction của user

## Key API Endpoints

### Authentication (`/api/auth`)
- `POST /login` - Get JWT token

### System Administration (`/api/qtht`)
- `GET /menus` - Get accessible menus
- `POST /users` - Register/Manage users

### HR & Contracts (`/api/qlns`)
- `GET /contract` - List contracts
- `POST /contract` - Create new contract

### Content Management (`/api/qtbv`)
- `GET /article` - List articles

### Task Management (`/api/tasks`)
- `GET /tasks` - List tasks
- `PATCH /tasks/{id}/status` - Update task status

### AI (`/ai/api/v1`)
- `POST /scrape` - Google Maps scraping
- `POST /analyze-document` - OCR document analysis
- `POST /zalo` - Send Zalo messages

## MinIO Object Storage

- **Endpoint**: http://localhost:9000
- **Console**: http://localhost:9001
- **Default credentials**: minioadmin / minioadmin
- **Bucket**: frezo-files (auto-created)

## Coding Standards

- **DTOs**: MapStruct for Entity ↔ DTO mapping (Snake Case)
- **Entities**: All extend `BaseEntity` (String UUID, auditing, soft delete)
- **Repositories**: Standard JPA Repositories
- **Services**: Interface + Implementation pattern
- **Controllers**: RESTful style, always returning `ApiResponse<T>`
- **Exceptions**: `AppException` with i18n keys

## Architecture Diagram

Xem chi tiết tại `architecture.md` ở thư mục gốc.
