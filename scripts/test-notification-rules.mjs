import assert from 'node:assert/strict';
const project = 'demo-carelink-notifications';
const host = 'http://127.0.0.1:8180';
const base = `${host}/v1/projects/${project}/databases/(default)/documents`;
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = uid => `${encode({alg:'none',typ:'JWT'})}.${encode({sub:uid,user_id:uid,aud:project,iss:`https://securetoken.google.com/${project}`,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'custom'}})}.`;
const headers = uid => ({'Content-Type':'application/json',...(uid ? {Authorization:`Bearer ${uid === 'owner' ? 'owner' : token(uid)}`} : {})});
const fields = data => Object.fromEntries(Object.entries(data).map(([key,value]) => [key, typeof value === 'boolean' ? {booleanValue:value} : {stringValue:value}]));
const path = 'users/patient/notifications/one';
async function get(uid, suffix = path) { return fetch(`${base}/${suffix}`, {headers:headers(uid)}); }
async function patch(uid, data, mask = true) {
  return fetch(`${base}/${path}${mask ? '?' + Object.keys(data).map(key => `updateMask.fieldPaths=${key}`).join('&') : ''}`, {method:'PATCH',headers:headers(uid),body:JSON.stringify({fields:fields(data)})});
}
assert.equal((await patch('owner', {title:'Care update',message:'Review care activity',isRead:false}, false)).status,200);
assert.equal((await get('patient')).status,200);
assert.equal((await get('patient','users/patient/notifications')).status,200);
assert.equal((await get('other')).status,403);
assert.equal((await get(null)).status,403);
assert.equal((await patch('other',{isRead:true})).status,403);
assert.equal((await patch('patient',{title:'Changed'})).status,403);
assert.equal((await patch('patient',{isRead:true,title:'Changed'})).status,403);
assert.equal((await patch('patient',{isRead:'true'})).status,403);
assert.equal((await patch('patient',{isRead:true})).status,200);
assert.equal((await patch('patient',{isRead:false})).status,403);
assert.equal((await fetch(`${base}/users/patient/notifications/new`,{method:'PATCH',headers:headers('patient'),body:JSON.stringify({fields:fields({title:'Forged',isRead:false})})})).status,403);
assert.equal((await fetch(`${base}/${path}`,{method:'DELETE',headers:headers('patient')})).status,403);
console.log('Notification rules passed: owner read/mark-read, cross-account and content mutations denied.');
