# SecureVault Migration Guide: v1.2.4 → v1.2.5+

## ⚠️ Important Notice

If you have **SecureVault v1.2.4 or earlier** installed, you **must** perform a one-time migration to upgrade to v1.2.5+.

**Why is this necessary?** Previous versions had signing key inconsistencies that prevent direct in-place updates. This is a **one-time migration** - once you're on v1.2.5, all future updates will work seamlessly.

---

## Migration Overview

**Estimated Time:** 5-10 minutes
**Data Loss Risk:** None (if you follow the backup steps)
**Difficulty:** Easy

---

## Step-by-Step Migration Instructions

### Step 1: Backup Your Data

**CRITICAL: Do this first!**

1. Open SecureVault app (current version)
2. Tap the **Settings** icon (⚙️) in the top-right
3. Scroll to **Backup & Restore** section
4. Tap **Create Backup**
5. Choose a save location (Downloads folder recommended)
6. Remember the backup file name (e.g., `securevault_backup_2025_01_05.json`)

✅ **Verify:** Check that the backup file exists in your chosen location

---

### Step 2: Uninstall Old Version

1. Go to **Android Settings** → **Apps** → **SecureVault**
2. Tap **Uninstall**
3. Confirm uninstallation

**Note:** This removes the app but your backup file remains safe.

---

### Step 3: Download New Version

1. Go to the [SecureVault Releases page](https://github.com/YOUR_USERNAME/SecureVault-Android-v2/releases)
2. Find the **latest release** (v1.2.5 or higher)
3. Download **`app-release.apk`** (NOT `app-debug.apk`)

**Verify your download (optional but recommended):**
```bash
# Download the .sha256 file as well, then:
sha256sum -c app-release.apk.sha256
```

---

### Step 4: Enable Unknown Sources

1. Go to **Android Settings** → **Security**
2. Enable **Install from Unknown Sources** (or allow for your file manager/browser)

**Note:** You can disable this after installation for security.

---

### Step 5: Install New Version

1. Open your file manager or Downloads folder
2. Tap on **`app-release.apk`**
3. Tap **Install**
4. Wait for installation to complete
5. Tap **Open**

---

### Step 6: Restore Your Data

1. The app will ask for biometric setup (fingerprint/face)
2. Complete the biometric setup or set up a PIN
3. Once on the main screen, tap **Settings** (⚙️)
4. Scroll to **Backup & Restore**
5. Tap **Restore Backup**
6. Navigate to your backup file location
7. Select the backup file you created in Step 1
8. Wait for restoration to complete

✅ **Verify:** Check that all your passwords are restored

---

### Step 7: Verify Migration Success

1. **Check version:** Settings → About → Version should show **1.2.5** or higher
2. **Test functionality:**
   - View a password
   - Edit a password
   - Create a new test password
   - Test biometric unlock
3. **Delete test password** if created

---

## Troubleshooting

### Issue: "Backup file not found"

**Solution:**
- Check you're looking in the correct folder (Downloads)
- Use a file manager app to search for `securevault_backup`
- The file should have a `.json` extension

### Issue: "Restore failed" or "Invalid backup file"

**Solution:**
- Ensure the backup file is not corrupted
- Try creating a new backup from the old app (if not yet uninstalled)
- Check the backup file size (should be > 1 KB)

### Issue: "Installation blocked"

**Solution:**
- Ensure "Unknown Sources" is enabled in Settings
- Check you downloaded `app-release.apk`, not `app-debug.apk`
- Clear browser/download cache and re-download

### Issue: "App won't open after install"

**Solution:**
- Restart your device
- Check Android version is 7.0 (API 24) or higher
- Ensure you have enough storage space (~20 MB free)
- Try uninstalling and reinstalling

### Issue: "Biometric setup fails"

**Solution:**
- Ensure your device has biometric hardware (fingerprint/face)
- Check biometric is registered in Android Settings
- Use PIN fallback if biometric unavailable
- Grant all requested permissions

---

## Future Updates (v1.2.6+)

**Good news!** Once you're on v1.2.5, future updates will work normally:

1. Download new APK from releases page
2. Tap to install
3. Android will automatically update in-place
4. **No backup/restore needed**

The signing key consistency issue is permanently fixed in v1.2.5+.

---

## Technical Details

### Why Was Migration Necessary?

- **Root Cause:** v1.1 and earlier versions used different signing keys
- **Android Behavior:** Android treats apps with different signatures as completely different apps
- **Solution:** v1.2.5 establishes consistent signing with V1/V2/V3/V4 signatures

### What Changed in v1.2.5?

1. ✅ Enhanced signing configuration (all signature versions enabled)
2. ✅ Fixed ProGuard rules for better crash reporting
3. ✅ Improved CI/CD pipeline with automated testing
4. ✅ Added comprehensive test suite
5. ✅ Better security and error handling

### Data Safety

- **Encryption:** Your passwords remain encrypted throughout the process
- **Backup format:** Encrypted JSON file
- **No cloud:** All data stays on your device
- **Zero data loss:** Backup/restore preserves everything

---

## FAQ

**Q: Can I keep both old and new versions installed?**
A: No. Android prevents installing apps with different signatures under the same package name.

**Q: Will I lose my data if I don't backup?**
A: YES. Always backup before uninstalling. There's no way to recover data after uninstalling without a backup.

**Q: How large is the backup file?**
A: Typically 10-100 KB depending on how many passwords you have.

**Q: Can I use the debug version instead?**
A: Not recommended. Debug versions have a different package name (`com.securevault.debug`) and are for developers only.

**Q: Is this safe?**
A: Yes. The backup file uses the same encryption as the app itself. Store it securely and delete after migration.

**Q: How do I know migration succeeded?**
A: Check Settings → About → Version shows 1.2.5+, and all your passwords are accessible.

---

## Support

If you encounter issues during migration:

1. **Check troubleshooting section above**
2. **Open an issue:** [GitHub Issues](https://github.com/YOUR_USERNAME/SecureVault-Android-v2/issues)
3. **Include:**
   - Android version
   - Device model
   - Error messages
   - Steps you've completed

---

## Acknowledgments

This migration is necessary due to historical signing configuration issues. We apologize for the inconvenience and have implemented measures to ensure this never happens again.

**Thank you for using SecureVault!** 🔐

---

*Last Updated: January 5, 2026*
*Applies to: v1.2.5 and later*
