# Privacy Policy for SecureVault

**Last Updated:** January 25, 2026

## Overview

SecureVault is a privacy-first password manager for Android that prioritizes user security and data protection. This privacy policy explains what data we collect, how we use it, and your rights.

## Data Collection

### Data Stored Locally on Your Device

SecureVault stores the following data **locally on your device only**:

- **Passwords:** Encrypted with AES-256-GCM using hardware-backed keys (Android Keystore)
- **Usernames and account details:** Encrypted before database storage
- **App settings:** Theme preferences, biometric settings
- **Backup files:** Encrypted with user-provided password and quantum-resistant encryption

**This data NEVER leaves your device** except when you manually create and share backup files.

### Data Collected by Third Parties

SecureVault integrates Google AdMob for advertising. AdMob may collect:

- **Device Advertising ID:** Resettable identifier for ad targeting
- **Device Information:** Model, OS version, screen size, language
- **Usage Analytics:** App opens, ad impressions (anonymous)
- **Network Information:** IP address, network type

**AdMob does NOT have access to:**
- Your passwords (encrypted in hardware Keystore)
- Usernames or account details
- Backup files
- Encryption keys
- Any personal identifiable information (PII)

For AdMob's privacy policy, visit: https://policies.google.com/privacy

### Data We (Developers) Collect

**We collect ZERO data.** SecureVault does not transmit any information to our servers because we don't operate any servers.

## Data Usage

### How Your Data is Used

- **Passwords:** Stored encrypted locally, decrypted only when you view them
- **Settings:** Used to customize app behavior (themes, biometric setup)
- **Backup files:** Created only when you initiate a backup, encrypted with your password

### How AdMob Data is Used

- **Advertising:** To display relevant ads and support app development
- **Analytics:** To measure ad performance (anonymous)

## Data Sharing

**We do NOT share your data with anyone** because we don't have access to it.

AdMob may share anonymized advertising data with third-party advertisers. See AdMob's privacy policy for details.

## Data Security

### Encryption

- **Database:** AES-256-GCM with hardware-backed keys (Android Keystore)
- **Backups:** ML-KEM-768 + X25519 + AES-256-GCM (quantum-resistant)
- **Transport:** N/A (app is offline, no data transmission)

### Security Features

- Biometric authentication (fingerprint/face unlock)
- Secure PIN fallback
- GCM authentication tags (tamper detection)
- Keys stored in hardware TEE/Secure Element

## Your Rights

### Control Your Data

- **Access:** All your data is on your device - you have full access
- **Deletion:** Uninstall the app or use "Delete All Passwords" in settings
- **Portability:** Export encrypted backups at any time
- **Opt-out of Ads:** Reset advertising ID in Android Settings → Privacy → Ads

### Control Ad Personalization

You can opt out of personalized ads:
1. Open Android **Settings**
2. Go to **Privacy** → **Ads**
3. Enable **Opt out of Ads Personalization**
4. Or tap **Reset advertising ID**

## Children's Privacy

SecureVault is not directed at children under 13. We do not knowingly collect data from children.

## Changes to This Policy

We may update this privacy policy. Changes will be posted on this page with an updated "Last Updated" date.

## Open Source

SecureVault is open source. You can review our code on GitHub:
https://github.com/akshitharsola/Secure-Vault

## Contact

For privacy concerns or questions:
- GitHub Issues: https://github.com/akshitharsola/Secure-Vault/issues
- Security Vulnerabilities: https://github.com/akshitharsola/Secure-Vault/security

## Third-Party Services

### Google AdMob
- Privacy Policy: https://policies.google.com/privacy
- Terms: https://policies.google.com/terms
- Opt-out: Android Settings → Privacy → Ads

### Bouncy Castle (Cryptography Library)
- License: MIT / Apache 2.0
- Used for: Quantum-resistant encryption (ML-KEM-768)

## Legal

SecureVault is provided "as-is" under the MIT License. See our LICENSE file for details.

While we implement industry-standard security practices, no software is 100% secure. Use at your own risk.

---

**Summary:** Your passwords stay on your device, encrypted with hardware keys. We don't collect any personal data. Ads are served by Google AdMob, which collects standard advertising data but has zero access to your passwords.
