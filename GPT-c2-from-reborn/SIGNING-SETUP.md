# Permanent APK signing setup — GPT c2 from reborn

## Why Android rejects some upgrades
The current public GitHub Actions workflow runs Gradle `assembleDebug` on
fresh `ubuntu-latest` machines. A debug signing key is generated in each
new runner, so later versions have the same Android package ID
(`com.moshenuch.c2reborn`) but different signing certificates. Android
correctly refuses to overwrite the installed app.

Changing `versionName` or `versionCode` does not solve this.

## Permanent, safer path
Create **one** app signing keystore, retain a secure backup, and store the
binary + signing credentials as **encrypted GitHub Actions repository secrets**
(not as a file committed to this public repository). The workflow uses this
identity for `assembleRelease` and future builds. Never rotate or delete the
signing key without a migration plan.

### 1. Generate a key on your own trusted PC
Requires Java JDK `keytool`:

```sh
keytool -genkeypair -v -keystore c2-update-signing.jks -storetype PKCS12 -alias c2-update -keyalg RSA -keysize 3072 -validity 10000
```

Record the strong **keystore password** in your password manager.
PKCS12 normally uses that same password for the private key.

**Back up `c2-update-signing.jks` securely offline.** Without it and its
password, future Android updates cannot use the same signing identity.

### 2. Encode the binary as text
Linux:

```sh
base64 -w0 c2-update-signing.jks > c2-signing-base64.txt
```

macOS:

```sh
base64 < c2-update-signing.jks | tr -d '\n' > c2-signing-base64.txt
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\c2-update-signing.jks))) | Set-Content -NoNewline c2-signing-base64.txt
```

Keep the encoded file private as well; it is the full keystore, not a hash.

### 3. Add four GitHub Actions secrets

Go to [repository Actions secrets settings](https://github.com/moshenuch/moshenuch/settings/secrets/actions),
then choose **New repository secret** for each:

| Secret name | Value |
| --- | --- |
| `C2_SIGNING_KEYSTORE_BASE64` | Entire one-line encoded keystore |
| `C2_SIGNING_STORE_PASSWORD` | The keystore password |
| `C2_SIGNING_KEY_ALIAS` | `c2-update` |
| `C2_SIGNING_KEY_PASSWORD` | Private-key password (normally same as store password for PKCS12) |

Do not paste values into issues, PR descriptions, source files, chat transcripts,
or release notes.

### 4. Merge the signing PR and verify
Review and merge [stable signing PR](https://github.com/moshenuch/moshenuch/pulls).
The next release build should say `persistent-release` and run
`assembleRelease`. It also invokes `apksigner verify --print-certs`.
Record the APK signing certificate SHA-256 fingerprint and ensure it stays
**identical on every future release**. A failed or incomplete signing setup
must never silently get a different persistent key.

## One-time migration limitation
Previous C2 APKs were signed with different ephemeral debug certificates.
No newly generated key can update an already installed app signed by one of
those keys. **You must uninstall the old APK once to install the first
persistently signed release.** Uninstalling may remove app-only data such as
notes, group assignments and message read-state overrides. The Android system
SMS and contacts database are separate and must not be deleted by uninstall.

If app-only data matters, arrange a backup **before** uninstalling.

## Current fallback behaviour
If **none** of the signing secrets is configured, the workflow continues to
build the legacy disposable debug APK and issues a GitHub Actions warning
stating it will not be upgradable. That path should be treated as a temporary
development workaround. If **some** but not all secrets are set, the workflow
fails to avoid publishing a misleading half-configured release.

This PR only prepares the pipeline. It does not create or store a new private
key on the owner's behalf and does not claim that signing is configured.
