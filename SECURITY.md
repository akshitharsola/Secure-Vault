# Security Policy

## Supported Versions

| Version | Supported          | Status |
| ------- | ------------------ | ------ |
| 2.0.x   | :white_check_mark: | Current - Hardware-backed encryption |
| 1.5.x   | :white_check_mark: | Quantum backups, but vulnerable database |
| < 1.5   | :x:                | VULNERABLE - Plain text storage |

**Recommendation**: All users should upgrade to v2.0.0+ immediately for critical security fixes.

---

## Reporting a Vulnerability

**DO NOT** open public issues for security vulnerabilities.

### Reporting Channels

#### 1. GitHub Security Advisories (Recommended)
- Navigate to: [Security Advisories](https://github.com/akshitharsola/Secure-Vault/security/advisories)
- Click "Report a vulnerability"
- Provide detailed information including:
  - Vulnerability description
  - Steps to reproduce
  - Affected versions
  - Impact assessment
  - Suggested fix (if known)

#### 2. Private Email (Alternative)
- Check LICENSE file for current contact information
- Subject: `[SECURITY] SecureVault Vulnerability Report`
- Include all details from the advisory template above

### Response Timeline

| Severity | Initial Response | Fix Target | Public Disclosure |
|----------|-----------------|------------|-------------------|
| **Critical** | 24 hours | 7 days | After patch release |
| **High** | 48 hours | 14 days | After patch release |
| **Medium** | 72 hours | 30 days | After patch release |
| **Low** | 1 week | 60 days | After patch release |

### Responsible Disclosure

We appreciate security researchers who:
- Allow reasonable time for fixes before public disclosure
- Follow responsible disclosure practices
- Provide detailed reproduction steps
- Suggest potential fixes when possible

**Recognition**: Security researchers will be credited in:
- Release notes
- CONTRIBUTORS.md (with permission)
- Security advisories
- Special mention in README (for critical findings)

---

## Security Features (v2.0+)

### Hardware-Backed Encryption
- **Algorithm**: AES-256-GCM (Galois/Counter Mode)
- **Key Storage**: Android Keystore (TEE/Secure Element)
- **Key Properties**:
  - Hardware-backed (non-extractable)
  - StrongBox Keymaster support (on compatible devices)
  - Automatic IV generation from hardware entropy
  - GCM authentication tags for tamper detection

### Quantum-Resistant Backups
- **Algorithm**: Hybrid ML-KEM-768 + X25519 + AES-256-GCM
- **Key Derivation**: PBKDF2-HMAC-SHA512 (100,000 iterations)
- **Post-Quantum**: NIST-standardized ML-KEM (formerly CRYSTALS-Kyber)
- **Classical Security**: X25519 elliptic curve key exchange

### Authentication
- **Biometric**: Fingerprint and face authentication via BiometricPrompt API
- **Fallback**: Secure PIN authentication
- **Key Binding**: Optional biometric-bound encryption keys

### Data Protection
- **Database**: All passwords encrypted before storage
- **Backups**: User password-protected with quantum-resistant encryption
- **Clipboard**: Automatic clearing after configurable timeout
- **Memory**: Sensitive data cleared after use

### Application Security
- **Permissions**: Zero internet permissions - completely offline
- **Code Obfuscation**: ProGuard/R8 with aggressive optimization
- **Root Detection**: Warning displayed on rooted devices
- **Tamper Protection**: GCM authentication tags verify data integrity

---

## Known Vulnerabilities

### CVE-SECUREVAULT-2024-001 (CRITICAL - FIXED)

**Vulnerability**: Plain Text Password Storage
**Affected Versions**: v1.0.0 - v1.5.0
**Fixed In**: v2.0.0
**Discovered**: Internal security audit (January 2026)
**CVE ID**: Pending (internal reference only)

**Description**:
Passwords were stored in plain text in the Room database. Encryption keys were stored in SharedPreferences instead of Android Keystore. The SecurityManager existed but was not integrated into the password save/retrieve flow.

**Impact**:
- Anyone with device access could read passwords directly from database file
- Root access or ADB backup extraction exposed all passwords
- Encryption keys easily extractable from SharedPreferences

**Attack Vector**:
```bash
# Plain text passwords readable
adb backup com.securevault
# Extract backup.ab → tar → read database
```

**Fix**:
- All passwords now encrypted with AES-256-GCM before database storage
- Encryption keys migrated to Android Keystore (hardware-backed)
- Automatic one-time migration from plain text to encrypted storage
- Transaction-safe restore with atomic rollback

**CVSS Score**: 9.1 (Critical)
- Attack Vector: Local
- Attack Complexity: Low
- Privileges Required: None
- User Interaction: None
- Scope: Unchanged
- Confidentiality: High
- Integrity: High
- Availability: None

**Recommendation**: **IMMEDIATE UPGRADE REQUIRED** - All users on v1.x must update to v2.0.0+

---

### CVE-SECUREVAULT-2026-002 (HIGH - FIXED)

**Vulnerability**: Backup Restore Failure (Keystore IV Generation)
**Affected Versions**: v2.0.0 - v2.0.1
**Fixed In**: v2.0.2
**Discovered**: User report (January 2026)
**CVE ID**: N/A (operational bug, not exploitable)

**Description**:
SecurityManager manually generated IV when `setRandomizedEncryptionRequired(true)` requires Keystore to auto-generate. This caused `InvalidAlgorithmParameterException` preventing backup restoration.

**Impact**:
- Users unable to restore backups after v2.0.0 upgrade
- Denial of service (data recovery failure)
- No confidentiality or integrity impact

**Attack Vector**: None (operational failure, not exploitable)

**Fix**:
```kotlin
// Before (Broken)
val iv = ByteArray(12)
SecureRandom().nextBytes(iv)
cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

// After (Fixed)
cipher.init(Cipher.ENCRYPT_MODE, key)  // Keystore generates IV
val iv = cipher.getIV()  // Extract auto-generated IV
```

**CVSS Score**: 5.5 (Medium-High)
- Attack Vector: Local
- Attack Complexity: Low
- Privileges Required: None
- User Interaction: Required
- Scope: Unchanged
- Confidentiality: None
- Integrity: None
- Availability: High

**Recommendation**: Upgrade to v2.0.2+ if experiencing backup restore issues

---

## Security Audit History

### January 2026 - v2.0.0 Security Overhaul
**Auditor**: Internal (Akshit Harsola + Claude Sonnet 4.5)
**Scope**: Complete security architecture review
**Duration**: 14 days

**Areas Audited**:
- [x] Encryption implementation (Android Keystore integration)
- [x] Key generation and storage mechanisms
- [x] Password save/retrieve flow
- [x] Backup encryption (quantum-resistant design)
- [x] Database security (migration from plain text)
- [x] Biometric authentication implementation
- [x] Transaction safety (atomic operations)
- [x] Memory management (sensitive data clearing)

**Findings**:
1. **Critical**: Plain text password storage (CVE-SECUREVAULT-2024-001)
2. **Critical**: Encryption keys in SharedPreferences (insecure)
3. **High**: No encryption integration in repository layer
4. **Medium**: Backup encryption vulnerable to quantum attacks

**Remediation**:
- Implemented hardware-backed Android Keystore encryption
- Migrated all passwords to encrypted storage
- Added quantum-resistant backup encryption (ML-KEM-768)
- Automatic migration system for existing users

**Result**: All critical and high severity findings resolved in v2.0.0

---

### Future Audit Plans

**Next Audit**: Planned for Q2 2026 (v2.1.0 release)
**Focus Areas**:
- Third-party security audit (external firm)
- Penetration testing
- Quantum encryption verification
- Android 15+ compatibility
- Hardware security module integration

**Community Audits**: We welcome security researchers to audit the codebase and report findings responsibly.

---

## Security Best Practices for Users

### Device Security
1. **Keep Device Updated**: Install Android security patches regularly
2. **Strong Lock Screen**: Use PIN/password/biometric lock screen
3. **Avoid Root**: Rooted devices have reduced security guarantees
4. **Trusted Sources**: Only install SecureVault from official GitHub releases

### Password Security
1. **Strong Master Password**: Use 16+ character backup password
2. **Unique Passwords**: Never reuse passwords across services
3. **Regular Backups**: Create encrypted backups monthly
4. **Secure Storage**: Store backups offline (USB drive, not cloud)

### Backup Security
1. **Strong Backup Password**: Use long, random password for backups
2. **Offline Storage**: Store backups on encrypted USB drive
3. **Test Restores**: Periodically verify backup restoration works
4. **Version Tracking**: Keep backups from multiple app versions

### Privacy
1. **Zero Permissions**: App requires no internet - keep it that way
2. **Clipboard Security**: Enable auto-clear clipboard (default: 30 seconds)
3. **Screen Privacy**: Enable screenshot blocking (if supported)
4. **App Verification**: Verify APK signature matches official releases

---

## Threat Model

### Protected Against

| Threat | Protection | Confidence |
|--------|-----------|------------|
| **Device theft (locked)** | Hardware-backed encryption, biometric auth | ⭐⭐⭐⭐⭐ |
| **Root access** | Keystore keys non-extractable from TEE | ⭐⭐⭐⭐⭐ |
| **ADB backup extraction** | Database encrypted, keys in Keystore | ⭐⭐⭐⭐⭐ |
| **Memory dumps** | Keys never in app memory | ⭐⭐⭐⭐⭐ |
| **Quantum computer attacks (backups)** | ML-KEM-768 quantum-resistant | ⭐⭐⭐⭐⭐ |
| **Data tampering** | GCM authentication tags | ⭐⭐⭐⭐⭐ |
| **Side-channel attacks** | GCM constant-time operations | ⭐⭐⭐⭐ |

### Not Protected Against

| Threat | Reason | Mitigation |
|--------|--------|------------|
| **Device unlocked + malicious accessibility service** | OS-level access | Use trusted apps only |
| **Compromised Android Keystore** | Hardware/firmware vulnerability | Keep device updated |
| **Physical device compromise (unlocked)** | Direct memory access | Lock device when not in use |
| **Weak user backup passwords** | User choice | Use strong backup passwords |
| **Shoulder surfing** | Social engineering | Be aware of surroundings |

### Assumptions

1. **Android Keystore is secure** - We trust Google's implementation
2. **Hardware TEE is not compromised** - Device firmware is trusted
3. **Android OS is not backdoored** - Stock Android is secure
4. **ML-KEM-768 is quantum-safe** - NIST standardization is correct
5. **User follows best practices** - Strong passwords, device security

---

## Cryptographic Implementation Details

### Encryption Algorithms

**Database Encryption**:
```
Algorithm: AES-256-GCM
Mode: Galois/Counter Mode
Key Size: 256 bits
IV Size: 12 bytes (96 bits)
Tag Size: 16 bytes (128 bits)
Key Storage: Android Keystore (hardware-backed)
```

**Backup Encryption (v2.0)**:
```
Key Exchange: Hybrid ML-KEM-768 + X25519
Symmetric: AES-256-GCM
KDF: PBKDF2-HMAC-SHA512 (100,000 iterations)
Salt: 32 bytes (random per backup)
Nonce: 12 bytes (random per backup)
```

**Legacy Backup Encryption (v1.0)**:
```
Algorithm: AES-256-CBC
KDF: PBKDF2-HMAC-SHA512 (100,000 iterations)
Key Size: 256 bits
IV Size: 16 bytes
Salt: 32 bytes
```

### Key Management

**Key Generation**:
- Android Keystore generates keys in hardware TEE
- `setRandomizedEncryptionRequired(true)` forces IV randomization
- Keys bound to device (non-exportable)
- Optional biometric binding

**Key Rotation**:
- Database key: One-time generation, persistent
- Backup key: Derived per-backup from user password
- Migration: Automatic re-encryption with new Keystore key

**Key Deletion**:
- On app uninstall: Keystore keys automatically deleted
- On data clear: Keystore keys preserved (reinstall works)
- Manual: User can reset app data to delete keys

---

## Security Checklist for Developers

### Before Committing
- [ ] No hardcoded secrets or API keys
- [ ] No plain text password logging
- [ ] Encryption uses Android Keystore
- [ ] GCM authentication tags verified
- [ ] IV generation uses Keystore (not manual)
- [ ] Transaction safety for database operations
- [ ] Backward compatibility maintained
- [ ] Migration tested on real data

### Before Releasing
- [ ] ProGuard mappings saved
- [ ] All security tests passing
- [ ] No debug logging in production
- [ ] Version code incremented
- [ ] Changelog includes security notes
- [ ] APK signature verified
- [ ] Security audit completed

---

## Contact

**Security Issues**: [GitHub Security Advisories](https://github.com/akshitharsola/Secure-Vault/security/advisories)

**General Issues**: [GitHub Issues](https://github.com/akshitharsola/Secure-Vault/issues)

**Maintainer**: [@akshitharsola](https://github.com/akshitharsola)

---

**Last Updated**: January 2026 (v2.0.3)

**Contributors**: Akshit Harsola, Claude Sonnet 4.5
