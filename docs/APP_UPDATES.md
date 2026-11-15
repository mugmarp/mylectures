# App release checks

Settings checks for a newer stable release on opening and offers a scrollable
release-highlights dialog. Tap the release row to open the dialog or retry a
failed check. The action opens the official `mugmarp/mylectures` GitHub release
page, where the student chooses the APK and follows installation instructions.
A recommended update can always be dismissed to keep using the offline timetable.

The app no longer downloads APKs or launches the package installer itself. The
installer permission and update-cache FileProvider path are removed; the notes
attachment FileProvider remains. Old cached update files are cleaned up without
touching notes or attachments.

## Sources and comparison

The checker first requests the optional repository `version.json`. A missing,
malformed or unavailable response falls back to the latest published GitHub
release API. At implementation time the repository had no `version.json`; its
latest published release was `v1.1.0`, older than the current `1.1.1`/build 3.
That correctly produces no update prompt. No release or version bump is created
by this change.

Only a successful, validated candidate is cached, for 15 minutes. Manual retries
bypass the cache. Foreground and background checks share the checker. Request
cancellation propagates, and failure of both sources produces an error rather
than an "up to date" result. Periodic notifications are deduplicated per installed
build and remote release. Disabled notifications do not consume that marker.

If metadata supplies an Android version code, it must be a positive integer and
strictly exceed the installed code; its display version must not be older. A
GitHub tag has no Android version code: compare its numeric semantic version
without inventing a code from the tag. Tag-only fallback cannot detect a new APK
with the same display version; publish metadata with its real code for that case.
Drafts, prereleases and malformed release versions are not offered.

Metadata may use camelCase fields, the existing snake_case aliases, or an
`app_update` wrapper. Example for a future release (the values must match the
actual signed APK and published release):

```json
{
  "versionName": "1.2.0",
  "versionCode": 4,
  "releaseNotes": "- Improved offline notes\n- More reliable reminders",
  "releaseUrl": "https://github.com/mugmarp/mylectures/releases/tag/v1.2.0",
  "isMandatory": false
}
```

Legacy HTTPS `downloadUrl` fields are accepted, but the app opens the official
release page rather than the APK URL. Explicit release pages must belong to this
repository and match the advertised version. Metadata transport uses HTTPS,
limits response size, closes responses and blocks redirects that change scheme.

## Verification

The JVM suite covers metadata contracts, version/code comparisons, fallback,
cache expiry and refresh, cancellation, concurrent checks, browser intent/error
handling, legacy cache cleanup, changelog expansion and update notification
wording/permissions. Android CI compiles Settings and the dialog, builds debug and
release APKs, verifies signing and runs the full suite.

Before release, use a device to verify: offline checks show retry feedback;
a newer published fixture opens a readable dialog; all highlights expand; Later
and Back dismiss recommended updates; View Update Release opens the correct page;
and a missing/disabled browser displays an error. Check small-screen/landscape
layout and an upgrade from a previously installed signed build. Actual downloads
and Android's install/signature checks happen outside this app, in the browser
and system installer.
