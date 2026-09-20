// Requires the Firestore emulator on localhost:8180, project demo-carelink-review.
import assert from 'node:assert/strict';
const base = 'http://127.0.0.1:8180/v1/projects/demo-carelink-review/databases/(default)/documents';
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = uid => `${encode({alg:'none',typ:'JWT'})}.${encode({sub:uid,user_id:uid,
  aud:'demo-carelink-review',iss:'https://securetoken.google.com/demo-carelink-review',
  iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,
  firebase:{sign_in_provider:'custom'}})}.`;
const field = value => typeof value === 'string' ? {stringValue:value} : typeof value === 'boolean'
  ? {booleanValue:value} : Array.isArray(value) ? {arrayValue:{values:value.map(field)}}
  : {integerValue:String(value)};
async function request(uid, method, path, data) {
  return fetch(`${base}/${path}`, {method, headers:{
    Authorization:`Bearer ${token(uid)}`, 'Content-Type':'application/json'},
    body:data && JSON.stringify({fields:Object.fromEntries(Object.entries(data).map(([k,v])=>[k,field(v)]))})});
}
const medication = {patientId:'owner',name:'Test medication',strength:'5 mg',dose:'1 tablet',
  frequency:'Daily',reminderTimes:['08:00','20:00'],instructions:'',active:true};
const path = 'users/owner/medications/rules-test';
assert.equal((await request('owner','PATCH',path,medication)).status,200,'owner can create');
assert.equal((await request('stranger','GET',path)).status,403,'other patient cannot read');
assert.equal((await request('stranger','PATCH',path,medication)).status,403,'other patient cannot edit');
assert.equal((await request('owner','PATCH',path,{...medication,patientId:'stranger'})).status,403,'cannot change owner');
assert.equal((await request('owner','PATCH',path,{...medication,name:''})).status,403,'invalid medication rejected');
assert.equal((await request('owner','GET',path)).status,200,'owner can retrieve');
assert.equal((await request('owner','PATCH',path,{...medication,dose:'2 tablets'})).status,200,'owner can update');
for (const [index,status] of ['taken','missed','delayed','skipped'].entries()) {
  const dose={medicationId:'rules-test',scheduledTimeMillis:1800000000000+index,status,completionTimeMillis:1800000001000+index};
  const dosePath=`users/owner/doseRecords/rules-dose-${index}`;
  assert.equal((await request('owner','PATCH',dosePath,dose)).status,200,`record ${status}`);
  assert.equal((await request('owner','GET',dosePath)).status,200,'retrieve dose');
  assert.equal((await request('stranger','GET',dosePath)).status,403,'isolate dose history');
}
assert.equal((await request('owner','PATCH','users/owner/doseRecords/invalid',{
  medicationId:'rules-test',scheduledTime:'08:00',status:'taken',completionTimeMillis:1800000001000})).status,403,'reject malformed dose');
assert.equal((await request('owner','DELETE',path)).status,200,'owner can remove');
console.log('Medication CRUD, dose recording, validation, and account isolation passed.');
