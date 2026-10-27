// Run against the local Firestore emulator on port 8180.
import assert from 'node:assert/strict';

const project = 'demo-carelink-owner';
const host = 'http://127.0.0.1:8180';
const base =
  `${host}/v1/projects/${project}/databases/(default)/documents`;

const encode = value =>
  Buffer.from(JSON.stringify(value)).toString('base64url');

function token(uid) {
  const now = Math.floor(Date.now() / 1000);
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode({
    sub: uid,
    user_id: uid,
    aud: project,
    iss: `https://securetoken.google.com/${project}`,
    iat: now,
    exp: now + 3600,
    firebase: { sign_in_provider: 'custom' }
  })}.`;
}

function field(value) {
  if (typeof value === 'string') return { stringValue: value };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (Array.isArray(value)) {
    return { arrayValue: { values: value.map(field) } };
  }
  return { integerValue: String(value) };
}

async function check(uid, method, path, data, expected, label) {
  const response = await fetch(`${base}/${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(uid ? { Authorization: `Bearer ${token(uid)}` } : {})
    },
    ...(data ? {
      body: JSON.stringify({
        fields: Object.fromEntries(
          Object.entries(data).map(([key, value]) => [key, field(value)])
        )
      })
    } : {})
  });

  assert.equal(
    response.status,
    expected,
    `${label}: ${await response.text()}`
  );
}

assert.equal(
  (await fetch(
    `${host}/emulator/v1/projects/${project}/databases/(default)/documents`,
    { method: 'DELETE' }
  )).status,
  200,
  'Reset disposable local owner-test database'
);

const fixtures = [
  ['users/owner', { fullName: 'Test owner' }],
  ['users/owner/medications/record', {
    patientId: 'owner',
    name: 'Test medication',
    strength: '5 mg',
    dose: '1 tablet',
    frequency: 'Daily',
    reminderTimes: ['08:00'],
    instructions: '',
    active: true
  }],
  ['users/owner/appointments/record', {
    patientId: 'owner', title: 'Test appointment'
  }],
  ['users/owner/healthConcerns/record', {
    patientId: 'owner', title: 'Test concern'
  }],
  ['users/owner/careTasks/record', {
    patientId: 'owner', title: 'Test task'
  }],
  ['careTasks/owner-record', {
    patientId: 'owner', title: 'Legacy task'
  }],
  ['users/owner/doseRecords/record', {
    medicationId: 'record',
    scheduledTimeMillis: 1000,
    completionTimeMillis: 2000,
    status: 'taken'
  }]
];

for (const [path, data] of fixtures) {
  // Exercise creation denials before the owner creates the document.
  for (const actor of [null, 'stranger']) {
    await check(actor, 'PATCH', path, data, 403,
      `${actor ?? 'Signed-out user'} cannot create ${path}`);
  }

  await check('owner', 'PATCH', path, data, 200,
    `Owner creates ${path}`);
  await check('owner', 'GET', path, null, 200,
    `Owner reads ${path}`);

  const updated = { ...data };
  if ('title' in updated) updated.title = 'Updated title';
  else if ('dose' in updated) updated.dose = '2 tablets';
  else if ('status' in updated) updated.status = 'delayed';
  else updated.fullName = 'Updated owner';

  for (const actor of [null, 'stranger']) {
    const label = actor ?? 'Signed-out user';
    await check(actor, 'GET', path, null, 403,
      `${label} cannot read ${path}`);
    await check(actor, 'PATCH', path, updated, 403,
      `${label} cannot update ${path}`);
    await check(actor, 'DELETE', path, null, 403,
      `${label} cannot delete ${path}`);
  }

  await check('owner', 'PATCH', path, updated, 200,
    `Owner updates ${path}`);

  if ('patientId' in data) {
    await check('owner', 'PATCH', path,
      { ...updated, patientId: 'stranger' }, 403,
      `Owner cannot transfer ownership of ${path}`);
  }
}

const historyPath = 'users/owner/careHistory/medication-event';
const historyEvent = {
  patientId: 'owner',
  actorId: 'owner',
  occurredAtMillis: 1000,
  type: 'MEDICATION',
  summary: 'Medication added',
  relatedRecordId: 'record'
};

for (const actor of [null, 'stranger']) {
  await check(actor, 'PATCH', historyPath, historyEvent, 403,
    'Unauthorized user cannot create medication history');
}

await check('owner', 'PATCH', historyPath, historyEvent, 200,
  'Owner creates medication history');
await check('owner', 'GET', historyPath, null, 200,
  'Owner reads medication history');
await check('owner', 'GET', 'users/owner/careHistory', null, 200,
  'Owner lists care history');

for (const actor of [null, 'stranger']) {
  await check(actor, 'GET', historyPath, null, 403,
    'Unauthorized user cannot read medication history');
  await check(actor, 'GET', 'users/owner/careHistory', null, 403,
    'Unauthorized user cannot list care history');
  await check(actor, 'PATCH', historyPath,
    { ...historyEvent, summary: 'Changed' }, 403,
    'Unauthorized user cannot update medication history');
  await check(actor, 'DELETE', historyPath, null, 403,
    'Unauthorized user cannot delete medication history');
}

await check('owner', 'PATCH', historyPath,
  { ...historyEvent, summary: 'Changed' }, 403,
  'Existing medication history cannot be edited');
await check('owner', 'DELETE', historyPath, null, 403,
  'Existing medication history cannot be deleted');

await check('owner', 'PATCH',
  'users/owner/careHistory/forged-actor',
  { ...historyEvent, actorId: 'stranger' }, 403,
  'Owner cannot forge the history actor');

await check('owner', 'PATCH',
  'users/owner/careHistory/wrong-patient',
  { ...historyEvent, patientId: 'stranger' }, 403,
  'History patient must match the owner path');

console.log('Owner create/read/update, ownership protection, and '
  + 'signed-out/stranger denials passed.');

