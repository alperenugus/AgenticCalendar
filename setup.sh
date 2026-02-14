#!/bin/bash

# Agentic Calendar - Setup Script
# This script sets up both backend and frontend for the project

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

check_command() {
    if command -v "$1" &> /dev/null; then
        print_success "$1 is installed"
        return 0
    else
        print_error "$1 is not installed"
        return 1
    fi
}

check_version() {
    local cmd=$1
    local version_flag=$2
    local min_version=$3
    
    if command -v "$cmd" &> /dev/null; then
        local version=$($cmd $version_flag 2>&1 | head -n 1)
        print_success "$cmd version: $version"
        return 0
    else
        print_error "$cmd is not installed"
        return 1
    fi
}

# Start of setup
print_header "Agentic Appointment Scheduler - Setup"

# Check prerequisites
print_header "Checking Prerequisites"

MISSING_DEPS=0

# Check Java
if check_version "java" "-version" "21"; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java 21 or higher is required. Found Java $JAVA_VERSION"
        MISSING_DEPS=1
    fi
else
    MISSING_DEPS=1
fi

# Check Maven
if ! check_version "mvn" "-version" "3.6"; then
    MISSING_DEPS=1
fi

# Check Node.js
if ! check_version "node" "--version" "18"; then
    MISSING_DEPS=1
fi

# Check npm
if ! check_command "npm"; then
    MISSING_DEPS=1
fi

# Check Docker
if ! check_command "docker"; then
    print_warning "Docker is not installed. PostgreSQL will need to be set up manually."
    DOCKER_AVAILABLE=0
else
    DOCKER_AVAILABLE=1
    # Check if Docker is running
    if docker info &> /dev/null; then
        print_success "Docker is running"
    else
        print_warning "Docker is installed but not running. Please start Docker Desktop."
        DOCKER_AVAILABLE=0
    fi
fi

# Check Ollama (optional but recommended)
if check_command "ollama"; then
    print_success "Ollama is installed"
    OLLAMA_AVAILABLE=1
    
    # Check if Ollama is running
    if curl -s http://localhost:11434/api/tags &> /dev/null; then
        print_success "Ollama is running"
    else
        print_warning "Ollama is installed but not running. Start it with: ollama serve"
        OLLAMA_AVAILABLE=0
    fi
else
    print_warning "Ollama is not installed. Install it for local LLM support."
    print_info "  macOS: brew install ollama"
    print_info "  Linux: curl -fsSL https://ollama.com/install.sh | sh"
    OLLAMA_AVAILABLE=0
fi

if [ $MISSING_DEPS -eq 1 ]; then
    print_error "Some required dependencies are missing. Please install them and run this script again."
    exit 1
fi

# Setup Backend
print_header "Setting Up Backend"

if [ ! -d "backend" ]; then
    print_error "Backend directory not found!"
    exit 1
fi

cd backend

# Check if pom.xml exists
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found in backend directory!"
    exit 1
fi

# Download Maven dependencies
print_info "Downloading Maven dependencies..."
if mvn dependency:resolve -q; then
    print_success "Maven dependencies downloaded"
else
    print_error "Failed to download Maven dependencies"
    exit 1
fi

# Compile backend
print_info "Compiling backend..."
if mvn clean compile -q; then
    print_success "Backend compiled successfully"
else
    print_error "Backend compilation failed"
    exit 1
fi

# Setup Docker for PostgreSQL
if [ $DOCKER_AVAILABLE -eq 1 ]; then
    print_info "Setting up PostgreSQL with Docker..."
    
    # Check if container already exists
    if docker ps -a --format '{{.Names}}' | grep -q "^appointmentscheduler-postgres$"; then
        print_info "PostgreSQL container already exists"
        
        # Check if it's running
        if docker ps --format '{{.Names}}' | grep -q "^appointmentscheduler-postgres$"; then
            print_success "PostgreSQL container is already running"
        else
            print_info "Starting existing PostgreSQL container..."
            if docker start appointmentscheduler-postgres &> /dev/null; then
                print_success "PostgreSQL container started"
            else
                print_error "Failed to start PostgreSQL container"
                exit 1
            fi
        fi
    else
        print_info "Creating PostgreSQL container..."
        if docker-compose up -d; then
            print_success "PostgreSQL container created and started"
            
            # Wait for PostgreSQL to be ready
            print_info "Waiting for PostgreSQL to be ready..."
            sleep 5
            
            MAX_RETRIES=30
            RETRY_COUNT=0
            while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
                if docker exec appointmentscheduler-postgres pg_isready -U postgres &> /dev/null; then
                    print_success "PostgreSQL is ready"
                    break
                fi
                RETRY_COUNT=$((RETRY_COUNT + 1))
                sleep 1
            done
            
            if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
                print_warning "PostgreSQL might not be ready yet. Please wait a few seconds."
            fi
        else
            print_error "Failed to create PostgreSQL container"
            exit 1
        fi
    fi
else
    print_warning "Docker is not available. Please set up PostgreSQL manually."
    print_info "  Database: appointmentscheduler"
    print_info "  Username: postgres"
    print_info "  Password: postgres"
    print_info "  Port: 5432"
fi

cd ..

# Setup Frontend
print_header "Setting Up Frontend"

if [ ! -d "frontend" ]; then
    print_error "Frontend directory not found!"
    exit 1
fi

cd frontend

# Check if package.json exists
if [ ! -f "package.json" ]; then
    print_error "package.json not found in frontend directory!"
    exit 1
fi

# Install npm dependencies
print_info "Installing npm dependencies..."
if npm install; then
    print_success "Frontend dependencies installed"
else
    print_error "Failed to install frontend dependencies"
    exit 1
fi

cd ..

# Setup Ollama Model (if available)
if [ $OLLAMA_AVAILABLE -eq 1 ]; then
    print_header "Setting Up Ollama Model"
    
    print_info "Checking for llama3.1 model..."
    if ollama list | grep -q "llama3.1"; then
        print_success "llama3.1 model is already installed"
    else
        print_info "Pulling llama3.1 model (this may take a while)..."
        if ollama pull llama3.1; then
            print_success "llama3.1 model installed"
        else
            print_warning "Failed to pull llama3.1 model. You can pull it manually later with: ollama pull llama3.1"
        fi
    fi
fi

# Final summary
print_header "Setup Complete!"

echo -e "\n${GREEN}✓ Backend:${NC}"
echo "  - Dependencies installed"
echo "  - Code compiled"
if [ $DOCKER_AVAILABLE -eq 1 ]; then
    echo "  - PostgreSQL running in Docker"
fi

echo -e "\n${GREEN}✓ Frontend:${NC}"
echo "  - Dependencies installed"

if [ $OLLAMA_AVAILABLE -eq 1 ]; then
    echo -e "\n${GREEN}✓ Ollama:${NC}"
    echo "  - Model ready"
fi

echo -e "\n${BLUE}Next Steps:${NC}"
echo ""
echo "1. Start the backend:"
echo "   ${YELLOW}./start-backend.sh${NC}  (or: cd backend && mvn spring-boot:run)"
echo ""
echo "2. In a new terminal, start the frontend:"
echo "   ${YELLOW}./start-frontend.sh${NC}  (or: cd frontend && npm run dev)"
echo ""
echo "3. Open your browser to:"
echo "   ${YELLOW}http://localhost:5173${NC}"
echo ""

if [ $OLLAMA_AVAILABLE -eq 0 ]; then
    echo -e "${YELLOW}Note:${NC} Install and start Ollama for local LLM support:"
    echo "   ${YELLOW}ollama serve${NC}"
    echo "   ${YELLOW}ollama pull llama3.1${NC}"
    echo ""
fi

if [ $DOCKER_AVAILABLE -eq 0 ]; then
    echo -e "${YELLOW}Note:${NC} Set up PostgreSQL manually or install Docker."
    echo ""
fi

print_success "Setup completed successfully! 🎉"

