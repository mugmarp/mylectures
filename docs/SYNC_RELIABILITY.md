# Account sync behavior and verification

Personal notes, coursework tasks and activities are visible only to their account
and the guest session. Signing in and running a full sync promotes guest records
to the authenticated account. New edits are stamped with the active account.
Public timetable listeners stay active when personal account listeners stop.

A full sync runs on sign-in or from the account/settings sync action. Continuous
listeners apply incoming cloud changes; local edits still require a full sync to
upload. Notes preserve divergent text as explicit draft sections. Repeating the
same merge does not append duplicate drafts. Tasks and activities use timestamps
for updates and deletions while preserving their existing SQLite IDs.

Account changes invalidate queued personal listener work. Local rows are checked
again when merging cloud snapshots so another account's record cannot be
reassigned by a colliding note key or record UUID. Such collisions require
resolving the other local account and produce a sync error. Upload failures are
reported, and tombstone cleanup is skipped. Successful sync removes only the
active account's tombstones older than 30 days.

Explicit sign-out removes that account's local notes, tasks and activities and
cancels its scheduled task and note reminders. Guest and other account rows are
retained. **Sync before signing out:** unsent edits are removed by sign-out and
are not guaranteed to exist in the cloud. The confirmation dialog explains this.
Attachment filenames can sync, but private attachment paths stay on their own
device; files are not uploaded by this feature.

Settings revisions are persisted when a preference changes. Exporting unchanged
settings does not create a new revision. Imported settings retain the cloud
revision, allowing a later device to adopt newer settings.

## Automated checks

Android CI runs the existing suite plus session, conflict preservation,
account-scoped Room, cleanup and settings revision regressions. The separate
Firestore rules workflow validates owner access, payload validation and existing
public timetable, anonymous telemetry and feedback contracts.

Run rules tests locally with Node 22 and Java 21 or newer:

```sh
cd tests/firestore
npm ci --no-audit --no-fund
npm run test:emulator
```

The harness requires a loopback emulator and uses `demo-mylectures-sync`. It does
not use production credentials or deploy rules. Production rules deployment is a
separate release step and must be coordinated with the compatible app release.

## Device checks before release

Real Google sign-in requires the release package/signing configuration and web
client ID. Test on two devices: edit a note offline on both, sync in each order,
verify both drafts, update/delete a task and activity, then repeat sync. Simulate
loss of connectivity and permission-denied responses; the app should report an
incomplete sync and retain local records. Switch accounts during an active sync,
then sign out and verify private lists, reminders and notifications are cleared.
Confirm settings edited on the second device replace older preferences. These
live authentication, delivery and multi-device checks are not covered by the
local rules emulator or JVM suite.
