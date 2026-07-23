package com.napxstream.admin

/** Yönetim panelinin tek-sayfa arayüzü. Sunucu tarafında gömülü olarak servis edilir. */
object AdminPanelHtml {

    val PAGE = """
<!DOCTYPE html>
<html lang="tr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Napxstream — Yönetim Paneli</title>
<style>
  :root{
    --bg:#0E1116; --surface:#171B22; --surface2:#232833;
    --primary:#8B5CF6; --accent:#22D3EE; --danger:#EC4899;
    --text:#FFFFFF; --text2:#A0A6B1; --divider:#2A2F3A;
  }
  *{box-sizing:border-box;}
  body{margin:0;background:var(--bg);color:var(--text);font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;padding:20px;}
  h1{font-size:20px;margin:0 0 4px;}
  h2{font-size:15px;margin:0 0 14px;color:var(--text);border-bottom:1px solid var(--divider);padding-bottom:8px;}
  .sub{color:var(--text2);font-size:13px;margin:0 0 24px;}
  .card{background:var(--surface);border-radius:12px;padding:18px;margin-bottom:20px;max-width:760px;}
  .row{display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin-bottom:10px;}
  label{font-size:12px;color:var(--text2);display:block;margin-bottom:4px;}
  input[type=text],input[type=password],input[type=number],select{
    background:var(--surface2);border:1px solid var(--divider);color:var(--text);
    padding:9px 10px;border-radius:8px;font-size:13px;width:100%;
  }
  button{
    background:var(--primary);color:#fff;border:none;border-radius:8px;
    padding:9px 16px;font-size:13px;font-weight:600;cursor:pointer;
  }
  button.secondary{background:var(--surface2);}
  button.danger{background:var(--danger);}
  button:disabled{opacity:.5;cursor:not-allowed;}
  table{width:100%;border-collapse:collapse;font-size:13px;}
  th{text-align:left;color:var(--text2);font-weight:600;padding:8px;border-bottom:1px solid var(--divider);}
  td{padding:8px;border-bottom:1px solid var(--divider);}
  .badge{background:var(--primary);color:#fff;font-size:10px;padding:2px 8px;border-radius:10px;}
  .badge.inactive{background:var(--surface2);color:var(--text2);}
  .tabs{display:flex;gap:8px;margin-bottom:14px;}
  .tab{padding:7px 14px;border-radius:16px;background:var(--surface2);color:var(--text2);font-size:12px;cursor:pointer;}
  .tab.active{background:var(--primary);color:#fff;}
  .field-group{display:none;}
  .field-group.active{display:block;}
  .toast{position:fixed;bottom:20px;right:20px;background:var(--surface2);padding:12px 18px;border-radius:8px;font-size:13px;display:none;}
  .checkbox-row{display:flex;align-items:center;gap:8px;padding:6px 0;font-size:13px;}
  .grid2{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
  @media(max-width:600px){.grid2{grid-template-columns:1fr;}}
</style>
</head>
<body>

<h1>📡 Napxstream — Yönetim Paneli</h1>
<p class="sub" id="statusLine">Yükleniyor…</p>

<div class="card">
  <h2>Hesaplar</h2>
  <table id="accountsTable"><thead><tr><th>Ad</th><th>Tür</th><th>Bilgi</th><th>Durum</th><th></th></tr></thead><tbody></tbody></table>

  <div style="margin-top:16px;">
    <div class="tabs">
      <div class="tab active" data-type="xtream" onclick="selectType('xtream')">Xtream Codes</div>
      <div class="tab" data-type="m3u" onclick="selectType('m3u')">M3U Playlist</div>
    </div>
    <div class="row"><div style="flex:1"><label>Hesap Adı</label><input type="text" id="newLabel" placeholder="ör. Ev Aboneliği"></div></div>

    <div class="field-group active" id="xtreamFields">
      <div class="grid2">
        <div><label>Sunucu Adresi</label><input type="text" id="newHost" placeholder="http://sunucu.com"></div>
        <div><label>Port</label><input type="text" id="newPort" placeholder="8080"></div>
        <div><label>Kullanıcı Adı</label><input type="text" id="newUsername"></div>
        <div><label>Şifre</label><input type="password" id="newPassword"></div>
      </div>
      <div class="checkbox-row"><input type="checkbox" id="newHttps"><label style="margin:0">HTTPS kullan</label></div>
    </div>

    <div class="field-group" id="m3uFields">
      <div class="grid2">
        <div><label>M3U Playlist URL</label><input type="text" id="newM3uUrl" placeholder="http://.../playlist.m3u"></div>
        <div><label>EPG URL (opsiyonel)</label><input type="text" id="newM3uEpgUrl"></div>
      </div>
    </div>

    <div class="row" style="margin-top:12px;"><button onclick="addAccount()">+ Hesap Ekle</button></div>
  </div>
</div>

<div class="card">
  <h2>🔒 Ebeveyn Kilidi</h2>
  <div class="checkbox-row"><input type="checkbox" id="parentalEnabled" onchange="saveParental()"><label style="margin:0">Ebeveyn kilidini etkinleştir (yetişkin kategoriler varsayılan gizlenir)</label></div>
  <div class="row" style="margin-top:10px;">
    <div style="flex:1"><label>Yeni PIN belirle</label><input type="password" id="parentalPin" placeholder="4 haneli PIN" maxlength="8"></div>
    <button class="secondary" style="margin-top:18px" onclick="savePin()">PIN Kaydet</button>
  </div>
  <p class="sub" id="parentalStatus" style="margin:8px 0 0;"></p>
</div>

<div class="card">
  <h2>🚫 Kanal / Kategori Engelleme</h2>
  <p class="sub" style="margin-top:-6px;">Seçilen kategoriler aktif Xtream hesabında tüm cihazlarda gizlenir.</p>
  <div id="categoryList" style="max-height:260px;overflow-y:auto;"></div>
</div>

<div class="toast" id="toast"></div>

<script>
let selectedType = 'xtream';
function selectType(t){
  selectedType = t;
  document.querySelectorAll('.tab').forEach(el => el.classList.toggle('active', el.dataset.type === t));
  document.getElementById('xtreamFields').classList.toggle('active', t === 'xtream');
  document.getElementById('m3uFields').classList.toggle('active', t === 'm3u');
}

function showToast(msg){
  const t = document.getElementById('toast');
  t.textContent = msg; t.style.display = 'block';
  setTimeout(() => t.style.display = 'none', 2500);
}

async function api(path, opts){
  const res = await fetch(path, Object.assign({headers:{'Content-Type':'application/json'}}, opts || {}));
  if(!res.ok){ const e = await res.json().catch(()=>({error:'Hata'})); throw new Error(e.error || 'Hata'); }
  return res.json();
}

async function loadStatus(){
  const s = await api('/api/status');
  document.getElementById('statusLine').textContent =
    `Aktif hesap: ${'$'}{s.activeAccountLabel} · Kayıtlı hesap sayısı: ${'$'}{s.accountCount}`;
}

async function loadAccounts(){
  const data = await api('/api/accounts');
  const tbody = document.querySelector('#accountsTable tbody');
  tbody.innerHTML = '';
  data.accounts.forEach(a => {
    const info = a.type === 'xtream' ? `${'$'}{a.host}:${'$'}{a.port} (${'$'}{a.username})` : a.m3uUrl;
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${'$'}{a.label}</td>
      <td>${'$'}{a.type === 'xtream' ? 'Xtream' : 'M3U'}</td>
      <td style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${'$'}{info}</td>
      <td>${'$'}{a.isActive ? '<span class="badge">Aktif</span>' : '<span class="badge inactive">Pasif</span>'}</td>
      <td style="white-space:nowrap;">
        ${'$'}{!a.isActive ? `<button class="secondary" onclick="activateAccount(${'$'}{a.id})">Aktif Yap</button>` : ''}
        <button class="danger" onclick="deleteAccount(${'$'}{a.id})">Sil</button>
      </td>`;
    tbody.appendChild(tr);
  });
}

async function addAccount(){
  const label = document.getElementById('newLabel').value || 'Hesap';
  let body;
  if(selectedType === 'xtream'){
    body = {
      type: 'xtream', label,
      host: document.getElementById('newHost').value,
      port: document.getElementById('newPort').value,
      username: document.getElementById('newUsername').value,
      password: document.getElementById('newPassword').value,
      useHttps: document.getElementById('newHttps').checked
    };
  } else {
    body = {
      type: 'm3u', label,
      m3uUrl: document.getElementById('newM3uUrl').value,
      m3uEpgUrl: document.getElementById('newM3uEpgUrl').value
    };
  }
  try{
    await api('/api/accounts', {method:'POST', body: JSON.stringify(body)});
    showToast('Hesap eklendi');
    await loadAccounts(); await loadStatus();
  }catch(e){ showToast('Hata: ' + e.message); }
}

async function activateAccount(id){
  await api(`/api/accounts/${'$'}{id}/activate`, {method:'POST'});
  showToast('Hesap değiştirildi');
  await loadAccounts(); await loadStatus(); await loadCategories();
}

async function deleteAccount(id){
  if(!confirm('Bu hesap silinsin mi?')) return;
  await api(`/api/accounts/${'$'}{id}`, {method:'DELETE'});
  showToast('Hesap silindi');
  await loadAccounts(); await loadStatus();
}

async function loadParental(){
  const p = await api('/api/parental');
  document.getElementById('parentalEnabled').checked = p.enabled;
  document.getElementById('parentalStatus').textContent = p.hasPin ? 'PIN tanımlı.' : 'Henüz PIN belirlenmedi.';
}

async function saveParental(){
  const enabled = document.getElementById('parentalEnabled').checked;
  await api('/api/parental', {method:'POST', body: JSON.stringify({enabled})});
  showToast('Kaydedildi');
}

async function savePin(){
  const pin = document.getElementById('parentalPin').value;
  if(!pin){ showToast('PIN girin'); return; }
  await api('/api/parental', {method:'POST', body: JSON.stringify({pin})});
  document.getElementById('parentalPin').value = '';
  showToast('PIN kaydedildi');
  await loadParental();
}

let blockedSet = new Set();
async function loadCategories(){
  const [cats, blocked] = await Promise.all([api('/api/categories'), api('/api/blocked')]);
  blockedSet = new Set(blocked.blocked.filter(b => b.targetType === 'category').map(b => b.targetId));
  const container = document.getElementById('categoryList');
  container.innerHTML = '';
  if(cats.categories.length === 0){
    container.innerHTML = '<p class="sub">Kategori bulunamadı (Xtream hesabı gerekli).</p>';
    return;
  }
  cats.categories.forEach(c => {
    const row = document.createElement('div');
    row.className = 'checkbox-row';
    const checked = blockedSet.has(c.id) ? 'checked' : '';
    row.innerHTML = `<input type="checkbox" ${'$'}{checked} onchange="toggleBlock('${'$'}{c.id}', '${'$'}{c.name.replace(/'/g, "\\'")}' , this.checked)"><label style="margin:0">${'$'}{c.name}</label>`;
    container.appendChild(row);
  });
}

async function toggleBlock(id, name, blocked){
  if(blocked){
    await api('/api/blocked', {method:'POST', body: JSON.stringify({targetType:'category', targetId:id, label:name})});
  } else {
    await api('/api/blocked', {method:'DELETE', body: JSON.stringify({targetType:'category', targetId:id})});
  }
  showToast('Güncellendi');
}

(async function init(){
  try{
    await loadStatus();
    await loadAccounts();
    await loadParental();
    await loadCategories();
  }catch(e){ showToast('Yükleme hatası: ' + e.message); }
})();
</script>
</body>
</html>
""".trimIndent()
}
