# NetGuardian production-readiness audit

Audit date: 2026-10-03. Baseline commit: `5a6c36e`. Package: `com.aistudio.netguardian.wzrtqk`. This is a source audit with executed checks listed below, not certification of every runtime path. No previous production APK, version-1 Room schema, Play Console, published privacy policy, or upload signing credentials were supplied. Existing user files were clean before changes. The connected device is a Samsung SM-A525F, Android API 34.

### 1. Production Readiness Status

**NOT READY FOR PRODUCTION.** Several bounded defects have been corrected, but advertised background/new-app/DNS protection is not fully implemented, historical upgrade safety is unverified, and legal/Play declarations and production signing remain outstanding. Do not upload the unsigned review bundle.

### 2. Critical Issues

**C1 — Incomplete protection contract (OPEN)**
- File: `app/src/main/java/com/example/firewall/FirewallVpnService.kt`, `data/AppRepository.kt`, `ui/onboarding/OnboardingWizardScreen.kt`.
- Problem: Only packages already in the rules database and evaluated as blocked enter the sinkhole. Allowed apps bypass it. `isBackground` is always false. Installed packages are loaded through the UI repository; there is no package-install receiver or live refresh, and new rules default to allowed regardless of the block-new preference. `ASK_PER_APP` has no enforcement workflow. Upstream/domain filter preferences cannot deliver device-wide filtering with this architecture.
- Impact: Users can believe background apps, newly installed apps, ads, trackers, or threats are blocked when they are not. A cold boot before the app inventory has loaded is especially important to test.
- Fix: Global DNS effectiveness now stays false and the privacy screen reports the limitation. Blocked applications' DNS is answered locally, never forwarded upstream. Complete and verify the missing enforcement paths before publishing those features; a forwarding engine is a substantive project beyond a safe audit patch. Controls have not been silently removed.

**C2 — Pause, packet processing and tunnel replacement (FIXED IN SOURCE; DEVICE FLOW VALIDATION REQUIRED)**
- File: `firewall/FirewallVpnService.kt`.
- Problem: Pause only changed a flag, leaving the sinkhole active. Concurrent rebuilds could race; an empty allow-list after uninstall races could capture every app. Nonblocking reads could spin/exit. DNS coroutines captured a reusable packet buffer and could send blocked-app queries externally. Errors were overwritten by stopped state and included raw exception text.
- Impact: Connectivity loss, incorrect responses, privacy leakage, misleading state, battery drain.
- Fix: Pause reconfigures the tunnel and a start request while paused resumes it; rebuilds serialize and check stopped/paused state; empty allow-lists do not establish a tunnel; blocking reads are used; blocked DNS is handled synchronously and locally. Teardown preserves errors, handles revocation, closes the logger, and persists disabled state before normal self-stop. This does not establish seamless fail-closed behavior across every Android lifecycle transition.

**C3 — Upgrade data loss (PARTIALLY FIXED; MIGRATION BLOCKER OPEN)**
- File: `database/AppDatabase.kt`, `app/schemas/com.example.database.AppDatabase/2.json`.
- Problem: Destructive fallback erased rules, logs, and custom domains when a migration was missing. Available Git history begins with database version 2; no genuine version-1 schema can be recovered here.
- Impact: Silent loss of user security policy on upgrade.
- Fix: Removed destructive fallback and exported the current schema. Version-2 installs remain supported. Obtain historical schemas/databases and implement tested migrations for any released older version. Unsupported databases are now preserved rather than deleted; they still cannot be used until a migration exists.

**C4 — Corrupt legacy launcher icons (FIXED IN SOURCE)**
- File: all ten `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher*.webp` resources.
- Problem: Every bitmap failed independent decoding; some headers declared absurd dimensions. Lint identified density-size inconsistencies.
- Impact: Missing/failed launcher icon decoding on older devices and launchers that use fallback resources.
- Fix: Replaced corrupt files with `mipmap-anydpi` layer-list fallbacks using the existing shield foreground/background vectors. API-26+ adaptive icons remain unchanged. Added icon-load tests for API 24 and 34.

**C5 — Release publication prerequisites (OPEN)**
- File: `app/build.gradle.kts`, store/legal deliverables absent from the repository.
- Problem: The configured upload keystore is missing. No verifiable privacy policy/support/license surface or complete store submission package was found. Broad package visibility, VPN use and special-use foreground service require justification and Console review.
- Impact: Cannot sign a production artifact or claim Play acceptance.
- Fix: Release builds can produce explicitly unsigned review artifacts; environment-supplied owner credentials are required for signing. Complete the Console and policy checklist below.

### 3. High-Priority Issues

- **Fixed:** stale remembered app-rule callbacks prevented reliable repeated toggles (`ui/apps/AppsScreen.kt`). Callbacks now update with their rule values.
- **Fixed:** mobile-data tile selected MOBILE_ONLY while claiming to block mobile data. It now uses WIFI_ONLY consistently with snapshot flags. Emergency-mode toggling no longer wipes individually saved block rules.
- **Fixed:** unused Firebase AI, App Check release debug provider, Retrofit/Moshi/OkHttp/logging dependencies and client-secret injection plugin removed from the application. No source calls used them.
- **Fixed:** automatic backup/transfer could copy network history and rules. Both are explicitly excluded; cleartext application traffic is explicitly disallowed. This flag alone does not encrypt raw sockets.
- **Fixed:** malformed DNS bounds, incomplete names and incorrect AAAA/EDNS sinkhole replies; malformed interceptor input is rejected. DNS destination/server/exception logging removed. UDP resolver sockets are connected to the selected server and failed VPN socket protection aborts the request.
- **Open:** LAN exclusions bypass blocking on API 33+, while older versions do not support those exclusions. A local proxy/gateway can defeat the user's assumed complete block; this exception needs an explicit product policy and adversarial testing. IPv6 is not explicitly forwarded: Android blocks the family by default when no IPv6 address/route/DNS server is configured, but physical-device verification is still required.
- **Open:** always-on/lockdown compatibility has not been proven. Allowed apps bypass the VPN and the tunnel is closed when no blocked packages exist, which is incompatible with assuming a full-device always-on forwarding VPN.
- **Open:** physical-network selection uses the first eligible network from deprecated `allNetworks`; simultaneous Wi-Fi/cellular and handover policy can be wrong. Callback exclusion of the VPN avoids self-triggered rebuilds but is not a complete default-network selection solution.
- **Open:** no public legal/privacy policy URL, provenance/update process for the small bundled malware list, or consent text covering retained network history and package inventory.

### 4. Medium-Priority Issues

- **Fixed:** theme preference ignored at the activity root; light/dark/system preference now applies.
- **Fixed:** network logs were unbounded and logger/tile coroutine scopes were not released. History is pruned on the first write and every 100 writes to seven days and the newest 10,000 entries (up to 99 extra entries between pruning). This is an app retention decision, not an M3/Android requirement; disclose it. Historical data is not pruned while the logger is idle.
- **Fixed:** import limits payload size and count, verifies format/version/package IDs and duplicates, preserves stored UID/system identity, and ignores unknown packages. Reset preserves installed-app identity. Export failures show a safe message. Many ViewModel storage failures now show a recoverable error instead of killing the activity.
- **Fixed:** notification destination PendingIntents use different identities; non-start actions do not start a new foreground-service deadline. Notification permission descriptions no longer call it mandatory to run a foreground service. Startup safety content can scroll at large font sizes.
- **Open:** arbitrary imported boolean value types still use permissive JSON conversion; import can report success for a subset of installed packages. Offer a preview/count and stronger per-field validation.
- **Open:** simultaneous per-app field updates still replace whole rows from UI snapshots, so rapid different-field actions can lose updates. Use transactional field updates and regression tests.
- **Open:** stats group by generic packet labels instead of actual source UID; “allowed” logs are not complete traffic accounting. “Today” query cutoff is captured when the ViewModel initializes, so it does not roll over at midnight.
- **Open:** missing package-update refresh, persistent-notification preference without corresponding service behavior, onboarding filter preferences not consistently connected to category state, and boot logic starting for `startOnBoot OR firewallEnabled` need product clarification/testing.
- **Open:** database callbacks launch independent coroutines; corrupted storage and initialization failures need broader device fault-injection coverage. Preferences failures are surfaced, not automatically repaired or reset.
- **Open:** permission settings are offered before permanent denial is established, and the notification request button can be retried after Android stops showing the dialog. There is a skip path and no automatic repeat request, but rationale/permanent-denial UX needs testing.

### 5. Low-Priority Improvements

- Remove unused version-catalog entries, root plugin declarations, template comments and `.env.example` after checking external build tooling.
- Before enabling R8/resource shrinking, replace broad keep rules and nonexistent PackageChangeReceiver/quicksettings rules with actual consumer requirements. Shrinking was already disabled and remains disabled; it was not disabled to hide errors.
- Move remaining literal UI strings into resources; bind displayed version to BuildConfig.
- Review icon caches by byte budget, APK ABI delivery and the large extended-icons library. Main resources are small; screenshot fixtures are test-only.
- Replace sample instrumentation coverage with end-to-end protection/lifecycle tests. Keep useful parser, rule, UI and accessibility tests.

### 6. Security Findings

No production private keys, confidential API credentials, authentication tokens, WebViews, Javascript interfaces, FileProviders, account flow, purchases, push SDK, custom cryptography, app-owned C/C++ or remote HTTP API client were found in application source. Debug signing passwords are conventional public debug credentials, not production secrets. Signing material is ignored by Git. The exported launcher only accepts whitelisted route names; other-app intents cannot directly bind the private VPN service. Exported tiles are protected by the platform tile binding permission. PendingIntents are immutable.

Rules, package inventory and destinations are sensitive metadata kept in app-private Room/DataStore/SharedPreferences, not application-level encrypted. Android sandbox/device encryption is the current protection; no custom Keystore scheme was added. Rooted/unlocked-device extraction and explicit clipboard exports remain risks. DNS upstream code remains in the repository for future integration; it uses plaintext DNS, fallback resolvers, and does not validate the full response question/transaction. It is **not used by the production sinkhole packet path after this audit** and must not be reconnected without further security work.

### 7. Privacy Findings

| Data | Location/use | Transmission/disclosure considerations |
|---|---|---|
| Installed package names, labels, UIDs, system flags, rules | Private `netguardian_database`, installed-package enumeration | Necessary for per-app rules; sensitive installed-app inventory. No app-owned upload path found after SDK removal. |
| Destination IP/domain, protocol, port, timestamp, bytes, block reason | Private Room connection logs | Browsing/network activity. Local history must be explained; delete-logs UI exists. Retention described above. |
| Custom domains and DNS settings | Room + `files/datastore/netguardian_prefs.preferences_pb` | Saved locally. Allowed apps use their own network DNS; blocked DNS is locally sinkholed. Resolver controls currently cannot affect allowed apps. |
| Theme/onboarding/boot/policy preferences | DataStore + `shared_prefs/netguardian_sync_prefs.xml` | Local settings; excluded from automatic backup/transfer. |
| Exported rules including installed package names | User-initiated clipboard JSON | Clipboard/other apps may expose exported data. Prefer Storage Access Framework export with a disclosure. |

No analytics/crash-reporting SDK remains in the app's declared runtime dependencies. Confirm the final merged manifest and dependency graph for each release. Data processed only on-device is not automatically off-device “collection”; complete the Play Data Safety form from the final actual behavior and Google's definitions, including exports and any future DNS backend. Do not simply declare “no data” from this audit. Developer-provided public privacy policy, retention/deletion details, support contact and dependency license notices are required deliverables; no legal policy was fabricated.

### 8. Permission Review

| Permission | Why Used | Required? | Runtime Handling | Play Store Concern |
|---|---|---|---|---|
| INTERNET | VPN/network functionality; resolver module | Yes for network/VPN role | Normal | Explain local VPN behavior and any external DNS use |
| ACCESS_NETWORK_STATE | Connectivity and DNS/network detection | Yes | Normal | None beyond accurate disclosure |
| CHANGE_NETWORK_STATE | VPN/underlying network management | Retained for platform VPN/network APIs | Normal | Verify necessity against final API paths |
| FOREGROUND_SERVICE | Long-running firewall service | Yes | Start failures handled in manager; lifecycle tests needed | Declare continuous user-visible purpose |
| FOREGROUND_SERVICE_SPECIAL_USE | `specialUse` VPN service | Yes for selected type | Manifest subtype present | Console declaration and demonstration required |
| POST_NOTIFICATIONS | Status/error notifications | Optional for notification drawer | Onboarding request, skip, settings retry | Never gate VPN operation on notification grant |
| RECEIVE_BOOT_COMPLETED | Restore requested firewall | Yes for boot feature | Normal; async receiver completes | Verify reboot restrictions and user preference semantics |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Direct exemption request | Conditional on acceptable-use justification | User-initiated Settings flow with fallback | Justify or remove permission and direct exemption flow |
| QUERY_ALL_PACKAGES | Per-app inventory including non-launchable packages | Needed by current design | Normal; no runtime dialog | Restricted; declaration/eligibility approval required |
| BIND_VPN_SERVICE | Protect VPN service from arbitrary binding | Component permission, not requested runtime permission | Android VPN consent | VPN declaration, prominent disclosure/consent where applicable |
| BIND_QUICK_SETTINGS_TILE | Protect tile services | Component permission | Platform-managed | No separate dangerous permission |

No storage, location, contacts, microphone, camera, exact alarm, usage-access, accessibility-service, advertising-ID or account permissions are declared in the source manifest. The packaged manifest additionally contains the AndroidX-generated signature-only `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, an internal receiver protection rather than a user-data permission.

### 9. Dependency Review

| Dependency | Current Version | Issue | Recommended Action |
|---|---|---|---|
| AGP / Gradle | 9.1.1 / 9.3.1 | Lint had an initial internal crash; final evidence below | Keep compatible toolchain; use final lint result, not initial failure alone |
| Compose compiler / KSP | 2.2.10 / 2.3.5 | AGP built-in Kotlin; compile validation needed | Do not blindly upgrade; record resolved graph |
| Compose BOM / Material3 / UI / icons | 2024.12.01 (Material3 1.3.1, UI 1.7.6 resolved) | Older BOM, extended icons increase size; material versions resolved by BOM | Retain pending tested upgrade; evaluate shrinker separately |
| Core KTX / Activity Compose | 1.18.0 / 1.10.1 | Mixed library ages | Build/lint and min/latest-device validation |
| Lifecycle / Navigation Compose | 2.8.7 / 2.8.9 | No source-confirmed vulnerability | Retain; verify restored navigation/device behavior |
| Room runtime/ktx/compiler | 2.7.0 | Missing historical schema/migration | Schema export added; test real upgrades |
| DataStore preferences | 1.1.7 | Corruption/I/O failure recovery incomplete | Add fault-injection tests without silently resetting security rules |
| Coroutines core/android | 1.10.2 | Scope/lifecycle/cancellation risks addressed in touched paths | Exercise shutdown and storage failures |
| Firebase BOM/AI/App Check (including debug provider) | 34.17.0 BOM | Unused release SDKs; unjustified privacy/size surface | Removed from application dependencies |
| Retrofit/converter / OkHttp/logging / Moshi/codegen | 2.12.0 / 4.10.0 / 1.15.2 | Unused | Removed from application dependencies |
| Secrets / Google services app plugins | 2.0.1 / 4.5.0 | Unused client-secret injection/configuration | Removed from app; root apply-false aliases are not packaged |
| JUnit / Robolectric / Roborazzi | 4.13.2 / 4.16.1 / 1.59.0 | Test-only; some tests are shallow | Retain useful suites; expand protection tests |
| AndroidX test runner / ext JUnit / Espresso | 1.6.2 / 1.3.0 / 3.7.0 | Instrumentation currently only a context smoke assertion | Retain; expand device coverage |

No exhaustive CVE/SBOM vulnerability scan was run. Absence of a source finding is not proof that every transitive dependency is vulnerability-free.

### 10. Google Play Checklist

| Requirement | Status | Evidence/action |
|---|---|---|
| Target API level | PASS (configuration) | targetSdk 36; official August 31, 2026 phone-app requirement is API 36 |
| VPN functionality accurately represented | FAIL | Feature gaps and routing limitations above |
| VPN declaration / eligibility / organization-account requirements | MANUAL VERIFICATION REQUIRED | Console owner must validate applicable requirements |
| QUERY_ALL_PACKAGES declaration | MANUAL VERIFICATION REQUIRED | Justify core firewall inventory and lack of narrower alternative |
| Foreground special-use service declaration | MANUAL VERIFICATION REQUIRED | Manifest subtype exists; Console declaration/demo still needed |
| Battery exemption acceptable use | MANUAL VERIFICATION REQUIRED | Direct request needs justification |
| Notification consent/denial UX | MANUAL VERIFICATION REQUIRED | Optional grant path present; test denial and Settings return |
| Privacy policy and Data Safety | FAIL | No published policy/form verified |
| Data deletion | MANUAL VERIFICATION REQUIRED | Clear logs/reset/custom deletion exist; explain app-data deletion and retention |
| Account deletion / app-access credentials / OAuth / billing / push | NOT APPLICABLE | No such app functionality found |
| Advertising ID / background location / exact alarms / accessibility API | NOT APPLICABLE | Absent from source manifest |
| Native 16 KB page-size compatibility | MANUAL VERIFICATION REQUIRED | ZIP alignment and all eight native ELF libraries passed 16 KB alignment checks; still test on a 16 KB device |
| Release signing ownership / Play App Signing / versionCode uniqueness | MANUAL VERIFICATION REQUIRED | Version code 1; owner key absent |
| App icon / adaptive icon / label | PASS (resources) | Present; branding quality and listing screenshots need human review |
| Store screenshots / feature graphic / descriptions / contact / content rating | MANUAL VERIFICATION REQUIRED | No completed Console submission package verified |
| Open-source license notices | FAIL | No complete in-app/license deliverable found |
| Play Integrity | NOT APPLICABLE | No backend/Integrity flow; reassess if product later requires it |

Sources checked on audit date: [target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-EN), [VPN policy](https://support.google.com/googleplay/android-developer/answer/12564964?hl=en), [package visibility policy](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en), [foreground service requirements](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en), [Android VPN Builder](https://developer.android.com/reference/android/net/VpnService.Builder). Console approval is never implied by configuration checks.

### 11. Build Verification

The final clean verification uses `./gradlew clean assembleDebug assembleRelease bundleRelease testDebugUnitTest lint connectedDebugAndroidTest --no-build-cache --console=plain`. Its outcome and exact artifact hashes are recorded in `docs/audit-evidence/` after completion. Earlier runs exposed an internal lint failure, missing signing key, and one obsolete test that expected malformed DNS forwarding; these were investigated rather than suppressed. A subsequent complete run passed 116 unit/Robolectric tests and one physical-device instrumentation test.

R8/minification is **NOT EXECUTED / DISABLED** in the existing release configuration. No shrinking PASS is claimed. The release signing task name is not proof of a signed bundle. APK/AAB outputs without owner credentials are unsigned. Local release smoke-test signing, if performed, uses only the development key on a copy and does not validate upload-key ownership.

Warnings reviewed: deprecated physical-network enumeration; native debug symbols could not be stripped; library update suggestions; obsolete minSdk guards; unused resources; KTX suggestions; string plural/ellipsis and redundant label warnings; and the battery-exemption acceptable-use warning. Corrupt icon fallbacks and the API-26 notification Settings warning were corrected. No lint baseline or blanket suppression was added. The optional AndroidX test-services appops setup emitted `No UID for androidx.test.services`; the actual instrumentation test still completed successfully.

### 12. Manual Testing Required

- Protection matrix: block/unblock, Wi-Fi/mobile/ethernet transitions, VPN coexistence, pause/resume, rapid start/stop, local-network exceptions, IPv4/IPv6, DNS-over-HTTPS/TLS/QUIC, new installs/uninstalls, shared UIDs, work profiles and multiple users.
- Lifecycle: screen off, idle, reboot, revoked VPN consent, killed process, low memory, foreground-start rejection, aggressive OEM battery management and always-on/lockdown. No destructive device reset or permission revocation was performed during the smoke test.
- Storage: genuine previous production upgrade, full disk, corrupted Room/DataStore, concurrent rule edits/import, reset, empty/oversized/invalid JSON, unknown-package imports and logged-data retention/deletion.
- UX: fresh-install onboarding with every denial combination; permanent denial/settings return; all routes and notification actions; font scaling, TalkBack, keyboard/switch access, RTL, small phones, tablets/foldables, landscape, gesture/three-button navigation and dark/light/system mode. Existing automated component screenshots do not prove every full screen.
- Offline: airplane mode, captive portal, no usable DNS, slow/dropped networks and network transitions while rules rebuild. Do not infer resilient VPN operation from resolver unit tests.
- Distribution: owner-signed release install, Android minimum API 24 and latest API 36, 16 KB device/emulator, release pre-launch report and internal/closed-track testing, Play declaration eligibility, policy URL, licenses, signing ownership, listing and version uniqueness.
- OAuth/backend/purchases/push/third-party dashboards: not applicable to current application; no fabricated integration checks.

### 13. Files Changed

| File | Change |
|---|---|
| `.gitignore` | Ignore upload/debug signing material. |
| `app/build.gradle.kts` | Remove unused production SDKs and injection plugins; export Room schema; make unsigned review builds explicit. |
| `app/schemas/com.example.database.AppDatabase/2.json` | Export the actual current Room schema for future migration validation. |
| `app/src/main/AndroidManifest.xml` | Disable automatic backup and cleartext application traffic. |
| `app/src/main/java/com/example/MainActivity.kt` | Apply saved theme and display storage-operation failures. |
| `app/src/main/java/com/example/data/PreferencesRepository.kt` | Avoid synchronous SharedPreferences commit in observed state mapping. |
| `app/src/main/java/com/example/data/SetupDiagnostics.kt` | Correct optional notification guidance and guard notification Settings intent below API 26. |
| `app/src/main/java/com/example/database/AppDatabase.kt` | Remove destructive fallback and export schema version 2. |
| `app/src/main/java/com/example/database/FirewallDao.kt` | Add bounded-history pruning query. |
| `app/src/main/java/com/example/dns/DnsInterceptor.kt` | Reject malformed queries and remove sensitive DNS logs. |
| `app/src/main/java/com/example/dns/DnsPacketParser.kt` | Validate bounds/questions and generate correct A/AAAA/EDNS sinkhole answers. |
| `app/src/main/java/com/example/dns/DnsResolver.kt` | Remove destination/server logging, propagate cancellation, require socket protection and connect UDP peer. |
| `app/src/main/java/com/example/firewall/BlockAllTileService.kt` | Preserve individual rules when toggling quick mode; cancel scope and handle write errors. |
| `app/src/main/java/com/example/firewall/ConnectionLogger.kt` | Close/cancel worker and prune retained history. |
| `app/src/main/java/com/example/firewall/FirewallManager.kt` | Persist mode before reload, guard inactive reloads, correct mobile flag and handle start/storage failures. |
| `app/src/main/java/com/example/firewall/FirewallNotificationManager.kt` | Separate route PendingIntent identities and use foreground start only for START action. |
| `app/src/main/java/com/example/firewall/FirewallStateRepository.kt` | Correct mobile flag, clear effective DNS on pause/error and handle preference failure. |
| `app/src/main/java/com/example/firewall/FirewallVpnService.kt` | Serialize tunnel rebuilds; fix pause/resume, blocked DNS leakage, empty allow-list, worker lifecycle and errors. |
| `app/src/main/java/com/example/firewall/MobileDataTileService.kt` | Correct mobile-block policy; cancel scope and handle write errors. |
| `app/src/main/java/com/example/firewall/WifiOnlyTileService.kt` | Avoid inactive service reload, cancel scope and handle write errors. |
| `app/src/main/java/com/example/ui/MainViewModel.kt` | Surface storage errors, preserve package identity in reset/import and bound/validate imported JSON. |
| `app/src/main/java/com/example/ui/apps/AppsScreen.kt` | Refresh remembered callbacks when the current rule changes. |
| `app/src/main/java/com/example/ui/home/FirewallStartupSafetyDialog.kt` | Make long startup disclosure content scrollable. |
| `app/src/main/java/com/example/ui/privacy/PrivacyScreen.kt` | Show limited DNS protection accurately and remove forced nullable error access. |
| `app/src/main/java/com/example/ui/settings/ProtectionSetupScreen.kt` | Correct notification and battery-optimization claims. |
| `app/src/main/java/com/example/ui/settings/SettingsScreen.kt` | Show an understandable export failure instead of an uncaught exception. |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | Reuse existing shield artwork as the legacy launcher fallback. |
| `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` | Reuse existing shield artwork as the legacy launcher fallback. |
| `app/src/main/res/mipmap-hdpi/ic_launcher.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-mdpi/ic_launcher.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` | Remove corrupt legacy launcher bitmap; vector fallback replaces it. |
| `app/src/main/res/values/strings.xml` | Correct DNS/privacy/version/overhead claims and add limited-protection status. |
| `app/src/main/res/xml/backup_rules.xml` | Exclude private data from legacy backup. |
| `app/src/main/res/xml/data_extraction_rules.xml` | Exclude private data from cloud backup and device transfer. |
| `app/src/test/java/com/example/LauncherResourceTest.kt` | Verify legacy/adaptive icon resources load on API 24 and 34. |
| `app/src/test/java/com/example/database/FirewallStorageTest.kt` | Verify rule reset retains package identity and pruning removes old history. |
| `app/src/test/java/com/example/dns/DnsInterceptorTest.kt` | Assert malformed queries never reach the resolver. |
| `app/src/test/java/com/example/dns/DnsPacketParserTest.kt` | Add malformed bounds, offset, truncation, AAAA and EDNS regression cases. |
| `docs/PRODUCTION_READINESS_AUDIT.md` | Full findings, verification, limitations and release checklist. |
| `docs/audit-evidence/` | Build/test/dependency/artifact evidence; audit outputs, not app runtime files. |

### 14. Remaining Risks

The app is a selective local sinkhole, not a complete forwarding firewall or universal DNS filter. Do not advertise full traffic inspection, full malware protection, reliable background blocking, new-app protection or complete fail-closed behavior based on these changes. Missing legacy migrations, sensitive metadata disclosure, inaccurate statistics, LAN bypass, network selection, lifecycle races and untested device combinations remain. Unsigned artifacts are review outputs, not production release candidates approved for upload.

### 15. Final Release Checklist

- [ ] Complete and test the missing protection features or explicitly agree on a truthful product scope.
- [ ] Obtain historical production schema/artifact and pass data-preserving upgrade tests.
- [ ] Verify blocked/allowed traffic, pause, handover, reboot, consent denial and all supported transports on real devices.
- [ ] Publish the actual privacy policy, support and open-source notices; verify deletion/retention disclosures.
- [ ] Complete VPN, broad package visibility, special-use FGS, battery exemption, Data Safety, rating and store-listing declarations.
- [ ] Set intentional unique versionCode/versionName and verify final application ID/branding.
- [ ] Supply owner-controlled upload signing credentials; build and verify the signed production AAB.
- [ ] Run clean builds, automated tests, lint, release startup, native/page-size checks and Play pre-launch report on the exact candidate.
- [ ] Review every final diff and dependency/manifest change; confirm no debug providers, secrets, misleading claims or temporary signing modifications are present.
- [ ] Obtain release-owner approval only after all blockers are closed.
