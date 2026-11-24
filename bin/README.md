# Employee and Leave Management System

A Spring Boot REST API for employee records, departments, leave requests, leave balances, holidays, and JWT authentication.

## Requirements

- Java 21
- Maven 3.9+
- MySQL 8

## Configuration

Create the MySQL database:

```sql
CREATE DATABASE ems_db;
```

Configure the application using environment variables or `src/main/resources/application.properties`:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRATION_MS`
- `JWT_REFRESH_EXPIRATION_MS`

## Run

```powershell
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

At startup, an initial administrator is created:

- Username: `admin`
- Password: `Admin123!`
- Change the default password after first login.

## Test

```powershell
mvn test
```
