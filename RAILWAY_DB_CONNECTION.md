# Railway PostgreSQL Connection Guide

## Quick Fix: Connect Database to Backend

### Step 1: Verify PostgreSQL is Created

1. Go to Railway dashboard
2. Check if you have a **PostgreSQL** service
3. If not, create it: **New** → **Database** → **PostgreSQL**

### Step 2: Connect Database Variables to Backend

Railway provides database connection variables, but you need to make them available to your backend service.

#### Option A: Reference Variables (Recommended)

1. Go to your **Backend service** → **Variables** tab
2. Click **"New Variable"** → **"Reference from Service"**
3. Select your **PostgreSQL service**
4. Add these variable references:
   - `PGHOST`
   - `PGPORT`
   - `PGDATABASE`
   - `PGUSER`
   - `PGPASSWORD`

This way, if Railway changes the database credentials, your backend automatically gets the new values.

#### Option B: Manual Copy (Alternative)

1. Go to **PostgreSQL service** → **Variables** tab
2. Copy the values for:
   - `PGHOST`
   - `PGPORT`
   - `PGDATABASE`
   - `PGUSER`
   - `PGPASSWORD`
3. Go to **Backend service** → **Variables** tab
4. Manually add each variable with the copied values

### Step 3: Verify Connection

After adding the variables:

1. **Redeploy** your backend service
2. Check the **Logs** tab
3. You should see:
   ```
   HikariPool-1 - Starting...
   HikariPool-1 - Start completed.
   ```
4. If you see connection errors, verify all 5 variables are set correctly

## How It Works

The `application.yml` is now configured to read from Railway's PostgreSQL environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${PGHOST:localhost}:${PGPORT:5432}/${PGDATABASE:appointmentscheduler}
    username: ${PGUSER:postgres}
    password: ${PGPASSWORD:postgres}
```

- `${PGHOST:localhost}` means: use `PGHOST` env var, or default to `localhost`
- This works for both Railway (production) and local development

## Troubleshooting

### Error: "Connection refused"

- **Check**: `PGHOST` is set correctly
- **Check**: `PGPORT` is `5432` (default PostgreSQL port)
- **Check**: Backend service can reach PostgreSQL service (same Railway project)

### Error: "Authentication failed"

- **Check**: `PGUSER` and `PGPASSWORD` are correct
- **Check**: Values match what's in PostgreSQL service Variables tab

### Error: "Database does not exist"

- **Check**: `PGDATABASE` is set correctly (usually `railway` for Railway PostgreSQL)
- Railway creates the database automatically, so this should be `railway`

### Variables Not Found

- **Check**: Variables are added to **Backend service**, not just PostgreSQL service
- **Check**: Variable names are exactly: `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
- **Check**: No typos or extra spaces

## Quick Checklist

- [ ] PostgreSQL service exists in Railway project
- [ ] Backend service exists in same Railway project
- [ ] All 5 database variables are referenced/copied to backend service
- [ ] Backend service has been redeployed after adding variables
- [ ] Check logs for successful database connection

## After Connection Works

Once connected, Spring Boot will:
1. Automatically create tables (because `ddl-auto: update`)
2. Run `DataInitializer` to seed initial data
3. Your app is ready to use!

