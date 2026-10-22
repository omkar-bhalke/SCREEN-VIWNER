# ScreenStream — GitHub Push & APK Build Guide

## Step 1 — Add gradle-wrapper.jar (one time)

The CI needs `gradle/wrapper/gradle-wrapper.jar`. Generate it locally first:

```bash
# If you have Gradle installed:
gradle wrapper --gradle-version 8.7

# OR download directly:
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
```

## Step 2 — Add google-services.json

Firebase is required for the release build.

**Option A — Use Firebase (recommended):**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create project → Add Android app → package `info.dvkr.screenstream`
3. Download `google-services.json` → place in `app/google-services.json`
4. In GitHub → Settings → Secrets → Add secret:
   - Name: `GOOGLE_SERVICES_JSON`
   - Value: paste the entire contents of `google-services.json`

**Option B — No Firebase (debug only):**
The workflow auto-generates a dummy `google-services.json` for debug builds.
Remove Firebase deps from `app/build.gradle.kts` if you want a fully offline build.

## Step 3 — Push to GitHub

```bash
cd screenstream-studio

git init
git add .
git commit -m "Initial commit — ScreenStream 4.4.2 reversed source"

# Create repo on GitHub, then:
git remote add origin https://github.com/YOUR_USERNAME/screenstream.git
git branch -M main
git push -u origin main
```

## Step 4 — Trigger the build

GitHub Actions starts automatically on push.

To trigger manually:
- Go to your repo → **Actions** tab
- Select **Build APK** workflow
- Click **Run workflow** → choose `debug` or `release`

## Step 5 — Download APK

After the workflow completes (~5–10 min):
- Actions → latest run → **Artifacts** section
- Download `ScreenStream-debug-<sha>.zip`
- Extract → install `app-debug.apk` on your device

## Step 6 (optional) — Signed release APK

For a proper signed APK that can be distributed:

### Generate keystore locally:
```bash
keytool -genkey -v \
  -keystore screenstream.jks \
  -alias screenstream \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Add secrets to GitHub repo (Settings → Secrets):
| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 screenstream.jks` output |
| `KEYSTORE_PASSWORD` | your keystore password |
| `KEY_ALIAS` | `screenstream` (or whatever alias you set) |
| `KEY_PASSWORD` | your key password |

Then run workflow with build type = `release`.

## Step 7 (optional) — Auto-release on version tag

Push a version tag to trigger a GitHub Release with APK attached:

```bash
git tag v4.4.2
git push origin v4.4.2
```

---

## Workflow file location
`.github/workflows/build-apk.yml`

## What the workflow does
1. Sets up JDK 17
2. Caches Gradle dependencies
3. Writes `google-services.json` from secret (or generates dummy for debug)
4. Builds debug or release APK
5. Signs release APK if keystore secrets are set
6. Uploads APK as artifact
7. Creates GitHub Release if triggered by a version tag (`v*`)
