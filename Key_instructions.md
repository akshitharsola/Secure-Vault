# Key Instructions: Android Signing & Version Management

## Overview
This document explains how SecureVault Android handles signing keys and provides the recommended solution to avoid conflicts between local development and GitHub releases.

## Current Signing Configuration

### GitHub Actions (Production Releases)
- **Keystore**: Uses `keystore.jks` stored as base64-encoded secret (`KEYSTORE_BASE64`)
- **Key Alias**: `securevault` (stored in `SIGNING_KEY_ALIAS` secret)
- **Passwords**: Stored in GitHub secrets (`SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`)
- **Location**: `app/build.gradle.kts:24-40` and `.github/workflows/release.yml:44-55`

### Local Development (Debug Builds)
- **Default**: Uses Android's default debug keystore
- **Location**: `~/.android/debug.keystore`
- **Alias**: `androiddebugkey`
- **Password**: `android` (standard debug password)

## The Conflict Problem

When you develop locally and then push to GitHub, conflicts occur because:

1. **Local builds** use debug signing (different certificate)
2. **GitHub releases** use production signing (release certificate)
3. **Android treats these as different apps** due to different signatures

## Recommended Solution: Different Application IDs

### Implementation
Modify `app/build.gradle.kts` to use different application IDs:

```kotlin
android {
    defaultConfig {
        applicationId = "com.securevault.debug"  // Different ID for local
        // ... other config
    }
    
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            // This creates com.securevault.debug
        }
        
        release {
            // applicationId remains "com.securevault" for production
        }
    }
}
```

### Benefits:
- Local and GitHub apps install separately
- No conflicts during development
- Clear distinction between dev and prod versions
- Simple to implement and maintain
- Lowest security risk

### Development Workflow
1. **Local Development**: Always use debug builds (`com.securevault.debug`)
2. **Testing**: Install debug APK locally for testing
3. **Production Releases**: Only push to GitHub for production builds
4. **Coexistence**: Both debug and production apps can coexist on device
5. **No Local Signing**: No keystore management required for local development

### Note
For all three available solutions, see `three_solutions_key.md`

## Security Considerations

### GitHub Secrets Management
- `KEYSTORE_BASE64`: Base64-encoded production keystore
- `SIGNING_KEY_ALIAS`: Key alias (usually `securevault`)
- `SIGNING_KEY_PASSWORD`: Private key password
- `SIGNING_STORE_PASSWORD`: Keystore password

### Local Security
- Debug builds use standard Android debug keystore
- No sensitive production keys stored locally
- Minimal security risk

## Version Management

### Current Version Info
- **Version Code**: 6 (auto-incremented for each release)
- **Version Name**: "1.2.3" (semantic versioning)
- **Location**: `app/build.gradle.kts:18-19`

### Update Process
1. **Before releasing**: Increment version code and name
2. **Test locally**: Use debug build (`com.securevault.debug`)
3. **Push to GitHub**: Create tag (e.g., `v1.2.0`)
4. **GitHub Actions**: Automatically builds and releases both APKs
   - `app-release.apk`: Production version (recommended for users)
   - `app-debug.apk`: Debug version (for testing only)

## Files to Modify

To implement the recommended solution:

1. **`app/build.gradle.kts`** - Add application ID suffixes and version name suffixes

## Update Mechanism & Download Issues

### UpdateManager Configuration
The app includes an in-app update mechanism (`UpdateManager.kt`) that:

1. **Checks GitHub Releases**: Uses GitHub API to detect new versions
2. **Prioritizes Release APK**: Automatically selects `app-release.apk` over `app-debug.apk`
3. **Prevents Debug Updates**: Debug builds (`com.securevault.debug`) don't offer production updates
4. **Smart Asset Selection**: Falls back properly if release APK not found

### Critical: Download the Correct APK
When installing from GitHub releases:

⚠️ **ALWAYS download `app-release.apk` (NOT `app-debug.apk`)**

**Why this matters:**
- `app-release.apk` → Updates production app (`com.securevault`)
- `app-debug.apk` → Installs separate debug app (`com.securevault.debug`)

**Result of downloading wrong APK:**
- Two separate apps on device instead of an update
- Package conflicts when trying to update
- Confusion about which app is "production"

### UpdateManager Code Location
- **File**: `app/src/main/java/com/securevault/utils/UpdateManager.kt`
- **Key Function**: `getPreferredDownloadUrl()` - prioritizes release APK
- **UI Integration**: Settings screen shows update notifications

## Troubleshooting

### Common Issues:
- **"App not installed"**: Different signatures between builds (normal with this solution)
- **"Update failed"**: Version code not incremented
- **Two apps on device**: Expected behavior - one for development, one for production
- **"Package conflicts"**: Downloaded debug APK instead of release APK
- **Update button not working**: Fixed in UpdateManager v1.2.1+
- **"Package conflicts v1.1 → v1.2+"**: Migration issue - see below

### ⚠️ Migration Issue: v1.1 → v1.2+ 

**Problem**: Apps built before v1.2 cannot be updated to v1.2+ due to signing changes.

**Cause**: v1.1 was built before the debug/release separation was implemented.

**Solution**: 
1. **Backup app data** (if export feature available)
2. **Uninstall** v1.1 completely 
3. **Fresh install** v1.2.3+ release APK
4. **Restore data** (if applicable)

**Note**: This is a **one-time migration**. All updates from v1.2.3+ will work normally.

## 🚨 Best Practices & Future Prevention

### Critical Rules to Follow

#### During Development (Android Studio)
1. **ONLY use debug builds locally**:
   ```bash
   ./gradlew assembleDebug
   # This creates: com.securevault.debug (safe for testing)
   ```

2. **NEVER build release APKs locally**:
   ```bash
   # ❌ AVOID: ./gradlew assembleRelease
   # This creates unsigned/incorrectly signed production APKs
   ```

3. **Install debug APK for testing**:
   - File: `app/build/outputs/apk/debug/app-debug.apk`
   - Package: `com.securevault.debug`
   - Version: Shows as "X.X.X-DEBUG"

#### Before Committing & Releasing
1. **Test with debug build only**:
   - Verify all features work in debug version
   - Debug and production apps can coexist safely

2. **Commit workflow**:
   ```bash
   # 1. Update version in build.gradle.kts
   versionCode = X
   versionName = "X.X.X"
   
   # 2. Commit changes
   git add .
   git commit -m "Release vX.X.X with [features]"
   
   # 3. Create and push tag
   git tag vX.X.X
   git push origin master
   git push origin vX.X.X
   ```

3. **Let GitHub Actions handle production builds**:
   - GitHub Actions builds both debug and release APKs
   - Release APK gets proper production signing
   - Users download release APK for updates

#### Testing Production Builds
1. **NEVER install local release APKs on production apps**
2. **Download release APK from GitHub** for production testing
3. **Use separate device/emulator** for production testing if needed

### What Went Wrong (v1.1 Issue)
- **Problem**: Mixed local and GitHub builds with different signing
- **Result**: v1.1 → v1.2+ update failures due to certificate mismatch
- **Prevention**: Always use GitHub-built APKs for production

### Key Principles
1. **Local = Debug Only**: `com.securevault.debug` for development
2. **GitHub = Production**: `com.securevault` for users
3. **Never Mix**: Don't install local release builds on production
4. **Consistent Signing**: GitHub Actions ensures consistent production signing

### Debug Commands:
```bash
# Check APK signature
jarsigner -verify -verbose -certs app/build/outputs/apk/debug/app-debug.apk

# Check installed app signatures
adb shell pm dump com.securevault | grep -A 10 "signatures"
adb shell pm dump com.securevault.debug | grep -A 10 "signatures"
```

## Implementation Steps

1. **Back up current setup**
2. **Modify `app/build.gradle.kts`** with application ID suffixes
3. **Test locally** - build debug APK
4. **Verify separate app installation**
5. **Push to GitHub** - production builds remain unchanged

This approach ensures smooth development workflow while maintaining production app compatibility.