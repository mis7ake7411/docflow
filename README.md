# DocFlow Lite

DocFlow Lite 是一個以 **Spring Boot** 為核心的文件管理 side project，目標是展示後端工程中常見且重要的能力：

- 模組化後端架構設計
- Spring Security 認證與授權
- PostgreSQL 資料持久化
- Redis 快取與一致性策略
- Local file storage 檔案儲存
- Activity log / audit trail
- Docker 化環境配置
- OpenAPI 文件化

> 這個專案偏向 **backend architecture showcase**，不是完整商業產品。

---

## 1. Tech Stack

### Backend
- Java 17 *(開發環境目前對齊 Java 17；原始設計可升回 Java 21)*
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- Lombok

### Database
- PostgreSQL

### Cache
- Redis

### Infrastructure
- Docker Compose

### Documentation
- OpenAPI / springdoc

### Testing
- JUnit 5
- Mockito

---

## 2. Architecture

```text
Client
  |
  v
Spring Boot API
  |---- PostgreSQL
  |---- Redis
  |---- Local File Storage
```

### Package structure

```text
com.docflow

common
 ├─ config
 ├─ exception
 ├─ response
 ├─ security
 └─ util

auth
 ├─ controller
 ├─ service
 ├─ dto
 └─ repository

user
 ├─ controller
 ├─ service
 ├─ dto
 ├─ entity
 └─ repository

folder
 ├─ controller
 ├─ service
 ├─ dto
 ├─ entity
 └─ repository

document
 ├─ controller
 ├─ service
 ├─ dto
 ├─ entity
 ├─ repository
 └─ storage

stats
 ├─ controller
 ├─ service
 └─ dto

activity
 ├─ controller
 ├─ service
 ├─ dto
 ├─ entity
 └─ repository

infra
 ├─ redis
 ├─ docker
 └─ openapi
```

---

## 3. Core Features

### Auth
- Register
- Login
- Refresh token
- Logout
- JWT authentication
- Refresh token persistence
- Access token blacklist (Redis)

### Folder
- Create folder
- Update folder
- Soft delete folder
- Get folder tree

### Document
- Create document metadata
- Upload file
- Download file
- Update metadata
- Soft delete document
- Local file storage

### Redis
- Document detail cache
- Hot documents ranking
- Recent viewed documents
- Token blacklist
- Document update lock baseline

### Activity Log
- Register / login / logout audit log
- Folder create / update / delete audit log
- Document create / upload / update / delete / download audit log

### Documentation
- Swagger UI / OpenAPI
- API grouping by module

---

## 4. Database Schema

### users
- id
- username
- email
- password_hash
- role
- status
- created_at
- updated_at

### folders
- id
- name
- parent_id
- sort_order
- created_by
- created_at
- updated_at
- deleted_flag

### documents
- id
- folder_id
- title
- description
- file_name
- stored_file_name
- content_type
- file_size
- version
- status
- created_by
- created_at
- updated_at
- deleted_flag

### document_views
- id
- document_id
- user_id
- viewed_at

### activity_logs
- id
- user_id
- target_type
- target_id
- action
- detail_json
- created_at

### refresh_tokens
- id
- user_id
- token
- expired_at
- revoked_flag
- created_at

---

## 5. Redis Design

### Keys
- `doc:detail:{docId}`
  - Document detail cache
- `doc:hot`
  - Sorted set for hot documents
- `user:recent:view:{userId}`
  - Sorted set for recently viewed documents
- `auth:blacklist:{token}`
  - Token blacklist
- `lock:doc:update:{docId}`
  - Distributed lock for document update

### Cache Pattern
#### Document Detail Cache
**Pattern:** Cache Aside

Flow:
1. Query Redis
2. If miss → query DB
3. Store in Redis
4. Return result

#### Cache Invalidation
When document updated:
- delete cache key
- `doc:detail:{id}`

---

## 6. API Endpoints

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### Folder
- `POST /api/folders`
- `GET /api/folders/tree`
- `PUT /api/folders/{id}`
- `DELETE /api/folders/{id}`

### Document
- `POST /api/documents`
- `POST /api/documents/{id}/upload`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `PUT /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `GET /api/documents/{id}/download`

### Stats
- `GET /api/stats/hot-documents`
- `GET /api/users/me/recent-views`

### Activity
- `GET /api/activities`

---

## 7. Local Development

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL
- Redis

### Run compile check
```bash
mvn -q -DskipTests compile
```

### Run application
```bash
mvn spring-boot:run
```

---

## 8. Docker

### Services
- `app`
- `postgres`
- `redis`

### Ports
- app: `8080`
- postgres: `5432`
- redis: `6379`

### Volumes
- `postgres-data`
- `uploaded-files`

### Start with Docker Compose
```bash
docker compose up --build
```

> 注意：目前開發環境如果不是完整 Docker host，可能只能保留 compose 檔而無法在該環境實際啟動 daemon。

---

## 9. OpenAPI

Swagger UI 路徑：
```text
/swagger-ui.html
```

OpenAPI docs 路徑：
```text
/api-docs
```

---

## 10. Project Status

### Implemented
- Project skeleton
- Docker baseline
- Auth module
- Folder module
- Document module
- Redis integration baseline
- Statistics baseline
- Activity log
- OpenAPI documentation
- README

### Not yet completed / possible improvements
- DB migration tool (Flyway / Liquibase)
- More complete integration tests
- Role-based authorization refinement
- Deeper folder tree cycle validation
- File cleanup strategy after soft delete
- Stronger distributed lock integration in document update flow
- More detailed OpenAPI schema examples
- Pagination / filtering for activities and documents

---

## 11. Notes

This project focuses on demonstrating:
- backend modularization
- realistic Redis usage
- authentication workflow design
- cache consistency strategy
- auditable business operations

It is intended as a **practical backend portfolio project**, not a full production-ready DMS.
