# Employee Management System & Leave Management System

A Spring Boot REST API for managing employees, departments, holidays, and leave requests, secured with JWT authentication and role-based access control.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Default Admin Account](#default-admin-account)
- [Roles and Permissions](#roles-and-permissions)
- [API Reference](#api-reference)
- [Leave Rules](#leave-rules)
- [Example Usage](#example-usage)
- [Error Responses](#error-responses)
- [Running Tests](#running-tests)
- [Security Notes](#security-notes)

## Features

- **Authentication** – login, token refresh, change password, admin-only user registration (JWT access and refresh tokens)
- **Employee management** – create, update, search, paginate, soft delete, status changes, manager/subordinate hierarchy
- **Department management** – CRUD plus a list of employees per department
- **Leave management** – apply, update, cancel, approve, reject, and view leave requests
- **Leave balances** – per-employee, per-year balances created automatically on first use
- **Holiday management** – company holiday list, with support for recurring holidays
- **Role-based access control** – `ADMIN`, `HR`, `MANAGER`, `EMPLOYEE`
- **Validation and consistent error responses** through a global exception handler

## Tech Stack

| Layer       | Technology                                  |
|-------------|---------------------------------------------|
| Language    | Java 21                                     |
| Framework   | Spring Boot 3.4.4 (Web, Data JPA, Validation) |
| Security    | Spring Security, JWT (jjwt 0.12.6), BCrypt  |
| Database    | MySQL 8 (H2 in-memory for tests)            |
| Build tool  | Maven                                       |
| Other       | Lombok                                      |

## Project Structure

```
src/main/java/com/ems
├── EmsApplication.java        # Application entry point
├── config/                    # SecurityConfig, DataInitializer (seeds admin user)
├── controller/                # REST controllers (Auth, Employee, Department, Leave, Holiday)
├── domain/                    # JPA entities and enums
├── dto/                       # Request/response records
├── exception/                 # ApiException and GlobalExceptionHandler
├── repository/                # Spring Data JPA repositories
├── security/                  # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/                   # Business logic
src/main/resources/application.properties
src/test/                      # Tests (H2 in-memory configuration)
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8

### 1. Create the database

```sql
CREATE DATABASE ems_db;
```

### 2. Configure the application

Edit `src/main/resources/application.properties` and set your database details:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ems_db
spring.datasource.username=<your-mysql-username>
spring.datasource.password=<your-mysql-password>
```

Tables are created and updated automatically (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Run

```bash
mvn spring-boot:run
```

The API starts at **http://localhost:8080**.

### 4. Build a JAR (optional)

```bash
mvn clean package
java -jar target/employee-leave-management-1.0.0.jar
```

## Configuration

| Property                      | Description                          | Default                                  |
|-------------------------------|--------------------------------------|------------------------------------------|
| `spring.datasource.url`       | JDBC URL of the MySQL database       | set in `application.properties`          |
| `spring.datasource.username`  | Database user                        | set in `application.properties`          |
| `spring.datasource.password`  | Database password                    | set in `application.properties`          |
| `JWT_SECRET`                  | Secret used to sign JWTs             | placeholder value (**change it**)        |
| `JWT_ACCESS_EXPIRATION_MS`    | Access token lifetime                | `900000` (15 minutes)                    |
| `JWT_REFRESH_EXPIRATION_MS`   | Refresh token lifetime               | `604800000` (7 days)                     |

The JWT settings can be supplied as environment variables, for example:

```bash
export JWT_SECRET="a-long-random-secret-at-least-32-characters"
mvn spring-boot:run
```

## Default Admin Account

If the `users` table is empty at startup, an administrator is created automatically:

| Username | Password    | Role  |
|----------|-------------|-------|
| `admin`  | `Admin123!` | ADMIN |

**Change this password immediately after the first login** using `PUT /api/auth/change-password`.

## Roles and Permissions

| Role       | Summary                                                                       |
|------------|-------------------------------------------------------------------------------|
| `ADMIN`    | Full access, including deleting records and registering users                |
| `HR`       | Manage employees, departments, holidays; view and approve leave               |
| `MANAGER`  | View employees and subordinates; approve/reject leave; view leave balances    |
| `EMPLOYEE` | View own profile, apply for and manage own leave, view holidays and departments |

## API Reference

All endpoints except `/api/auth/login` and `/api/auth/refresh` require the header:

```
Authorization: Bearer <accessToken>
```

### Authentication – `/api/auth`

| Method | Endpoint           | Access        | Description                  |
|--------|--------------------|---------------|------------------------------|
| POST   | `/login`           | Public        | Log in and receive tokens    |
| POST   | `/refresh`         | Public        | Get new tokens via refresh token |
| GET    | `/me`              | Authenticated | Current user details         |
| POST   | `/register`        | ADMIN         | Create a new user            |
| PUT    | `/change-password` | Authenticated | Change own password          |
| POST   | `/logout`          | Authenticated | Logout (client discards tokens) |

### Employees – `/api/employees`

| Method | Endpoint             | Access             | Description                                   |
|--------|----------------------|--------------------|-----------------------------------------------|
| GET    | `/me`                | Authenticated      | Own employee profile                          |
| GET    | `/`                  | ADMIN, HR, MANAGER | Paginated list of employees                   |
| GET    | `/search`            | ADMIN, HR, MANAGER | Search by `keyword`, `status`, `departmentId` |
| GET    | `/{id}`              | ADMIN, HR, MANAGER | Get an employee                               |
| POST   | `/`                  | ADMIN, HR          | Create an employee                            |
| PUT    | `/{id}`              | ADMIN, HR, MANAGER | Update an employee                            |
| PATCH  | `/{id}/status`       | ADMIN, HR          | Change status (`?status=ACTIVE`)              |
| DELETE | `/{id}`              | ADMIN              | Soft delete an employee                       |
| GET    | `/{id}/subordinates` | ADMIN, HR, MANAGER | List direct reports                           |

Employment statuses: `ACTIVE`, `ON_LEAVE`, `SUSPENDED`, `TERMINATED`

### Departments – `/api/departments`

| Method | Endpoint           | Access             | Description                  |
|--------|--------------------|--------------------|------------------------------|
| GET    | `/`                | Authenticated      | List departments             |
| POST   | `/`                | ADMIN, HR          | Create a department          |
| PUT    | `/{id}`            | ADMIN, HR          | Update a department          |
| DELETE | `/{id}`            | ADMIN              | Delete a department          |
| GET    | `/{id}/employees`  | ADMIN, HR, MANAGER | Employees in the department  |

### Leaves – `/api/leaves`

| Method | Endpoint               | Access             | Description                           |
|--------|------------------------|--------------------|---------------------------------------|
| POST   | `/`                    | Authenticated      | Apply for leave                       |
| GET    | `/my-requests`         | Authenticated      | Own leave requests (paginated)        |
| GET    | `/{id}`                | Authenticated      | Get a leave request (own, or non-employee roles) |
| PUT    | `/{id}`                | Authenticated      | Update own pending request            |
| POST   | `/{id}/cancel`         | Authenticated      | Cancel a pending request              |
| GET    | `/pending`             | MANAGER, HR, ADMIN | Pending requests                      |
| POST   | `/{id}/approve`        | MANAGER, HR, ADMIN | Approve a request                     |
| POST   | `/{id}/reject`         | MANAGER, HR, ADMIN | Reject a request (optional `reason`)  |
| GET    | `/`                    | ADMIN, HR, MANAGER | All leave requests                    |
| GET    | `/calendar`            | ADMIN, HR, MANAGER | Leave calendar view                   |
| GET    | `/balance`             | Authenticated      | Own leave balance                     |
| GET    | `/balance/{empId}`     | ADMIN, HR, MANAGER | Leave balance of an employee          |
| POST   | `/balance/initialize`  | ADMIN, HR          | Initialize balance for a year (`?year=2026`) |

Leave types: `ANNUAL`, `SICK`, `PERSONAL`, `MATERNITY`, `PATERNITY`, `UNPAID`
Leave statuses: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`

### Holidays – `/api/holidays`

| Method | Endpoint | Access        | Description        |
|--------|----------|---------------|--------------------|
| GET    | `/`      | Authenticated | List holidays      |
| POST   | `/`      | ADMIN, HR     | Create a holiday   |
| PUT    | `/{id}`  | ADMIN, HR     | Update a holiday   |
| DELETE | `/{id}`  | ADMIN         | Delete a holiday   |

Paginated endpoints accept the standard Spring parameters: `page`, `size`, and `sort` (for example `?page=0&size=10&sort=id,desc`).

## Leave Rules

- The end date must not be before the start date.
- Leave cannot start in the past.
- Only **pending** requests can be updated or cancelled.
- Only the owner can update a request; the owner or a non-employee role can cancel it.
- Balances are created automatically per employee and year with these defaults:
  - Annual entitlement: **20 days**
  - Sick entitlement: **10 days**
- On **approval**, working days (Monday–Friday) in the range are deducted from the balance:
  - `ANNUAL` and `SICK` are checked against the entitlement; approval fails with *"Insufficient … leave balance"* if exceeded.
  - `PERSONAL` and `UNPAID` are tracked without a limit.
  - Other types (`MATERNITY`, `PATERNITY`) are currently deducted from the annual balance.
- Rejecting a request does not affect the balance.

## Example Usage

**Login**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}'
```

Response:

```json
{ "accessToken": "eyJhbGciOi...", "refreshToken": "eyJhbGciOi..." }
```

**Create an employee**

```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeCode": "EMP001",
    "firstName": "Asha",
    "lastName": "Rao",
    "email": "asha.rao@example.com",
    "position": "Software Engineer",
    "hireDate": "2026-01-15",
    "departmentId": 1,
    "username": "asha",
    "password": "Password123!"
  }'
```

**Apply for leave**

```bash
curl -X POST http://localhost:8080/api/leaves \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "leaveType": "ANNUAL",
    "startDate": "2026-12-22",
    "endDate": "2026-12-24",
    "reason": "Family trip"
  }'
```

**Approve a leave request**

```bash
curl -X POST http://localhost:8080/api/leaves/1/approve \
  -H "Authorization: Bearer <accessToken>"
```

## Error Responses

Business errors:

```json
{
  "timestamp": "2026-11-24T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Leave cannot start in the past"
}
```

Validation errors:

```json
{
  "timestamp": "2026-11-24T10:15:30",
  "status": 400,
  "errors": { "email": "must be a well-formed email address" }
}
```

## Running Tests

Tests use an in-memory H2 database (configured in `src/test/resources/application.properties`), so MySQL is not required.

```bash
mvn test
```

## Security Notes

- Do not commit real database credentials or JWT secrets. Use environment variables or an untracked configuration file for production.
- Set a strong `JWT_SECRET` (32+ random characters) in every non-local environment.
- Change the default `admin` password after the first login.
- `/api/auth/logout` is stateless; tokens remain valid until they expire, so the client should discard them on logout.
