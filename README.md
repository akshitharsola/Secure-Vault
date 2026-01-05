# SecureVault Android

A secure, offline-first password manager for Android built with modern technologies including Jetpack Compose, Room Database, and biometric authentication.

## Features

### Security
- **AES Encryption**: All passwords encrypted using Android Keystore
- **Biometric Authentication**: Fingerprint and face unlock support
- **PIN Fallback**: Secure PIN authentication when biometrics unavailable
- **Offline-First**: No internet permissions - your data stays local
- **Encrypted Backups**: Export/import with password-protected encryption

### User Experience
- **Material 3 Design**: Modern, clean interface following Material Design 3
- **Dark/Light Themes**: Automatic theme switching based on system preference
- **Search & Filter**: Quick password search and category filtering
- **Auto-Clear Clipboard**: Automatic clipboard clearing after password copy
- **Intuitive Navigation**: Simple, user-friendly interface

### Technical
- **Clean Architecture**: Repository pattern with separation of concerns
- **Jetpack Compose**: Modern declarative UI framework
- **Room Database**: Local encrypted database storage
- **Kotlin**: 100% Kotlin codebase
- **Modern Android**: Targets Android 14 (API 35), supports Android 7.0+ (API 24)

## ⚠️ Important: v1.2.5 Migration Required

**If you're upgrading from v1.2.4 or earlier**, you must perform a one-time migration:

1. **Backup your data** (Settings → Backup)
2. **Uninstall the old version**
3. **Install v1.2.5**
4. **Restore your backup**

📖 **[Read the complete Migration Guide](MIGRATION_GUIDE.md)** for detailed instructions.

**Why?** Previous versions had signing key inconsistencies. This is a **one-time migration** - future updates (v1.2.6+) will work seamlessly without requiring uninstallation.

---

## Screenshots

| Main Screen | Add Password | Settings | Advanced Theme |
|-------------|--------------|----------|----------------|
| ![Main Screen](Screenshots/main_page.jpg) | ![Add Password](Screenshots/add_password_page.jpg) | ![Settings](Screenshots/settings_page.jpg) | ![Advanced Theme](Screenshots/advanced%20theme_main_page.jpg) |

## Installation

### From Releases (Recommended)
1. Download the latest APK from [Releases](https://github.com/YourUsername/SecureVault-Android/releases)
2. Enable "Install from unknown sources" in Android settings
3. Install the APK file
4. Grant biometric permissions when prompted

### From Source
```bash
git clone https://github.com/YourUsername/SecureVault-Android.git
cd SecureVault-Android
./gradlew assembleRelease
```

## Requirements

- **Minimum Android Version**: Android 7.0 (API level 24)
- **Target Android Version**: Android 14 (API level 35)
- **Recommended**: Device with biometric hardware (fingerprint/face)
- **Storage**: ~10MB for app installation

## Architecture

SecureVault follows Clean Architecture principles:

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  ┌─────────────────┐  ┌─────────────────────────────┐│
│  │     Screens     │  │       ViewModels           ││
│  │   (Compose)     │  │    (State Management)      ││
│  └─────────────────┘  └─────────────────────────────┘│
└─────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────┐
│                  Domain Layer                       │
│  ┌─────────────────────────────────────────────────┐│
│  │              Use Cases                          ││
│  │         (Business Logic)                        ││
│  └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────┐
│                   Data Layer                        │
│  ┌─────────────────┐  ┌─────────────────────────────┐│
│  │   Repository    │  │     Local Database         ││
│  │ (Abstraction)   │  │   (Room + Encryption)      ││
│  └─────────────────┘  └─────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### Key Components

- **Screens**: Jetpack Compose UI screens
- **ViewModels**: State management and UI logic
- **Use Cases**: Domain-specific business logic
- **Repository**: Data access abstraction
- **Room Database**: Local encrypted storage
- **Security Manager**: Encryption and key management

## Security Details

### Encryption
- **Algorithm**: AES-256-GCM encryption
- **Key Storage**: Android Keystore system
- **Data**: All passwords encrypted at rest
- **Backups**: Export files are password-encrypted

### Authentication
- **Primary**: Biometric authentication (fingerprint/face)
- **Fallback**: PIN-based authentication
- **Session**: Automatic logout after inactivity

### Privacy
- **No Network**: Zero internet permissions
- **Local Only**: All data stored locally on device
- **No Analytics**: No tracking or data collection
- **Open Source**: Fully auditable code

## Development

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 11 or newer
- Android SDK with API 35

### Setup
```bash
git clone https://github.com/YourUsername/SecureVault-Android.git
cd SecureVault-Android
./gradlew build
```

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

### Build Commands
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing)
./gradlew assembleRelease
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Ensure all tests pass before submitting

## Technologies Used

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit
- **Room Database**: Local database solution
- **Android Keystore**: Secure key management
- **Biometric API**: Fingerprint/face authentication
- **Security Crypto**: Encryption utilities
- **Material 3**: Design system
- **Clean Architecture**: Architectural pattern

## Roadmap

- [ ] Cloud sync (optional, encrypted)
- [ ] Password generator with customizable rules
- [ ] Secure notes storage
- [ ] Password strength analysis
- [ ] Import from other password managers
- [ ] Wear OS companion app
- [ ] Auto-fill service integration

## Support

- **Issues**: Report bugs via [GitHub Issues](https://github.com/YourUsername/SecureVault-Android/issues)
- **Discussions**: Feature requests and questions in [Discussions](https://github.com/YourUsername/SecureVault-Android/discussions)
- **Security**: Report security vulnerabilities privately

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Android Jetpack team for excellent libraries
- Material Design team for design guidelines
- Contributors and testers
- Open source community

---

**⚠️ Security Notice**: This is a password manager application. Please review the code and security practices before using it to store sensitive information. While we follow security best practices, use at your own discretion.