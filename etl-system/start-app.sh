#!/bin/bash

# Database Migrator - Start Script
# This script starts the Spring Boot application with environment variables from .env

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Database Migrator - Starting App${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}ERROR: .env file not found!${NC}"
    echo "Please create a .env file with the following variables:"
    echo "  DB_URL=..."
    echo "  DB_USERNAME=..."
    echo "  DB_PASSWORD=..."
    echo "  JWT_SECRET=..."
    echo "  JWT_EXPIRATION=..."
    exit 1
fi

# Load environment variables from .env
echo -e "${YELLOW}Loading environment variables from .env...${NC}"
export $(cat .env | xargs)

# Check if required variables are set
if [ -z "$JWT_SECRET" ]; then
    echo -e "${RED}ERROR: JWT_SECRET not set in .env file${NC}"
    exit 1
fi

if [ -z "$DB_URL" ]; then
    echo -e "${RED}ERROR: DB_URL not set in .env file${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Environment variables loaded${NC}"
echo -e "${YELLOW}Database URL: ${DB_URL}${NC}"

# Stop any existing Spring Boot processes
echo -e "${YELLOW}Checking for existing Spring Boot processes...${NC}"
if pkill -f "spring-boot:run" 2>/dev/null; then
    echo -e "${GREEN}✓ Stopped existing Spring Boot process${NC}"
    sleep 2
else
    echo -e "${YELLOW}No existing process found${NC}"
fi

# Start the application
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Starting Spring Boot Application${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

mvn spring-boot:run

# Press Ctrl+C to stop the application
