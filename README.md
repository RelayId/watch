# RelayId Wear OS

RelayId Wear OS is the Wear OS companion for the RelayId mobile application. It lets a user select an enrolled Relay account and display its short-lived browser sign-in code on a watch.

The watch is not an authentication client. The phone remains responsible for device keys, Relay credentials, request signing, code generation, and communication with the Relay service.

## Functionality

The watch application provides:

- selection of an enrolled Relay account;
- display of the account email and Relay host;
- display of the current short-lived sign-in code;
- code grouping with hyphens;
- display of code expiry and automatic refresh requests;
- light and dark appearance following the system setting;
- a circular Wear OS layout;
- no watch authentication prompt.

The phone publishes enrolled account metadata to the watch. When the user selects an account or requests a new code, the watch sends a request to the phone. The phone's native Wear listener uses a phone-side scoped capability to call Relay's code-only watch endpoint and sends the resulting code, group count, and expiry only to the requesting watch.

## Security boundary

The watch does not store or receive:

- P-256 private keys;
- ML-DSA seeds;
- opaque Relay credentials;
- OAuth tokens;
- provider client secrets.

The watch receives account display metadata and short-lived code results only. The capability cannot approve logins, enroll a provider, or call normal device endpoints, and it never leaves the phone. Capability registration requires the phone to be unlocked once. After that, the phone UI can be locked while its native Wear listener handles code requests; the phone must remain powered, paired, and able to reach Relay.

## Project layout

```text
wear/
  src/main/java/com/relayid/watch/
    MainActivity.kt
    RelayIdWearListenerService.kt
    WatchState.kt
    WearTransport.kt
```

`MainActivity` renders the Compose UI. `WearTransport` handles Data Layer messages and the local state cache. `RelayIdWearListenerService` receives state updates while the application is not in the foreground. The watch application never calls the Relay service directly.

## Requirements

- Android SDK with API level 36;
- Java 17;
- Gradle 8.13;
- a Wear OS device or emulator for runtime testing.

## Local commands

Run these commands from this directory:

```bash
gradle :wear:assembleDebug --no-daemon
gradle :wear:assembleRelease --no-daemon
gradle :wear:lint --no-daemon
```

The application ID is `com.relayid.app`. The release output is written to `wear/build/outputs/apk/release/`.

The generated Gradle directories and APKs are local build output and are excluded from version control. No signing key is included in this repository; production signing must be configured by the distribution environment.

## Installation and pairing

Install RelayId Watch from the Google Play listing after pairing the watch with the phone application. Open RelayId on the phone, unlock it once, and wait for the enrolled accounts to appear in the watch selector. The phone application must remain available when a code is requested because it performs the protected Relay request.

The watch application is not a replacement for the phone application. Enrollment, Relay setup, provider login, approval, credential storage, and account management remain phone functions.

## Continuous integration

`.github/workflows/ci.yml` verifies the source tree and compiles the debug APK. Its release job builds the release APK, uploads `relayid-wear.apk` as an artifact, and updates the rolling `wear-latest` prerelease for non-pull-request runs. The workflow grants repository write access only to the release job because publishing the prerelease requires it.

## License

RelayId Wear OS is distributed under the GNU General Public License v3. The complete license text is in `LICENSE`.
