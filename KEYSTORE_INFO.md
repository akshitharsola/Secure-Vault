# 🔐 SecureVault Keystore Information

## ⚠️ CRITICAL: Keep This File Secure!

**DO NOT commit this file to GitHub or share publicly!**

This file contains sensitive credentials for signing your Android app. Store it in a secure password manager.

---

## Keystore Details

**File Location:** `app/keystore.jks`
**File Size:** 2.7 KB
**Created:** January 5, 2026

### Credentials

**Key Alias:** `securevault`

**Keystore Password:** `SecureVault2026!Key`

**Key Password:** `SecureVault2026!Key`

**Certificate Fingerprint (SHA-256):**
```
DB:B3:72:AD:43:5A:15:94:F0:FE:E9:D1:BC:77:8F:0A:AA:02:3E:8F:BE:1E:99:12:7A:18:22:C1:87:B2:42:AC
```

### Owner Information

- **Common Name (CN):** Akshit Harsola
- **Organizational Unit (OU):** SecureVault
- **Organization (O):** SecureVault
- **Locality (L):** Unknown
- **State (ST):** Unknown
- **Country (C):** IN

### Key Properties

- **Algorithm:** RSA
- **Key Size:** 2048 bits
- **Validity:** 10,000 days (approximately 27 years)
- **Signature Algorithm:** SHA256withRSA

---

## For GitHub Actions (GitHub Secrets)

When setting up GitHub repository, use these commands:

### 1. Generate Base64 for KEYSTORE_BASE64

```bash
cd /Users/akshitharsola/Documents/SecureAttend/Secure-Vault
base64 -i app/keystore.jks | pbcopy
```

Then:
```bash
gh secret set KEYSTORE_BASE64
# Paste the base64 string when prompted (already in clipboard)
```

### 2. Set Signing Credentials

```bash
gh secret set SIGNING_KEY_ALIAS --body "securevault"
gh secret set SIGNING_KEY_PASSWORD --body "SecureVault2026!Key"
gh secret set SIGNING_STORE_PASSWORD --body "SecureVault2026!Key"
```

### 3. Verify Secrets

```bash
gh secret list
```

Expected output:
```
KEYSTORE_BASE64              Updated 2026-01-05
SIGNING_KEY_ALIAS            Updated 2026-01-05
SIGNING_KEY_PASSWORD         Updated 2026-01-05
SIGNING_STORE_PASSWORD       Updated 2026-01-05
```

---

## Keystore Backup

**IMPORTANT:** Back up the keystore file in multiple secure locations:

1. **Password Manager** (1Password, Bitwarden, etc.)
2. **Encrypted USB drive**
3. **Secure cloud storage** (encrypted)

**Why?** If you lose the keystore, you can NEVER update your published app. Users will have to uninstall and reinstall, losing all data.

---

## Security Notes

1. ✅ The keystore is **excluded from git** via `.gitignore`
2. ✅ Never share these credentials publicly
3. ✅ Store in a secure password manager
4. ✅ Keep multiple encrypted backups
5. ✅ Only use in GitHub Secrets (never hardcode in code)

---

## Verifying Keystore

To verify the keystore is valid:

```bash
keytool -list -v -keystore app/keystore.jks -storepass "SecureVault2026!Key"
```

To view certificate details:

```bash
keytool -list -v -keystore app/keystore.jks -storepass "SecureVault2026!Key" -alias securevault
```

---

## In Case of Compromise

If you suspect the keystore has been compromised:

1. **Generate a new keystore immediately**
2. **Change the app package name** (e.g., `com.securevault.v2`)
3. **Release as a new app** on GitHub
4. **Notify users** about the migration
5. **Create a migration guide** for users

---

**Last Updated:** January 5, 2026
**Owner:** Akshit Harsola
