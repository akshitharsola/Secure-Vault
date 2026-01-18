# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SecureVault is a quantum-resistant, hardware-backed Android password manager built with Kotlin and Jetpack Compose. Version 2.0 introduces **hardware-backed encryption via Android Keystore** and **quantum-resistant backup encryption** with ML-KEM-768, making it one of the most secure password managers available for Android.

**Key Security Features (v2.0+):**
- Hardware-backed AES-256-GCM encryption (Android Keystore)
- Quantum-resistant backups (ML-KEM-768 + X25519 + AES-256-GCM)
- Automatic migration from plain text to encrypted storage
- Biometric authentication with PIN fallback
- Zero internet permissions - completely offline
- Transaction-safe backup restore with rollback

## Version History

| Version | Date | Key Changes |
|---------|------|-------------|
| **v2.0.3** | Jan 2026 | Update mechanism improvements, enhanced diagnostics |
| **v2.0.2** | Jan 2026 | **CRITICAL FIX**: Keystore IV generation bug (backup restore) |
| **v2.0.1** | Jan 2026 | Search UX improvements (keyboard auto-focus, BackHandler) |
| **v2.0.0** | Jan 2026 | **MAJOR SECURITY OVERHAUL**: Android Keystore encryption, automatic migration |
| v1.5.1 | Jan 2026 | Quantum-resistant backup encryption (ML-KEM-768 + X25519) |
| v1.5.0 | Jan 2026 | Backward-compatible backup format v2.0 |
| v1.4.0 | 2025 | In-app update mechanism |
| v1.0-1.3 | 2024-2025 | ⚠️ **VULNERABLE**: Plain text password storage |

## Security Architecture (v2.0+)

### Critical Security Evolution

**v1.x (VULNERABLE):**
- ❌ Passwords stored in **PLAIN TEXT** in Room database
- ❌ Encryption keys in SharedPreferences (easily extractable on rooted devices)
- ❌ No actual encryption integration in save/retrieve flow
- ⚠️ Backups used PBKDF2 + AES-256-CBC (secure, but not quantum-resistant)

**v2.0+ (SECURE):**
- ✅ All passwords **ENCRYPTED** before database storage
- ✅ Encryption keys in **Android Keystore** (hardware-backed, non-extractable)
- ✅ Full encryption integration in PasswordRepositoryImpl
- ✅ Backups use **hybrid quantum-resistant** encryption (ML-KEM-768 + X25519)
- ✅ Automatic migration from plain text → encrypted (one-time, seamless)
- ✅ Transaction safety with atomic rollback

### Encryption Architecture Layers

```
┌──────────────────────────────────────────────────────────┐
│                  UI Layer (Compose)                      │
│  Screens: MainScreen, FormScreen, DetailScreen          │
└──────────────────────────────────────────────────────────┘
                         ▼
┌──────────────────────────────────────────────────────────┐
│              Domain Layer (Use Cases)                    │
│  Business logic for password CRUD operations             │
└──────────────────────────────────────────────────────────┘
                         ▼
┌──────────────────────────────────────────────────────────┐
│            Data Layer (Repository Pattern)               │
│  PasswordRepositoryImpl - ENCRYPTION INTEGRATION         │
│  • encrypt() before save                                 │
│  • decrypt() after retrieve                              │
│  • @Transaction for atomic restore                       │
└──────────────────────────────────────────────────────────┘
                         ▼
┌──────────────────────────────────────────────────────────┐
│          Security Layer (Android Keystore)               │
│  SecurityManager - CRITICAL COMPONENT                    │
│  • Hardware-backed key generation                        │
│  • AES-256-GCM encryption/decryption                     │
│  • Automatic IV generation (setRandomizedEncryptionRequired) │
│  • Keys stored in TEE/Secure Element                     │
└──────────────────────────────────────────────────────────┘
                         ▼
┌──────────────────────────────────────────────────────────┐
│          Storage Layer (Room Database)                   │
│  Encrypted password data stored as Base64 strings        │
└──────────────────────────────────────────────────────────┘
```

### Database Encryption Flow (v2.0+)

**Save Password:**
```kotlin
User Input (plain text password)
      ↓
PasswordRepositoryImpl.savePassword()
      ↓
SecurityManager.encrypt(plaintext) {
    1. Get/create key from Android Keystore (hardware-backed)
    2. Initialize cipher: AES/GCM/NoPadding
    3. Keystore auto-generates random IV (12 bytes)
    4. Encrypt with AES-256-GCM
    5. Extract IV from cipher.getIV()
    6. Combine: IV + Ciphertext (includes GCM auth tag)
    7. Base64 encode
}
      ↓
PasswordDao.insertPassword(PasswordEntity with encrypted password field)
      ↓
Room Database (password stored as Base64-encoded encrypted blob)
```

**Retrieve Password:**
```kotlin
Room Database (encrypted password as Base64 string)
      ↓
PasswordDao.getPasswordById()
      ↓
PasswordRepositoryImpl.getPassword()
      ↓
SecurityManager.decrypt(ciphertext) {
    1. Base64 decode
    2. Extract IV (first 12 bytes)
    3. Extract ciphertext (remaining bytes, includes auth tag)
    4. Get key from Android Keystore
    5. Initialize cipher with extracted IV
    6. Decrypt with AES-256-GCM (verifies auth tag)
    7. Return plaintext
}
      ↓
User sees decrypted password
```

**Critical Implementation Detail (v2.0.2 Fix):**
```kotlin
// ❌ WRONG (v2.0.0 - v2.0.1) - Caused "Caller-provided IV not permitted" error
val iv = ByteArray(12)
SecureRandom().nextBytes(iv)
cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

// ✅ CORRECT (v2.0.2+) - Let Keystore generate IV automatically
cipher.init(Cipher.ENCRYPT_MODE, key)  // No IV parameter!
val iv = cipher.getIV()  // Extract auto-generated IV
```

**Why this matters:** When `setRandomizedEncryptionRequired(true)` is set in KeyGenParameterSpec, Android Keystore **automatically generates** the IV and does **not allow** caller-provided IVs. This ensures true randomness from hardware entropy sources.

### Backup Encryption Flow (v2.0+)

**Backup Creation (Quantum-Resistant):**
```kotlin
Password List → JSON
      ↓
User Password Input
      ↓
PBKDF2-HMAC-SHA512 (100,000 iterations, 32-byte salt)
      ↓
Derived AES Key (256-bit)
      ↓
AES-256-GCM Encryption
      ↓
Quantum Metadata Generation {
    - ML-KEM-768 keypair (post-quantum)
    - X25519 keypair (classical)
    - Hybrid key exchange
}
      ↓
BackupData v2.0 JSON {
    version: "2.0",
    encryptionType: "hybrid-mlkem768-x25519-aes256gcm",
    mlkemPublicKey: <base64>,
    x25519PublicKey: <base64>,
    salt: <base64>,
    nonce: <base64>,
    data: <base64-encrypted>
}
      ↓
.backup file saved
```

**Backup Restore (Backward Compatible):**
```kotlin
Read .backup file
      ↓
Parse JSON → Detect version field
      ↓
      ├─→ version: "2.0" → QuantumBackupEncryption.decrypt()
      │   (ML-KEM-768 + X25519 hybrid decryption)
      │
      ├─→ version: "1.0" or null → BackupEncryption.decrypt()
      │   (PBKDF2 + AES-256-CBC - legacy support)
      │
      └─→ No version field → Try legacy obfuscated/raw formats
      ↓
Decrypted password list (JSON)
      ↓
@Transaction PasswordRepositoryImpl.replaceAllPasswords() {
    1. Delete all existing passwords
    2. Encrypt each password with SecurityManager
    3. Insert encrypted passwords
    4. Commit transaction (atomic - rollback on failure)
}
      ↓
Passwords restored successfully
```

### Migration System (v2.0+)

**MigrationManager** handles automatic one-time migration from v1.x plain text to v2.0 encrypted storage:

```kotlin
App Launch (MainActivity.onCreate)
      ↓
MigrationManager.checkAndMigrateIfNeeded()
      ↓
Check SharedPreferences: migration_v2_completed?
      ↓
      ├─→ YES → Skip migration, continue normally
      │
      └─→ NO → Start migration:
          1. Get all passwords from Room (plain text)
          2. For each password:
             - Detect if already encrypted (Base64 check)
             - If plain text: encrypt with SecurityManager
             - Update database entry
          3. Delete old SharedPreferences encryption key
          4. Mark migration complete
          5. Migration time: < 1 second for typical datasets
      ↓
App continues with encrypted database
```

**Migration Detection Logic:**
```kotlin
private fun isPasswordEncrypted(password: String): Boolean {
    // Encrypted passwords are Base64-encoded, >100 chars, have IV+ciphertext+tag
    if (password.length < 100) return false

    return try {
        val decoded = Base64.decode(password, Base64.DEFAULT)
        // Check for IV (12 bytes) + minimum ciphertext
        decoded.size >= 12 + 16
    } catch (e: Exception) {
        false  // Not valid Base64 = not encrypted
    }
}
```

## Build Commands

### Building the app
```bash
./gradlew build
```

### Running tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Creating APK
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

### Linting
```bash
./gradlew lint
```

## Architecture

The app follows Clean Architecture principles with strict separation of concerns:

### Core Components

| Directory | Purpose | Key Files |
|-----------|---------|-----------|
| **data/** | Data layer | Room database, entities, DAOs, repositories |
| **domain/** | Business logic | Use cases for CRUD operations |
| **ui/** | Presentation | Compose screens, ViewModels, state management |
| **di/** | Dependency injection | AppModule (manual DI) |
| **utils/** | Cross-cutting concerns | Security, backup, biometric, file operations |

### Critical Security Components (v2.0+)

#### 1. SecurityManager.kt
**Location**: `app/src/main/java/com/securevault/utils/SecurityManager.kt`
**Purpose**: Core encryption layer using Android Keystore
**Key Methods**:
```kotlin
class SecurityManager(context: Context) {
    private val keyStore: KeyStore  // Android Keystore instance

    // Hardware-backed key generation
    private fun generateNewKey(): SecretKey {
        // Creates AES-256 key in hardware TEE
        // setRandomizedEncryptionRequired(true) forces Keystore IV generation
    }

    // Encrypt with automatic IV generation
    fun encrypt(text: String): String {
        // 1. Get key from Keystore (never leaves hardware)
        // 2. Initialize cipher: AES/GCM/NoPadding
        // 3. Keystore auto-generates IV
        // 4. Encrypt with AES-256-GCM
        // 5. Extract IV: cipher.getIV()
        // 6. Return Base64(IV + Ciphertext + AuthTag)
    }

    // Decrypt with extracted IV
    fun decrypt(encryptedText: String): String {
        // 1. Base64 decode
        // 2. Extract IV (first 12 bytes)
        // 3. Extract ciphertext (remaining bytes)
        // 4. Decrypt and verify GCM auth tag
    }
}
```

**Security Guarantees**:
- Keys stored in hardware TEE/Secure Element
- Keys non-extractable even on rooted devices
- Automatic IV generation from hardware entropy
- GCM authentication prevents tampering

#### 2. PasswordRepositoryImpl.kt
**Location**: `app/src/main/java/com/securevault/data/repository/PasswordRepositoryImpl.kt`
**Purpose**: Data access with encryption integration
**Key Methods**:
```kotlin
class PasswordRepositoryImpl(
    private val passwordDao: PasswordDao,
    private val securityManager: SecurityManager  // Injected in v2.0+
) : PasswordRepository {

    // Save with encryption
    override suspend fun savePassword(password: Password): Boolean {
        val encryptedEntity = encryptPassword(password)  // Encrypt before save
        passwordDao.insertPassword(encryptedEntity)
    }

    // Retrieve with decryption
    override suspend fun getPassword(id: String): Password? {
        val entity = passwordDao.getPasswordById(id)
        return entity?.let { decryptPasswordEntity(it) }  // Decrypt after retrieve
    }

    // Atomic restore with transaction
    @Transaction
    override suspend fun replaceAllPasswords(passwords: List<Password>): Boolean {
        passwordDao.deleteAllPasswords()  // Step 1
        val encrypted = passwords.map { encryptPassword(it) }
        passwordDao.insertPasswords(encrypted)  // Step 2
        // Commit only if both succeed, rollback otherwise
    }
}
```

#### 3. MigrationManager.kt
**Location**: `app/src/main/java/com/securevault/utils/MigrationManager.kt`
**Purpose**: One-time migration from v1.x plain text to v2.0 encrypted
**Trigger**: MainActivity.onCreate() on first launch after v2.0 update
**Process**:
1. Check if migration already completed (SharedPreferences flag)
2. Get all passwords from database
3. Detect which are plain text vs already encrypted
4. Re-encrypt plain text passwords with SecurityManager
5. Delete old SharedPreferences encryption key
6. Mark migration complete

**User Experience**: Seamless, automatic, < 1 second for typical datasets

#### 4. QuantumBackupEncryption.kt
**Location**: `app/src/main/java/com/securevault/utils/QuantumBackupEncryption.kt`
**Purpose**: Quantum-resistant backup encryption (v2.0 format)
**Algorithm**: Hybrid ML-KEM-768 + X25519 + AES-256-GCM
**Dependencies**: Bouncy Castle 1.79 (`org.bouncycastle:bcprov-jdk18on`)

#### 5. BackupEncryption.kt
**Location**: `app/src/main/java/com/securevault/utils/BackupEncryption.kt`
**Purpose**: Legacy backup encryption (v1.0 format) - **BACKWARD COMPATIBILITY**
**Algorithm**: PBKDF2-HMAC-SHA512 (100k iterations) + AES-256-CBC
**Status**: Still used for decrypting old backups from v1.x

#### 6. BackupManager.kt
**Location**: `app/src/main/java/com/securevault/utils/BackupManager.kt`
**Purpose**: Backup/restore orchestration with version detection
**Key Logic**:
```kotlin
fun restoreFromData(backupJson: String, password: String): RestoreResult {
    val backupData = gson.fromJson(backupJson, BackupData::class.java)

    // Version-based router
    val decryptedJson = when (backupData?.version) {
        "2.0" -> quantumBackupEncryption.decrypt(backupData, password)
        "1.0", null -> backupEncryption.decrypt(backupData.data, password)
        else -> throw UnsupportedVersionException()
    }

    // Parse and restore with encryption
    val passwords = parsePasswords(decryptedJson)
    passwordRepository.replaceAllPasswords(passwords)  // @Transaction
}
```

### Navigation Architecture

**Single-activity with Compose Navigation:**
- **MainScreen**: Password list with search (FocusRequester + BackHandler for UX)
- **FormScreen**: Add/edit passwords with validation
- **DetailScreen**: View password details with clipboard management
- **SettingsScreen**: Backup/restore, biometric setup, theme selection

**State Management**:
- ViewModels hold UI state (StateFlow)
- Repository pattern for data access
- Compose state for reactive UI updates

### Dependencies Management

**Gradle Version Catalogs** (`gradle/libs.versions.toml`):
- **Compose BOM**: Jetpack Compose UI framework
- **Room**: Local encrypted database (`androidx.room.runtime`, `androidx.room.ktx`)
- **Biometric**: Fingerprint/face authentication
- **Security Crypto**: Android security utilities
- **Bouncy Castle**: Post-quantum cryptography (`org.bouncycastle:bcprov-jdk18on:1.79`)
- **Gson**: JSON serialization for backups
- **Kotlin kapt**: Annotation processing for Room

**ProGuard Configuration** (`app/proguard-rules.pro`):
```proguard
# Keep Bouncy Castle PQC classes
-keep class org.bouncycastle.pqc.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-dontwarn org.bouncycastle.**

# Keep Room entities
-keep class com.securevault.data.model.** { *; }
```

## Testing

### Test Structure
- **Unit Tests**: `app/src/test/` - Business logic, encryption, migration
- **Instrumented Tests**: `app/src/androidTest/` - UI, database, integration
- **Frameworks**: JUnit 4, Espresso, Compose UI Testing

### Critical Test Scenarios (v2.0+)

**Encryption Tests**:
```bash
# Test SecurityManager encryption/decryption
./gradlew test --tests SecurityManagerTest

# Verify encryption key in Keystore (not SharedPreferences)
./gradlew test --tests "SecurityManagerTest.encryption key stored in Android Keystore"
```

**Migration Tests**:
```bash
# Test plain text → encrypted migration
./gradlew test --tests MigrationManagerTest

# Verify migration runs only once
./gradlew test --tests "MigrationManagerTest.migration does not run twice"
```

**Backup Compatibility Tests**:
```bash
# Test v1.0 backup restore (backward compatibility)
./gradlew connectedAndroidTest --tests "BackupRestoreIntegrationTest.restore v1_0 backup"

# Test v2.0 quantum-encrypted backup
./gradlew connectedAndroidTest --tests "BackupRestoreIntegrationTest.restore v2_0 backup"
```

**Database Encryption Verification**:
```bash
# Install debug build
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Add test password

# Verify password is encrypted in database (not plain text)
adb shell "run-as com.securevault.debug cat /data/data/com.securevault.debug/databases/password_database" | strings

# Should see Base64 garbage, NOT plain text passwords
```

### Manual Testing Checklist (v2.0)

**Database Encryption**:
- [ ] Save password → verify encrypted in database
- [ ] Retrieve password → verify decrypts correctly
- [ ] Check SharedPreferences → NO encryption key present
- [ ] Root device → verify key not extractable from Keystore

**Backup/Restore**:
- [ ] Create v2.0 backup → verify quantum metadata in JSON
- [ ] Restore v2.0 backup → all passwords restored correctly
- [ ] Restore v1.0 backup (from v1.4.0) → backward compatibility works
- [ ] Wrong password → restore fails gracefully

**Migration**:
- [ ] Fresh install → no migration needed
- [ ] Upgrade from v1.x → migration completes automatically
- [ ] Migration completes only once
- [ ] After migration → all passwords encrypted

**Performance**:
- [ ] Password save: < 50ms
- [ ] Password retrieve: < 50ms
- [ ] Backup creation (100 passwords): < 10 seconds
- [ ] Restore (100 passwords): < 15 seconds

## Critical Bugs & Fixes

### Bug #1: Backup Restore Failure (v2.0.0 - v2.0.1)
**Symptom**: `java.security.InvalidAlgorithmParameterException: Caller-provided IV not permitted`

**Root Cause**: SecurityManager was manually generating IV when `setRandomizedEncryptionRequired(true)` requires Keystore to auto-generate.

**Fixed in**: v2.0.2

**Fix**:
```kotlin
// BEFORE (Broken)
val iv = ByteArray(12)
SecureRandom().nextBytes(iv)
cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

// AFTER (Fixed)
cipher.init(Cipher.ENCRYPT_MODE, key)  // No IV parameter
val iv = cipher.getIV()  // Extract auto-generated IV
```

### Bug #2: Search UX Issues (v2.0.0)
**Symptom**: Keyboard doesn't auto-show, back button exits app from search

**Fixed in**: v2.0.1

**Fix**:
```kotlin
// Auto-focus and keyboard show
val focusRequester = remember { FocusRequester() }
val keyboardController = LocalSoftwareKeyboardController.current

LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    keyboardController?.show()
}

// Back button handling
BackHandler(enabled = showSearch) {
    showSearch = false
    viewModel.updateSearchQuery("")
}
```

### Bug #3: Update Mechanism Diagnostics (v2.0.2)
**Symptom**: "Download URL not available" error

**Fixed in**: v2.0.3

**Improvements**:
- Enhanced logging for GitHub API responses
- Android 10+ scoped storage compatibility (`setDestinationInExternalFilesDir`)
- User-Agent headers for API requests
- Browser fallback (already working)

## Troubleshooting

### Database Inspection
```bash
# View database file (should show encrypted passwords)
adb shell "run-as com.securevault cat /data/data/com.securevault/databases/password_database" | strings

# Check SharedPreferences (should NOT have encryption key in v2.0+)
adb shell "run-as com.securevault cat /data/data/com.securevault/shared_prefs/security_prefs.xml"

# List Keystore aliases (should contain securevault_master_key)
adb shell "run-as com.securevault ls /data/misc/keystore/user_0"
```

### Logcat Debugging
```bash
# Filter SecureVault logs
adb logcat | grep -E "(SecurityManager|MigrationManager|BackupManager|PasswordRepositoryImpl)"

# Watch for encryption errors
adb logcat | grep -E "(Encryption failed|Decryption failed|Migration failed)"

# Watch for backup/restore
adb logcat | grep -E "(Backup|Restore|QuantumBackup)"
```

### Common Issues

**Issue**: Migration not running
**Solution**: Clear app data or check `migration_prefs.xml` for completion flag

**Issue**: Passwords not decrypting
**Solution**: Verify Android Keystore key exists, check for key rotation

**Issue**: v1.0 backup not restoring
**Solution**: Check version detection logic in BackupManager.kt, verify BackupEncryption.kt unchanged

**Issue**: Bouncy Castle ClassNotFoundException
**Solution**: Verify ProGuard rules keep BC classes, check packaging exclusions in build.gradle.kts

## Release & Deployment

### Version Management

**Version Configuration** (`app/build.gradle.kts`):
```kotlin
versionCode = 20        // Increment by 1 for each release
versionName = "2.0.3"   // Semantic versioning: MAJOR.MINOR.PATCH
```

**Versioning Strategy**:
- **MAJOR** (2.x.x): Breaking changes, major security overhauls
- **MINOR** (x.1.x): New features, backward-compatible changes
- **PATCH** (x.x.1): Bug fixes, performance improvements

**Version Code History**:
- versionCode 20 = v2.0.3 (update improvements)
- versionCode 19 = v2.0.2 (backup fix)
- versionCode 18 = v2.0.1 (search UX)
- versionCode 17 = v2.0.0 (security overhaul)
- versionCode 15 = v1.5.1 (quantum backups)
- versionCode 14 = v1.4.0 (update mechanism)

### Release Process (Automated)

**Step 1: Update Version**
```bash
# Edit app/build.gradle.kts
versionCode = 21
versionName = "2.0.4"
```

**Step 2: Commit Changes**
```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.0.4

Changes in this release:
- Bug fix: ...
- Feature: ...

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

**Step 3: Create Git Tag**
```bash
# Tag format: v<version>
git tag v2.0.4
git push origin main
git push origin v2.0.4
```

**Step 4: GitHub Actions (Automatic)**
- Workflow: `.github/workflows/release.yml`
- Trigger: Tag push matching `v*`
- Actions:
  1. Checkout repository
  2. Set up JDK 11
  3. Decode keystore from secrets
  4. Build release APK (signed)
  5. Create GitHub Release
  6. Upload `app-release.apk` as asset

**Step 5: Verify Release**
- Check GitHub Releases page
- Download APK and verify signature
- Test in-app update mechanism

### GitHub Actions Workflow

**File**: `.github/workflows/release.yml`

**Key Steps**:
```yaml
- name: Decode Keystore
  run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks

- name: Build Release APK
  env:
    SIGNING_KEY_ALIAS: ${{ secrets.SIGNING_KEY_ALIAS }}
    SIGNING_KEY_PASSWORD: ${{ secrets.SIGNING_KEY_PASSWORD }}
    SIGNING_STORE_PASSWORD: ${{ secrets.SIGNING_STORE_PASSWORD }}
  run: ./gradlew assembleRelease --no-daemon

- name: Create Release
  uses: softprops/action-gh-release@v1
  with:
    files: app/build/outputs/apk/release/app-release.apk
```

### Signing Configuration

**GitHub Secrets Required**:
- `KEYSTORE_BASE64`: Base64-encoded keystore file
  ```bash
  base64 -i app/keystore.jks | pbcopy  # macOS
  base64 app/keystore.jks              # Linux
  ```
- `SIGNING_KEY_ALIAS`: `securevault` (default)
- `SIGNING_KEY_PASSWORD`: Key password
- `SIGNING_STORE_PASSWORD`: Keystore password

**Local Keystore Generation**:
```bash
keytool -genkey -v \
  -keystore app/keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias securevault
```

**Signing Versions Enabled** (for maximum compatibility):
```kotlin
signingConfigs {
    create("release") {
        enableV1Signing = true  // JAR signing
        enableV2Signing = true  // APK Signature Scheme v2
        enableV3Signing = true  // APK Signature Scheme v3
        enableV4Signing = true  // APK Signature Scheme v4
    }
}
```

### ProGuard Mapping Preservation

**Purpose**: Deobfuscate crash reports from production

**Configuration** (`app/build.gradle.kts`):
```kotlin
tasks.register("saveProguardMapping", Copy::class) {
    from(layout.buildDirectory.dir("outputs/mapping/release"))
    into(rootProject.layout.projectDirectory.dir("proguard-mappings/v${android.defaultConfig.versionName}"))
    include("mapping.txt")
}

afterEvaluate {
    tasks.findByName("assembleRelease")?.finalizedBy("saveProguardMapping")
}
```

**Mapping Files Location**: `proguard-mappings/v2.0.3/mapping.txt`

**Usage**: Upload mapping.txt to Google Play Console or use locally to deobfuscate stack traces

### Manual Release (Emergency)

**Build Locally**:
```bash
# Set signing environment variables
export SIGNING_KEY_ALIAS=securevault
export SIGNING_KEY_PASSWORD=your_password
export SIGNING_STORE_PASSWORD=your_password

# Build release APK
./gradlew assembleRelease

# APK location
ls -lh app/build/outputs/apk/release/app-release.apk
```

**Create Release Manually**:
1. Go to GitHub → Releases → Draft new release
2. Choose tag: v2.0.x
3. Release title: SecureVault v2.0.x
4. Description: Copy from commit messages
5. Upload `app-release.apk`
6. Publish release

### In-App Update Mechanism

**UpdateManager.kt** checks GitHub Releases API:
```kotlin
// API endpoint
https://api.github.com/repos/akshitharsola/Secure-Vault/releases/latest

// Response parsing
{
  "tag_name": "v2.0.3",
  "assets": [
    {
      "name": "app-release.apk",
      "browser_download_url": "https://github.com/.../app-release.apk"
    }
  ]
}

// Version comparison
if (latestVersion > currentVersion) {
  showUpdateDialog()
}
```

**User Flow**:
1. App checks for updates on launch (debounced)
2. If update available, shows dialog
3. User taps "Update"
4. Downloads APK via DownloadManager or opens browser
5. User installs update (requires "Install from unknown sources")

## Project Documentation Files

| File | Purpose | Last Updated |
|------|---------|--------------|
| **README.md** | Comprehensive project documentation, features, architecture | v2.0.3 (Jan 2026) |
| **LICENSE** | MIT License with security disclaimers, contributors | v2.0.3 (Jan 2026) |
| **CONTRIBUTORS.md** | Contributor guidelines, recognition system, code of conduct | v2.0.3 (Jan 2026) |
| **CLAUDE.md** | Developer guide for Claude Code (this file) | v2.0.3 (Jan 2026) |
| **MIGRATION_GUIDE.md** | v1.x → v2.0 upgrade instructions | v1.5.0 |
| **DATABASE_ENCRYPTION_IMPLEMENTATION.md** | Technical deep dive on database encryption | v2.0.0 |
| **QUANTUM_BACKUP_PROGRESS.md** | Quantum encryption implementation notes | v1.5.1 |
| **MULTI_AGENT_WORKFLOW.md** | Development workflow documentation | v1.4.0 |
| `.github/workflows/release.yml` | Automated release workflow | v1.4.0 |

## Commit Message Guidelines

### Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **refactor**: Code refactoring (no functional changes)
- **perf**: Performance improvements
- **test**: Adding/updating tests
- **chore**: Build process, dependencies, version bumps
- **security**: Security-related changes

### Examples

**Feature Commit**:
```bash
git commit -m "$(cat <<'EOF'
feat(encryption): implement hardware-backed database encryption

- Migrate from SharedPreferences to Android Keystore
- AES-256-GCM encryption for all password fields
- Automatic IV generation via setRandomizedEncryptionRequired
- Keys stored in hardware TEE/Secure Element

BREAKING CHANGE: Requires automatic migration from v1.x

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

**Bug Fix Commit**:
```bash
git commit -m "$(cat <<'EOF'
fix(security): resolve Keystore IV generation error

Fixed "Caller-provided IV not permitted" exception when
setRandomizedEncryptionRequired(true) is set. Now using
cipher.getIV() to extract auto-generated IV instead of
manually creating IV.

Fixes backup restore failure in v2.0.0-v2.0.1.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

**Documentation Commit**:
```bash
git commit -m "$(cat <<'EOF'
docs: comprehensive documentation overhaul for v2.0

- Updated LICENSE with contributors and security disclaimers
- Rewrote README.md with v2.0 architecture and features
- Created CONTRIBUTORS.md with guidelines and recognition

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

### Co-Author Attribution

**All commits** should include Claude Code co-authorship when AI assisted:
```
Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

This ensures proper recognition in GitHub's contributor graph and commit history.

## Development Workflow

### Initial Setup
```bash
# Clone repository
git clone https://github.com/akshitharsola/Secure-Vault.git
cd Secure-Vault

# Open in Android Studio
# File → Open → Select Secure-Vault directory

# Sync Gradle
./gradlew build

# Run on emulator/device
./gradlew installDebug
```

### Development Cycle
```bash
# Create feature branch (optional)
git checkout -b feature/password-strength-analyzer

# Make changes
# ... edit files ...

# Test locally
./gradlew test
./gradlew connectedAndroidTest

# Commit with co-authorship
git add .
git commit -m "feat: add password strength analyzer

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

# Push to main (or create PR)
git push origin main
```

### Release Cycle
```bash
# Update version in app/build.gradle.kts
# versionCode = 21
# versionName = "2.0.4"

# Commit version bump
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.0.4"
git push origin main

# Create and push tag
git tag v2.0.4
git push origin v2.0.4

# GitHub Actions builds and releases automatically
# Verify at: https://github.com/akshitharsola/Secure-Vault/releases
```

## Security Considerations for Developers

### Critical Security Rules

1. **NEVER** store encryption keys outside Android Keystore
2. **NEVER** log decrypted passwords or sensitive data
3. **NEVER** disable encryption for "debugging purposes"
4. **ALWAYS** use `@Transaction` for multi-step database operations
5. **ALWAYS** test migration paths before releasing
6. **ALWAYS** verify backward compatibility with old backups

### Code Review Checklist (Security)

**Before Committing**:
- [ ] No hardcoded secrets or API keys
- [ ] No plain text password storage
- [ ] Encryption keys use Android Keystore
- [ ] GCM authentication tags verified
- [ ] IV generation uses Keystore (not manual)
- [ ] Transaction safety for critical operations
- [ ] Backward compatibility maintained
- [ ] Migration tested on real data

**Before Releasing**:
- [ ] ProGuard mappings saved
- [ ] All tests passing
- [ ] No debug logging in production
- [ ] Version code incremented
- [ ] Changelog updated
- [ ] GitHub Secrets configured
- [ ] APK signature verified

### Security Testing Commands

```bash
# Check for hardcoded secrets
grep -r "password\|secret\|key" app/src/main/java/ | grep -v "// "

# Verify encryption in database
adb shell "run-as com.securevault cat databases/password_database" | strings | grep -i "password"
# Should NOT show plain text passwords

# Verify Keystore key exists
adb shell "run-as com.securevault ls -la /data/misc/keystore/user_0" | grep securevault

# Test root access (on rooted device)
adb shell su -c "cat /data/data/com.securevault/databases/password_database"
# Should still be encrypted
```

## Useful Resources

### Android Security
- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [Biometric Authentication](https://developer.android.com/training/sign-in/biometric-auth)
- [Security Best Practices](https://developer.android.com/topic/security/best-practices)

### Cryptography
- [NIST Post-Quantum Cryptography](https://csrc.nist.gov/projects/post-quantum-cryptography)
- [ML-KEM Specification](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.203.pdf)
- [AES-GCM Best Practices](https://tools.ietf.org/html/rfc5116)

### Jetpack Compose
- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [State Management](https://developer.android.com/jetpack/compose/state)

### Testing
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Espresso Documentation](https://developer.android.com/training/testing/espresso)
- [JUnit 4](https://junit.org/junit4/)

---

## Quick Reference

### Important File Locations
```
app/src/main/java/com/securevault/
├── data/
│   ├── model/          # Password, BackupData entities
│   ├── local/          # Room DAO, database
│   └── repository/     # PasswordRepositoryImpl ⚠️ ENCRYPTION HERE
├── domain/
│   └── usecase/        # Business logic use cases
├── ui/
│   ├── screens/        # Compose UI screens
│   └── viewmodel/      # State management
├── utils/
│   ├── SecurityManager.kt             ⚠️ CRITICAL - Keystore encryption
│   ├── MigrationManager.kt            ⚠️ v1.x → v2.0 migration
│   ├── QuantumBackupEncryption.kt     v2.0 quantum backups
│   ├── BackupEncryption.kt            v1.0 legacy backups
│   ├── BackupManager.kt               Backup/restore orchestration
│   ├── BiometricHelper.kt             Biometric authentication
│   └── UpdateManager.kt               In-app updates
└── di/
    └── AppModule.kt                   Dependency injection
```

### Key Version Milestones
- **v2.0.0**: Security overhaul (Android Keystore, migration)
- **v1.5.1**: Quantum-resistant backups (ML-KEM-768)
- **v1.4.0**: In-app update mechanism
- **v1.0-1.3**: ⚠️ VULNERABLE (plain text storage)

### Emergency Contacts
- **Security Issues**: [GitHub Security Advisories](https://github.com/akshitharsola/Secure-Vault/security)
- **Bug Reports**: [GitHub Issues](https://github.com/akshitharsola/Secure-Vault/issues)
- **Maintainer**: [@akshitharsola](https://github.com/akshitharsola)

---

**Last Updated**: January 2026 (v2.0.3)
**Contributors**: Akshit Harsola, Claude Sonnet 4.5