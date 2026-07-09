# Passkey PoC — plain overview

This is a proof of concept. Nothing here is done, decided, or ready for
production. The point of this branch is to show what passkeys *could* look
like in the wallet, so we can discuss and decide — not to ship it.

## What we are doing

We replaced two PIN moments with passkeys, in the places where a real
implementation would sit:

- **Onboarding**: the "confirm your PIN" step is now "create a passkey"
  (fingerprint/face instead of retyping the PIN).
- **Issuance and presentation**: where the app asked you to retype your PIN
  before signing, it now asks for one passkey tap + biometric.

That's it. Everything else in the app is unchanged.

## What actually works

- Creating a passkey through Android Credential Manager and using it with
  biometrics.
- Checking the passkey signature locally each time it is used.
- A fallback so the demo runs without any server: if Google can't validate
  our domain (because `assetlinks.json` isn't deployed), we use a
  device-local biometric key instead. Same flow, just not a "real" synced
  passkey.

## What is NOT done (and why this is only a PoC)

- **No server.** Real passkeys need a WebAuthn relying-party server that
  issues challenges and verifies responses. We fake all of that on the
  device. This is not acceptable for production, it's just enough for a demo.
- **The PIN is still there.** The backend login (OPAQUE) still needs the raw
  PIN, so we store the PIN encrypted on the device and unlock it after a
  passkey check. This is a workaround, not a design. A real implementation
  would change the backend flow instead.
- **`assetlinks.json` is not deployed.** Until it is served on
  `wallet.sandbox.digg.se`, everyone gets the local fallback, not real
  Google-synced passkeys.
- **No recovery, no multi-device story, no settings UI, no migration for
  existing users.** None of that is designed or built.
- **No security review.** Nothing here has been audited.

## What a real implementation would need

1. A decision that we actually want passkeys.
2. A WebAuthn server (challenges, registration, assertion verification,
   credential storage).
3. A backend auth flow that doesn't need the raw PIN (replace or re-key
   OPAQUE).
4. `assetlinks.json` deployed for the app's signing certificates.
5. Recovery/device-loss handling, and a plan for existing users.

Details, demo script, and setup instructions: see [passkey-poc.md](passkey-poc.md).
