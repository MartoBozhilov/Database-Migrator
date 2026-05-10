# Phase 4 - Complete Manual Testing Guide: Transformation System

## Prerequisites

### 1. Complete Phase 3 First

Before testing Phase 4, you must complete Phase 3 setup:
- ✅ Application running on port 8080
- ✅ JWT token obtained from login
- ✅ SOURCE connector created (ID saved)
- ✅ TARGET connector created (ID saved)
- ✅ System scan completed successfully (ID saved)

**If Phase 3 is not complete**, follow `Phase3-Manual-Testing-Guide.md` first.

### 2. Required Data from Phase 3

You will need these IDs:
- **JWT Token**: From login endpoint
- **Source Connector ID**: SOURCE type connector (e.g., ID: 1)
- **Target Connector ID**: TARGET type connector (e.g., ID: 2)
- **System Scan ID**: COMPLETED status (e.g., ID: 1)

### 3. Environment

- **Base URL**: `http://localhost:8080`
- **Database**: school_system (5 tables: courses, departments, teachers, students, enrollments)
- **User Role**: TRANSFORMATION_MODEL_USER (or MIGRATION_ADMIN)
- **Test Organization**: Test Organization

---

## Test Scenario Overview

This guide tests **Phase 4 - Complete Transformation System**:

### Features Covered:
1. ✅ **Transformation Model Management** - CRUD operations
2. ✅ **Table Transformations** - RENAME, ADD, DELETE
3. ✅ **Column Transformations** - RENAME, CHANGE_TYPE, ADD, DELETE
4. ✅ **Relation Transformations** - ADD, DELETE foreign keys
5. ✅ **Model Confirmation** - DAG validation and immutability
6. ✅ **Type Mapping Validation** - Cross-database type compatibility
7. ✅ **SQL Identifier Validation** - Reserved keywords, patterns
8. ✅ **Auto-Update References** - Table/column renames cascade to relations
9. ✅ **Multi-Tenant Isolation** - Organization boundaries

---

## Section 1: Transformation Model Management

### **TEST 1: Create Transformation Model**

**Purpose**: Create a transformation model that auto-includes all scan metadata

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "name": "School System Migration",
  "systemScanId": 1,
  "targetConnectorId": 2
}
```

**Expected Response** (200 OK):
```json
{
  "id": 1,
  "name": "School System Migration",
  "isConfirmed": false,
  "systemScanId": 1,
  "systemScanName": "School System Initial Scan",
  "targetConnectorId": 2,
  "targetConnectorName": "MySQL Target Database",
  "targetDatabaseType": "MYSQL",
  "createdById": 3,
  "createdByName": "connector_user",
  "createdAt": "2026-05-03T20:15:00.000Z",
  "tableCount": 5,
  "columnCount": 34,
  "tables": [...],
  "relations": [...]
}
```

**Validation**:
- ✅ Status code is 200
- ✅ isConfirmed is false (editable state)
- ✅ tableCount is 5 (auto-included)
- ✅ columnCount is 34 (auto-included)
- ✅ All tables from scan auto-included
- ✅ All columns from scan auto-included
- ✅ All relations from scan auto-included

**Save**: Transformation Model ID = 1

---

### **TEST 2: Get Transformation Model Details**

**Purpose**: Verify complete model structure with all metadata

**Endpoint**: 
```
GET http://localhost:8080/api/transformation-models/1/details
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
{
  "id": 1,
  "name": "School System Migration",
  "isConfirmed": false,
  "systemScanId": 1,
  "targetConnectorId": 2,
  "targetDatabaseType": "MYSQL",
  "tableCount": 5,
  "columnCount": 34,
  "tables": [
    {
      "id": 1,
      "sourceTableName": "courses",
      "sourceTableMetadataId": 1,
      "tableTransformations": [],
      "columns": [
        {
          "id": 1,
          "sourceColumnName": "id",
          "sourceDataType": "bigint",
          "sourceColumnMetadataId": 1,
          "resolvedTargetType": "bigint",
          "columnTransformations": []
        }
      ]
    }
  ],
  "relations": [
    {
      "id": 1,
      "sourceRelationMetadataId": 1,
      "isDeleted": false,
      "foreignTable": "courses",
      "foreignColumn": "department_id",
      "primaryTable": "departments",
      "primaryColumn": "id"
    }
  ]
}
```

**Validation**:
- ✅ All 5 tables included
- ✅ All columns have resolvedTargetType (auto-resolved)
- ✅ All relations included with isDeleted: false
- ✅ No transformations applied yet (empty arrays)

---

### **TEST 3: List All Transformation Models**

**Endpoint**: 
```
GET http://localhost:8080/api/transformation-models
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "School System Migration",
    "isConfirmed": false,
    "systemScanId": 1,
    "targetConnectorId": 2,
    "tableCount": 5,
    "columnCount": 34
  }
]
```

**Validation**:
- ✅ Only models from current organization shown
- ✅ Summary view (no nested tables/columns)

---

### **TEST 4: Update Transformation Model Name**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "name": "School System Migration v2"
}
```

**Expected Response** (200 OK):
```json
{
  "id": 1,
  "name": "School System Migration v2",
  "isConfirmed": false,
  ...
}
```

**Validation**:
- ✅ Name updated successfully
- ✅ isConfirmed still false
- ✅ systemScanId unchanged (immutable)
- ✅ targetConnectorId unchanged (immutable)

---

## Section 2: Table Transformations

### **TEST 5: Rename Table**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/rename
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "newName": "course_catalog"
}
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "id": 1,
      "sourceTableName": "courses",
      "tableTransformations": [
        {
          "id": 1,
          "transformationType": "RENAME_TABLE",
          "newName": "course_catalog",
          "createdAt": "2026-05-03T20:16:00.000Z"
        }
      ]
    }
  ],
  "relations": [
    {
      "foreignTable": "course_catalog",
      "foreignColumn": "department_id",
      "primaryTable": "departments",
      "primaryColumn": "id"
    }
  ]
}
```

**Validation**:
- ✅ RENAME_TABLE transformation applied
- ✅ All relations auto-updated to new table name
- ✅ sourceTableName unchanged (original preserved)

---

### **TEST 6: Rename Table - Reserved Keyword (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/2/rename
```

**Request Body**:
```json
{
  "newName": "SELECT"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "error": "Internal Server Error",
  "message": "Identifier 'SELECT' is a reserved keyword in MYSQL",
  "status": 500
}
```

**Validation**:
- ✅ Reserved keyword rejected
- ✅ Transformation NOT applied

---

### **TEST 7: Rename Table - Invalid Characters (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/2/rename
```

**Request Body**:
```json
{
  "newName": "dept-table!"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Identifier 'dept-table!' is invalid. Must start with letter or underscore and contain only letters, numbers, and underscores"
}
```

**Validation**:
- ✅ Invalid pattern rejected
- ✅ Only alphanumeric and underscores allowed

---

### **TEST 8: Rename Table - Duplicate Name (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/3/rename
```

**Request Body**:
```json
{
  "newName": "course_catalog"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Table with name 'course_catalog' already exists in this transformation model"
}
```

**Validation**:
- ✅ Duplicate name rejected (case-insensitive)
- ✅ Includes renamed tables in check

---

### **TEST 9: Add New Table**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/tables
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "tableName": "audit_logs",
  "idGenerationStrategy": "AUTO_INCREMENT",
  "idColumnName": "log_id",
  "idColumnDataType": "BIGINT"
}
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "id": 6,
      "sourceTableName": null,
      "sourceTableMetadataId": null,
      "tableTransformations": [
        {
          "transformationType": "ADD_TABLE",
          "tableName": "audit_logs",
          "idGenerationStrategy": "AUTO_INCREMENT"
        }
      ],
      "columns": [
        {
          "id": 35,
          "sourceColumnName": null,
          "columnTransformations": [
            {
              "transformationType": "ADD_COLUMN",
              "columnName": "log_id",
              "dataType": "BIGINT",
              "isNullable": false,
              "isPrimaryKey": true
            }
          ]
        }
      ]
    }
  ]
}
```

**Validation**:
- ✅ New table created with sourceTableMetadataId = null
- ✅ ADD_TABLE transformation applied
- ✅ ID column auto-created with ADD_COLUMN
- ✅ tableCount increased to 6

---

### **TEST 10: Add Table with UUID Primary Key**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/tables
```

**Request Body**:
```json
{
  "tableName": "session_logs",
  "idGenerationStrategy": "UUID",
  "idColumnName": "session_id",
  "idColumnDataType": "VARCHAR(36)"
}
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "tableTransformations": [
        {
          "transformationType": "ADD_TABLE",
          "tableName": "session_logs",
          "idGenerationStrategy": "UUID"
        }
      ],
      "columns": [
        {
          "columnTransformations": [
            {
              "columnName": "session_id",
              "dataType": "VARCHAR(36)",
              "isPrimaryKey": true
            }
          ]
        }
      ]
    }
  ]
}
```

**Validation**:
- ✅ UUID strategy supported
- ✅ VARCHAR(36) ID column created

---

### **TEST 11: Delete Table with FK Dependencies (Should Fail)**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/tables/2
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot delete table 'departments' because it has 2 active foreign key relationship(s). Please delete the following relationship(s) first:\n\nPrimary Key Relationships (other tables reference this table):\n  - Column 'id' is referenced by course_catalog.department_id\n  - Column 'id' is referenced by teachers.department_id\n"
}
```

**Validation**:
- ✅ Deletion blocked by FK dependencies
- ✅ Error lists all blocking relationships
- ✅ Shows both FK and PK sides

---

### **TEST 12: Delete Table (No Dependencies)**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/tables/6
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "id": 6,
      "tableTransformations": [
        {
          "transformationType": "ADD_TABLE",
          "tableName": "audit_logs"
        },
        {
          "transformationType": "DELETE_TABLE",
          "createdAt": "2026-05-03T20:20:00.000Z"
        }
      ]
    }
  ]
}
```

**Validation**:
- ✅ DELETE_TABLE transformation added
- ✅ Table soft-deleted (still visible)
- ✅ Phase 5 will skip this table

---

## Section 3: Column Transformations

### **TEST 13: Rename Column**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/columns/2/rename
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "newName": "course_code_number"
}
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "columns": [
        {
          "id": 2,
          "sourceColumnName": "course_code",
          "columnTransformations": [
            {
              "transformationType": "RENAME_COLUMN",
              "newName": "course_code_number"
            }
          ]
        }
      ]
    }
  ],
  "relations": [
    {
      "foreignTable": "course_catalog",
      "foreignColumn": "course_code_number",
      "primaryTable": "...",
      "primaryColumn": "..."
    }
  ]
}
```

**Validation**:
- ✅ RENAME_COLUMN transformation applied
- ✅ Relations auto-updated if column is in FK
- ✅ sourceColumnName unchanged

---

### **TEST 14: Rename Column - Reserved Keyword (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/columns/3/rename
```

**Request Body**:
```json
{
  "newName": "WHERE"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Identifier 'WHERE' is a reserved keyword in MYSQL"
}
```

**Validation**:
- ✅ Reserved keyword rejected

---

### **TEST 15: Rename Column - Duplicate Name (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/columns/4/rename
```

**Request Body**:
```json
{
  "newName": "course_code_number"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Column with name 'course_code_number' already exists in this table"
}
```

**Validation**:
- ✅ Duplicate column name rejected (case-insensitive)

---

### **TEST 16: Change Column Type**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/columns/2/change-type
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "targetDataType": "text"
}
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "columns": [
        {
          "id": 2,
          "sourceDataType": "varchar",
          "resolvedTargetType": "text",
          "columnTransformations": [
            {
              "transformationType": "CHANGE_TYPE",
              "targetDataType": "text"
            }
          ]
        }
      ]
    }
  ]
}
```

**Validation**:
- ✅ CHANGE_TYPE transformation applied
- ✅ resolvedTargetType updated
- ✅ Type compatibility validated

---

### **TEST 17: Change Column Type - Invalid Conversion (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/columns/1/change-type
```

**Request Body**:
```json
{
  "targetDataType": "invalid_type"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Invalid type conversion: POSTGRESQL bigint -> MYSQL invalid_type. Check allowed type mappings."
}
```

**Validation**:
- ✅ Invalid type conversion rejected
- ✅ Type mapping validated

---

### **TEST 18: Add Column**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/tables/1/columns
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "columnName": "created_at",
  "dataType": "TIMESTAMP",
  "isNullable": false,
  "isPrimaryKey": false,
  "defaultValue": "CURRENT_TIMESTAMP"
}
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "columns": [
        {
          "id": 36,
          "sourceColumnName": null,
          "resolvedTargetType": "timestamp",
          "columnTransformations": [
            {
              "transformationType": "ADD_COLUMN",
              "columnName": "created_at",
              "dataType": "TIMESTAMP",
              "isNullable": false,
              "isPrimaryKey": false,
              "defaultValue": "CURRENT_TIMESTAMP"
            }
          ]
        }
      ]
    }
  ]
}
```

**Validation**:
- ✅ New column created with sourceColumnName = null
- ✅ ADD_COLUMN transformation applied
- ✅ resolvedTargetType set

---

### **TEST 19: Add Column - Invalid Type (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/tables/1/columns
```

**Request Body**:
```json
{
  "columnName": "temp_col",
  "dataType": "INVALID_TYPE",
  "isNullable": true,
  "isPrimaryKey": false
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Invalid data type 'INVALID_TYPE' for target database MYSQL. Please use a valid type for this database."
}
```

**Validation**:
- ✅ Invalid target type rejected
- ✅ Type validated against target database

---

### **TEST 20: Delete Column - Primary Key in FK (Should Fail)**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/tables/2/columns/1
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot delete column 'id' because it is referenced as a primary key in foreign key relationships. Please delete the foreign key relationships first."
}
```

**Validation**:
- ✅ Cannot delete PK column referenced by FK
- ✅ Error explains reason

---

### **TEST 21: Delete Column - Used in FK (Should Fail)**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/tables/1/columns/3
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot delete column 'department_id' because it is part of a foreign key relationship. Please delete the foreign key relationship first."
}
```

**Validation**:
- ✅ Cannot delete FK column
- ✅ Must delete relationship first

---

### **TEST 22: Delete Column - NOT NULL without Default (Should Fail)**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/tables/1/columns/2
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot delete NOT NULL column 'course_code' without a default value. Either add a DEFAULT_VALUE transformation first, or make the column nullable."
}
```

**Validation**:
- ✅ Cannot delete NOT NULL column without default
- ✅ Data integrity protected

---

### **TEST 23: Delete Column (User-Created)**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/tables/1/columns/36
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
{
  "tables": [
    {
      "columns": [
        {
          "id": 36,
          "columnTransformations": [
            {
              "transformationType": "ADD_COLUMN",
              "columnName": "created_at"
            },
            {
              "transformationType": "DELETE_COLUMN"
            }
          ]
        }
      ]
    }
  ]
}
```

**Validation**:
- ✅ User-created columns can be deleted
- ✅ DELETE_COLUMN transformation added

---

## Section 4: Relation Transformations

### **TEST 24: Add Relation (User-Defined FK)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/relations
```

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "foreignTable": "audit_logs",
  "foreignColumn": "user_id",
  "primaryTable": "students",
  "primaryColumn": "id"
}
```

**Expected Response** (200 OK):
```json
{
  "relations": [
    {
      "id": 6,
      "sourceRelationMetadataId": null,
      "isDeleted": false,
      "foreignTable": "audit_logs",
      "foreignColumn": "user_id",
      "primaryTable": "students",
      "primaryColumn": "id"
    }
  ]
}
```

**Validation**:
- ✅ New relation created with sourceRelationMetadataId = null
- ✅ isDeleted is false
- ✅ User-defined FK added

---

### **TEST 25: Add Relation - Table Not Found (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/relations
```

**Request Body**:
```json
{
  "foreignTable": "nonexistent_table",
  "foreignColumn": "col1",
  "primaryTable": "students",
  "primaryColumn": "id"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Foreign table 'nonexistent_table' not found in transformation model"
}
```

**Validation**:
- ✅ Table existence validated
- ✅ Only included tables allowed

---

### **TEST 26: Add Relation - Duplicate (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/relations
```

**Request Body**:
```json
{
  "foreignTable": "course_catalog",
  "foreignColumn": "department_id",
  "primaryTable": "departments",
  "primaryColumn": "id"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Relation already exists between course_catalog.department_id and departments.id"
}
```

**Validation**:
- ✅ Duplicate relations prevented
- ✅ Case-insensitive check

---

### **TEST 27: Delete Relation**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/relations/3
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
{
  "relations": [
    {
      "id": 3,
      "isDeleted": true,
      "foreignTable": "students",
      "foreignColumn": "department_id",
      "primaryTable": "departments",
      "primaryColumn": "id"
    }
  ]
}
```

**Validation**:
- ✅ Relation soft-deleted (isDeleted = true)
- ✅ Still visible in response
- ✅ Excluded from DAG validation

---

## Section 5: Model Confirmation & Immutability

### **TEST 28: Confirm Transformation Model (With Cycle - Should Fail)**

**Purpose**: Test DAG cycle detection

**Setup**: First, create a cycle by adding relations that form a loop:
1. students → enrollments
2. enrollments → courses
3. courses → students (creates cycle)

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/confirm
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Transformation model validation failed:\nCycle detected in relationships: students -> enrollments -> courses -> students"
}
```

**Validation**:
- ✅ Cycle detected by DAG algorithm
- ✅ Error shows cycle path
- ✅ Model NOT confirmed
- ✅ isConfirmed remains false

---

### **TEST 29: Fix Cycle by Deleting Relation**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1/relations/7
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
{
  "relations": [
    {
      "id": 7,
      "isDeleted": true,
      "foreignTable": "courses",
      "foreignColumn": "student_id",
      "primaryTable": "students",
      "primaryColumn": "id"
    }
  ]
}
```

**Validation**:
- ✅ Relation deleted
- ✅ Cycle broken

---

### **TEST 30: Confirm Transformation Model (Success)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/confirm
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (200 OK):
```json
{
  "id": 1,
  "name": "School System Migration v2",
  "isConfirmed": true,
  "systemScanId": 1,
  "targetConnectorId": 2,
  ...
}
```

**Validation**:
- ✅ Status code is 200
- ✅ isConfirmed is true
- ✅ No validation errors
- ✅ DAG validation passed
- ✅ Model is now immutable

---

### **TEST 31: Try to Rename Table on Confirmed Model (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1/tables/1/rename
```

**Request Body**:
```json
{
  "newName": "new_table_name"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot modify confirmed transformation model. Model is immutable after confirmation."
}
```

**Validation**:
- ✅ Modification blocked
- ✅ Model is immutable
- ✅ All transformation operations blocked

---

### **TEST 32: Try to Update Model Name on Confirmed Model (Should Fail)**

**Endpoint**: 
```
PUT http://localhost:8080/api/transformation-models/1
```

**Request Body**:
```json
{
  "name": "New Name"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot modify confirmed transformation model. Model is immutable after confirmation."
}
```

**Validation**:
- ✅ Name change blocked
- ✅ Confirmed models cannot be edited

---

### **TEST 33: Try to Add Column on Confirmed Model (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/tables/1/columns
```

**Request Body**:
```json
{
  "columnName": "new_col",
  "dataType": "VARCHAR(50)",
  "isNullable": true,
  "isPrimaryKey": false
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot modify confirmed transformation model. Model is immutable after confirmation."
}
```

**Validation**:
- ✅ Column addition blocked
- ✅ All modifications blocked

---

### **TEST 34: Try to Add Relation on Confirmed Model (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models/1/relations
```

**Request Body**:
```json
{
  "foreignTable": "table1",
  "foreignColumn": "col1",
  "primaryTable": "table2",
  "primaryColumn": "id"
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot modify confirmed transformation model. Model is immutable after confirmation."
}
```

**Validation**:
- ✅ Relation addition blocked
- ✅ Immutability enforced

---

## Section 6: Validation Rules

### **TEST 35: SystemScan Deletion Protection**

**Endpoint**: 
```
DELETE http://localhost:8080/api/scans/1
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Cannot delete scan 'School System Initial Scan' because it has associated transformation models. Please delete the transformation models first."
}
```

**Validation**:
- ✅ Scan deletion blocked
- ✅ Must delete transformation models first

---

### **TEST 36: Create Model with Non-COMPLETED Scan (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models
```

**Request Body**:
```json
{
  "name": "Test Invalid Scan",
  "systemScanId": 2,
  "targetConnectorId": 2
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "System scan must be in COMPLETED status. Current status: PENDING"
}
```

**Validation**:
- ✅ Only COMPLETED scans allowed
- ✅ Validation prevents invalid references

---

### **TEST 37: Create Model with SOURCE Connector as Target (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models
```

**Request Body**:
```json
{
  "name": "Invalid Target Type",
  "systemScanId": 1,
  "targetConnectorId": 1
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Connector must be of type TARGET. Current type: SOURCE"
}
```

**Validation**:
- ✅ Only TARGET connectors allowed
- ✅ Connector type validated

---

### **TEST 38: Duplicate Model Name (Should Fail)**

**Endpoint**: 
```
POST http://localhost:8080/api/transformation-models
```

**Request Body**:
```json
{
  "name": "School System Migration v2",
  "systemScanId": 1,
  "targetConnectorId": 2
}
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Transformation model with name 'School System Migration v2' already exists in your organization"
}
```

**Validation**:
- ✅ Duplicate names rejected per organization
- ✅ Case-sensitive check

---

## Section 7: Multi-Tenant Isolation

### **TEST 39: Access Other Organization's Model (Should Fail)**

**Setup**: Login as user from different organization

**Endpoint**: 
```
GET http://localhost:8080/api/transformation-models/1
```

**Headers**:
```
Authorization: Bearer <OTHER_ORG_JWT_TOKEN>
```

**Expected Response** (500 Internal Server Error):
```json
{
  "message": "Transformation model not found"
}
```

**Validation**:
- ✅ Cannot access other org's models
- ✅ Organization isolation enforced
- ✅ 404-style error (resource not found)

---

## Section 8: Cleanup

### **TEST 40: Delete Transformation Model**

**Endpoint**: 
```
DELETE http://localhost:8080/api/transformation-models/1
```

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Expected Response** (204 No Content):
```
(empty body)
```

**Validation**:
- ✅ Status code is 204
- ✅ Model deleted
- ✅ All transformations cascade deleted
- ✅ Can now delete SystemScan

---

## Summary of Phase 4 Features Tested

### ✅ Transformation Model Management (4 tests)
- Create, list, get details, update name, delete
- isConfirmed flag tracking
- Auto-include all scan metadata
- Immutability of systemScanId and targetConnectorId

### ✅ Table Transformations (8 tests)
- RENAME_TABLE with SQL validation
- ADD_TABLE with custom ID columns (AUTO_INCREMENT, UUID)
- DELETE_TABLE with FK dependency checking
- Reserved keyword validation
- Invalid character validation
- Duplicate name validation (case-insensitive)

### ✅ Column Transformations (11 tests)
- RENAME_COLUMN with validation
- CHANGE_TYPE with cross-database type checking
- ADD_COLUMN with type validation
- DELETE_COLUMN with comprehensive checks:
  - Cannot delete PK in FK
  - Cannot delete FK column
  - Cannot delete NOT NULL without default
- Reserved keyword and duplicate name validation

### ✅ Relation Transformations (4 tests)
- ADD_RELATION (user-defined foreign keys)
- DELETE_RELATION (soft delete)
- Table existence validation
- Duplicate relation prevention

### ✅ Model Confirmation & Immutability (7 tests)
- DAG cycle detection on confirmation
- isConfirmed flag prevents all modifications
- Immutability enforced on:
  - Model name update
  - Table transformations
  - Column transformations
  - Relation transformations

### ✅ Validation Rules (4 tests)
- SystemScan deletion protection
- Only COMPLETED scans allowed
- Only TARGET connectors allowed
- Duplicate model names rejected

### ✅ Multi-Tenant Isolation (1 test)
- Organization boundaries enforced
- Cannot access other org's data

### ✅ Auto-Update References (Built-in)
- Table renames cascade to relations
- Column renames cascade to relations
- Both foreign and primary key sides updated

---

## Complete API Endpoint Reference

### Transformation Models
```
POST   /api/transformation-models                           # Create model
GET    /api/transformation-models                           # List all models
GET    /api/transformation-models/{id}                      # Get model summary
GET    /api/transformation-models/{id}/details              # Get model details
PUT    /api/transformation-models/{id}                      # Update name
DELETE /api/transformation-models/{id}                      # Delete model
POST   /api/transformation-models/{id}/confirm              # Confirm model
```

### Table Transformations
```
PUT    /api/transformation-models/{modelId}/tables/{tableId}/rename    # Rename table
POST   /api/transformation-models/{modelId}/tables                     # Add table
DELETE /api/transformation-models/{modelId}/tables/{tableId}           # Delete table
```

### Column Transformations
```
PUT    /api/transformation-models/{modelId}/tables/{tableId}/columns/{columnId}/rename       # Rename column
PUT    /api/transformation-models/{modelId}/tables/{tableId}/columns/{columnId}/change-type  # Change type
POST   /api/transformation-models/{modelId}/tables/{tableId}/columns                         # Add column
DELETE /api/transformation-models/{modelId}/tables/{tableId}/columns/{columnId}              # Delete column
```

### Relation Transformations
```
POST   /api/transformation-models/{modelId}/relations              # Add relation
DELETE /api/transformation-models/{modelId}/relations/{relationId} # Delete relation
```

---

## Expected Test Results Summary

| Test # | Feature | Expected Result |
|--------|---------|-----------------|
| 1-4 | Model Management | ✅ CRUD operations work |
| 5-9 | Table Rename/Add | ✅ Validation and transformations applied |
| 10-12 | Table Delete | ✅ FK checks and soft delete |
| 13-15 | Column Rename | ✅ Validation and FK updates |
| 16-17 | Column Type Change | ✅ Type compatibility checked |
| 18-19 | Column Add | ✅ Type validation |
| 20-23 | Column Delete | ✅ Comprehensive validation |
| 24-27 | Relations | ✅ Add/delete with validation |
| 28-30 | Confirmation | ✅ DAG validation |
| 31-34 | Immutability | ✅ All modifications blocked |
| 35-38 | Validation Rules | ✅ Business rules enforced |
| 39 | Multi-Tenant | ✅ Organization isolation |
| 40 | Cleanup | ✅ Cascade deletion |

---

## Tips for Testing

1. **Use GET /details Frequently**: Call after each transformation to see full state
2. **Save All IDs**: Track model, table, column, and relation IDs
3. **Test Invalid Cases**: Verify all validation rules work
4. **Monitor Logs**: Check for validation messages
5. **Test Confirmation Flow**: Create cycles, fix them, then confirm
6. **Verify Immutability**: Try to modify confirmed model (should fail)

---

## Phase 4 Complete Testing - All Features! 🎉

All transformation system features tested:
- ✅ Transformation Model CRUD
- ✅ Table Transformations (RENAME, ADD, DELETE)
- ✅ Column Transformations (RENAME, CHANGE_TYPE, ADD, DELETE)
- ✅ Relation Transformations (ADD, DELETE)
- ✅ Model Confirmation with DAG Validation
- ✅ Immutability Enforcement
- ✅ Type Mapping Validation
- ✅ SQL Identifier Validation
- ✅ Auto-Update FK References
- ✅ Multi-Tenant Isolation

**System is ready for Phase 5: Cycle Execution!** 🚀
