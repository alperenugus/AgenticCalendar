# Renaming Guide: AgenticAppointmentScheduler → AgenticCalendar

This guide will help you rename the local folder and GitHub repository to `AgenticCalendar`.

## ✅ Code References Updated

All code references have been updated:
- ✅ Application name in `application.yml`
- ✅ Database name in `application.yml` and `docker-compose.yml`
- ✅ README.md clone instructions
- ✅ Setup script header
- ✅ Default Railway redirect URI (update when you rename Railway service)
- ✅ Container name in `docker-compose.yml`

**Note**: The Java package name (`com.agent.appointmentscheduler`) remains unchanged to avoid breaking changes. This is internal and doesn't affect the application name.

## 📁 Step 1: Rename Local Folder

### Option A: Using Terminal (Recommended)

```bash
# Navigate to parent directory
cd /Users/alperenugus/Desktop/MyProjects/AgenticAI

# Rename the folder
mv AgenticAppointmentScheduler AgenticCalendar

# Navigate into the renamed folder
cd AgenticCalendar
```

### Option B: Using Finder (macOS)

1. Navigate to `/Users/alperenugus/Desktop/MyProjects/AgenticAI/`
2. Right-click on `AgenticAppointmentScheduler` folder
3. Select "Rename"
4. Enter `AgenticCalendar`
5. Press Enter

## 🔄 Step 2: Update Git Remote (After GitHub Repo Rename)

After renaming the GitHub repository (see Step 3), update your local git remote:

```bash
cd AgenticCalendar  # (or your new folder path)

# Check current remote
git remote -v

# Update remote URL
git remote set-url origin https://github.com/alperenugus/AgenticCalendar.git

# Verify
git remote -v
```

## 🐙 Step 3: Rename GitHub Repository

1. **Go to GitHub:**
   - Navigate to: https://github.com/alperenugus/AgenticAppointmentScheduler
   - Click on **Settings** (top right of the repository page)

2. **Rename the Repository:**
   - Scroll down to the **"Repository name"** section
   - Change `AgenticAppointmentScheduler` to `AgenticCalendar`
   - Click **"Rename"** button
   - GitHub will automatically redirect old URLs to the new name

3. **Update Railway Deployment (if applicable):**
   - If you're using Railway, you may need to reconnect the repository
   - Railway should automatically detect the rename, but verify the connection

## 🚂 Step 4: Update Railway Service Names (Optional but Recommended)

If you want to rename your Railway services to match:

1. **Backend Service:**
   - Go to Railway dashboard
   - Select your backend service
   - Settings → General → Service Name
   - Rename to: `agenticcalendar-backend` (or similar)
   - Update `GOOGLE_REDIRECT_URI` environment variable with new URL

2. **Frontend Service:**
   - Go to Railway dashboard
   - Select your frontend service
   - Settings → General → Service Name
   - Rename to: `agenticcalendar-frontend` (or similar)

3. **Update Environment Variables:**
   - Update `GOOGLE_REDIRECT_URI` in backend service with new backend URL
   - Update `VITE_API_BASE_URL` and `VITE_WS_BASE_URL` in frontend service with new backend URL
   - Update Google OAuth Console with new redirect URI

## 🔐 Step 5: Update Google OAuth Console

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **APIs & Services** → **Credentials**
3. Find your OAuth 2.0 Client ID
4. Update **Authorized redirect URIs**:
   - Remove: `https://agenticappointmentschedulerbackend-production.up.railway.app/login/oauth2/code/google`
   - Add: `https://your-new-backend-url.railway.app/login/oauth2/code/google`
5. Save changes

## ✅ Step 6: Verify Everything Works

1. **Test Local Development:**
   ```bash
   cd AgenticCalendar
   ./setup.sh  # Should work with new folder name
   ```

2. **Test Git Connection:**
   ```bash
   git fetch origin
   git status
   ```

3. **Test Railway Deployment:**
   - Push a test commit
   - Verify deployment succeeds
   - Test OAuth login

4. **Test Application:**
   - Sign in with Google
   - Create a test event
   - Verify recurring events display correctly

## 📝 Summary of Changes

- ✅ **Code**: All references updated to `AgenticCalendar`
- ✅ **Database**: Default database name changed to `agenticcalendar`
- ✅ **Container**: Docker container name updated
- ⚠️ **Java Package**: Remains `com.agent.appointmentscheduler` (internal, no impact)
- ⚠️ **Maven Artifact**: Remains `appointmentscheduler` (to avoid breaking Dockerfile)

## 🆘 Troubleshooting

### Issue: Git remote not found
**Solution**: Run `git remote set-url origin https://github.com/alperenugus/AgenticCalendar.git`

### Issue: Railway deployment fails
**Solution**: 
- Check Railway service is connected to renamed repo
- Verify environment variables are correct
- Check build logs for errors

### Issue: OAuth redirect error
**Solution**: 
- Verify Google OAuth Console has correct redirect URI
- Check `GOOGLE_REDIRECT_URI` environment variable matches Railway URL
- Ensure Railway service name matches the URL

---

**After completing these steps, your application will be fully renamed to AgenticCalendar! 🎉**

