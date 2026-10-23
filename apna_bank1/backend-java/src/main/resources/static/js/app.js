let allAssets = [];
let activeCategory = 'ALL';
let assetChart = null;
let currentTradeSymbol = null;
let currentTradePrice = 0;
let currentTradeAction = 'BUY';

function formatINR(val) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(val);
}

// 1. Language Engine
async function loadLanguage(lang) {
  try {
    const res = await fetch(`/i18n/${lang}.json`);
    const dict = await res.json();
    document.getElementById('global-lang-select').value = lang;
  } catch (e) { console.error(e); }
}

async function changeLanguage(lang) {
  await loadLanguage(lang);
  await fetch('/api/settings/language', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ language: lang })
  });
}

// 2. Authentication
function switchAuthTab(tab) {
  document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
  document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
  document.getElementById('tab-login-btn').className = tab === 'login' ? 'active' : '';
  document.getElementById('tab-register-btn').className = tab === 'register' ? 'active' : '';
}

async function handleRegister(e) {
  e.preventDefault();
  const res = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fullName: document.getElementById('reg-fullname').value,
      username: document.getElementById('reg-username').value,
      password: document.getElementById('reg-password').value,
      preferredLanguage: document.getElementById('reg-language').value
    })
  });
  if (res.ok) initApp();
  else {
    const data = await res.json();
    document.getElementById('auth-error').innerText = data.error || 'Registration failed';
  }
}

async function handleLogin(e) {
  e.preventDefault();
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: document.getElementById('login-username').value,
      password: document.getElementById('login-password').value
    })
  });
  if (res.ok) initApp();
  else document.getElementById('auth-error').innerText = 'Invalid credentials';
}

async function handleLogout() {
  await fetch('/api/auth/logout', { method: 'POST' });
  document.getElementById('app-container').style.display = 'none';
  document.getElementById('auth-container').style.display = 'block';
}

// 3. Navigation
function navigate(viewName) {
  document.querySelectorAll('.view-section').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));
  document.getElementById(`view-${viewName}`).classList.add('active');
  event.target.classList.add('active');

  if (viewName === 'market') renderMarketTable();
  if (viewName === 'portfolio') refreshPortfolio();
  if (viewName === 'banking') refreshTransactions();
  if (viewName === 'loans') refreshLoans();
  if (viewName === 'messages') refreshMessages();
}

// 4. Data Refreshers
async function refreshDashboard() {
  const balRes = await fetch('/api/banking/balance');
  if (!balRes.ok) return;
  const bal = await balRes.json();
  document.getElementById('card-cash-balance').innerText = formatINR(bal.balance);

  const nwRes = await fetch('/api/networth');
  const nw = await nwRes.json();
  document.getElementById('card-networth').innerText = formatINR(nw.net_worth);

  const totalInv = (nw.stocks_value || 0) + (nw.commodities_value || 0) + (nw.etf_value || 0) + (nw.real_estate_value || 0) + (nw.crypto_value || 0);
  document.getElementById('card-investment-val').innerText = formatINR(totalInv);

  updateChart(nw.cash_balance, nw.stocks_value, nw.commodities_value, nw.etf_value, nw.real_estate_value, nw.crypto_value);
  await fetchMarketAssets();
  renderMiniTicker();
}

function updateChart(cash, stocks, commodities, etfs, realEstate, crypto) {
  const ctx = document.getElementById('assetChart').getContext('2d');
  if (assetChart) assetChart.destroy();
  assetChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Cash', 'Stocks', 'Commodities', 'ETFs', 'Real Estate', 'Crypto'],
      datasets: [{
        data: [cash, stocks, commodities, etfs, realEstate, crypto],
        backgroundColor: ['#D4AF37', '#0A84FF', '#E5A93C', '#997D29', '#8B5CF6', '#10B981'],
        borderWidth: 1,
        borderColor: '#0F1117'
      }]
    },
    options: {
      responsive: true,
      plugins: { legend: { position: 'bottom', labels: { color: '#F4F6F9', font: { size: 11 } } } }
    }
  });
}

async function fetchMarketAssets() {
  const res = await fetch('/api/market');
  allAssets = await res.json();
}

function renderMiniTicker() {
  const tbody = document.getElementById('ticker-tbody');
  tbody.innerHTML = '';
  allAssets.slice(0, 6).forEach(a => {
    tbody.innerHTML += `
      <tr>
        <td><strong>${a.symbol}</strong><br><small class="text-muted">${a.name}</small></td>
        <td class="gold-text">${formatINR(a.price)}</td>
        <td><span class="badge badge-gold">${a.category}</span></td>
      </tr>
    `;
  });
}

function setAssetCategory(cat, btn) {
  activeCategory = cat;
  document.querySelectorAll('.cat-pill').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  renderMarketTable();
}

function filterAssets() {
  renderMarketTable();
}

function renderMarketTable() {
  const query = (document.getElementById('market-search').value || '').toLowerCase();
  const tbody = document.getElementById('full-market-tbody');
  tbody.innerHTML = '';

  const filtered = allAssets.filter(a => {
    const matchCat = (activeCategory === 'ALL' || a.category === activeCategory);
    const matchQuery = a.symbol.toLowerCase().includes(query) || a.name.toLowerCase().includes(query);
    return matchCat && matchQuery;
  });

  filtered.forEach(a => {
    tbody.innerHTML += `
      <tr>
        <td><strong>${a.symbol}</strong></td>
        <td>${a.name}</td>
        <td><span class="badge badge-gold">${a.category}</span></td>
        <td class="gold-text" style="font-weight:700;">${formatINR(a.price)}</td>
        <td>0.0001 unit</td>
        <td>
          <button class="btn btn-gold btn-sm" onclick="openTradeModal('${a.symbol}', ${a.price}, 'BUY')">Trade Fractional</button>
        </td>
      </tr>
    `;
  });
}

// 5. Portfolio & P&L
async function refreshPortfolio() {
  const res = await fetch('/api/investments/portfolio');
  const holdings = await res.json();
  const tbody = document.getElementById('portfolio-tbody');
  tbody.innerHTML = '';

  if (holdings.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-muted" style="text-align:center;">No fractional assets in vault yet.</td></tr>`;
    return;
  }

  holdings.forEach(h => {
    const currentPrice = h.asset.price;
    const currentVal = h.quantity * currentPrice;
    const costBasis = h.quantity * h.averagePurchasePrice;
    const pnl = currentVal - costBasis;
    const pnlPct = costBasis > 0 ? (pnl / costBasis) * 100 : 0;
    const pnlClass = pnl >= 0 ? 'green-text' : 'red-text';
    const sign = pnl >= 0 ? '+' : '';

    tbody.innerHTML += `
      <tr>
        <td><strong>${h.asset.symbol}</strong><br><small class="text-muted">${h.asset.name}</small></td>
        <td>${parseFloat(h.quantity).toFixed(4)}</td>
        <td>${formatINR(h.averagePurchasePrice)}</td>
        <td class="gold-text">${formatINR(currentPrice)}</td>
        <td>${formatINR(currentVal)}</td>
        <td class="${pnlClass}">${sign}${formatINR(pnl)} (${sign}${pnlPct.toFixed(2)}%)</td>
        <td>
          <button class="btn btn-sell btn-sm" onclick="openTradeModal('${h.asset.symbol}', ${currentPrice}, 'SELL', ${h.quantity})">Sell Units</button>
        </td>
      </tr>
    `;
  });
}

// 6. Modal Trading Logic
function openTradeModal(symbol, price, action, maxQty = 0) {
  currentTradeSymbol = symbol;
  currentTradePrice = price;
  currentTradeAction = action;

  document.getElementById('modal-title').innerText = `${action} Fractional ${symbol}`;
  document.getElementById('modal-price').innerText = formatINR(price);
  document.getElementById('trade-quantity').value = '';
  document.getElementById('trade-amount-inr').value = '';
  document.getElementById('modal-est-cost').innerText = '₹0.00';
  document.getElementById('modal-confirm-btn').innerText = action === 'BUY' ? 'Execute Buy Order' : 'Execute Sell Order';
  document.getElementById('trade-modal').style.display = 'flex';
}

function closeTradeModal() {
  document.getElementById('trade-modal').style.display = 'none';
}

function calcTradeCost(from) {
  const qtyInput = document.getElementById('trade-quantity');
  const amtInput = document.getElementById('trade-amount-inr');

  if (from === 'qty') {
    const qty = parseFloat(qtyInput.value) || 0;
    const total = qty * currentTradePrice;
    amtInput.value = total > 0 ? total.toFixed(2) : '';
    document.getElementById('modal-est-cost').innerText = formatINR(total);
  } else {
    const amt = parseFloat(amtInput.value) || 0;
    const qty = currentTradePrice > 0 ? (amt / currentTradePrice) : 0;
    qtyInput.value = qty > 0 ? qty.toFixed(6) : '';
    document.getElementById('modal-est-cost').innerText = formatINR(amt);
  }
}

async function executeTrade() {
  const qty = parseFloat(document.getElementById('trade-quantity').value);
  if (!qty || qty < 0.0001) {
    alert('Minimum fractional trade is 0.0001 units');
    return;
  }

  const endpoint = currentTradeAction === 'BUY' ? '/api/investments/buy' : '/api/investments/sell';
  const res = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol: currentTradeSymbol, quantity: qty })
  });

  const data = await res.json();
  if (res.ok) {
    alert(data.message || 'Trade Executed');
    closeTradeModal();
    refreshDashboard();
    if (document.getElementById('view-portfolio').classList.contains('active')) refreshPortfolio();
  } else {
    alert('Error: ' + (data.message || 'Trade failed'));
  }
}

// 7. Banking
async function handleTransfer(e) {
  e.preventDefault();
  const res = await fetch('/api/banking/transfer', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      recipientUsername: document.getElementById('transfer-recipient').value,
      amount: parseFloat(document.getElementById('transfer-amount').value),
      note: document.getElementById('transfer-note').value
    })
  });
  if (res.ok) { alert('Transfer successful! 6% Royal Tax deducted.'); refreshDashboard(); refreshTransactions(); }
  else { const d = await res.json(); alert(d.message || 'Transfer failed'); }
}

async function handleDeposit(e) {
  e.preventDefault();
  await fetch('/api/banking/deposit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: parseFloat(document.getElementById('deposit-amount').value), note: 'Capital Injection' })
  });
  alert('Funds Added');
  refreshDashboard();
  refreshTransactions();
}

async function handleWithdraw(e) {
  e.preventDefault();
  const res = await fetch('/api/banking/withdraw', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: parseFloat(document.getElementById('withdraw-amount').value), note: 'Withdrawal' })
  });
  if (res.ok) { alert('Withdrawal executed'); refreshDashboard(); refreshTransactions(); }
  else alert('Insufficient funds');
}

async function refreshTransactions() {
  const res = await fetch('/api/banking/transactions');
  const txs = await res.json();
  const tbody = document.getElementById('tx-tbody');
  tbody.innerHTML = '';
  txs.forEach(t => {
    tbody.innerHTML += `
      <tr>
        <td><strong>${t.transactionType}</strong></td>
        <td class="gold-text">${formatINR(t.amount)}</td>
        <td>${t.taxAmount ? formatINR(t.taxAmount) : '₹0.00'}</td>
        <td>${t.description}</td>
        <td>${new Date(t.createdAt).toLocaleTimeString()}</td>
      </tr>
    `;
  });
}

// 8. Loans
async function handleApplyLoan(e) {
  e.preventDefault();
  const amt = parseFloat(document.getElementById('loan-amount').value);
  const res = await fetch('/api/loans/apply', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: amt })
  });
  if (res.ok) { alert('Credit Line Approved & Credited'); refreshLoans(); refreshDashboard(); }
  else alert('Credit request declined');
}

async function refreshLoans() {
  const res = await fetch('/api/loans');
  const data = await res.json();
  const tbody = document.getElementById('loans-tbody');
  tbody.innerHTML = '';
  data.loans.forEach(l => {
    tbody.innerHTML += `
      <tr>
        <td class="gold-text">${formatINR(l.principal)}</td>
        <td>${l.interestRate}%</td>
        <td><span class="badge ${l.status === 'ACTIVE' ? 'badge-gold' : ''}">${l.status}</span></td>
        <td>
          ${l.status === 'ACTIVE' ? `<button class="btn btn-gold btn-sm" onclick="repayLoan('${l.id}', ${l.principal})">Repay</button>` : 'Discharged'}
        </td>
      </tr>
    `;
  });
}

async function repayLoan(id, amt) {
  await fetch(`/api/loans/${id}/repay`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: amt })
  });
  alert('Repayment processed');
  refreshLoans();
  refreshDashboard();
}

// 9. Dispatch Messaging
async function handleSendMessage(e) {
  e.preventDefault();
  const res = await fetch('/api/messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      recipientUsername: document.getElementById('msg-recipient').value,
      body: document.getElementById('msg-body').value
    })
  });
  if (res.ok) { alert('Dispatch Transmitted'); document.getElementById('msg-body').value = ''; refreshMessages(); }
  else alert('User handle not found');
}

async function refreshMessages() {
  const msgs = await (await fetch('/api/messages/inbox')).json();
  const list = document.getElementById('messages-list');
  list.innerHTML = '';
  msgs.forEach(m => {
    list.innerHTML += `<div class="msg-card"><small class="gold-text">From: ${m.sender.username} (${new Date(m.createdAt).toLocaleTimeString()})</small><p style="margin-top:4px;">${m.body}</p></div>`;
  });
}

// 10. Bootstrapper
async function initApp() {
  const authRes = await fetch('/api/auth/me');
  if (authRes.ok) {
    const user = await authRes.json();
    document.getElementById('auth-container').style.display = 'none';
    document.getElementById('app-container').style.display = 'block';
    document.getElementById('user-display-name').innerText = `👑 ${user.fullName}`;
    await loadLanguage(user.language || 'en');
    refreshDashboard();
    setInterval(refreshDashboard, 8000); // 8-second live market tick
  } else {
    document.getElementById('auth-container').style.display = 'block';
    document.getElementById('app-container').style.display = 'none';
    loadLanguage('en');
  }
}

window.onload = initApp;
