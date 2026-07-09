# Passkey proof of concept

This branch demonstrates what a passkey implementation would look like in the
wallet, placed where it would sit in a real implementation:

- **Onboarding**: the "confirm PIN" step is replaced by a passkey creation step
  (`OnboardingStep.SETUP_PASSKEY`). The system passkey sheet (Google Password
  Manager + biometrics) is shown when the user taps "Create passkey".
- **Issuance and presentation**: where the app previously asked the user to
  retype their PIN before signing, it now asks for a passkey confirmation
  instead (`PasskeyConfirm`), triggering the one-tap biometric sheet.

## What is real and what is PoC scaffolding

Real:

- Passkey creation and assertion through Android Credential Manager
  (`androidx.credentials`), including biometric user verification and
  Google Password Manager sync.
- Cryptographic verification of each assertion: the app checks the returned
  challenge and verifies the ES256 signature against the public key captured
  at registration (`PasskeyManager.assertPasskey`).

PoC scaffolding (would move server-side in a real implementation):

- Challenges are generated on-device and assertions are verified on-device.
  Production would use a WebAuthn relying-party server (challenge issuance,
  attestation/assertion validation, credential storage).
- The OPAQUE access mechanism still needs the raw PIN, so the PIN captured at
  onboarding is stored locally, encrypted with an Android Keystore AES-GCM key
  (`KeystoreManager.encryptPin`), and released only after a successful passkey
  assertion. A production passkey design would replace or re-key the OPAQUE
  flow instead of bridging it this way.

## No-server fallback (default demo path)

Nothing needs to be deployed to run the demo. When Google Play services rejects
passkey creation because the relying party cannot be validated (error 50152,
"RP ID cannot be validated" — assetlinks.json not deployed), `PasskeyManager`
automatically falls back to `LocalPasskeyAuthenticator`: a device-local ES256
key in the Android Keystore that can only sign after the system biometric
prompt (`android.hardware.biometrics.BiometricPrompt`). The rest of the flow is
identical — same creation step in onboarding, same "Confirm with passkey" in
issuance/presentation, same local signature verification per use.

Differences from a real passkey: it is device-bound (no Google Password Manager
sync, no cross-device sign-in) and shows the plain biometric dialog instead of
Google's passkey sheet. Once assetlinks.json (below) is deployed, newly created
passkeys automatically use the real Credential Manager flow — no code change.

The only device requirement is an enrolled fingerprint/face (on the emulator:
Extended controls → Fingerprint, after adding a screen lock).

## Server prerequisite for real passkeys: assetlinks.json

Android only shows passkey UI for a relying party whose domain vouches for the
app. Deploy this at `https://wallet.sandbox.digg.se/.well-known/assetlinks.json`
(served as `application/json`, no redirect):

```json
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "se.digg.wallet.demo",
      "sha256_cert_fingerprints": [
        "BF:0A:E0:ED:18:94:7D:9A:EB:2E:3F:F2:24:F6:01:E9:CB:FD:84:22:45:B9:F3:77:06:D1:72:91:89:5D:C6:15"
      ]
    }
  },
  {
    "relation": [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "se.digg.wallet",
      "sha256_cert_fingerprints": [
        "BF:0A:E0:ED:18:94:7D:9A:EB:2E:3F:F2:24:F6:01:E9:CB:FD:84:22:45:B9:F3:77:06:D1:72:91:89:5D:C6:15"
      ]
    }
  }
]
```

The fingerprint above is the shared Android **debug** keystore on the machine
that produced this branch (`keytool -list -v -keystore ~/.android/debug.keystore`).
Every developer/CI keystore that should be able to demo needs its fingerprint
added to the array. For a release build, add the release certificate instead.

Verify the deployment with:

```text
https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://wallet.sandbox.digg.se&relation=delegate_permission/common.get_login_creds
```

## Demo prerequisites

- Device or emulator with a screen lock + fingerprint/face enrolled.
- `demoDebug` build variant.
- Without assetlinks.json deployed, the local biometric fallback is used
  (see above). With it deployed — and a Google account signed in on the
  device — the real Google passkey sheet appears instead.

## Demo script

1. Start onboarding, enter a PIN.
2. Step 2 is now "Create a passkey" — tap the button, show the system sheet
   and fingerprint prompt, point out the passkey is synced to the user's
   Google account (visible in Settings → Google → Password Manager).
3. Finish onboarding (wallet + PID setup unchanged).
4. Issue a credential or share attributes: where the app used to ask for the
   PIN again, it now shows "Approve with passkey" — one tap + fingerprint
   instead of typing the PIN.
