# Railway Groq API Key Setup

## Error: Invalid API Key

If you see this error:
```
Invalid API Key
```

It means the Groq API key is not set correctly in Railway.

## Step-by-Step Fix

### Step 1: Get Your Groq API Key

1. Go to https://console.groq.com/keys
2. Sign in (or create account - it's free!)
3. Click **"Create API Key"**
4. Copy the key - it should start with `gsk_...`
5. **Important**: Save it immediately - you won't see it again!

### Step 2: Set Environment Variables in Railway

Go to your **Backend service** in Railway → **Variables** tab:

**Add these variables:**

```bash
# LLM Provider - MUST be set to "groq"
LANGCHAIN4J_PROVIDER=groq

# Groq API Key (starts with gsk_...)
LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key-here

# Groq Model (optional - defaults to llama-3.1-8b-instant)
# Note: llama-3.1-70b-versatile was decommissioned
# llama-3.1-8b-instant: Fast, efficient, uses ~10x fewer tokens (recommended to avoid rate limits)
# llama-3.3-70b-versatile: More capable but uses many more tokens (may hit rate limits)
LANGCHAIN4J_GROQ_MODEL=llama-3.1-8b-instant

# Temperature (optional - defaults to 0.7)
LANGCHAIN4J_GROQ_TEMPERATURE=0.7
```

### Step 3: Verify the Key Format

**Correct format:**
- Starts with `gsk_`
- Example: `gsk_1234567890abcdefghijklmnopqrstuvwxyz`

**Common mistakes:**
- ❌ Missing `gsk_` prefix
- ❌ Extra spaces before/after the key
- ❌ Using OpenAI key instead of Groq key
- ❌ Key is expired or revoked

### Step 4: Redeploy Backend

After setting the variables:
1. Go to **Backend service** → **Deployments**
2. Click **Redeploy**
3. Wait for deployment to complete

### Step 5: Verify It Works

Check the backend logs. You should see:
- No "Invalid API Key" errors
- Successful LLM responses when you send a message

## Troubleshooting

### Error: "Invalid API Key"

**Check:**
1. ✅ `LANGCHAIN4J_PROVIDER=groq` is set (not `ollama` or `openai`)
2. ✅ `LANGCHAIN4J_GROQ_API_KEY` is set
3. ✅ Key starts with `gsk_`
4. ✅ No extra spaces in the key
5. ✅ Key is active in Groq console
6. ✅ Backend has been redeployed after setting variables

### Error: "Provider not found"

**Solution:**
- Make sure `LANGCHAIN4J_PROVIDER=groq` (lowercase)
- Check for typos in variable name

### Error: "API key is empty"

**Solution:**
- Make sure `LANGCHAIN4J_GROQ_API_KEY` is set
- Check that the value is not empty
- Verify no quotes around the value in Railway

## Quick Checklist

- [ ] Got API key from https://console.groq.com/keys
- [ ] Key starts with `gsk_`
- [ ] Set `LANGCHAIN4J_PROVIDER=groq` in Railway
- [ ] Set `LANGCHAIN4J_GROQ_API_KEY` in Railway
- [ ] No extra spaces in the key
- [ ] Backend redeployed after setting variables
- [ ] Check logs - no "Invalid API Key" errors

## Example Railway Variables

```bash
LANGCHAIN4J_PROVIDER=groq
LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key-here
LANGCHAIN4J_GROQ_MODEL=llama-3.1-8b-instant
LANGCHAIN4J_GROQ_TEMPERATURE=0.7
```

**Available Models:**
- `llama-3.1-8b-instant` - **Recommended** (fast, efficient, uses ~10x fewer tokens)
- `llama-3.3-70b-versatile` - More capable but token-heavy (may hit rate limits)
- `mixtral-8x7b-32768` - Alternative model

**Note**: Replace `your-groq-api-key-here` with your actual API key from https://console.groq.com/keys

## Free Tier Limits

- **100,000 tokens/day** (resets daily)
- **30 requests/minute** rate limit
- **No credit card required**
- **No expiration** (as long as within limits)

**Note**: The smaller `llama-3.1-8b-instant` model uses approximately 10x fewer tokens than `llama-3.3-70b-versatile`, making it much less likely to hit daily limits.

## Still Not Working?

1. **Double-check the API key** in Groq console
2. **Create a new API key** if the old one might be invalid
3. **Check Railway logs** for more detailed error messages
4. **Verify all environment variables** are set correctly
5. **Make sure backend is redeployed** after setting variables

