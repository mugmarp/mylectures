import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, deleteDoc, serverTimestamp } from 'firebase/firestore';

// A demo project and loopback-only emulator prevent accidental production access.
const host = process.env.FIRESTORE_EMULATOR_HOST;
if (!host || !/^(127\.0\.0\.1|localhost):\d+$/.test(host)) {
  throw new Error('Run npm run test:emulator; a loopback Firestore emulator is required.');
}
let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-mylectures-sync',
    firestore: { rules: await readFile(new URL('../../firestore.rules', import.meta.url), 'utf8') }
  });
});
beforeEach(async () => env.clearFirestore());
after(async () => env?.cleanup());
const db = (uid) => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
const note = { natural_key: 'note', content: 'Offline lecture notes', is_pinned: false, is_deleted: false, owner_id: 'alice', updated_at: 1 };
const event = { client_uuid: 'event', title: 'Study', day_of_week: 'Monday', start_time: '10:00', end_time: '11:00', location: '', notes: '', repeat_weekly: true, category: 'Study', color_tag: '#2563EB', owner_id: 'alice', updated_at: 1, is_deleted: false };
const assignment = { client_uuid: 'task', title: 'Coursework', course_code: 'CS101', due_date: '2026-11-20', priority: 'Medium', notes: '', completed: false, owner_id: 'alice', updated_at: 1, is_deleted: false };
const settings = { theme_mode: 'LIGHT', theme_accent: 0, notifications_enabled: true, class_alarm_lead_minutes: 30, task_reminder_lead_hours: 2, alarm_vibration: true, alarm_sound: 'Chime', academic_year: '2026/2027', semester: 'Semester 1', updated_at: 1 };
const cases = [['notes/note', note], ['events/event', event], ['assignments/task', assignment], ['settings/preferences', settings]];

for (const [path, payload] of cases) {
  test(`${path}: owner can write, read, update and delete`, async () => {
    const ref = doc(db('alice'), `users/alice/${path}`);
    await assertSucceeds(setDoc(ref, payload));
    await assertSucceeds(getDoc(ref));
    await assertSucceeds(setDoc(ref, { ...payload, updated_at: 2 }));
    await assertSucceeds(deleteDoc(ref));
  });
  test(`${path}: anonymous and other accounts cannot access records`, async () => {
    await assertSucceeds(setDoc(doc(db('alice'), `users/alice/${path}`), payload));
    for (const uid of [null, 'bob']) {
      const ref = doc(db(uid), `users/alice/${path}`);
      await assertFails(getDoc(ref));
      await assertFails(setDoc(ref, payload));
      await assertFails(deleteDoc(ref));
    }
  });
  test(`${path}: unexpected fields and missing required fields are rejected`, async () => {
    const ref = doc(db('alice'), `users/alice/${path}`);
    await assertFails(setDoc(ref, { ...payload, admin: true }));
    const { updated_at, ...missingRevision } = payload;
    await assertFails(setDoc(ref, missingRevision));
  });
}

test('nullable note fields match the Android upload payload', async () => {
  await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/notes/note'), {
    ...note, title: null, colour_tag: null, alarm_minutes: null, tag: null,
    attached_class: null, attachment_name: null, attachment_path: null
  }));
  await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/events/event'), { ...event, alarm_minutes: null }));
  await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/assignments/task'), { ...assignment, reminder_minutes: null }));
  await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/settings/preferences'), { ...settings, programme: null }));
});

test('owner spoofing and mismatched document identities are rejected', async () => {
  for (const [path, payload] of cases.slice(0, 3)) {
    await assertFails(setDoc(doc(db('alice'), `users/alice/${path}`), { ...payload, owner_id: 'bob' }));
    await assertFails(setDoc(doc(db('alice'), `users/alice/${path}-other`), payload));
  }
});

test('oversized content, invalid types, unknown settings and subcollections are rejected', async () => {
  await assertFails(setDoc(doc(db('alice'), 'users/alice/notes/note'), { ...note, content: 'x'.repeat(100001) }));
  await assertFails(setDoc(doc(db('alice'), 'users/alice/notes/note'), { ...note, alarm_minutes: '30' }));
  await assertFails(setDoc(doc(db('alice'), 'users/alice/settings/other'), settings));
  await assertFails(setDoc(doc(db('alice'), 'users/alice/private/item'), { secret: true }));
});

test('public timetable is readable and cannot be changed by clients', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => setDoc(doc(ctx.firestore(), 'timetable/class'), { course: 'CS101' }));
  for (const uid of [null, 'alice']) {
    const ref = doc(db(uid), 'timetable/class');
    await assertSucceeds(getDoc(ref));
    await assertFails(setDoc(ref, { course: 'other' }));
    await assertFails(deleteDoc(ref));
  }
});

test('existing anonymous feedback accepts generated document IDs and remains create-only', async () => {
  const ref = doc(db(null), 'feedback/generated-document-id');
  const payload = { id: 'client-id', instanceId: 'install', category: 'General', categoryKey: 'general', message: 'Useful app', formattedDate: '2026-11-14', appVersion: '1.0', androidRelease: '16', deviceModel: 'Test', enrolledProgramme: 'BCS I', submittedAt: 1 };
  await assertSucceeds(setDoc(ref, payload));
  await assertFails(getDoc(ref));
  await assertFails(setDoc(ref, payload));
  await assertFails(deleteDoc(ref));
});

test('existing telemetry accepts bounded writes and denies reads, deletes and identity mismatch', async () => {
  const payload = { install_id: 'install', app_version: '1.0', enrolled_programme: 'BCS I', device_model: 'Test', android_version: '16', sdk_int: 36, first_launch_timestamp: 1, last_active_at: serverTimestamp(), total_launches: 1, features_used: {} };
  const ref = doc(db(null), 'user_telemetry/install');
  await assertSucceeds(setDoc(ref, payload));
  await assertFails(getDoc(ref));
  await assertFails(deleteDoc(ref));
  await assertFails(setDoc(doc(db(null), 'user_telemetry/other'), payload));
});
