const state = JSON.parse(localStorage.getItem('wayfareState') || '{}');
const $ = (id) => document.getElementById(id);
const messageForm = $('messageForm');
const messageText = $('messageText');
const acceptUser1 = $('acceptUser1');
const acceptUser2 = $('acceptUser2');

function persist() { localStorage.setItem('wayfareState', JSON.stringify(state)); renderState(); }
function toast(message, error = false) {
  const el = $('toast'); el.querySelector('p').textContent = message;
  el.classList.toggle('error', error); el.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove('show'), 3600);
}
async function api(path, options = {}, acceptedStatuses = []) {
  const response = await fetch(path, { headers: {'Content-Type':'application/json', ...(options.headers || {})}, ...options });
  const text = await response.text(); let body = null;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (acceptedStatuses.includes(response.status)) return null;
  if (!response.ok) { const error=new Error(body?.message || body?.error || `Request failed (${response.status})`); error.status=response.status; throw error; }
  return body;
}
function formObject(form) { return Object.fromEntries(new FormData(form).entries()); }
function initializeDefaults() {
  document.querySelectorAll('[data-dynamic-email]').forEach(input => {
    if (!input.value) input.value = `${input.dataset.dynamicEmail}.${Date.now()}@example.com`;
  });
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
  tomorrow.setSeconds(0,0);
  const local = new Date(tomorrow.getTime() - tomorrow.getTimezoneOffset() * 60000).toISOString().slice(0,16);
  document.querySelector('#ride1Form [name=departureTime]').value ||= local;
  const plusTen = new Date(tomorrow.getTime() + 10 * 60000);
  document.querySelector('#ride2Form [name=departureTime]').value ||= new Date(plusTen.getTime() - plusTen.getTimezoneOffset()*60000).toISOString().slice(0,16);
}
function badge(id, value, empty) { const el=$(id); el.textContent=value ? `${value.slice(0,8)}…` : empty; el.classList.toggle('ready', !!value); }
function renderState() {
  badge('user1Badge', state.user1Id, 'Not created'); badge('user2Badge', state.user2Id, 'Not created');
  badge('ride1Badge', state.ride1Id, 'Waiting'); badge('ride2Badge', state.ride2Id, 'Waiting');
  if (state.match) renderMatch(state.match);
  if (state.chatId) unlockChat();
}
async function checkHealth() {
  try { await api('/actuator/health'); $('apiStatus').textContent='API online'; document.querySelector('.status-pill').className='status-pill online'; }
  catch { $('apiStatus').textContent='API offline'; document.querySelector('.status-pill').className='status-pill offline'; }
}
function bindCreateUser(formId, key, label) {
  $(formId).addEventListener('submit', async event => {
    event.preventDefault(); const button=event.submitter; button.disabled=true;
    try {
      const request=formObject(event.currentTarget);
      let user=await api(`/api/users?email=${encodeURIComponent(request.email)}`, {}, [404]);
      if (user) toast(`${label} already exists — account reused`);
      else { user=await api('/api/users',{method:'POST',body:JSON.stringify(request)}); toast(`${label} created`); }
      state[key]=user.id; persist();
    }
    catch(error){toast(error.message,true)} finally{button.disabled=false}
  });
}
function bindCreateRide(formId, userKey, rideKey, label) {
  $(formId).addEventListener('submit', async event => {
    event.preventDefault(); if(!state[userKey]) return toast(`Create ${label}'s traveler first`,true);
    const data=formObject(event.currentTarget); data.userId=state[userKey]; data.departureTime=new Date(data.departureTime).toISOString();
    const button=event.submitter; button.disabled=true; button.textContent='Resolving route…';
    try { const ride=await api('/api/rides',{method:'POST',body:JSON.stringify(data)}); state[rideKey]=ride.id; persist(); toast(`${label}'s ride created`); if(rideKey==='ride2Id') await loadMatches(); }
    catch(error){toast(error.message,true)} finally{button.disabled=false;button.textContent=rideKey==='ride2Id'?'Create ride B & find match':'Create ride A'}
  });
}
async function loadMatches() {
  if(!state.ride1Id) return toast('Create traveler A\'s ride first',true);
  try { const matches=await api(`/api/rides/${state.ride1Id}/matches`); if(!matches.length){state.match=null;persist();$('matchCard').classList.add('hidden');$('matchEmpty').classList.remove('hidden');return toast('No compatible match found yet',true)} state.match=matches[0];persist();toast('Compatible ride found'); }
  catch(error){toast(error.message,true)}
}
function renderMatch(match) {
  $('matchEmpty').classList.add('hidden'); $('matchCard').classList.remove('hidden');
  const score=Math.round(match.matchScore*100); $('matchScore').textContent=`${score}%`;
  $('scoreCircle').style.strokeDashoffset=327-(327*match.matchScore);
  $('pickupDistance').textContent=`${(match.pickupDistanceMeters/1000).toFixed(1)} km`;
  $('destinationDistance').textContent=match.destinationDistanceMeters<1000?`${Math.round(match.destinationDistanceMeters)} m`:`${(match.destinationDistanceMeters/1000).toFixed(1)} km`;
  $('routeSimilarity').textContent=`${Math.round(match.routeSimilarity*100)}%`;
  $('timeDifference').textContent=`${Math.round(match.departureTimeDifferenceSeconds/60)} min`;
  acceptUser1.textContent=match.rideRequest1AcceptedAt?'Traveler A accepted ✓':'Traveler A accepts';
  acceptUser2.textContent=match.rideRequest2AcceptedAt?'Traveler B accepted ✓':'Traveler B accepts';
}
async function accept(userKey) {
  if(!state.match || !state[userKey]) return toast('A match and traveler are required',true);
  try { const result=await api(`/api/matches/${state.match.id}/accept`,{method:'POST',body:JSON.stringify({userId:state[userKey]})}); if(result.chatId){state.chatId=result.chatId;await loadMessages()} await loadMatches();persist();toast(result.chatId?'Both accepted — chat created':'Acceptance recorded'); }
  catch(error){toast(error.message,true)}
}
function unlockChat() {
  const chatState=$('chatState');
  chatState.classList.add('live'); chatState.innerHTML='<i></i> Private chat is live'; $('chatIdText').textContent=state.chatId;
  messageText.disabled=false; messageForm.querySelector('button').disabled=false;
}
async function loadMessages() {
  if(!state.chatId)return;
  try { const page=await api(`/api/chats/${state.chatId}/messages?page=0&size=50`); renderMessages(page.content||[]); }
  catch(error){toast(error.message,true)}
}
function renderMessages(messages) {
  const box=$('messages'); if(!messages.length)return;
  box.innerHTML=messages.map(message=>`<div class="message ${message.senderId===state.user1Id?'mine':''}"><div><p>${escapeHtml(message.message)}</p><small>${new Date(message.sentAt).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'})}</small></div></div>`).join(''); box.scrollTop=box.scrollHeight;
}
function escapeHtml(value){const div=document.createElement('div');div.textContent=value;return div.innerHTML}

bindCreateUser('user1Form','user1Id','Traveler A'); bindCreateUser('user2Form','user2Id','Traveler B');
bindCreateRide('ride1Form','user1Id','ride1Id','Traveler A'); bindCreateRide('ride2Form','user2Id','ride2Id','Traveler B');
$('refreshMatches').addEventListener('click',loadMatches); acceptUser1.addEventListener('click',()=>accept('user1Id')); acceptUser2.addEventListener('click',()=>accept('user2Id'));
$('newBooking').addEventListener('click',()=>{
  delete state.ride1Id; delete state.ride2Id; delete state.match; delete state.chatId; persist();
  $('matchCard').classList.add('hidden'); $('matchEmpty').classList.remove('hidden');
  const chatState=$('chatState');
  chatState.classList.remove('live'); chatState.innerHTML='<i></i> Chat unlocks after both accept';
  $('chatIdText').textContent='Not created';
  $('messages').innerHTML='<div class="chat-placeholder"><span>✦</span><p>Your conversation will appear here.</p></div>';
  messageText.disabled=true; messageForm.querySelector('button').disabled=true;
  document.querySelector('#ride1Form [name=departureTime]').value='';
  document.querySelector('#ride2Form [name=departureTime]').value='';
  initializeDefaults(); toast('Ready for another booking with the same travelers');
});
messageForm.addEventListener('submit',async event=>{event.preventDefault();const senderId=state[$('messageSender').value],message=messageText.value.trim();if(!message)return;try{await api(`/api/chats/${state.chatId}/messages`,{method:'POST',body:JSON.stringify({senderId,message})});messageText.value='';await loadMessages();toast('Message sent')}catch(error){toast(error.message,true)}});
initializeDefaults();renderState();checkHealth();if(state.chatId)loadMessages();
