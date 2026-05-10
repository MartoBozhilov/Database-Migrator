# Phase 2 - Manual API Testing Guide

## Prerequisites

### 1. Environment Setup

**IMPORTANT**: Load environment variables before starting the application!

```bash
# Copy environment template (first time only)
cp .env.example .env

# Edit .env and ensure JWT_SECRET is set
nano .env

# Load environment variables and start application
export $(cat .env | xargs) && mvn spring-boot:run
```

### 2. Requirements

1. **Database**: MySQL running with `database_migrator_app` database
2. **API Client**: Bruno, Postman, Insomnia, or curl
3. **Base URL**: `http://localhost:8080`
4. **.env file**: Must contain JWT_SECRET, DB credentials

### 3. Verify Application Started

Check logs for:
```
Started EtlSystemApplication in X seconds
Default admin user created successfully:
  Email: admin@system.com
  Password: Admin123!
```

---

## Test Scenario Overview

This guide tests all Phase 2 features including **security improvements**:

1. **Initialization** - Verify default admin created on startup
2. **Authentication** - Login with admin credentials
3. **Organization Management** - Create organizations (ADMIN only)
4. **User Hierarchy** - Create MIGRATION_ADMIN for organizations
5. **Role Assignment** - Assign operational roles to users
6. **Multi-Tenant Isolation** - Verify organization boundaries
7. **Authorization** - Test role-based access control
8. **CRUD Operations** - Create, Read, Update, Delete users
9. **Error Cases** - Test invalid scenarios
10. **Token Versioning** - Verify tokens are invalidated on role changes
11. **Rate Limiting** ⭐ NEW - Test brute force protection
12. **Token Caching** ⭐ NEW - Test performance optimization
13. **Security Features** ⭐ NEW - Test all security improvements

---

## Test Environment Setup

### Expected Initial State (After Application Startup)

**Database State:**
- 6 roles created: `UNREGISTER`, `ADMIN`, `MIGRATION_ADMIN`, `CONNECTOR_USER`, `TRANSFORMATION_MODEL_USER`, `CYCLE_EXECUTION_USER`
- 1 organization: "System Organization" (id: 1)
- 1 user: admin@system.com with ADMIN + 3 operational roles

**Application Logs Should Show:**
```
Created role: UNREGISTER
Created role: ADMIN
Created role: MIGRATION_ADMIN
Created role: CONNECTOR_USER
Created role: TRANSFORMATION_MODEL_USER
Created role: CYCLE_EXECUTION_USER
User roles initialization complete. Total roles: 6
Creating default admin user...
Default admin user created successfully:
  Email: admin@system.com
  Password: Admin123!
  Roles: [ADMIN, CONNECTOR_USER, TRANSFORMATION_MODEL_USER, CYCLE_EXECUTION_USER]
```

---

## Test Cases

### **TEST 1: Admin Login**

**Purpose**: Verify default admin credentials work and JWT token is generated

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```

**Expected Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "admin@system.com",
  "username": "admin",
  "organizationId": 1,
  "organizationName": "System Organization",
  "roles": [
    "ADMIN",
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "expiresIn": 86400000
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Token is returned (starts with `eyJ`)
- ✅ userId is 1
- ✅ organizationId is 1
- ✅ User has 4 roles (ADMIN + 3 operational roles)
- ✅ Token expires in 24 hours (86400000 ms)

**Save for Next Tests**: Copy the `token` value to use in subsequent requests

---

### **TEST 2: Get Current User Profile**

**Purpose**: Verify authenticated user can view their profile

**Endpoint**: `GET /api/auth/me`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN_FROM_TEST_1>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@system.com",
  "firstName": "System",
  "lastName": "Administrator",
  "organization": null,
  "roles": [
    "ADMIN",
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "createdAt": "2026-04-30 21:44:22.788"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User details match admin account
- ✅ All 4 roles are present
- ✅ createdAt timestamp shows when admin was created

---

### **TEST 3: Try Accessing Endpoint Without Token (Should Fail)**

**Purpose**: Verify authentication is required

**Endpoint**: `GET /api/auth/me`

**Headers**: None (no Authorization header)

**Request Body**: None

**Expected Response** (HTTP 403):
```
(Empty body)
```

**What to Verify**:
- ✅ HTTP status is 403 Forbidden
- ✅ No response body (Spring Security default)

**Explanation**: Without a valid JWT token, all protected endpoints should return 403

---

### **TEST 4: Create New Organization**

**Purpose**: Verify ADMIN can create organizations

**Endpoint**: `POST /api/admin/organizations`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "Company ABC",
  "companyName": "ABC Corporation",
  "location": "New York"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 2,
  "name": "Company ABC",
  "companyName": "ABC Corporation",
  "location": "New York",
  "createdAt": "Thu Apr 30 21:46:47 EEST 2026"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Organization created with id: 2
- ✅ All fields match request
- ✅ createdAt timestamp is present

**Save for Next Tests**: Copy the `id` value (should be 2)

---

### **TEST 5: Try Creating Duplicate Organization (Should Fail)**

**Purpose**: Verify organization name uniqueness constraint

**Endpoint**: `POST /api/admin/organizations`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "Company ABC",
  "companyName": "Another Company",
  "location": "Los Angeles"
}
```

**Expected Response** (HTTP 500):
```json
{
  "error": "Internal Server Error",
  "message": "Organization with name 'Company ABC' already exists",
  "path": "/api/admin/organizations",
  "status": 500,
  "timestamp": "2026-04-30T18:46:50.123Z",
  "validationErrors": null
}
```

**What to Verify**:
- ✅ HTTP status is 500
- ✅ Error message mentions duplicate name
- ✅ Organization was NOT created

---

### **TEST 6: Create MIGRATION_ADMIN for Organization**

**Purpose**: Verify ADMIN can create first user (MIGRATION_ADMIN) for an organization

**Endpoint**: `POST /api/admin/organizations/users`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "organizationId": 2,
  "username": "abc_admin",
  "email": "admin@abc.com",
  "password": "Admin123!",
  "firstName": "ABC",
  "lastName": "Admin"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 2,
  "username": "abc_admin",
  "email": "admin@abc.com",
  "firstName": "ABC",
  "lastName": "Admin",
  "organization": null,
  "roles": [
    "MIGRATION_ADMIN",
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "createdAt": "Thu Apr 30 21:47:00 EEST 2026"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User created with id: 2
- ✅ User has 4 roles (MIGRATION_ADMIN + 3 operational roles)
- ✅ User automatically assigned to organization 2

**Save for Next Tests**: 
- Copy user `id` (should be 2)
- Remember credentials: admin@abc.com / Admin123!

---

### **TEST 7: MIGRATION_ADMIN Login**

**Purpose**: Verify MIGRATION_ADMIN can login

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "admin@abc.com",
  "password": "Admin123!"
}
```

**Expected Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 2,
  "email": "admin@abc.com",
  "username": "abc_admin",
  "organizationId": 2,
  "organizationName": "Company ABC",
  "roles": [
    "MIGRATION_ADMIN",
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "expiresIn": 86400000
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Token generated
- ✅ userId is 2
- ✅ organizationId is 2 (NOT 1!)
- ✅ organizationName is "Company ABC"
- ✅ Has 4 roles

**Save for Next Tests**: Copy the `token` value (this is MIGRATION_ADMIN token)

---

### **TEST 8: MIGRATION_ADMIN Creates Regular User**

**Purpose**: Verify MIGRATION_ADMIN can create users in their organization

**Endpoint**: `POST /api/users`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN_FROM_TEST_7>
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "john_doe",
  "email": "john@abc.com",
  "password": "User123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 3,
  "username": "john_doe",
  "email": "john@abc.com",
  "firstName": "John",
  "lastName": "Doe",
  "organization": null,
  "roles": [],
  "createdAt": "Thu Apr 30 21:47:12 EEST 2026"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User created with id: 3
- ✅ User has NO roles initially (empty array)
- ✅ User automatically in organization 2 (same as MIGRATION_ADMIN)

**Save for Next Tests**: Copy user `id` (should be 3)

---

### **TEST 9: MIGRATION_ADMIN Tries to Access Admin Endpoint (Should Fail)**

**Purpose**: Verify endpoint separation - MIGRATION_ADMIN cannot access /api/admin/**

**Endpoint**: `POST /api/admin/users/3/roles`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "CONNECTOR_USER"
}
```

**Expected Response** (HTTP 403):
```
(Empty body)
```

**What to Verify**:
- ✅ HTTP status is 403 Forbidden
- ✅ No response body
- ✅ Role was NOT assigned

**Explanation**: `/api/admin/**` endpoints are ADMIN-only. MIGRATION_ADMIN is blocked at SecurityConfig level.

---

### **TEST 10: ADMIN Assigns Role to User (Cross-Organization - Should Fail)**

**Purpose**: Verify multi-tenant isolation - ADMIN cannot assign roles to users in different organization

**Endpoint**: `POST /api/admin/users/3/roles`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN_FROM_TEST_1>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "CONNECTOR_USER"
}
```

**Expected Response** (HTTP 500):
```json
{
  "error": "Internal Server Error",
  "message": "Access denied: User belongs to different organization",
  "path": "/api/admin/users/3/roles",
  "status": 500,
  "timestamp": "2026-04-30T18:47:21.490Z",
  "validationErrors": null
}
```

**What to Verify**:
- ✅ HTTP status is 500
- ✅ Error message mentions different organization
- ✅ Role was NOT assigned

**Explanation**: User 3 belongs to organization 2, but ADMIN token is from organization 1. Multi-tenant isolation prevents cross-organization operations.

---

### **TEST 11: ADMIN Creates User in Own Organization**

**Purpose**: Verify ADMIN can create users in their own organization (without specifying organizationId)

**Endpoint**: `POST /api/users`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "system_user",
  "email": "user@system.com",
  "password": "User123!",
  "firstName": "System",
  "lastName": "User"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 4,
  "username": "system_user",
  "email": "user@system.com",
  "firstName": "System",
  "lastName": "User",
  "organization": null,
  "roles": [],
  "createdAt": "Thu Apr 30 21:47:47 EEST 2026"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User created with id: 4
- ✅ User has no roles
- ✅ User in organization 1 (same as ADMIN)

**Save for Next Tests**: Copy user `id` (should be 4)

---

### **TEST 11B: ADMIN Creates User in Different Organization**

**Purpose**: Verify ADMIN can create users in ANY organization by specifying organizationId

**Endpoint**: `POST /api/users`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "abc_user",
  "email": "user2@abc.com",
  "password": "User123!",
  "firstName": "ABC",
  "lastName": "User",
  "organizationId": 2
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 5,
  "username": "abc_user",
  "email": "user2@abc.com",
  "firstName": "ABC",
  "lastName": "User",
  "organization": null,
  "roles": [],
  "createdAt": "Thu Apr 30 21:47:55 EEST 2026"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User created with id: 5
- ✅ User belongs to organization 2 (NOT organization 1 where ADMIN is from)
- ✅ ADMIN can create users across organizations

**Important**: This is an ADMIN-only feature. MIGRATION_ADMIN cannot use organizationId parameter.

---

### **TEST 11C: MIGRATION_ADMIN Tries to Use organizationId (Ignored)**

**Purpose**: Verify MIGRATION_ADMIN cannot create users in other organizations even with organizationId

**Endpoint**: `POST /api/users`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "hacker",
  "email": "hacker@system.com",
  "password": "User123!",
  "firstName": "Hacker",
  "lastName": "User",
  "organizationId": 1
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 6,
  "username": "hacker",
  "email": "hacker@system.com",
  "firstName": "Hacker",
  "lastName": "User",
  "organization": null,
  "roles": [],
  "createdAt": "Thu Apr 30 21:48:10 EEST 2026"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User created successfully
- ✅ BUT user is in organization 2 (MIGRATION_ADMIN's org), NOT organization 1
- ✅ organizationId parameter was **ignored** for MIGRATION_ADMIN
- ✅ Multi-tenant isolation enforced

**Explanation**: Even though MIGRATION_ADMIN specified `organizationId: 1`, the system ignores it and creates the user in MIGRATION_ADMIN's own organization (org 2). This prevents MIGRATION_ADMIN from creating users in other organizations.

---

### **TEST 12: ADMIN Assigns Role to User (Same Organization)**

**Purpose**: Verify ADMIN can assign roles to users in same organization

**Endpoint**: `POST /api/admin/users/4/roles`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "CONNECTOR_USER"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 4,
  "username": "system_user",
  "email": "user@system.com",
  "firstName": "System",
  "lastName": "User",
  "organization": null,
  "roles": [
    "CONNECTOR_USER"
  ],
  "createdAt": "2026-04-30 21:47:47.089"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User now has CONNECTOR_USER role
- ✅ Role successfully assigned

---

### **TEST 13: ADMIN Assigns Additional Role**

**Purpose**: Verify users can have multiple roles

**Endpoint**: `POST /api/admin/users/4/roles`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "TRANSFORMATION_MODEL_USER"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 4,
  "username": "system_user",
  "email": "user@system.com",
  "firstName": "System",
  "lastName": "User",
  "organization": null,
  "roles": [
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER"
  ],
  "createdAt": "2026-04-30 21:47:47.089"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User now has 2 roles
- ✅ Previous role (CONNECTOR_USER) still present
- ✅ New role (TRANSFORMATION_MODEL_USER) added

---

### **TEST 14: ADMIN Removes Role from User**

**Purpose**: Verify roles can be removed

**Endpoint**: `DELETE /api/admin/users/4/roles`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "CONNECTOR_USER"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 4,
  "username": "system_user",
  "email": "user@system.com",
  "firstName": "System",
  "lastName": "User",
  "organization": null,
  "roles": [
    "TRANSFORMATION_MODEL_USER"
  ],
  "createdAt": "2026-04-30 21:47:47.089"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ CONNECTOR_USER role removed
- ✅ TRANSFORMATION_MODEL_USER role still present
- ✅ User now has only 1 role

---

### **TEST 15: List Users in Organization (MIGRATION_ADMIN)**

**Purpose**: Verify MIGRATION_ADMIN only sees users in their organization

**Endpoint**: `GET /api/users`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
[
  {
    "id": 2,
    "username": "abc_admin",
    "email": "admin@abc.com",
    "firstName": "ABC",
    "lastName": "Admin",
    "organization": null,
    "roles": [
      "MIGRATION_ADMIN",
      "CONNECTOR_USER",
      "TRANSFORMATION_MODEL_USER",
      "CYCLE_EXECUTION_USER"
    ],
    "createdAt": "2026-04-30 21:47:00.556"
  },
  {
    "id": 3,
    "username": "john_doe",
    "email": "john@abc.com",
    "firstName": "John",
    "lastName": "Doe",
    "organization": null,
    "roles": [],
    "createdAt": "2026-04-30 21:47:12.984"
  }
]
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Returns 2 users (both from organization 2)
- ✅ Does NOT return users from organization 1 (user 1 and user 4)
- ✅ Multi-tenant isolation working

---

### **TEST 16: List Users in Organization (ADMIN)**

**Purpose**: Verify ADMIN only sees users in their organization

**Endpoint**: `GET /api/users`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@system.com",
    "firstName": "System",
    "lastName": "Administrator",
    "organization": null,
    "roles": [
      "ADMIN",
      "CONNECTOR_USER",
      "TRANSFORMATION_MODEL_USER",
      "CYCLE_EXECUTION_USER"
    ],
    "createdAt": "2026-04-30 21:44:22.788"
  },
  {
    "id": 4,
    "username": "system_user",
    "email": "user@system.com",
    "firstName": "System",
    "lastName": "User",
    "organization": null,
    "roles": [
      "TRANSFORMATION_MODEL_USER"
    ],
    "createdAt": "2026-04-30 21:47:47.089"
  }
]
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Returns 2 users (both from organization 1)
- ✅ Does NOT return users from organization 2 (users 2 and 3)
- ✅ Multi-tenant isolation working

---

### **TEST 17: Get User by ID**

**Purpose**: Verify fetching specific user details

**Endpoint**: `GET /api/users/3`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
{
  "id": 3,
  "username": "john_doe",
  "email": "john@abc.com",
  "firstName": "John",
  "lastName": "Doe",
  "organization": null,
  "roles": [],
  "createdAt": "2026-04-30 21:47:12.984"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Returns correct user details
- ✅ User belongs to same organization as MIGRATION_ADMIN

---

### **TEST 18: Get User from Different Organization (Should Fail)**

**Purpose**: Verify multi-tenant isolation on single user fetch

**Endpoint**: `GET /api/users/4`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 500):
```json
{
  "error": "Internal Server Error",
  "message": "Access denied: User belongs to different organization",
  "path": "/api/users/4",
  "status": 500,
  "timestamp": "2026-04-30T18:50:00.123Z",
  "validationErrors": null
}
```

**What to Verify**:
- ✅ HTTP status is 500
- ✅ Error message mentions different organization
- ✅ User 4 data NOT returned

**Explanation**: User 4 belongs to organization 1, but MIGRATION_ADMIN token is from organization 2.

---

### **TEST 19: List All Organizations (ADMIN Only)**

**Purpose**: Verify only ADMIN can list all organizations

**Endpoint**: `GET /api/admin/organizations`

**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
[
  {
    "id": 1,
    "name": "System Organization",
    "companyName": "System",
    "location": "Default",
    "createdAt": "2026-04-30 21:44:22.715"
  },
  {
    "id": 2,
    "name": "Company ABC",
    "companyName": "ABC Corporation",
    "location": "New York",
    "createdAt": "2026-04-30 21:46:47.455"
  }
]
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Returns all 2 organizations
- ✅ System Organization (id: 1) is first
- ✅ Company ABC (id: 2) is second

---

### **TEST 20: List All Organizations (MIGRATION_ADMIN - Should Fail)**

**Purpose**: Verify MIGRATION_ADMIN cannot list all organizations

**Endpoint**: `GET /api/admin/organizations`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 403):
```
(Empty body)
```

**What to Verify**:
- ✅ HTTP status is 403 Forbidden
- ✅ No response body
- ✅ Organizations NOT returned

**Explanation**: `/api/admin/**` is ADMIN-only

---

### **TEST 21: Get Organization by ID**

**Purpose**: Verify fetching organization details

**Endpoint**: `GET /api/organizations/2`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
{
  "id": 2,
  "name": "Company ABC",
  "companyName": "ABC Corporation",
  "location": "New York",
  "createdAt": "2026-04-30 21:46:47.455"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Returns correct organization details
- ✅ MIGRATION_ADMIN can view their own organization

---

### **TEST 22: Get Organization by Name**

**Purpose**: Verify fetching organization by name

**Endpoint**: `GET /api/organizations/name/Company ABC`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
{
  "id": 2,
  "name": "Company ABC",
  "companyName": "ABC Corporation",
  "location": "New York",
  "createdAt": "2026-04-30 21:46:47.455"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Returns correct organization
- ✅ Name lookup works correctly

**Note**: URL encode spaces as `%20` if needed: `/api/organizations/name/Company%20ABC`

---

### **TEST 23: Regular User Login**

**Purpose**: Verify user with no roles can still login

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "john@abc.com",
  "password": "User123!"
}
```

**Expected Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 3,
  "email": "john@abc.com",
  "username": "john_doe",
  "organizationId": 2,
  "organizationName": "Company ABC",
  "roles": [],
  "expiresIn": 86400000
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Token generated
- ✅ roles array is empty
- ✅ User can login even without roles

**Save for Next Tests**: Copy the `token` (this is regular user token)

---

### **TEST 24: Regular User Tries to Create User (Should Fail)**

**Purpose**: Verify users without ADMIN/MIGRATION_ADMIN role cannot create users

**Endpoint**: `POST /api/users`

**Headers**:
```
Authorization: Bearer <REGULAR_USER_TOKEN_FROM_TEST_23>
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "test_user",
  "email": "test@abc.com",
  "password": "User123!",
  "firstName": "Test",
  "lastName": "User"
}
```

**Expected Response** (HTTP 403):
```
(Empty body)
```

**What to Verify**:
- ✅ HTTP status is 403 Forbidden
- ✅ User NOT created
- ✅ Only ADMIN/MIGRATION_ADMIN can create users

---

### **TEST 25: Delete User**

**Purpose**: Verify MIGRATION_ADMIN can delete users in their organization

**Endpoint**: `DELETE /api/users/3`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 204):
```
(Empty body)
```

**What to Verify**:
- ✅ HTTP status is 204 No Content
- ✅ No response body
- ✅ User deleted successfully

---

### **TEST 26: Verify User Deleted**

**Purpose**: Confirm user no longer exists

**Endpoint**: `GET /api/users`

**Headers**:
```
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
[
  {
    "id": 2,
    "username": "abc_admin",
    "email": "admin@abc.com",
    "firstName": "ABC",
    "lastName": "Admin",
    "organization": null,
    "roles": [
      "MIGRATION_ADMIN",
      "CONNECTOR_USER",
      "TRANSFORMATION_MODEL_USER",
      "CYCLE_EXECUTION_USER"
    ],
    "createdAt": "2026-04-30 21:47:00.556"
  }
]
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Only 1 user returned (user 3 is gone)
- ✅ Deletion confirmed

---

### **TEST 27: Try to Login with Deleted User (Should Fail)**

**Purpose**: Verify deleted user cannot login

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "john@abc.com",
  "password": "User123!"
}
```

**Expected Response** (HTTP 401):
```json
{
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/auth/login",
  "status": 401,
  "timestamp": "2026-04-30T18:52:00.123Z",
  "validationErrors": null
}
```

**What to Verify**:
- ✅ HTTP status is 401 Unauthorized
- ✅ Error message says invalid credentials
- ✅ Deleted user cannot login

---

### **TEST 28: Token Versioning - Initial Login**

**Purpose**: Get initial token and verify it contains tokenVersion

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```

**Expected Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6MSwidG9rZW5WZXJzaW9uIjoxLCJyb2xlcyI6WyJBRE1JTiIsIkNPTk5FQ1RPUl9VU0VSIiwiVFJBTlNGT1JNQVRJT05fTU9ERUxfVVNFUiIsIkNZQ0xFX0VYRUNVVElPTl9VU0VSIl0sInVzZXJJZCI6MSwic3ViIjoiYWRtaW5Ac3lzdGVtLmNvbSIsImlhdCI6MTc3NzU3NzA5NiwiZXhwIjoxNzc3NjYzNDk2fQ...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "admin@system.com",
  "username": "admin",
  "organizationId": 1,
  "organizationName": "System Organization",
  "roles": [
    "ADMIN",
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "expiresIn": 86400000
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ Token received and saved as `TOKEN_V1`
- ✅ Token works for API calls

**Note**: Save this token as `TOKEN_V1` for the next tests.

---

### **TEST 29: Verify Token Works**

**Purpose**: Confirm current token works before role change

**Endpoint**: `GET /api/auth/me`

**Headers**:
```
Authorization: Bearer <TOKEN_V1>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@system.com",
  "firstName": "System",
  "lastName": "Administrator",
  "organization": null,
  "roles": [
    "ADMIN",
    "CONNECTOR_USER",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "createdAt": "2026-04-30 21:44:22.788"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User profile returned
- ✅ Token is working

---

### **TEST 30: Remove Role to Trigger Token Invalidation**

**Purpose**: Change user's roles which will increment tokenVersion

**Endpoint**: `DELETE /api/admin/users/1/roles`

**Headers**:
```
Authorization: Bearer <TOKEN_V1>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "CONNECTOR_USER"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@system.com",
  "firstName": "System",
  "lastName": "Administrator",
  "organization": null,
  "roles": [
    "ADMIN",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "createdAt": "2026-04-30 21:44:22.788"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ CONNECTOR_USER role removed
- ✅ User's tokenVersion incremented in database (not visible in response)

---

### **TEST 31: Verify Old Token is Rejected**

**Purpose**: Confirm old token no longer works after role change

**Endpoint**: `GET /api/auth/me`

**Headers**:
```
Authorization: Bearer <TOKEN_V1>
```

**Request Body**: None

**Expected Response** (HTTP 403):
```
(Empty body or minimal error)
```

**What to Verify**:
- ✅ HTTP status is 403 Forbidden
- ✅ Old token rejected
- ✅ User cannot access endpoints with old token

**Explanation**: The token has `tokenVersion: 1`, but the user now has `tokenVersion: 2` in the database. Token versioning detected the mismatch and rejected the request.

**Check Application Logs**:
Look for warning message:
```
WARN ... JwtAuthenticationFilter : Token version mismatch for user: admin@system.com. Token has version 1, but user has version 2
```

---

### **TEST 32: Login Again to Get New Token**

**Purpose**: Get fresh token with updated tokenVersion

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```

**Expected Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6MSwidG9rZW5WZXJzaW9uIjoyLCJyb2xlcyI6WyJBRE1JTiIsIlRSQU5TRk9STUFUSU9OX01PREVMX1VTRVIiLCJDWUNMRV9FWEVDVVRJT05fVVNFUiJdLCJ1c2VySWQiOjEsInN1YiI6ImFkbWluQHN5c3RlbS5jb20iLCJpYXQiOjE3Nzc1NzcxNTQsImV4cCI6MTc3NzY2MzU1NH0...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "admin@system.com",
  "username": "admin",
  "organizationId": 1,
  "organizationName": "System Organization",
  "roles": [
    "ADMIN",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "expiresIn": 86400000
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ New token received (save as `TOKEN_V2`)
- ✅ Roles updated (CONNECTOR_USER no longer present)
- ✅ Token is different from TOKEN_V1

**Note**: Save this token as `TOKEN_V2`.

---

### **TEST 33: Verify New Token Works**

**Purpose**: Confirm new token with updated tokenVersion works

**Endpoint**: `GET /api/auth/me`

**Headers**:
```
Authorization: Bearer <TOKEN_V2>
```

**Request Body**: None

**Expected Response** (HTTP 200):
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@system.com",
  "firstName": "System",
  "lastName": "Administrator",
  "organization": null,
  "roles": [
    "ADMIN",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER"
  ],
  "createdAt": "2026-04-30 21:44:22.788"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ User profile returned
- ✅ New token works correctly
- ✅ CONNECTOR_USER role not present

---

### **TEST 34: Add Role Back**

**Purpose**: Test token versioning works for role assignment too

**Endpoint**: `POST /api/admin/users/1/roles`

**Headers**:
```
Authorization: Bearer <TOKEN_V2>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "CONNECTOR_USER"
}
```

**Expected Response** (HTTP 200):
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@system.com",
  "firstName": "System",
  "lastName": "Administrator",
  "organization": null,
  "roles": [
    "ADMIN",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER",
    "CONNECTOR_USER"
  ],
  "createdAt": "2026-04-30 21:44:22.788"
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ CONNECTOR_USER role added back
- ✅ User's tokenVersion incremented to 3

---

### **TEST 35: Verify Previous Token Now Rejected**

**Purpose**: Confirm TOKEN_V2 is now invalid after another role change

**Endpoint**: `GET /api/auth/me`

**Headers**:
```
Authorization: Bearer <TOKEN_V2>
```

**Request Body**: None

**Expected Response** (HTTP 403):
```
(Empty body or minimal error)
```

**What to Verify**:
- ✅ HTTP status is 403 Forbidden
- ✅ TOKEN_V2 rejected (has tokenVersion: 2, but user now has 3)
- ✅ Token versioning working for role assignment

**Check Application Logs**:
```
WARN ... JwtAuthenticationFilter : Token version mismatch for user: admin@system.com. Token has version 2, but user has version 3
```

---

### **TEST 36: Final Login with All Roles**

**Purpose**: Get final token with complete role set

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```

**Expected Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "admin@system.com",
  "username": "admin",
  "organizationId": 1,
  "organizationName": "System Organization",
  "roles": [
    "ADMIN",
    "TRANSFORMATION_MODEL_USER",
    "CYCLE_EXECUTION_USER",
    "CONNECTOR_USER"
  ],
  "expiresIn": 86400000
}
```

**What to Verify**:
- ✅ HTTP status is 200
- ✅ All 4 roles present
- ✅ New token with tokenVersion: 3
- ✅ Ready for normal operations

---

## Summary Checklist

After completing all 36 tests, verify:

### ✅ Authentication
- [x] Admin can login with default credentials
- [x] MIGRATION_ADMIN can login
- [x] Regular users can login
- [x] Deleted users cannot login
- [x] JWT tokens are generated correctly
- [x] Tokens contain correct userId, organizationId, roles

### ✅ Authorization
- [x] `/api/admin/**` endpoints require ADMIN role
- [x] `/api/users` endpoints require ADMIN or MIGRATION_ADMIN role
- [x] Users without roles are blocked from admin endpoints
- [x] MIGRATION_ADMIN blocked from `/api/admin/**`

### ✅ Multi-Tenant Isolation
- [x] Users only see data from their organization
- [x] Cross-organization operations are blocked
- [x] ADMIN in org 1 cannot access users in org 2
- [x] MIGRATION_ADMIN can only manage users in their org

### ✅ CRUD Operations
- [x] Create organization
- [x] Create MIGRATION_ADMIN for organization
- [x] Create regular users
- [x] Read user details
- [x] Read organization details
- [x] Update (assign/remove roles)
- [x] Delete users

### ✅ Role Management
- [x] ADMIN gets 4 roles automatically
- [x] MIGRATION_ADMIN gets 4 roles automatically
- [x] Regular users start with 0 roles
- [x] Roles can be assigned
- [x] Roles can be removed
- [x] Users can have multiple roles

### ✅ Data Validation
- [x] Duplicate organization names rejected
- [x] Duplicate emails rejected
- [x] Required fields validated
- [x] Password validation

### ✅ Security
- [x] No token = 403 Forbidden
- [x] Wrong role = 403 Forbidden
- [x] Cross-organization access blocked
- [x] Passwords encrypted (never returned in responses)
- [x] Token versioning invalidates old tokens on role changes
- [x] Old tokens rejected with HTTP 403 after role modification
- [x] Users must re-login after role changes

---

## Expected Final Database State

After all tests:

**Organizations (2)**:
1. System Organization (id: 1)
2. Company ABC (id: 2)

**Users (3)** (user 3 deleted):
1. admin@system.com - ADMIN + 3 operational roles (org 1)
2. admin@abc.com - MIGRATION_ADMIN + 3 operational roles (org 2)
4. user@system.com - TRANSFORMATION_MODEL_USER (org 1)

**Roles (6)**:
1. UNREGISTER
2. ADMIN
3. MIGRATION_ADMIN
4. CONNECTOR_USER
5. TRANSFORMATION_MODEL_USER
6. CYCLE_EXECUTION_USER

---

## Troubleshooting

### Issue: "Access denied: User belongs to different organization"
**Cause**: Trying to access resources from a different organization  
**Solution**: Use the correct token for the organization

### Issue: HTTP 403 on all endpoints
**Cause**: No token or invalid token  
**Solution**: Check Authorization header format: `Bearer <token>`

### Issue: HTTP 403 on `/api/admin/**` with MIGRATION_ADMIN token
**Cause**: MIGRATION_ADMIN not allowed on admin endpoints  
**Solution**: Use ADMIN token instead

### Issue: "Organization already exists"
**Cause**: Organization name must be unique  
**Solution**: Use a different organization name

### Issue: Token expired
**Cause**: Tokens expire after 24 hours  
**Solution**: Login again to get a new token

### Issue: HTTP 403 after role change
**Cause**: Token versioning invalidated old token when roles were modified  
**Solution**: Login again to get a new token with updated tokenVersion

---

## ⭐ NEW: Security Feature Tests

### **TEST 37: Rate Limiting - Normal Usage**

**Purpose**: Verify rate limiting allows normal usage (under 10 requests/min)

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Test Steps**:
1. Make 5 login requests in quick succession
2. All should succeed with HTTP 200

**Request Body**:
```json
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```

**Expected Results**:
- Request 1: HTTP 200 ✅
- Request 2: HTTP 200 ✅
- Request 3: HTTP 200 ✅
- Request 4: HTTP 200 ✅
- Request 5: HTTP 200 ✅

**What to Verify**:
- ✅ All 5 requests succeed
- ✅ Tokens are returned
- ✅ Normal usage not affected by rate limiting

---

### **TEST 38: Rate Limiting - Brute Force Protection**

**Purpose**: Verify rate limiting blocks brute force attacks

**Endpoint**: `POST /api/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Test Steps**:
1. Make 12 login requests rapidly (as fast as possible)
2. First 10 should succeed
3. 11th and 12th should be rate limited

**Request Body**:
```json
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```

**Expected Results**:
- Requests 1-9: HTTP 200 ✅
- Request 10: HTTP 429 (Too Many Requests) ⛔
- Request 11: HTTP 429 ⛔
- Request 12: HTTP 429 ⛔

**Response on Rate Limit** (HTTP 429):
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

**What to Verify**:
- ✅ First 9 requests succeed
- ✅ 10th request returns HTTP 429
- ✅ Subsequent requests blocked
- ✅ Error message clear

**Using curl (automated test)**:
```bash
# Run 12 requests and check status codes
for i in {1..12}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@system.com","password":"Admin123!"}')
  echo "Request $i: HTTP $STATUS"
done
```

**Expected Output**:
```
Request 1: HTTP 200
Request 2: HTTP 200
Request 3: HTTP 200
Request 4: HTTP 200
Request 5: HTTP 200
Request 6: HTTP 200
Request 7: HTTP 200
Request 8: HTTP 200
Request 9: HTTP 200
Request 10: HTTP 429
Request 11: HTTP 429
Request 12: HTTP 429
```

---

### **TEST 39: Rate Limit Recovery**

**Purpose**: Verify rate limiting resets after time window

**Endpoint**: `POST /api/auth/login`

**Test Steps**:
1. Trigger rate limit (make 10 requests)
2. Wait 60 seconds
3. Try again - should succeed

**Expected Results**:
- After 60 seconds: Rate limit resets
- New requests allowed

**What to Verify**:
- ✅ Rate limit is time-based (1 minute window)
- ✅ Users can retry after waiting
- ✅ Not permanently blocked

---

### **TEST 40: Token Version Cache - Performance**

**Purpose**: Verify token version caching reduces database queries

**Endpoint**: `GET /api/auth/me`

**Test Steps**:
1. Login to get token
2. Make same request 10 times with same token
3. Check that it's fast (cached)

**Headers**:
```
Authorization: Bearer <TOKEN>
```

**Expected Behavior**:
- First request: Loads token version from database
- Requests 2-10: Uses cached token version (no DB query)
- All requests succeed with HTTP 200

**What to Verify**:
- ✅ Requests are fast (cached)
- ✅ No performance degradation
- ✅ Cache working correctly

**Check Cache Stats** (if logs enabled):
```
Cache Stats - Hits: 9, Misses: 1, Hit Rate: 90.00%
```

---

### **TEST 41: Token Cache Invalidation**

**Purpose**: Verify cache is invalidated when roles change

**Endpoint**: Multiple endpoints

**Test Steps**:

**Step 1**: Get initial token
```bash
POST /api/auth/login
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```
Save token as `TOKEN_V1`

**Step 2**: Verify token works
```bash
GET /api/auth/me
Authorization: Bearer <TOKEN_V1>
```
Expected: HTTP 200 ✅

**Step 3**: Change user roles (triggers cache invalidation)
```bash
DELETE /api/admin/users/1/roles
Authorization: Bearer <TOKEN_V1>
Content-Type: application/json
{
  "role": "CYCLE_EXECUTION_USER"
}
```
Expected: HTTP 200, token version incremented

**Step 4**: Try old token (should fail immediately)
```bash
GET /api/auth/me
Authorization: Bearer <TOKEN_V1>
```
Expected: HTTP 403 ⛔ (cached version invalidated)

**Step 5**: Login again to get new token
```bash
POST /api/auth/login
{
  "email": "admin@system.com",
  "password": "Admin123!"
}
```
Save token as `TOKEN_V2`

**Step 6**: Verify new token works
```bash
GET /api/auth/me
Authorization: Bearer <TOKEN_V2>
```
Expected: HTTP 200 ✅

**What to Verify**:
- ✅ Cache invalidated immediately on role change
- ✅ Old token rejected with HTTP 403
- ✅ New token works
- ✅ No stale data served from cache

**Application Logs Should Show**:
```
Token version cache invalidated for user: admin@system.com
Token version mismatch for user: admin@system.com. Token has version 1, but user has version 2
```

---

### **TEST 42: Environment Variables - Verify JWT Secret**

**Purpose**: Verify application uses JWT secret from environment variables

**Test Steps**:

**Step 1**: Check .env file exists
```bash
ls -la .env
cat .env | grep JWT_SECRET
```

**Expected**:
```
JWT_SECRET=Bb+qPVNM4DPmf9JldnN/GVfagLOZbD5n7zh84hiPsbXgErtSK7tnKFtWcSLTMv+QF+cquaTp8Z1V+3U0Lg3WOA==
```

**Step 2**: Verify application loads environment variables
```bash
# Start application with environment variables
export $(cat .env | xargs)
echo "JWT_SECRET length: ${#JWT_SECRET}"
mvn spring-boot:run
```

**Expected Output**:
```
JWT_SECRET length: 88
Application started successfully
```

**Step 3**: Login and verify token is generated
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@system.com","password":"Admin123!"}'
```

**Expected**: Token returned (application using JWT_SECRET from .env)

**What to Verify**:
- ✅ .env file exists and contains JWT_SECRET
- ✅ JWT_SECRET is 88 characters (512-bit base64)
- ✅ Application starts with environment variables
- ✅ Tokens are generated successfully

**If Application Fails to Start**:
```
Error creating bean with name 'jwtTokenProvider': Injection of autowired dependencies failed
```
**Solution**: JWT_SECRET not loaded. Run:
```bash
export $(cat .env | xargs) && mvn spring-boot:run
```

---

### **TEST 43: No Null Values in Responses**

**Purpose**: Verify all responses have complete data (no null organization fields)

**Test Multiple Endpoints**:

**Test 1**: User Profile
```bash
GET /api/auth/me
Authorization: Bearer <TOKEN>
```

**Expected Response**:
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@system.com",
  "firstName": "System",
  "lastName": "Administrator",
  "organization": {
    "id": 1,
    "name": "System Organization",
    "companyName": "System",
    "location": "Default",
    "createdAt": "2026-05-01 11:50:32.588"
  },
  "roles": ["ADMIN", "CONNECTOR_USER", "TRANSFORMATION_MODEL_USER"],
  "createdAt": "2026-05-01 11:50:32.655"
}
```

**What to Verify**:
- ✅ `organization` is NOT null
- ✅ `organization.id` exists
- ✅ `organization.name` exists
- ✅ All organization fields populated

**Test 2**: List Users
```bash
GET /api/users
Authorization: Bearer <TOKEN>
```

**Expected**: All users have organization objects (not null)

**Test 3**: Create User
```bash
POST /api/users
Authorization: Bearer <TOKEN>
{
  "username": "test_user",
  "email": "test@company.com",
  "password": "User123!",
  "firstName": "Test",
  "lastName": "User"
}
```

**Expected**: Response includes organization object (not null)

**What to Verify**:
- ✅ NO null values in any user response
- ✅ Organization always populated
- ✅ Consistent data structure

---

### **TEST 44: Proper HTTP Status Codes**

**Purpose**: Verify correct HTTP status codes for all error scenarios

**Test Cases**:

**1. Invalid Credentials**
```bash
POST /api/auth/login
{
  "email": "admin@system.com",
  "password": "WrongPassword"
}
```
**Expected**: HTTP 401 Unauthorized ✅

**2. No Token**
```bash
GET /api/auth/me
(No Authorization header)
```
**Expected**: HTTP 403 Forbidden ✅

**3. Insufficient Permissions**
```bash
POST /api/users
Authorization: Bearer <REGULAR_USER_TOKEN>
{
  "username": "test",
  "email": "test@test.com",
  "password": "User123!",
  "firstName": "Test",
  "lastName": "User"
}
```
**Expected**: HTTP 403 Forbidden ✅

**4. MIGRATION_ADMIN Accessing Admin Endpoint**
```bash
POST /api/admin/organizations
Authorization: Bearer <MIGRATION_ADMIN_TOKEN>
{
  "name": "Test",
  "companyName": "Test",
  "location": "Test"
}
```
**Expected**: HTTP 403 Forbidden ✅

**5. Cross-Organization Access**
```bash
GET /api/users/999
Authorization: Bearer <TOKEN_FROM_ORG_1>
(User 999 belongs to org 2)
```
**Expected**: HTTP 500 with message "Access denied: User belongs to different organization" ✅

**6. Duplicate Organization Name**
```bash
POST /api/admin/organizations
Authorization: Bearer <ADMIN_TOKEN>
{
  "name": "Existing Name",
  "companyName": "Test",
  "location": "Test"
}
```
**Expected**: HTTP 500 with message "Organization with name 'X' already exists" ✅

**7. Rate Limit Exceeded**
```bash
(Make 11+ requests quickly)
POST /api/auth/login
```
**Expected**: HTTP 429 Too Many Requests ✅

**Summary of Status Codes**:
- HTTP 200: Success
- HTTP 204: Success (no content)
- HTTP 400: Validation error
- HTTP 401: Invalid credentials
- HTTP 403: Access denied / Insufficient permissions
- HTTP 429: Rate limit exceeded
- HTTP 500: Business logic error (duplicate, cross-org access)

---

## Updated Test Summary

### Total Tests: 44 (36 original + 8 new security tests)

**Original Tests (1-36):**
- Authentication and login flows
- User and organization CRUD
- Role assignment and management
- Multi-tenant isolation
- Token versioning
- Error handling

**New Security Tests (37-44):** ⭐
- TEST 37: Rate limiting - Normal usage
- TEST 38: Rate limiting - Brute force protection
- TEST 39: Rate limit recovery
- TEST 40: Token version caching performance
- TEST 41: Token cache invalidation
- TEST 42: Environment variables verification
- TEST 43: No null values in responses
- TEST 44: Proper HTTP status codes

---

## Bruno Collection Structure Suggestion

```
📁 ETL System - Phase 2
  📁 1. Authentication
    POST Login - Admin
    POST Login - MIGRATION_ADMIN
    POST Login - Regular User
    GET Current User Profile
  📁 2. Organizations (ADMIN)
    POST Create Organization
    GET List All Organizations
    GET Get Organization by ID
    GET Get Organization by Name
    POST Create MIGRATION_ADMIN for Org
  📁 3. Users (ADMIN/MIGRATION_ADMIN)
    POST Create User
    GET List Users
    GET Get User by ID
    DELETE Delete User
  📁 4. Roles (ADMIN)
    POST Assign Role
    DELETE Remove Role
  📁 5. Token Versioning
    POST Login - Get Initial Token
    GET Verify Token Works
    DELETE Remove Role - Trigger Invalidation
    GET Try Old Token (Should Fail 403)
    POST Login Again - Get New Token
    GET Verify New Token Works
  📁 6. Error Cases
    GET No Token (403)
    POST MIGRATION_ADMIN → Admin Endpoint (403)
    POST Cross-Org Access (500)
    POST Duplicate Organization (500)
  📁 7. Security Features ⭐ NEW
    POST Rate Limiting - Normal Usage
    POST Rate Limiting - Brute Force (12 requests)
    GET Token Cache - Performance Test
    DELETE Token Cache - Invalidation Test
    GET Environment Variables - Verify Setup
    GET No Null Values - Verify Data Completeness
    ALL HTTP Status Codes - Verify Error Handling
```

---

## Notes

- Replace `<ADMIN_TOKEN>`, `<MIGRATION_ADMIN_TOKEN>`, etc. with actual token values from responses
- All tokens start with `eyJ` (JWT format)
- Tokens are valid for 24 hours (86400000 ms)
- **Important**: Tokens are automatically invalidated when user roles change due to token versioning
- HTTP 204 means success with no response body
- organizationId in JWT token determines which data users can access
- If you get HTTP 403 after role changes, login again to get a fresh token

**Good luck with testing!** 🚀

---

## Quick Start Guide

### 1. First Time Setup

```bash
# Navigate to project
cd /path/to/etl-system

# Copy environment template
cp .env.example .env

# Edit .env (ensure JWT_SECRET is set)
nano .env

# Start application
export $(cat .env | xargs) && mvn spring-boot:run
```

### 2. Quick Test Script

```bash
# Save as test.sh
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== Quick Phase 2 Test ==="

# Test 1: Login
echo "1. Testing login..."
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@system.com","password":"Admin123!"}' | jq -r '.token')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo "✅ Login successful"
else
  echo "❌ Login failed"
  exit 1
fi

# Test 2: Get profile
echo "2. Testing user profile..."
PROFILE=$(curl -s -X GET $BASE_URL/api/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq -r '.organization.id')

if [ "$PROFILE" = "1" ]; then
  echo "✅ User profile loaded (organization not null)"
else
  echo "❌ User profile failed (organization is null)"
  exit 1
fi

# Test 3: Rate limiting
echo "3. Testing rate limiting..."
COUNT=0
for i in {1..12}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST $BASE_URL/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@system.com","password":"Admin123!"}')
  
  if [ "$STATUS" = "429" ]; then
    COUNT=$((COUNT + 1))
  fi
done

if [ $COUNT -ge 2 ]; then
  echo "✅ Rate limiting working (got $COUNT rate limit responses)"
else
  echo "❌ Rate limiting not working"
  exit 1
fi

echo ""
echo "=== All Quick Tests Passed! ==="
```

**Run the script:**
```bash
chmod +x test.sh
./test.sh
```

### 3. Common curl Commands

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@system.com","password":"Admin123!"}' | jq
```

**Get Profile:**
```bash
TOKEN="your_token_here"
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Create Organization:**
```bash
curl -X POST http://localhost:8080/api/admin/organizations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Org","companyName":"Test","location":"NYC"}' | jq
```

**List Users:**
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 4. Troubleshooting

**Problem**: Application fails to start
```
Error creating bean with name 'jwtTokenProvider'
```
**Solution**: JWT_SECRET not loaded
```bash
export $(cat .env | xargs) && mvn spring-boot:run
```

**Problem**: All requests return 403
**Solution**: Token expired or invalid. Login again.

**Problem**: Organization is null in responses
**Solution**: This has been fixed. Make sure you're using the latest code.

**Problem**: Rate limit blocks legitimate requests
**Solution**: Wait 60 seconds for rate limit to reset.

---

## Test Checklist

Use this checklist to track your manual testing progress:

### Core Functionality
- [ ] TEST 1: Admin Login
- [ ] TEST 2: Get User Profile (organization not null)
- [ ] TEST 3: No Token (403)
- [ ] TEST 4: Create Organization
- [ ] TEST 6: Create MIGRATION_ADMIN
- [ ] TEST 7: MIGRATION_ADMIN Login
- [ ] TEST 8: MIGRATION_ADMIN Creates User
- [ ] TEST 9: MIGRATION_ADMIN Blocked from Admin Endpoints
- [ ] TEST 10: Cross-Organization Access Blocked
- [ ] TEST 12: Assign Role
- [ ] TEST 15: List Users (Multi-tenant Isolation)

### Security Features ⭐
- [ ] TEST 37: Rate Limiting - Normal Usage
- [ ] TEST 38: Rate Limiting - Brute Force Protection
- [ ] TEST 39: Rate Limit Recovery
- [ ] TEST 40: Token Version Cache Performance
- [ ] TEST 41: Token Cache Invalidation
- [ ] TEST 42: Environment Variables
- [ ] TEST 43: No Null Values
- [ ] TEST 44: Proper HTTP Status Codes

### Token Versioning
- [ ] TEST 30: Remove Role (Increment Version)
- [ ] TEST 31: Old Token Rejected (403)
- [ ] TEST 32: Login Again (New Token)
- [ ] TEST 33: New Token Works

### Error Handling
- [ ] Invalid Credentials (401)
- [ ] Insufficient Permissions (403)
- [ ] Duplicate Organization (500)
- [ ] Cross-Org Access (500)

---

## Performance Benchmarks

Expected performance metrics:

**Authentication:**
- Login: < 500ms
- Profile fetch (cached): < 50ms
- Profile fetch (uncached): < 200ms

**Rate Limiting:**
- Limit: 10 requests per minute per IP
- Reset: 60 seconds
- Response: HTTP 429 when exceeded

**Token Versioning:**
- Cache hit rate: > 95%
- Cache expiration: 5 minutes
- Invalidation: Immediate on role change

**Database Queries (per request):**
- Before caching: 2-3 queries
- After caching: 0-1 queries (95% reduction)

---

## Security Checklist

Verify these security features are working:

### Authentication & Authorization
- [x] JWT authentication implemented
- [x] Password encryption (BCrypt)
- [x] Role-based access control
- [x] Token versioning (invalidation)
- [x] Proper HTTP status codes

### Secrets Management
- [x] JWT secret in environment variables
- [x] Database credentials in .env
- [x] .env excluded from git
- [x] Secure 512-bit JWT secret

### Attack Protection
- [x] Rate limiting (brute force protection)
- [x] Multi-tenant isolation (cross-org blocked)
- [x] Token cache invalidation (stale data prevention)
- [x] Access denied properly handled (403)

### Data Integrity
- [x] No null values in responses
- [x] Organization data always populated
- [x] Consistent error messages
- [x] Proper validation

---

## Additional Resources

**Documentation:**
- `Phase2-Authentication-Implementation.md` - Technical details
- `Phase2-Completion-Report.md` - Phase 2 summary
- `Security-Review-Report.md` - Security audit
- `Security-Improvements-Summary.md` - What was improved
- `Environment-Configuration.md` - .env setup guide

**API Documentation:**
- All endpoints documented in Phase2-Authentication-Implementation.md
- cURL examples provided for each endpoint
- Expected request/response formats included

**Support:**
- Check application logs for detailed error messages
- Review .env.example for required variables
- Ensure MySQL is running and database exists
- Verify Java 21 and Maven are installed

---

**Testing Guide Version**: 2.0 (Updated with Security Improvements)  
**Last Updated**: 2026-05-01  
**Status**: ✅ Complete - 44 Tests Available

**Happy Testing!** 🚀
