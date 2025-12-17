# Development Guide

## Prerequisites - Linux

1. Install [mise](https://mise.jdx.dev/) (manages linting tools):

   ```bash
   curl https://mise.run | sh
   ```

2. Activate mise in your shell:

   ```bash
   # For bash - add to ~/.bashrc
   eval "$(mise activate bash)"

   # For zsh - add to ~/.zshrc
   eval "$(mise activate zsh)"

   # For fish - add to ~/.config/fish/config.fish
   mise activate fish | source
   ```

   Then restart your terminal.

3. Install pipx (needed for reuse license linting):

   ```bash
   # Debian/Ubuntu
   sudo apt install pipx
   ```

4. Install Android Studio and Java 21

5. Install project tools:

   ```bash
   mise install
   ```

## Prerequisites - macOS

1. Install [mise](https://mise.jdx.dev/) (manages linting tools):

   ```bash
   brew install mise
   ```

2. Activate mise in your shell:

   ```bash
   # For zsh - add to ~/.zshrc
   eval "$(mise activate zsh)"

   # For bash - add to ~/.bashrc
   eval "$(mise activate bash)"

   # For fish - add to ~/.config/fish/config.fish
   mise activate fish | source
   ```

   Then restart your terminal.

3. Install newer bash than macOS default:

   ```bash
   brew install bash
   ```

4. Install pipx (needed for reuse license linting):

   ```bash
   brew install pipx
   ```

5. Install Android Studio and Java 21

6. Install project tools:

   ```bash
   mise install
   ```

## Quality Checks

### Setup

```shell
# Install all development tools
mise install

# Show all just jovs
just

# Setup shared linting tools
just setup-devtools

# Run all quality checks
just verify
```

### Available Commands

Run `just` to see all available commands. Key commands:

| Command | Description |
|---------|-------------|
| `just verify` | Run all checks (lint + test) |
| `just lint-all` | Run all linters |
| `just lint-fix` | Auto-fix linting issues |
| `just test` | Run tests |
| `just build` | Build project |
| `just clean` | Clean build artifacts |

### Linting Commands

| Command | Tool | Description |
|---------|------|-------------|
| `just lint-commits` | conform | Validate commit messages |
| `just lint-secrets` | gitleaks | Scan for secrets |
| `just lint-yaml` | yamlfmt | Lint YAML files |
| `just lint-markdown` | rumdl | Lint markdown files |
| `just lint-shell` | shellcheck | Lint shell scripts |
| `just lint-shell-fmt` | shfmt | Check shell formatting |
| `just lint-actions` | actionlint | Lint GitHub Actions |
| `just lint-license` | reuse | Check license compliance |

### Fix Commands

| Command | Description |
|---------|-------------|
| `just lint-yaml-fix` | Fix YAML formatting |
| `just lint-markdown-fix` | Fix markdown formatting |
| `just lint-shell-fmt-fix` | Fix shell formatting |

---

## Workflows Overview

This project uses two main workflows for building and releasing Android applications.

## Workflow Comparison Matrix

```text
┌─────────────────────────────┬──────────────┬──────────────┐
│ Feature                     │ release-dev  │ release      │
├─────────────────────────────┼──────────────┼──────────────┤
│ TRIGGER                     │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ When runs                   │ Manual only  │ Version tag  │
│                             │ (dispatch)   │ push         │
│ Auto trigger                │ ✗            │ ✓            │
├─────────────────────────────┼──────────────┼──────────────┤
│ WORKFLOW CALLED             │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Reusable workflow           │ android-     │ release-     │
│                             │ variants@v2  │ orchestr@v2  │
├─────────────────────────────┼──────────────┼──────────────┤
│ CONFIGURATION               │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Config source               │ Inline       │ artifacts.yml│
│ Java version                │ 21 (zulu)    │ 21 (file)    │
│ Product flavor              │ demo         │ demo (file)  │
├─────────────────────────────┼──────────────┼──────────────┤
│ BUILDS GENERATED            │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Debug APK                   │ ✓            │ ✗            │
│ Release APK                 │ ✓            │ ✓            │
│ AAB (App Bundle)            │ ✓            │ ✓            │
│ Total artifacts             │ 3            │ 2            │
├─────────────────────────────┼──────────────┼──────────────┤
│ ARTIFACT FEATURES           │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Date stamping               │ ✓            │ ✗            │
│ Custom prefix               │ ✓            │ ✗            │
│ Flavor in name              │ ✓            │ ✗            │
│ Example name                │ 2025-01-15-  │ Release      │
│                             │ testing_     │ asset        │
│                             │ store-app-   │              │
│                             │ demo-APK     │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ SECURITY                    │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Android signing             │ ✗            │ ✓            │
│ GPG signing                 │ ✗            │ ✓            │
│ SBOM                        │ ✗            │ ✓            │
│ SLSA attestation            │ ✗            │ ✓            │
│ Checksums                   │ ✗            │ ✓            │
├─────────────────────────────┼──────────────┼──────────────┤
│ RELEASE FEATURES            │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ GitHub Release              │ ✗            │ ✓            │
│ CHANGELOG update            │ ✗            │ ✓            │
│ Version bump                │ ✗            │ ✓            │
│ Release notes               │ ✗            │ ✓            │
├─────────────────────────────┼──────────────┼──────────────┤
│ STORAGE                     │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Location                    │ Actions      │ Releases     │
│ Retention                   │ 7 days       │ Permanent    │
│ Public access               │ ✗            │ ✓            │
├─────────────────────────────┼──────────────┼──────────────┤
│ PERMISSIONS                 │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ contents: write             │ ✗            │ ✓            │
│ packages: write             │ ✓            │ ✓            │
│ id-token: write             │ ✗            │ ✓            │
│ attestations: write         │ ✗            │ ✓            │
│ security-events: write      │ ✗            │ ✓            │
│ actions: read               │ ✗            │ ✓            │
├─────────────────────────────┼──────────────┼──────────────┤
│ USE CASE                    │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Purpose                     │ Manual test  │ Production   │
│                             │ builds       │ release      │
│ Frequency                   │ On-demand    │ 1-5x/month   │
│ Audience                    │ Internal QA  │ Public users │
├─────────────────────────────┼──────────────┼──────────────┤
│ WORKFLOW                    │              │              │
├─────────────────────────────┼──────────────┼──────────────┤
│ Duration                    │ 5-10 min     │ 15-25 min    │
│ Complexity                  │ Simple       │ Complex      │
│ Steps                       │ ~10          │ ~30+         │
└─────────────────────────────┴──────────────┴──────────────┘
```

## Workflows in Detail

### 1. release-dev-workflow.yml

**Purpose:** Create test builds for internal QA and development testing.

**When it runs:**

- Manual trigger only (workflow_dispatch)
- From GitHub Actions → Run workflow button

**What it builds:**

- Debug APK (unsigned, debuggable)
- Release APK (unsigned)
- AAB (Android App Bundle, unsigned)

**Where artifacts go:**

- GitHub Actions → Artifacts tab
- Retention: 7 days
- Access: Team members only

**Artifact naming:**

```text
2025-01-15 - testing_store - wallet-app - demo - APK debug
2025-01-15 - testing_store - wallet-app - demo - APK release
2025-01-15 - testing_store - wallet-app - demo - AAB release
```

**How to use:**

1. Go to GitHub Actions tab
2. Select "Release Dev Workflow"
3. Click "Run workflow" button
4. Wait 5-10 minutes
5. Download artifacts from the workflow run

**Use cases:**

- QA testing before release
- Internal demos
- Testing new features
- Development builds

---

### 2. release-workflow.yml

**Purpose:** Create official production releases for public distribution.

**When it runs:**

- Automatically when you push a version tag
- Examples:
  - `v1.0.0` (stable release)
  - `v1.0.0-alpha.1` (alpha release)
  - `v1.0.0-beta.1` (beta release)
  - `v1.0.0-rc.1` (release candidate)

**What it builds:**

- Release APK (signed)
- AAB (Android App Bundle, signed)

**Where artifacts go:**

- GitHub Releases page (permanent)
- Public download links
- Access: Everyone

**What else it does:**

1. Bumps version in `gradle.properties`
2. Updates `CHANGELOG.md` with release notes
3. Generates SBOM (Software Bill of Materials)
4. Creates SLSA attestation
5. Generates checksums (SHA256)
6. Creates GitHub Release with all artifacts

**How to use:**

1. Ensure code is ready for release
2. Create and push version tag:

   ```bash
   git tag -s v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

3. Wait 15-25 minutes
4. GitHub Release is created automatically
5. Download APK/AAB from Releases page

**Use cases:**

- Production releases
- Public distribution
- Play Store publishing
- Official versioned releases

---

## Artifact Distribution

### release-dev-workflow Artifacts

**Storage:** GitHub Actions Artifacts tab

**Access:**

1. Go to repository → Actions tab
2. Click on the workflow run
3. Scroll down to "Artifacts" section
4. Download ZIP files

**Retention:** 7 days (automatically deleted)

**Visibility:** Repository members only

---

### release-workflow Artifacts

**Storage:** GitHub Releases page

**Access:**

1. Go to repository → Releases
2. Find the version (e.g., v1.0.0)
3. Download files from "Assets" section

**Retention:** Permanent

**Visibility:** Public (everyone can download)

---

## Play Store Publishing

**Important:** Neither workflow automatically publishes to Google Play Store.

### Manual Play Store Publishing Process

1. **Create production release:**

   ```bash
   git tag -s v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

2. **Wait for release-workflow to complete** (~15-25 minutes)

3. **Download AAB file:**
   - Go to GitHub Releases
   - Find your version (v1.0.0)
   - Download the `.aab` file

4. **Upload to Play Console:**
   - Go to Google Play Console
   - Select your app
   - Navigate to: Production → Create new release
   - Upload the AAB file
   - Fill in release notes
   - Submit for review

5. **Wait for Google review** (typically 1-3 days)

---

## Configuration Files

### `.github/workflows/release-dev-workflow.yml`

Configures development builds:

```yaml
with:
  java-version: "21"
  jdk-distribution: "zulu"
  build-module: "app"
  product-flavor: "demo"
  build-types: "debug,release"
  include-aab: true
  artifact-name-prefix: "testing_store"
  include-date-stamp: true
  enable-signing: false  # No signing for dev builds
```

### `.github/workflows/release-workflow.yml`

Configures production releases:

```yaml
with:
  artifacts-config: .github/artifacts.yml
  release.attachartifacts: |
    app/build/outputs/apk/demo/release/*.apk
    app/build/outputs/bundle/demoRelease/*.aab
```

### `.github/artifacts.yml`

Defines build configuration for releases:

```yaml
artifacts:
  - name: wallet-android
    project-type: gradle
    working-directory: .
    build-type: application
    config:
      java-version: 21
      gradle-tasks: build assembleDemoRelease bundleDemoRelease
      build-module: app
      gradle-version-file: app/build.gradle.kts
```

---

## Security Features

### Development Builds (release-dev-workflow)

- ✗ No Android app signing
- ✗ No GPG signing
- ✗ No SBOM
- ✗ No SLSA attestation
- ✗ No checksums

**Why:** Speed and simplicity for testing

### Production Releases (release-workflow)

- ✓ Android app signing (keystore)
- ✓ GPG signing (optional)
- ✓ SBOM generation
- ✓ SLSA attestation
- ✓ SHA256 checksums

**Why:** Security and trust for public distribution

---

## Required Secrets

### For release-workflow (production signing)

Configure these in GitHub Settings → Secrets:

- `ANDROID_KEYSTORE` - Base64-encoded keystore file
- `ANDROID_KEYSTORE_PASSWORD` - Keystore password
- `ANDROID_KEY_ALIAS` - Key alias
- `ANDROID_KEY_PASSWORD` - Key password

### Optional secrets

- `GPG_SECRET_KEY` - For GPG artifact signing
- `GPG_PASSPHRASE` - GPG passphrase

---

## Best Practices

### Development workflow

- Use release-dev-workflow frequently for testing
- Always test debug builds before creating release
- Keep artifact retention at 7 days to save storage

### Release workflow

- Always create signed tags for releases
- Follow semantic versioning (v1.0.0, v1.1.0, v2.0.0)
- Update CHANGELOG before tagging
- Test thoroughly before creating release tag
- Use pre-release tags for testing (v1.0.0-rc.1)

### Versioning

- Stable releases: `v1.0.0`, `v1.1.0`, `v2.0.0`
- Alpha releases: `v1.0.0-alpha.1`
- Beta releases: `v1.0.0-beta.1`
- Release candidates: `v1.0.0-rc.1`

---

## Quick Reference

### Create dev build

```bash
# Manual trigger via GitHub UI
GitHub Actions → Release Dev Workflow → Run workflow
```

### Create production release

```bash
git tag -s v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

### Download dev builds

```text
Repository → Actions → Workflow run → Artifacts
```

### Download production release

```text
Repository → Releases → Version → Assets
```

### Publish to Play Store

```text
Releases → Download AAB → Play Console → Upload
```

---

## Additional Resources

- [Android Gradle Plugin Documentation](https://developer.android.com/build)
- [Google Play Console](https://play.google.com/console)
- [Reusable CI Documentation](https://github.com/diggsweden/reusable-ci)
- [Semantic Versioning](https://semver.org/)
