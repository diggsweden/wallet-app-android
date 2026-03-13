# Wallet Android

---

[![REUSE](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fapi.reuse.software%2Fstatus%2Fgithub.com%2Fdiggsweden%2Fwallet-app-android&query=status&style=for-the-badge&label=REUSE)](https://api.reuse.software/info/github.com/diggsweden/wallet-app-android)

[![Tag](https://img.shields.io/github/v/tag/diggsweden/wallet-app-android?style=for-the-badge&color=green)](https://github.com/diggsweden/wallet-app-android/tags)

[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/diggsweden/wallet-app-android/badge?style=for-the-badge)](https://scorecard.dev/viewer/?uri=github.com/diggsweden/wallet-app-android)

## Getting started

---

You must add the API keys by copying the secrets.properties.example file and renaming it to secrets.properties in the root folder of the project.

```bash
cp secrets.properties.example secrets.properties
```

Or using just:

```bash
just setup-secrets
```

Open secrets.properties and fill in the missing API keys.

---

## Build Flavours

The project defines these two product flavours:

- **local** – Local development/testing environment
- **demo** – Demo environment

### Build a Specific Flavour

In Android Studio:

Open Build

Select Build Variant

Choose the desired variant from the dropdown menu

- **localDebug**
- **demoDebug**

---

## Running the Android app against a local setup

To run the app against a local setup of wallet-ecosystem you have to fetch and follow the steps described [in the wallet-ecosystem repository](https://github.com/diggsweden/wallet-ecosystem). After you have successfully got the wallet-ecosystem running on your machine build the Android app with the localDebug build variant on your emulator.

### Installing the CA certificate on the emulator

The local setup uses TLS certificates issued by a local CA (via [mkcert](https://github.com/FiloSottile/mkcert)). To make the emulator trust these certificates, run:

```bash
just emulator-install-cert
```

Or directly:

```bash
./scripts/emulator-install-cert.sh
```

This pushes the mkcert root CA certificate to the emulator and opens the certificate settings. Follow the on-screen instructions to install it as a trusted CA certificate.

You can also pass a custom certificate path:

```bash
just emulator-install-cert /path/to/cert.pem
```

## Licenses

---

Source code is EUPL-1.2. Most other assets are CC0-1.0

Copies from Google Play Store are licensed under their store EULA plus our Terms of Use; that doesn’t change the EUPL license for the source.
