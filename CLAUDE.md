# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SecureVault is an Android password manager application built with Kotlin and Jetpack Compose. The app provides secure password storage with biometric authentication, encryption, and backup/restore functionality.

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

The app follows Clean Architecture principles with these key layers:

### Core Components
- **data/**: Data layer with Room database, entities, and DAOs
- **domain/**: Business logic with use cases
- **ui/**: Presentation layer with Compose screens and ViewModels
- **di/**: Dependency injection using manual DI (AppModule)
- **utils/**: Utility classes for security, backup, biometric, and file operations

### Key Architecture Details

**Database**: Room database with `PasswordEntity` storing encrypted password data
**Security**: 
- Biometric authentication with PIN fallback
- AES encryption for password storage and backups
- Android Keystore integration via `SecurityManager`

**Navigation**: Single-activity architecture using Compose Navigation with these screens:
- MainScreen: Password list with search
- FormScreen: Add/edit passwords
- DetailScreen: View password details
- SettingsScreen: App settings and backup/restore

**State Management**: ViewModels with Repository pattern, using Compose state for UI

### Dependencies Management
- Uses Gradle version catalogs (`gradle/libs.versions.toml`)
- Key dependencies: Compose, Room, Biometric, Security Crypto, Gson
- Kotlin kapt for Room annotation processing

### Security Features
- `BiometricHelper`: Handles biometric authentication setup
- `BackupEncryption`: Encrypts/decrypts backup files
- `SecurityManager`: Manages encryption keys via Android Keystore
- `ClipboardManager`: Secure clipboard operations with auto-clear

## Testing
- Unit tests in `src/test/`
- Instrumented tests in `src/androidTest/`
- Uses JUnit 4 and Espresso for testing

## Release & Deployment

### GitHub Actions
- Automated APK builds on tag pushes (e.g., `v1.0.0`)
- Release workflow in `.github/workflows/release.yml`
- Builds both debug and release APKs
- Creates GitHub releases with proper signing

### Release Process
1. Create git tag: `git tag v1.0.0 && git push origin v1.0.0`
2. GitHub Actions automatically builds and releases APKs
3. Manual builds via Actions tab also supported

### Signing Configuration
- Release APKs signed via GitHub Secrets:
  - `KEYSTORE_BASE64`: Base64 encoded keystore
  - `SIGNING_KEY_ALIAS`: Key alias (default: securevault)
  - `SIGNING_KEY_PASSWORD`: Key password
  - `SIGNING_STORE_PASSWORD`: Keystore password
- Keystore generated with: `keytool -genkey -v -keystore app/keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias securevault`

## Project Files
- `README.md`: Comprehensive project documentation
- `LICENSE`: MIT License with security disclaimer
- `RELEASE_SETUP.md`: GitHub Actions setup instructions
- `.github/workflows/release.yml`: Automated release workflow