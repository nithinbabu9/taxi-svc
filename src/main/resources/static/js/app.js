const $ = (id) => document.getElementById(id);
const sessionKey = 'wayfareSession';
const state = JSON.parse(localStorage.getItem(sessionKey) || '{}');
let authMode = 'login';
let rides = [];
let matches = [];
let chats = [];
let activeChatId = null;
let verificationEmail = null;

function persist() { localStorage.setItem(sessionKey, JSON.stringify(state)); }
function clearSession() { Object.keys(state).forEach(key => delete state[key]); localStorage.removeItem(sessionKey); }
function escapeHtml(value = '') { const element = document.createElement('div'); element.textContent = value; return element.innerHTML; }
function formatDistance(meters) { return meters < 1000 ? `${Math.round(meters)} m` : `${(meters / 1609.344).toFixed(1)} mi`; }
function formatDate(value) { return new Date(value).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }); }
function toast(message, error = false) { const element = $('toast'); element.querySelector('span').textContent = message; element.classList.toggle('error', error); element.classList.add('show'); clearTimeout(toast.timer); toast.timer = setTimeout(() => element.classList.remove('show'), 3800); }
async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...(options.headers || {}) };
  const response = await fetch(path, { ...options, headers });
  const text = await response.text(); let body = null;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (!response.ok) { const error = new Error(body?.message || body?.error || `Request failed (${response.status})`); error.status = response.status; error.fieldErrors = body?.fieldErrors || {}; throw error; }
  return body;
}
function defaultDeparture() { const date = new Date(Date.now() + 24 * 60 * 60 * 1000); date.setMinutes(Math.ceil(date.getMinutes() / 15) * 15, 0, 0); return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16); }
function setAuthMode(mode) {
  authMode = mode; const registering = mode === 'register';
  $('loginTab').classList.toggle('active', !registering); $('registerTab').classList.toggle('active', registering);
  $('registrationFields').classList.toggle('hidden', !registering);
  $('registrationFields').querySelectorAll('input').forEach(input => input.required = registering && input.name !== 'phoneNumber');
  $('authTitle').textContent = registering ? 'Start your journey' : 'Welcome back';
  $('authSubtitle').textContent = registering ? 'Create your account and find better ways to travel.' : 'Log in to continue planning better trips.';
  $('authSubmit').innerHTML = `${registering ? 'Create my account' : 'Log in'} <span>→</span>`;
  $('authFoot').textContent = registering ? 'Already have an account? Switch to Log in.' : 'New here? Create an account in less than a minute.';
}
function showVerification(email) {
  verificationEmail = email;
  $('authForm').classList.add('hidden'); $('authSwitch').classList.add('hidden'); $('verificationForm').classList.remove('hidden');
  $('authTitle').textContent = 'One quick check'; $('authSubtitle').textContent = 'We need to confirm that this Gmail address belongs to you.';
  $('verificationHelp').textContent = `Enter the six-digit code sent to ${email}.`;
  $('authFoot').classList.add('hidden'); $('verificationCode').value = ''; $('verificationCode').focus();
}
function hideVerification() {
  verificationEmail = null;
  $('verificationForm').classList.add('hidden'); $('authForm').classList.remove('hidden'); $('authSwitch').classList.remove('hidden');
  $('authFoot').classList.remove('hidden'); setAuthMode('login');
}
function showPublic() { $('publicView').classList.remove('hidden'); $('appView').classList.add('hidden'); }
function showApp() { $('publicView').classList.add('hidden'); $('appView').classList.remove('hidden'); renderProfile(); refreshDashboard(); }
function renderProfile() {
  const name = state.user?.firstName || 'Traveler'; const fullName = `${state.user?.firstName || ''} ${state.user?.lastName || ''}`.trim() || 'My profile';
  $('profileInitial').textContent = name[0].toUpperCase(); $('profileName').textContent = fullName; $('welcomeName').textContent = `${name.toLowerCase()}.`;
}
function checkHealth() { api('/actuator/health', { headers: {} }).then(() => { $('apiStatus').textContent = 'Connected'; $('apiStatus').className = 'connection online'; }).catch(() => { $('apiStatus').textContent = 'API offline'; $('apiStatus').className = 'connection offline'; }); }
async function refreshDashboard() {
  try {
    [rides, matches, chats] = await Promise.all([api('/api/rides/mine'), api('/api/matches/mine'), api('/api/chats/mine')]);
    renderDashboard();
  } catch (error) {
    if (error.status === 401) { clearSession(); showPublic(); toast('Your session expired. Please log in again.', true); return; }
    toast(error.message, true);
  }
}
function renderDashboard() {
  $('openRideCount').textContent = rides.filter(ride => ride.status === 'OPEN').length;
  $('pendingMatchCount').textContent = matches.filter(match => match.status === 'PENDING').length;
  $('chatCount').textContent = chats.length;
  renderRides(); renderMatches(); renderChats();
}
function renderRides() {
  const container = $('ridesList');
  if (!rides.length) { container.innerHTML = '<div class="empty-card">No rides yet. Add your pickup, destination, and time above to begin.</div>'; return; }
  container.innerHTML = rides.map(ride => `<article class="ride-card"><div class="ride-icon">↗</div><div><strong>${escapeHtml(ride.pickupAddress)} <span aria-hidden="true">→</span> ${escapeHtml(ride.destinationAddress)}</strong><p>${formatDate(ride.departureTime)} · ${ride.routeDistanceMeters ? formatDistance(ride.routeDistanceMeters) : 'Route pending'}</p></div><span class="ride-status ${ride.status.toLowerCase()}">${ride.status}</span>${ride.status === 'OPEN' ? `<button class="small-button reject" type="button" data-cancel-ride="${ride.id}">Cancel</button>` : ''}</article>`).join('');
  container.querySelectorAll('[data-cancel-ride]').forEach(button => button.addEventListener('click', () => cancelRide(button.dataset.cancelRide)));
}
function myRideForMatch(match) { return rides.find(ride => ride.id === match.rideRequest1Id || ride.id === match.rideRequest2Id); }
function hasAccepted(match) { const ownRide = myRideForMatch(match); return ownRide?.id === match.rideRequest1Id ? Boolean(match.rideRequest1AcceptedAt) : Boolean(match.rideRequest2AcceptedAt); }
function renderMatches() {
  const container = $('matchesList');
  if (!matches.length) { container.innerHTML = '<div class="empty-card">No matches yet. When another compatible traveler creates a ride, it will appear here.</div>'; return; }
  container.innerHTML = matches.map(match => { const ownRide = myRideForMatch(match); const accepted = hasAccepted(match); const pending = match.status === 'PENDING'; return `<article class="match-card"><div class="match-score">${Math.round(match.matchScore * 100)}%<small>MATCH</small></div><div><strong>Compatible traveler</strong><p>${ownRide ? `${escapeHtml(ownRide.pickupAddress)} → ${escapeHtml(ownRide.destinationAddress)}` : 'Compatible route'} · ${formatDistance(match.pickupDistanceMeters)} pickup gap · ${Math.round(match.departureTimeDifferenceSeconds / 60)} min apart</p></div><span class="match-status ${match.status.toLowerCase()}">${match.status}</span><div class="match-actions">${pending ? `<button class="small-button" type="button" data-accept-match="${match.id}" ${accepted ? 'disabled' : ''}>${accepted ? 'Awaiting traveler' : 'Accept match'}</button><button class="small-button reject" type="button" data-reject-match="${match.id}">Decline</button>` : ''}</div></article>`; }).join('');
  container.querySelectorAll('[data-accept-match]').forEach(button => button.addEventListener('click', () => decideMatch(button.dataset.acceptMatch, 'accept')));
  container.querySelectorAll('[data-reject-match]').forEach(button => button.addEventListener('click', () => decideMatch(button.dataset.rejectMatch, 'reject')));
}
function renderChats() {
  const container = $('chatsList');
  if (!chats.length) { container.innerHTML = '<div class="empty-card">Your chats will appear after both travelers accept a match.</div>'; return; }
  container.innerHTML = chats.map(chat => `<article class="chat-card" data-open-chat="${chat.id}"><div class="chat-icon">✦</div><div><strong>Ride coordination</strong><p>Private chat for your accepted route match</p></div><span>→</span></article>`).join('');
  container.querySelectorAll('[data-open-chat]').forEach(card => card.addEventListener('click', () => openChat(card.dataset.openChat)));
}
async function createRide(event) {
  event.preventDefault(); const button = $('createRideButton'); const data = Object.fromEntries(new FormData($('rideForm')).entries());
  button.disabled = true; button.innerHTML = 'Resolving your route…';
  try { data.departureTime = new Date(data.departureTime).toISOString(); await api('/api/rides', { method: 'POST', body: JSON.stringify(data) }); $('rideForm').reset(); $('departureTime').value = defaultDeparture(); await refreshDashboard(); toast('Your ride is live. We’re checking compatible routes.'); }
  catch (error) { toast(error.message, true); } finally { button.disabled = false; button.innerHTML = 'Find compatible rides <span>→</span>'; }
}
async function cancelRide(rideId) { if (!window.confirm('Cancel this ride? Pending matches will also be cancelled.')) return; try { await api(`/api/rides/${rideId}`, { method: 'DELETE' }); await refreshDashboard(); toast('Ride cancelled'); } catch (error) { toast(error.message, true); } }
async function decideMatch(matchId, action) { try { const result = await api(`/api/matches/${matchId}/${action}`, { method: 'POST' }); await refreshDashboard(); if (result.chatId) { toast('Both travelers accepted. Your chat is ready.'); await openChat(result.chatId); } else toast(action === 'accept' ? 'Accepted. Waiting for the other traveler.' : 'Match declined'); } catch (error) { toast(error.message, true); } }
async function openChat(chatId) { activeChatId = chatId; $('chatDialog').showModal(); $('messagesList').innerHTML = '<div class="empty-messages">Loading conversation…</div>'; try { const page = await api(`/api/chats/${chatId}/messages?page=0&size=50`); renderMessages(page.content || []); } catch (error) { $('messagesList').innerHTML = '<div class="empty-messages">Unable to load messages.</div>'; toast(error.message, true); } }
function renderMessages(messages) { const container = $('messagesList'); if (!messages.length) { container.innerHTML = '<div class="empty-messages">Start the conversation. Keep it kind and coordinate your ride safely.</div>'; return; } container.innerHTML = messages.map(message => `<div class="message ${message.senderId === state.user.id ? 'mine' : ''}"><div><p>${escapeHtml(message.message)}</p><small>${new Date(message.sentAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</small></div></div>`).join(''); container.scrollTop = container.scrollHeight; }
async function sendMessage(event) { event.preventDefault(); const input = $('messageInput'); const message = input.value.trim(); if (!message || !activeChatId) return; try { await api(`/api/chats/${activeChatId}/messages`, { method: 'POST', body: JSON.stringify({ message }) }); input.value = ''; await openChat(activeChatId); } catch (error) { toast(error.message, true); } }
function validationMessage(error) {
  const entries = Object.entries(error.fieldErrors || {});
  if (!entries.length) return error.message;
  const labels = { firstName: 'First name', lastName: 'Last name', email: 'Email', phoneNumber: 'Phone', password: 'Password' };
  return entries.map(([field, message]) => `${labels[field] || field}: ${message}`).join(' · ');
}
function isGmailAddress(value) { return /^[a-z0-9](?:[a-z0-9._+\-]{0,62}[a-z0-9])?@gmail\.com$/i.test(value); }
async function authenticate(event) {
  event.preventDefault();
  const form = $('authForm');
  if (!form.reportValidity()) return;
  const button = $('authSubmit');
  const request = Object.fromEntries(new FormData(form).entries());
  if (authMode === 'login') ['firstName', 'lastName', 'phoneNumber'].forEach(key => delete request[key]);
  else {
    request.firstName = request.firstName.trim();
    request.lastName = request.lastName.trim();
    request.phoneNumber = request.phoneNumber.trim();
  }
  request.email = request.email.trim();
  if (!isGmailAddress(request.email)) {
    $('emailInput').focus();
    toast('Enter a valid @gmail.com address.', true);
    return;
  }
  button.disabled = true;
  try {
    const result = await api(`/api/auth/${authMode}`, { method: 'POST', body: JSON.stringify(request) });
    if (authMode === 'register') { showVerification(result.email); toast('Verification code sent to your Gmail.'); }
    else { state.token = result.accessToken; state.user = result.user; persist(); showApp(); toast('Welcome back.'); }
  } catch (error) {
    if (authMode === 'login' && error.status === 403) showVerification(request.email);
    toast(validationMessage(error), true);
  }
  finally { button.disabled = false; }
}
async function verifyEmail(event) {
  event.preventDefault(); const code = $('verificationCode').value.trim();
  if (!verificationEmail || !/^[0-9]{6}$/.test(code)) { toast('Enter the six-digit code from your email.', true); return; }
  const button = $('verifyButton'); button.disabled = true;
  try { const result = await api('/api/auth/verify-email', { method: 'POST', body: JSON.stringify({ email: verificationEmail, code }) }); state.token = result.accessToken; state.user = result.user; persist(); showApp(); toast('Gmail verified. Welcome to Wayfare.'); }
  catch (error) { toast(validationMessage(error), true); } finally { button.disabled = false; }
}
async function resendVerification() {
  if (!verificationEmail) return;
  const button = $('resendButton'); button.disabled = true;
  try { await api('/api/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email: verificationEmail }) }); toast('A new verification code has been sent.'); }
  catch (error) { toast(validationMessage(error), true); } finally { button.disabled = false; }
}

$('loginTab').addEventListener('click', () => setAuthMode('login')); $('registerTab').addEventListener('click', () => setAuthMode('register'));
$('passwordToggle').addEventListener('click', () => { const input = $('passwordInput'); const showing = input.type === 'text'; input.type = showing ? 'password' : 'text'; $('passwordToggle').setAttribute('aria-label', showing ? 'Show password' : 'Hide password'); });
$('authForm').addEventListener('submit', authenticate); $('verificationForm').addEventListener('submit', verifyEmail); $('resendButton').addEventListener('click', resendVerification); $('backToAuthButton').addEventListener('click', hideVerification); $('rideForm').addEventListener('submit', createRide); $('refreshRides').addEventListener('click', refreshDashboard); $('refreshMatches').addEventListener('click', refreshDashboard); $('refreshChats').addEventListener('click', refreshDashboard); $('messageForm').addEventListener('submit', sendMessage); $('closeChat').addEventListener('click', () => $('chatDialog').close()); $('logoutButton').addEventListener('click', () => { clearSession(); showPublic(); setAuthMode('login'); toast('You have been logged out.'); });
$('departureTime').value = defaultDeparture(); checkHealth(); if (state.token && state.user) showApp(); else showPublic();
