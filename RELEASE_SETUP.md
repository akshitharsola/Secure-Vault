# GitHub Actions Release Setup

This guide explains how to set up automated APK releases for SecureVault Android.

## Quick Setup

1. **Push to GitHub**: Push your project to GitHub as `SecureVault-Android`
2. **Generate Keystore**: Create a signing keystore for release APKs
3. **Add Secrets**: Configure GitHub repository secrets
4. **Create Release**: Push a git tag to trigger the release workflow

## Keystore Generation

Generate a keystore for signing release APKs:

```bash
keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias securevault
```

**Keep these values secure:**
- Keystore password
- Key alias: `securevault` (or your chosen alias)
- Key password

## GitHub Secrets Configuration

Add these secrets in your GitHub repository settings (Settings → Secrets and variables → Actions):

### Required Secrets:
- `KEYSTORE_BASE64`: Base64 encoded keystore file
- `SIGNING_KEY_ALIAS`: Key alias (e.g., `securevault`)
- `SIGNING_KEY_PASSWORD`: Key password  
- `SIGNING_STORE_PASSWORD`: Keystore password

### Generate KEYSTORE_BASE64:
```bash
base64 -i keystore.jks | pbcopy  # macOS
base64 -w 0 keystore.jks         # Linux
```

## Release Process

### Automatic Releases (Recommended):
1. Create and push a git tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```

2. GitHub Actions will automatically:
   - Build debug and release APKs
   - Create a GitHub release
   - Upload APKs to the release

### Manual Releases:
1. Go to Actions tab in your GitHub repository
2. Select "Build and Release APK" workflow
3. Click "Run workflow"
4. The workflow will build and upload APKs as artifacts

## Workflow Features

- **Automatic signing**: Release APKs are signed with your keystore
- **Dual APKs**: Builds both debug and release versions
- **Caching**: Gradle dependencies are cached for faster builds
- **Rich releases**: Formatted release notes with installation instructions
- **Artifact storage**: APKs are stored as GitHub artifacts for each build

## Release APK Benefits

- **Optimized size**: Code shrinking and resource optimization enabled
- **Security**: Signed APKs verified by Android
- **Performance**: ProGuard optimizations applied
- **Production ready**: Suitable for distribution

## Troubleshooting

### Build Failures:
- Check that all secrets are properly configured
- Verify keystore file is valid
- Ensure Java 11 compatibility

### Signing Issues:
- Confirm keystore path and passwords are correct
- Check that KEYSTORE_BASE64 is properly encoded
- Verify signing config environment variables

## File Structure

```
.github/
  workflows/
    release.yml          # Main release workflow
app/
  build.gradle.kts       # Contains signing configuration
  keystore.jks          # Generated during CI (from secret)
```

The workflow automatically handles keystore placement and signing during the build process.