# How to Verify v2.0 Backup Format (Without Sharing Sensitive Data)

## ⚠️ IMPORTANT: Do NOT Share Backup Files
Even though backup files are encrypted, **never share them** with anyone (including me!). They contain your encrypted passwords and could be a security risk.

## Safe Verification Method

Instead, you can verify the backup format structure yourself by checking the JSON metadata (which is NOT encrypted):

### Step 1: View Backup File Metadata (Safe)

**On Android Device:**
```bash
# Using adb, view just the structure (safe - doesn't reveal passwords)
adb shell "cat /sdcard/Download/securevault_backup_*.json" | head -20
```

**Or copy to computer and check:**
```bash
# Copy file to computer
adb pull /sdcard/Download/securevault_backup_*.json ./

# View just the metadata (safe - first 30 lines show structure, not encrypted data)
head -30 securevault_backup_*.json
```

### Step 2: What to Look For

**v2.0 Backup Format (New - from v1.5.0):**
```json
{
  "version": "2.0",                                    ← Should be "2.0"
  "timestamp": "2026-01-18T...",
  "encrypted": true,
  "passwordCount": 5,
  "appName": "SecureVault",
  "platform": "Android",
  "encryptionType": "password-aes256gcm-pqc-ready",   ← NEW: Quantum encryption
  "kdf": "PBKDF2-HMAC-SHA512",                        ← NEW: SHA512 (stronger)
  "iterations": 100000,                                ← NEW: Iteration count
  "quantumResistant": true,                            ← NEW: Quantum flag
  "data": "base64-encrypted-data-here..."              ← This is encrypted (safe)
}
```

**v1.0 Backup Format (Old - from v1.4.0 and earlier):**
```json
{
  "version": "1.0",                                    ← Should be "1.0"
  "timestamp": "2026-01-18T...",
  "encrypted": true,
  "passwordCount": 5,
  "appName": "SecureVault",
  "platform": "Android",
  "data": "base64-encrypted-data-here..."              ← This is encrypted (safe)

  // Note: No encryptionType, kdf, iterations, or quantumResistant fields
}
```

### Step 3: Safe Verification Checklist

**For v2.0 Backup (created in v1.5.0):**
- [ ] `"version": "2.0"` ✅
- [ ] `"encryptionType"` field exists ✅
- [ ] `"kdf": "PBKDF2-HMAC-SHA512"` ✅
- [ ] `"iterations": 100000` ✅
- [ ] `"quantumResistant": true` ✅
- [ ] `"data"` field contains long Base64 string ✅

**For v1.0 Backup (created in v1.4.0 or earlier):**
- [ ] `"version": "1.0"` ✅
- [ ] NO `encryptionType` field ✅
- [ ] NO `kdf` field ✅
- [ ] NO `iterations` field ✅
- [ ] NO `quantumResistant` field ✅
- [ ] `"data"` field contains long Base64 string ✅

### Step 4: Security Verification (Safe)

You can share ONLY this metadata with me (redacted example):
```json
{
  "version": "2.0",              ← SAFE to share
  "timestamp": "REDACTED",       ← Redact timestamp
  "passwordCount": 5,            ← SAFE to share (just a count)
  "encryptionType": "...",       ← SAFE to share
  "kdf": "...",                  ← SAFE to share
  "iterations": 100000,          ← SAFE to share
  "quantumResistant": true,      ← SAFE to share
  "data": "REDACTED"             ← NEVER share this!
}
```

**What to NEVER Share:**
- ❌ The `"data"` field (encrypted passwords)
- ❌ The backup password
- ❌ The full backup file
- ❌ Any actual password data

### Example Safe Output

**This is SAFE to share:**
```bash
$ cat backup.json | jq 'del(.data, .timestamp)'
{
  "version": "2.0",
  "encrypted": true,
  "passwordCount": 5,
  "appName": "SecureVault",
  "platform": "Android",
  "encryptionType": "password-aes256gcm-pqc-ready",
  "kdf": "PBKDF2-HMAC-SHA512",
  "iterations": 100000,
  "quantumResistant": true
}
```

**This is NOT SAFE to share:**
```bash
$ cat backup.json  # ← NEVER run this and share!
# Shows encrypted data that could be attacked
```

---

## If You Want to Verify Format

Just paste the **metadata only** (using the safe method above) and I can confirm if:
- ✅ v2.0 format is correct
- ✅ Quantum encryption metadata is present
- ✅ Encryption parameters are strong

**Remember:** Even encrypted data should be treated as sensitive!
