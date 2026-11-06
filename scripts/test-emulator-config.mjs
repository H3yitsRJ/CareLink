import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const config = JSON.parse(readFileSync(new URL('../firebase.json', import.meta.url)));
const android = readFileSync(new URL('../app/src/debug/java/com/example/carelink/FirebaseEmulatorConfiguration.kt', import.meta.url), 'utf8');
for (const [service, client] of [['auth', 'FirebaseAuth'], ['firestore', 'FirebaseFirestore']]) {
  const match = android.match(new RegExp(`${client}\\.getInstance\\(\\)\\.useEmulator\\(ANDROID_EMULATOR_HOST, (\\d+)\\)`));
  assert.ok(match, `Android ${service} emulator routing exists`);
  assert.equal(Number(match[1]), config.emulators[service].port, `${service} app and emulator ports agree`);
}
console.log('Android Firebase emulator ports match firebase.json.');
