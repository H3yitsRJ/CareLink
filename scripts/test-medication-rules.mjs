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
// Provision only emulator profiles. Each caregiver grant is owned by the patient.
assert.equal((await request('caregiver','PATCH','users/caregiver',{fullName:'Caregiver'})).status,200);
const grantPath = 'users/owner/medicationCaregivers/caregiver';
const grant = {patientId:'owner',caregiverId:'caregiver',patientName:'Test patient',permissions:['VIEW'],revoked:false};
assert.equal((await request('caregiver','PATCH',grantPath,grant)).status,403,'caregiver cannot self-authorize');
assert.equal((await request('owner','PATCH',grantPath,grant)).status,200,'patient can grant view');
assert.equal((await request('caregiver','GET',grantPath)).status,200,'caregiver can verify own grant');
assert.equal((await request('stranger','GET',grantPath)).status,403,'unrelated accounts cannot read grants');
assert.equal((await request('caregiver','GET',path)).status,200,'view grant reads medication');
assert.equal((await request('caregiver','GET','users/owner/medications')).status,200,'view grant lists patient medications');
async function caregiverEdit(extra = {}, actor = 'caregiver', target = path, stamp = true) {
  const fields = {dose:'3 tablets',updatedById:actor,...extra};
  return fetch(base.replace('/documents','/documents:commit'), {method:'POST',headers:{
    Authorization:`Bearer ${token(actor)}`,'Content-Type':'application/json'},body:JSON.stringify({writes:[{
      update:{name:`projects/demo-carelink-review/databases/(default)/documents/${target}`,
        fields:Object.fromEntries(Object.entries(fields).map(([k,v])=>[k,field(v)]))},
      updateMask:{fieldPaths:Object.keys(fields)},
      ...(stamp ? {updateTransforms:[{fieldPath:'updatedAt',setToServerValue:'REQUEST_TIME'}]} : {})
    }]})});
}
assert.equal((await caregiverEdit()).status,403,'view-only cannot edit');
assert.equal((await request('owner','PATCH',grantPath,{...grant,permissions:['VIEW','EDIT']})).status,200);
assert.equal((await caregiverEdit()).status,200,'authorized caregiver edits');
const edited = await (await request('owner','GET',path)).json();
assert.equal(edited.fields.dose.stringValue,'3 tablets','edit persists to patient record');
assert.equal(edited.fields.patientId.stringValue,'owner','ownership preserved');
assert.equal(edited.fields.updatedById.stringValue,'caregiver','editor attributed');
assert.ok(edited.fields.updatedAt.timestampValue,'server edit timestamp stored');
assert.equal((await caregiverEdit({patientId:'caregiver'})).status,403,'cannot transfer ownership');
assert.equal((await caregiverEdit({active:false})).status,403,'cannot deactivate through edit permission');
assert.equal((await caregiverEdit({name:''})).status,403,'invalid required field rejected');
assert.equal((await caregiverEdit({updatedById:'owner'})).status,403,'cannot forge editor');
assert.equal((await request('caregiver','DELETE',path)).status,403,'edit does not grant delete');
assert.equal((await request('caregiver','PATCH','users/owner/medications/new',medication)).status,403,'edit does not grant create');
assert.equal((await request('caregiver','PATCH',grantPath,{...grant,permissions:['VIEW','EDIT']})).status,403,'cannot alter grants');
assert.equal((await request('owner','PATCH',grantPath,{...grant,permissions:['VIEW','DELETE']})).status,403,'only medication view/edit supported');
assert.equal((await request('owner','PATCH',grantPath,{...grant,revoked:true})).status,200,'owner revokes');
assert.equal((await request('caregiver','GET',path)).status,403,'revocation blocks reads');
assert.equal((await caregiverEdit()).status,403,'revocation blocks stale form save');
assert.equal((await request('owner','PATCH',grantPath,{...grant,permissions:['VIEW','EDIT']})).status,200,'owner restores access');
assert.equal((await caregiverEdit({},'stranger')).status,403,'unrelated account cannot edit');
for (const [index,status] of ['taken','missed','delayed','skipped'].entries()) {
  const dose={medicationId:'rules-test',scheduledTimeMillis:1800000000000+index,status,completionTimeMillis:1800000001000+index};
  const dosePath=`users/owner/doseRecords/rules-dose-${index}`;
  assert.equal((await request('owner','PATCH',dosePath,dose)).status,200,`record ${status}`);
  assert.equal((await request('owner','GET',dosePath)).status,200,'retrieve dose');
  assert.equal((await request('stranger','GET',dosePath)).status,403,'isolate dose history');
  assert.equal((await request('caregiver','GET',dosePath)).status,403,'medication access does not grant dose history');
}
assert.equal((await request('owner','PATCH','users/owner/doseRecords/invalid',{
  medicationId:'rules-test',scheduledTime:'08:00',status:'taken',completionTimeMillis:1800000001000})).status,403,'reject malformed dose');
assert.equal((await request('owner','DELETE',path)).status,200,'owner can remove');
console.log('Medication CRUD, dose recording, validation, and account isolation passed.');
