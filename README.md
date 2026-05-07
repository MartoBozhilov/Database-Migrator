# Database Migrator - Diploma Project

## Overview

This is my diploma project: a comprehensive **ETL (Extract, Transform, Load) system** that enables database migration between heterogeneous database systems (MySQL, PostgreSQL, MS SQL Server) with full schema transformation capabilities.

**Technology Stack:**
- Spring Boot 4.0.4 + Java 21
- MySQL (application database)
- Spring Security + JWT authentication
- Multi-tenant architecture with Role-Based Access Control

---

## Project Structure

```
Diploma-Project/
├── etl-system/              # Main Spring Boot application
│   ├── src/main/java/       # Java source code
│   └──src/main/resources/   # Configuration files (type mappings, dialects)
├── docs/                    # Additional documentation
└── README.md                # This file
```

---

## User Roles & Permissions

### **ADMIN** (Super Admin)
- Full system access
- Manage all organizations
- Create MIGRATION_ADMIN users

### **MIGRATION_ADMIN** (Organization Admin)
- Manage users within their organization
- Assign operational roles (CONNECTOR_USER, TRANSFORMATION_MODEL_USER, CYCLE_EXECUTION_USER)
- Has all operational permissions

### **CONNECTOR_USER**
- Create, view, update, delete database Connectors
- Test database connections
- Create, view and execute System scans to extract metadata from source database

### **TRANSFORMATION_MODEL_USER**
- Create transformation models based on System scans
- Apply transformations (rename tables/columns, change types, add/delete entities)
- Confirm transformation models for execution

### **CYCLE_EXECUTION_USER**
- Create execution cycles
- Execute data migrations
- View execution logs and status

---

## Domain Overview

The system is organized into 5 core domains:

### 1. **Auth Domain**
Manages authentication and authorization.
- User registration and JWT-based login
- Organization management (multi-tenant isolation)
- Role assignment and access control

### 2. **Connector Domain**
Maintains source and target database connection data.
- Store connection details (host, port, credentials, database type)
- Test database connectivity
- Classify as SOURCE (data source) or TARGET (migration destination)

### 3. **Scan Domain**
Extracts metadata from source databases.
- Scan database schema (tables, columns, relationships)
- Capture data types, constraints, primary/foreign keys
- Store metadata for transformation planning

### 4. **Transformation Domain**
Defines schema transformations for migration.
- Create transformation models linking source scan to target database
- Apply transformations:
  - **Tables:** Rename, Add new, Delete (exclude)
  - **Columns:** Rename, Change type, Add new, Delete (exclude)
  - **Relations:** Manage foreign key relationships
- Validate type conversions and constraints
- Confirm models to make them immutable before execution

### 5. **Execution Domain**
Executes data migration with transformations.
- Create execution cycles from confirmed transformation models
- Generate DDL (CREATE TABLE statements) for target database
- Migrate data in batches with transformations applied
- Resolve table dependencies (topological sort)
- Support parallel execution for independent tables
- Track execution status and logs

---

## Key Features

 - **Heterogeneous Database Migration** - MySQL, PostgreSQL, MS SQL Server
 - **Schema Transformation** - Rename, type change, add/delete entities
 - **User-Added Entities** - Add tables/columns that don't exist in source
 - **Type Mapping Validation** - Detect data loss risks in type conversions
 - **Foreign Key Management** - Maintain referential integrity
 - **Dependency Resolution** - Automatic task scheduling via DAG
 - **Parallel Execution** - Concurrent migration of independent tables
 - **Batch Processing** - Stream data in configurable batches (default: 1000 rows)
 - **Async Execution** - Background processing with status tracking
 - **Multi-Tenant** - Organization-level data isolation
 - **Role-Based Access Control** - Fine-grained permission management

---

## How It Works

1. **Create Connectors** → Define SOURCE and TARGET database connections
2. **Scan Source** → Extract complete schema metadata from source database
3. **Create Transformation Model** → Map source schema to target schema
4. **Apply Transformations** → Rename tables/columns, change types, add/delete entities
5. **Confirm Model** → Lock transformations for execution
6. **Create Execution Cycle** → Generate execution plan from confirmed model
7. **Execute Migration** → DDL + Data migration with transformations applied
8. **Monitor Progress** → Track status, logs, and row counts

---

## Getting Started

1. **Prerequisites:**
   - Java 21+
   - Maven 3.8+
   - MySQL (application database)

2. **Build & Run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. **Access API:**
   - Base URL: `http://localhost:8080`
   - Login: `POST /api/auth/login`
   - See `DataInitializer.java` for seed data
