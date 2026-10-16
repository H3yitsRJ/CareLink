// Run against the local Firestore emulator with this checkout's firestore.rules.
import assert from 'node:assert/strict';
const project = 'demo-carelink-permissions';
const host = 'http://127.0.0.1:8180';
const base = `${host}/v1/projects/${project}/databases/(default)/documents`;
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = uid => `${encode({alg:'none',typ:'JWT'})}.${encode({sub:uid,user_id:uid,aud:project,
  iss:`https://securetoken.google.com/${project}`,iat:Math.floor(Date.now()/1000),
  exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'custom'}})}.`;
const field = value => typeof value === 'string' ? {stringValue:value} : typeof value === 'boolean'
  ? {booleanValue:value} : Array.isArray(value) ? {arrayValue:{values:value.map(field)}} : {integerValue:String(value)};
const fields = data => Object.fromEntries(Object.entries(data).map(([k,v])=>[k,field(v)]));
const headers = uid => ({'Content-Type':'application/json',...(uid ? {Authorization:`Bearer ${token(uid)}`} : {})});
async function request(uid, method, path, data) {
  return fetch(`${base}/${path}`,{method,headers:headers(uid),body:data && JSON.stringify({fields:fields(data)})});
}
async function commit(uid, path, data, timestamp, mask) {
  return fetch(`${base}:commit`,{method:'POST',headers:headers(uid),body:JSON.stringify({writes:[{
    update:{name:`projects/${project}/databases/(default)/documents/${path}`,fields:fields(data)},
    ...(mask ? {updateMask:{fieldPaths:Object.keys(data)}} : {}),
    ...(timestamp ? {updateTransforms:[{fieldPath:timestamp,setToServerValue:'REQUEST_TIME'}]} : {})
  }]})});
}
// Seed grants as emulator administrator so malformed legacy grants can also be tested.
async function seed(path, data) {
  const response = await fetch(`${base}/${path}`,{method:'PATCH',headers:{'Content-Type':'application/json',Authorization:'Bearer owner'},body:JSON.stringify({fields:fields(data)})});
  assert.equal(response.status,200,`seed ${path}`);
}
assert.equal((await fetch(`${host}/emulator/v1/projects/${project}/databases/(default)/documents`,{method:'DELETE'})).status,200);
const medication = {patientId:'patient',name:'Test medication',strength:'5 mg',dose:'1 tablet',frequency:'Daily',reminderTimes:['08:00'],instructions:'',active:true};
const med = 'users/patient/medications/medication';
const concern = {patientId:'patient',title:'Test concern'};
const appointment = {patientId:'patient',title:'Test appointment'};
const task = {patientId:'patient',title:'Test task'};
for (const [path,data] of [[med,medication],['users/patient/healthConcerns/concern',concern],['users/patient/appointments/appointment',appointment],['users/patient/careTasks/task',task],['users/patient/doseRecords/dose',{medicationId:'medication',scheduledTimeMillis:1,completionTimeMillis:2,status:'taken'}]]) {
  assert.equal((await request('patient','PATCH',path,data)).status,200,`owner creates ${path}`);
  assert.equal((await request('patient','GET',path)).status,200,`owner reads ${path}`);
}
const grantPath = 'users/patient/medicationCaregivers/caregiver';
const grant = {patientId:'patient',caregiverId:'caregiver',permissions:['VIEW'],revoked:false};
const refill = actor => ({patientId:'patient',medicationId:'medication',requestedById:actor,note:'',status:'REQUESTED'});
const refillPath = 'users/patient/refillRequests/medication';
const cases = [
  ['absent',null,false,false], ['empty',{...grant,permissions:[]},false,false],
  ['view',grant,true,false], ['edit',{...grant,permissions:['VIEW','EDIT']},true,true],
  ['revoked',{...grant,permissions:['VIEW','EDIT'],revoked:true},false,false],
  ['edit without view',{...grant,permissions:['EDIT']},false,false],
  ['unknown permission',{...grant,permissions:['VIEW','DELETE']},false,false],
  ['wrong patient',{...grant,patientId:'other'},false,false],
  ['wrong caregiver',{...grant,caregiverId:'other'},false,false],
  ['missing revocation',{patientId:'patient',caregiverId:'caregiver',permissions:['VIEW']},false,false]
];
for (const [label,access,canView,canEdit] of cases) {
  if (access) await seed(grantPath,access);
  for (const collection of ['medications','healthConcerns','appointments']) {
    const id = {medications:'medication',healthConcerns:'concern',appointments:'appointment'}[collection];
    assert.equal((await request('caregiver','GET',`users/patient/${collection}/${id}`)).status,canView ? 200 : 403,`${label} read ${collection}`);
    assert.equal((await request('caregiver','GET',`users/patient/${collection}`)).status,canView ? 200 : 403,`${label} list ${collection}`);
  }
  assert.equal((await commit('caregiver',med,{dose:'2 tablets',updatedById:'caregiver'},'updatedAt',true)).status,canEdit ? 200 : 403,`${label} medication edit`);
  assert.equal((await request('caregiver','PATCH','users/patient/healthConcerns/concern',{...concern,title:label})).status,canEdit ? 200 : 403,`${label} concern edit`);
  assert.equal((await request('caregiver','PATCH',`users/patient/healthConcerns/new-${label}`,concern)).status,canEdit ? 200 : 403,`${label} concern create`);
  await seed('users/patient/healthConcerns/delete-test',concern);
  assert.equal((await request('caregiver','DELETE','users/patient/healthConcerns/delete-test')).status,canEdit ? 200 : 403,`${label} concern delete`);
  assert.equal((await request('caregiver','DELETE',med)).status,403,`${label} cannot delete medication`);
  assert.equal((await request('caregiver','PATCH','users/patient/medications/new',medication)).status,403,`${label} cannot create medication`);
  assert.equal((await request('caregiver','PATCH','users/patient/appointments/appointment',appointment)).status,403,`${label} cannot edit appointment`);
  for (const path of ['users/patient/careTasks/task','users/patient/doseRecords/dose','users/patient']) {
    assert.equal((await request('caregiver','GET',path)).status,403,`${label} excludes ${path}`);
  }
  assert.equal((await commit('caregiver',refillPath,refill('caregiver'),'requestedAt')).status,canEdit ? 200 : 403,`${label} refill create`);
  await seed(refillPath,{...refill('patient'),requestedAt:1});
  assert.equal((await request('caregiver','GET',refillPath)).status,canView ? 200 : 403,`${label} refill read`);
  assert.equal((await request('caregiver','DELETE',refillPath)).status,403,`${label} cannot delete refill`);
  assert.equal((await commit('caregiver',refillPath,refill('caregiver'),'requestedAt')).status,403,`${label} cannot replace refill`);
  assert.equal((await fetch(`${base}/${refillPath}`,{method:'DELETE',headers:{Authorization:'Bearer owner'}})).status,200);
}
await seed(grantPath,{...grant,permissions:['VIEW','EDIT']});
assert.equal((await commit('caregiver',med,{dose:'3 tablets',updatedById:'caregiver'},null,true)).status,403,'caregiver edit requires server timestamp');
assert.equal((await commit('caregiver',med,{dose:'3 tablets',updatedById:'patient'},'updatedAt',true)).status,403,'caregiver cannot forge actor');
assert.equal((await request('caregiver','PATCH','users/patient/healthConcerns/concern',{...concern,patientId:'caregiver'})).status,403,'caregiver cannot transfer concern ownership');
assert.equal((await request('caregiver','DELETE','users/patient/appointments/appointment')).status,403,'edit excludes appointment removal');
assert.equal((await request('caregiver','PATCH','users/patient/appointments/new',appointment)).status,403,'edit excludes appointment creation');
for (const actor of ['stranger',null]) {
  assert.equal((await request(actor,'GET',med)).status,403,`${actor} cannot read`);
  assert.equal((await commit(actor,med,{dose:'3 tablets',updatedById:'stranger'},'updatedAt',true)).status,403,`${actor} cannot write`);
  assert.equal((await commit(actor,refillPath,refill('stranger'),'requestedAt')).status,403,`${actor} cannot request refill`);
}
for (const patch of [{patientId:'other'},{medicationId:'other'},{medicationName:'Forged name'},{medicationName:30},{requestedById:'other'},{note:'x'.repeat(1001)},{note:30},{status:'COMPLETED'},{extra:'field'}]) {
  assert.equal((await commit('patient',refillPath,{...refill('patient'),...patch},'requestedAt')).status,403,`invalid refill ${JSON.stringify(patch).slice(0,80)}`);
}
assert.equal((await commit('patient',refillPath,refill('patient'))).status,403,'refill requires server timestamp');
await seed(med,{...medication,active:false});
assert.equal((await commit('patient',refillPath,refill('patient'),'requestedAt')).status,403,'inactive medication cannot be refilled');
await seed(med,{...medication,patientId:'other'});
assert.equal((await commit('patient',refillPath,refill('patient'),'requestedAt')).status,403,'refill requires owned medication');
await seed(med,medication);
assert.equal((await commit('patient','users/patient/refillRequests/missing',{...refill('patient'),medicationId:'missing'},'requestedAt')).status,403,'missing medication cannot be refilled');
assert.equal((await commit('patient',refillPath,refill('patient'),'requestedAt')).status,200,'owner requests refill');
assert.equal((await request('patient','GET',refillPath)).status,200,'owner reads refill');
assert.equal((await commit('patient',refillPath,refill('patient'),'requestedAt')).status,403,'owner cannot overwrite active refill');
assert.equal((await request('patient','DELETE',refillPath)).status,403,'submitted refill cannot be deleted');
for (const status of ['COMPLETED','CANCELLED']) {
  for (const actor of ['patient','caregiver']) {
    await seed(refillPath,{...refill('patient'),status,requestedAt:1});
    assert.equal((await commit(actor,refillPath,{...refill(actor),medicationName:medication.name},'requestedAt')).status,200,'authorized actor can request again with a verified medication name after terminal status');
  }
}
assert.equal((await request('patient','PATCH','caregiverAccess/caregiver/recipients/patient',{patientId:'patient'})).status,200,'active VIEW grant permits lookup');
await seed(grantPath,{...grant,permissions:[]});
assert.equal((await request('patient','PATCH','caregiverAccess/caregiver/recipients/patient',{patientId:'patient'})).status,403,'empty grant cannot publish lookup');
await seed(grantPath,{...grant,revoked:true});
assert.equal((await request('patient','PATCH','caregiverAccess/caregiver/recipients/patient',{patientId:'patient'})).status,403,'revoked grant cannot publish lookup');
assert.equal((await request('patient','DELETE',med)).status,200,'owner removes medication');
for (const [path,data] of [['users/patient/healthConcerns/concern',concern],['users/patient/appointments/appointment',appointment],['users/patient/careTasks/task',task]]) {
  assert.equal((await request('patient','PATCH',path,{...data,title:'Owner edit'})).status,200,`owner updates ${path}`);
  assert.equal((await request('patient','DELETE',path)).status,200,`owner deletes ${path}`);
}
console.log('Caregiver permission matrix, patient isolation, revocation, lookup visibility, and refill rules passed.');
