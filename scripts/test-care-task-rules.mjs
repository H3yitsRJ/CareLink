// Requires the Firestore emulator on localhost:8180, project demo-carelink-tasks.
import assert from 'node:assert/strict';
const base = 'http://127.0.0.1:8180/v1/projects/demo-carelink-tasks/databases/(default)/documents';
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = uid => `${encode({alg:'none',typ:'JWT'})}.${encode({sub:uid,user_id:uid,
  aud:'demo-carelink-tasks',iss:'https://securetoken.google.com/demo-carelink-tasks',
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
const path = 'users/owner/careTasks/task';
const task = {patientId:'owner',title:'Call clinic',description:'Ask about results',dueDate:'2099-01-01',time:'09:00',completed:false,status:'PENDING',appointmentId:'appointment'};
assert.equal((await request('owner','PATCH',path,task)).status,200,'patient creates appointment-linked task');
assert.equal((await request('owner','GET',path)).status,200,'patient retrieves task');
assert.equal((await request('owner','GET','users/owner/careTasks')).status,200,'patient lists tasks');
assert.equal((await request('owner','PATCH',path,{...task,title:'Edited task'})).status,200,'patient updates task');
assert.equal((await request('owner','PATCH',path,{...task,completed:true,status:'COMPLETED'})).status,200,'patient completes task');
const saved = await (await request('owner','GET',path)).json();
assert.equal(saved.fields.appointmentId.stringValue,'appointment');
assert.equal(saved.fields.completed.booleanValue,true);
assert.equal(saved.fields.status.stringValue,'COMPLETED');
assert.equal((await request('stranger','GET',path)).status,403,'other patient cannot read');
assert.equal((await request('stranger','GET','users/owner/careTasks')).status,403,'other patient cannot list');
assert.equal((await request('stranger','PATCH',path,task)).status,403,'other patient cannot write');
assert.equal((await request('owner','PATCH',path,{...task,patientId:'stranger'})).status,403,'cannot transfer ownership');
assert.equal((await request('stranger','PATCH','users/owner/careTasks/other',task)).status,403,'cannot create for another patient');
assert.equal((await fetch(`${base}/${path}`)).status,403,'unauthenticated read rejected');
assert.equal((await request('owner','DELETE',path)).status,200,'patient can remove test task');
console.log('Care-task Firestore authorization and persistence checks passed.');