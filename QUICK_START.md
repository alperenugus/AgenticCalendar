# Quick Start Guide

## ⚡ Fastest Way to Get Started

### 1. Run Setup Script

```bash
./setup.sh
```

### 2. Start Backend

```bash
# Easy way (from root):
./start-backend.sh

# Or manually:
cd backend && mvn spring-boot:run
```

### 3. Start Frontend (in a new terminal)

```bash
# Easy way (from root):
./start-frontend.sh

# Or manually:
cd frontend && npm run dev
```

### 4. Open Browser

Navigate to: `http://localhost:5173`

---

## 📝 Common Commands

### Backend

```bash
# Start the application (from root)
./start-backend.sh

# Or from backend directory:
cd backend
mvn spring-boot:run

# Run tests
cd backend && mvn test

# Build JAR
cd backend && mvn clean package

# Start PostgreSQL
cd backend && docker-compose up -d
```

### Frontend

```bash
# Start dev server (from root)
./start-frontend.sh

# Or from frontend directory:
cd frontend
npm run dev

# Build for production
cd frontend && npm run build

# Install dependencies
cd frontend && npm install
```

---

## 🐛 Troubleshooting

### "No plugin found for prefix 'spring-boot'"

**Problem**: Running Maven from wrong directory.

**Solution**: 
```bash
# Use the convenience script (from root):
./start-backend.sh

# Or navigate to backend first:
cd backend
mvn spring-boot:run
```

### "Cannot find module" (Frontend)

**Problem**: Dependencies not installed.

**Solution**:
```bash
cd frontend
npm install
```

### "Connection refused" (Backend)

**Problem**: Backend not running or wrong port.

**Solution**: 
- Check if backend is running: `curl http://localhost:8080/api/users/count`
- Make sure you're in `backend/` directory when starting

### "CORS error" (Frontend)

**Problem**: Backend CORS not configured.

**Solution**: 
- Verify `backend/src/main/java/.../config/WebConfig.java` exists
- Restart backend after adding CORS config

---

## 📚 More Information

See [README.md](README.md) for detailed documentation.

