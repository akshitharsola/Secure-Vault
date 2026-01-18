# GitHub Repository Settings Guide

This guide provides step-by-step instructions for updating the SecureVault repository settings to ensure proper visibility, discoverability, and contributor recognition on GitHub.

## Repository About Section

### Access Settings
1. Go to: https://github.com/akshitharsola/Secure-Vault
2. Click the ⚙️ (gear icon) next to "About" on the right sidebar

### Description
**Recommended Description** (max 350 characters):
```
A quantum-resistant, hardware-backed password manager for Android with zero internet permissions. Features AES-256-GCM encryption via Android Keystore, ML-KEM-768 quantum-resistant backups, automatic migration, and biometric authentication. 100% Kotlin, Jetpack Compose, Clean Architecture.
```

**Alternative Short Description** (if character limit exceeded):
```
Quantum-resistant Android password manager with hardware-backed encryption (Android Keystore), ML-KEM-768 backups, biometric auth, and zero permissions. 100% Kotlin + Compose.
```

### Website
**Add**: `https://github.com/akshitharsola/Secure-Vault/releases/latest`

This links directly to the latest release for easy APK downloads.

### Topics (Tags)
**Add the following topics** (click in the topics field and type each one):

**Security & Cryptography**:
- `password-manager`
- `android-security`
- `encryption`
- `android-keystore`
- `post-quantum-cryptography`
- `quantum-resistant`
- `ml-kem`
- `kyber`
- `aes-gcm`

**Android Development**:
- `android`
- `kotlin`
- `jetpack-compose`
- `material-design`
- `clean-architecture`
- `room-database`
- `biometric-authentication`

**Features**:
- `offline-first`
- `zero-permissions`
- `backup-restore`
- `open-source`

**Total**: ~20 topics (GitHub allows up to 20)

### Options to Check
- ✅ **Releases**: Already enabled (you have releases)
- ✅ **Packages**: Uncheck (not using GitHub Packages)
- ✅ **Deployments**: Uncheck (not using deployments)
- ❌ **Environments**: Uncheck (not needed)
- ✅ **Discussions**: Optional - enable if you want community discussions
- ✅ **Issues**: Already enabled
- ✅ **Projects**: Optional - for project management
- ✅ **Preserve this repository**: Optional - consider enabling for important projects
- ✅ **Sponsorships**: Optional - if you want to enable GitHub Sponsors

### Save Changes
Click **"Save changes"** at the bottom of the About section dialog.

---

## Social Preview Image

GitHub allows you to set a custom image that appears when sharing the repository link on social media.

### Recommended Dimensions
- **1280 x 640 pixels** (2:1 aspect ratio)
- PNG or JPG format
- Max 1 MB file size

### Design Suggestions
Create an image featuring:
- **SecureVault** logo/name prominently
- Key features: "Quantum-Resistant", "Hardware-Backed Encryption"
- Android robot or security shield icon
- Dark theme (matches app aesthetic)
- Version badge: "v2.0.3"

### Upload Steps
1. Settings → Options → Social preview
2. Click "Edit"
3. Upload your image
4. Preview and save

**Note**: If you don't have a custom image, GitHub will auto-generate one from the README.

---

## Repository Settings (Additional)

### General Settings

**Navigate to**: Settings → General

#### Features
- ✅ **Wikis**: Optional - enable if you want a wiki
- ✅ **Issues**: Already enabled (keep enabled)
- ✅ **Sponsorships**: Optional
- ✅ **Preserve this repository**: Recommended for important projects
- ✅ **Discussions**: Optional - good for community engagement

#### Pull Requests
- ✅ **Allow merge commits**: Enabled
- ✅ **Allow squash merging**: Enabled (recommended for cleaner history)
- ✅ **Allow rebase merging**: Enabled
- ✅ **Always suggest updating pull request branches**: Enabled
- ✅ **Automatically delete head branches**: Enabled (cleanup after merge)

#### Archives
- ❌ **Include Git LFS objects in archives**: Not needed (not using LFS)

---

## Contributor Visibility

### Ensuring Contributors Show in Sidebar

For contributors to appear in the right sidebar (like in the Samay repo example), GitHub automatically displays:
1. **Repository owner** (you - @akshitharsola)
2. **Top contributors** based on commit count

**Why Claude Sonnet 4.5 shows as contributor**:
- All commits since v2.0.0 include: `Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>`
- GitHub recognizes co-authored commits and displays them in the contributors graph

**Verify Contributors Are Visible**:
1. Go to: https://github.com/akshitharsola/Secure-Vault/graphs/contributors
2. You should see:
   - **Akshit Harsola** (@akshitharsola) - Most commits
   - **Claude Sonnet 4.5** - Co-authored commits since v2.0.0

**If Claude doesn't appear**:
- Wait 24-48 hours (GitHub caches contributor data)
- Ensure commits have proper co-author format:
  ```
  Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
  ```
- GitHub needs at least 1 co-authored commit to recognize the contributor

### Contributors Section in README

Already added in README.md:
```markdown
### Core Team

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/akshitharsola">
        <img src="https://github.com/akshitharsola.png" width="100px;" alt="Akshit Harsola"/>
        <br />
        <sub><b>Akshit Harsola</b></sub>
      </a>
      <br />
      <sub>Original Author & Maintainer</sub>
    </td>
    <td align="center">
      <a href="https://www.anthropic.com">
        <img src="https://www.anthropic.com/images/icons/apple-touch-icon.png" width="100px;" alt="Claude"/>
        <br />
        <sub><b>Claude Sonnet 4.5</b></sub>
      </a>
      <br />
      <sub>AI Pair Programming Assistant</sub>
    </td>
  </tr>
</table>
```

---

## Security Policy

### Create SECURITY.md

Add a security policy to help researchers report vulnerabilities responsibly:

**Navigate to**: Settings → Security → Policy

**Or create manually**: Create file `SECURITY.md` in repository root:

```markdown
# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | :white_check_mark: |
| 1.5.x   | :white_check_mark: |
| < 1.5   | :x: (VULNERABLE)   |

## Reporting a Vulnerability

**DO NOT** open public issues for security vulnerabilities.

**Instead:**

1. **GitHub Security Advisories** (Recommended):
   - Go to: https://github.com/akshitharsola/Secure-Vault/security/advisories
   - Click "Report a vulnerability"
   - Provide detailed information

2. **Email** (Alternative):
   - Contact: [your email] (check LICENSE for contact info)
   - Include: vulnerability description, reproduction steps, impact assessment

3. **Response Time**:
   - Initial response: Within 48 hours
   - Fix timeline: Depends on severity (critical: 7 days, high: 14 days, medium: 30 days)

4. **Disclosure Policy**:
   - Allow reasonable time for fixes before public disclosure
   - We appreciate responsible disclosure
   - Security researchers will be credited (with permission)

## Security Features (v2.0+)

- ✅ Hardware-backed AES-256-GCM encryption (Android Keystore)
- ✅ Quantum-resistant backups (ML-KEM-768 + X25519)
- ✅ Keys stored in TEE/Secure Element (non-extractable)
- ✅ Automatic migration from v1.x plain text storage
- ✅ Zero internet permissions (completely offline)
- ✅ Biometric authentication with PIN fallback

## Known Vulnerabilities

### v1.0 - v1.5.0 (CRITICAL - Fixed in v2.0.0)
- **Issue**: Passwords stored in PLAIN TEXT in Room database
- **Impact**: Anyone with device access could read passwords
- **Fix**: Upgrade to v2.0.0+ immediately
- **CVE**: N/A (internal discovery)

### v2.0.0 - v2.0.1 (HIGH - Fixed in v2.0.2)
- **Issue**: Backup restore failure due to Keystore IV generation
- **Impact**: Users cannot restore backups
- **Fix**: v2.0.2+ resolves Keystore IV handling
- **CVE**: N/A (internal discovery)

## Security Audit

Last security review: January 2026 (v2.0.0 release)

Areas audited:
- Encryption implementation (Android Keystore)
- Key generation and storage
- Backup encryption (quantum-resistant)
- Database security
- Biometric authentication flow

**Want to help?** We welcome security audits and penetration testing from the community.
```

**Save and commit**: Create this file and commit it to the repository.

---

## Repository Insights

### Enable Insights Features

**Navigate to**: Insights tab (automatically available)

**Available Insights**:
- **Contributors**: Shows commit activity (displays both contributors)
- **Community Standards**: Checklist for open source best practices
  - ✅ Description: Updated
  - ✅ README: Updated (v2.0.3)
  - ✅ License: MIT (updated with contributors)
  - ✅ Contributing guide: CONTRIBUTORS.md created
  - ⏳ Code of conduct: Included in CONTRIBUTORS.md
  - ⏳ Security policy: Create SECURITY.md (see above)
  - ⏳ Issue templates: Optional (can create later)
  - ⏳ Pull request template: Optional (can create later)

**Check Progress**:
1. Go to: https://github.com/akshitharsola/Secure-Vault/community
2. Review which standards are met
3. Add missing files if desired

---

## Badges for README

Already added in README.md, but here's the reference:

```markdown
[![Latest Release](https://img.shields.io/github/v/release/akshitharsola/Secure-Vault)](https://github.com/akshitharsola/Secure-Vault/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org)
```

**Additional Badge Options**:
```markdown
[![GitHub stars](https://img.shields.io/github/stars/akshitharsola/Secure-Vault?style=social)](https://github.com/akshitharsola/Secure-Vault/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/akshitharsola/Secure-Vault?style=social)](https://github.com/akshitharsola/Secure-Vault/network/members)
[![GitHub watchers](https://img.shields.io/github/watchers/akshitharsola/Secure-Vault?style=social)](https://github.com/akshitharsola/Secure-Vault/watchers)
[![GitHub contributors](https://img.shields.io/github/contributors/akshitharsola/Secure-Vault)](https://github.com/akshitharsola/Secure-Vault/graphs/contributors)
[![Downloads](https://img.shields.io/github/downloads/akshitharsola/Secure-Vault/total)](https://github.com/akshitharsola/Secure-Vault/releases)
```

---

## GitHub Actions Badge

Add build status badge to README:

```markdown
[![Build Status](https://github.com/akshitharsola/Secure-Vault/actions/workflows/release.yml/badge.svg)](https://github.com/akshitharsola/Secure-Vault/actions/workflows/release.yml)
```

This shows whether the latest release build succeeded or failed.

---

## Checklist: Repository Settings Complete

### About Section
- [ ] Description updated with quantum-resistant features
- [ ] Website set to latest release URL
- [ ] 15-20 topics added (password-manager, android-security, encryption, etc.)
- [ ] Features configured (Issues, Releases enabled)

### Contributor Visibility
- [ ] Co-authored commits pushed (already done)
- [ ] Check contributors graph in 24-48 hours
- [ ] Verify both contributors appear in sidebar
- [ ] CONTRIBUTORS.md created (already done)

### Security & Community Standards
- [ ] SECURITY.md created and committed
- [ ] LICENSE updated with contributors (already done)
- [ ] README.md comprehensive (already done)
- [ ] CONTRIBUTORS.md guidelines (already done)

### Optional Enhancements
- [ ] Social preview image designed and uploaded
- [ ] Issue templates created
- [ ] Pull request template created
- [ ] Wiki pages created
- [ ] GitHub Discussions enabled
- [ ] Sponsorship enabled (if desired)

### Verification
- [ ] Visit repository page and verify About section looks good
- [ ] Check contributors graph: https://github.com/akshitharsola/Secure-Vault/graphs/contributors
- [ ] Verify topics appear when searching GitHub
- [ ] Test sharing link on social media (social preview)

---

## Quick Actions

### 1. Update About Section Right Now

Visit: https://github.com/akshitharsola/Secure-Vault

1. Click ⚙️ gear icon next to "About"
2. Copy-paste description (see above)
3. Add website: `https://github.com/akshitharsola/Secure-Vault/releases/latest`
4. Add topics one by one (see list above)
5. Click "Save changes"

**Time required**: 2-3 minutes

### 2. Create SECURITY.md

```bash
# Copy the SECURITY.md template from this guide
# Save as SECURITY.md in repository root
# Commit and push

git add SECURITY.md
git commit -m "docs: add security policy and vulnerability reporting

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
git push origin main
```

**Time required**: 1 minute

### 3. Verify Contributors (Wait Period)

- Wait 24-48 hours after pushing co-authored commits
- Check: https://github.com/akshitharsola/Secure-Vault/graphs/contributors
- Both contributors should appear automatically

**Time required**: 0 minutes (automatic)

---

## Need Help?

If any settings are unclear or you encounter issues:
- Check GitHub's documentation: https://docs.github.com
- Repository settings guide: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features
- Community standards: https://docs.github.com/en/communities

---

**Last Updated**: January 2026 (v2.0.3)
**Contributors**: Akshit Harsola, Claude Sonnet 4.5
