/* ============================================================
   THÚ CƯNG ẢO — game nuôi thú ảo cho Android TV (D-pad)
   ============================================================ */
'use strict';

/* ---------------- Khớp sân khấu với màn hình ----------------
   Game vẽ trong #stage cố định 1600x900. Hàm này đo vùng nhìn thấy
   THẬT (visualViewport — chính xác cả khi WebView bị zoom/báo sai
   kích thước) rồi scale + căn giữa sân khấu vào đúng vùng đó.
   Nhờ vậy game luôn hiện đủ 100% khung hình trên mọi TV. */
const STAGE_W = 1600, STAGE_H = 900;
window.__stageScale = 1;
function fitStage() {
  const stage = document.getElementById('stage');
  if (!stage) return;
  const vv = window.visualViewport;
  const w  = (vv && vv.width)  || window.innerWidth  || document.documentElement.clientWidth  || STAGE_W;
  const h  = (vv && vv.height) || window.innerHeight || document.documentElement.clientHeight || STAGE_H;
  const ox = (vv && vv.offsetLeft) || 0;
  const oy = (vv && vv.offsetTop)  || 0;
  const s  = Math.min(w / STAGE_W, h / STAGE_H);
  window.__stageScale = s;
  const tx = ox + (w - STAGE_W * s) / 2;
  const ty = oy + (h - STAGE_H * s) / 2;
  stage.style.transform = 'translate(' + tx.toFixed(2) + 'px,' + ty.toFixed(2) + 'px) scale(' + s.toFixed(4) + ')';
}
window.addEventListener('resize', fitStage);
window.addEventListener('orientationchange', fitStage);
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', fitStage);
  window.visualViewport.addEventListener('scroll', fitStage);
}
document.addEventListener('DOMContentLoaded', fitStage);
setTimeout(fitStage, 300);
setTimeout(fitStage, 1500);
fitStage();

/* ---------------- Âm thanh (WebAudio synth) ---------------- */
const Sfx = {
  ctx: null,
  init() {
    if (!this.ctx) {
      try { this.ctx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) {}
    }
    if (this.ctx && this.ctx.state === 'suspended') this.ctx.resume();
  },
  tone(freq, dur, type, vol, when) {
    if (!this.ctx) return;
    type = type || 'sine'; vol = vol == null ? 0.16 : vol; when = when || 0;
    const t = this.ctx.currentTime + when;
    const o = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    o.type = type; o.frequency.value = freq;
    g.gain.setValueAtTime(vol, t);
    g.gain.exponentialRampToValueAtTime(0.001, t + dur);
    o.connect(g); g.connect(this.ctx.destination);
    o.start(t); o.stop(t + dur + 0.05);
  },
  play(name) {
    const T = this.tone.bind(this);
    switch (name) {
      case 'move':   T(520, .07, 'square', .06); break;
      case 'select': T(660, .09, 'triangle', .15); T(990, .12, 'triangle', .15, .07); break;
      case 'no':     T(180, .18, 'sawtooth', .12); break;
      case 'eat':    T(220, .08, 'square', .14); T(150, .08, 'square', .14, .12); T(200, .1, 'square', .14, .24); break;
      case 'coin':   T(880, .08, 'square', .1); T(1318, .16, 'square', .1, .08); break;
      case 'happy':  [523, 659, 784, 1046].forEach((f, i) => T(f, .13, 'triangle', .15, i * .09)); break;
      case 'sad':    T(392, .18, 'triangle', .14); T(311, .3, 'triangle', .14, .18); break;
      case 'wash':   [900, 1100, 1000, 1250].forEach((f, i) => T(f, .1, 'sine', .1, i * .12)); break;
      case 'sleep':  [660, 550, 440, 330].forEach((f, i) => T(f, .3, 'sine', .1, i * .3)); break;
      case 'levelup':[523, 659, 784, 1046, 1318].forEach((f, i) => T(f, .14, 'triangle', .16, i * .1)); break;
      case 'evolve': [392, 523, 659, 784, 1046, 784, 1046, 1318, 1568].forEach((f, i) => T(f, .18, 'triangle', .17, i * .13)); break;
      case 'win':    [784, 784, 1046, 1318, 1568].forEach((f, i) => T(f, .15, 'triangle', .16, i * .11)); break;
      case 'wrong':  T(160, .35, 'sawtooth', .14); break;
      case 'catch':  T(740, .07, 'square', .1); T(988, .1, 'square', .1, .05); break;
      case 'rock':   T(120, .2, 'sawtooth', .16); break;
      case 'pad0':   T(523, .3, 'triangle', .18); break;
      case 'pad1':   T(659, .3, 'triangle', .18); break;
      case 'pad2':   T(784, .3, 'triangle', .18); break;
      case 'pad3':   T(440, .3, 'triangle', .18); break;
      case 'warm':   T(620, .07, 'sine', .14); T(930, .1, 'sine', .12, .06); break;
      case 'crack':  T(300, .06, 'square', .16); T(180, .1, 'square', .14, .07); break;
    }
  }
};

/* ---------------- Dữ liệu ---------------- */
const SPECIES = {
  fire: {
    el: '🔥 Hệ Lửa', desc: 'Ấm áp và dũng cảm',
    stages: ['Lửa Nhí', 'Hỏa Hổ', 'Hỏa Long'],
    c: { body1: '#FFB35C', body2: '#FF6B5C', belly: '#FFE9C2', accent: '#FF3D2E', dark: '#B33A2B' }
  },
  water: {
    el: '💧 Hệ Nước', desc: 'Hiền lành và thông minh',
    stages: ['Giọt Nhí', 'Thủy Ngư', 'Thủy Long'],
    c: { body1: '#79D2FF', body2: '#3E8EF0', belly: '#E2F6FF', accent: '#2E6BD6', dark: '#22509E' }
  },
  leaf: {
    el: '🍀 Hệ Lá', desc: 'Vui vẻ và nhanh nhẹn',
    stages: ['Mầm Nhí', 'Lá Thỏ', 'Rồng Lá'],
    c: { body1: '#9CE86B', body2: '#4FBF4F', belly: '#EDFBD8', accent: '#2E9E4F', dark: '#237A3C' }
  }
};

const FOODS = [
  { icon: '🍎', name: 'Táo',      cost: 4,  hunger: 15, happy: 2 },
  { icon: '🍚', name: 'Cơm',      cost: 10, hunger: 40, happy: 3 },
  { icon: '🥛', name: 'Sữa',      cost: 8,  hunger: 22, happy: 4 },
  { icon: '🍰', name: 'Bánh kem', cost: 14, hunger: 30, happy: 12 },
  { icon: '🍬', name: 'Kẹo',      cost: 6,  hunger: 8,  happy: 10 }
];

const CHAT_IDLE = [
  'Tớ yêu cậu lắm! 💖', 'Chơi với tớ nhé!', 'Hôm nay vui quá!',
  'Cậu giỏi nhất! ⭐', 'Hihi~ 😊', 'Mình là bạn thân nhé!'
];
const CHAT_HUNGRY = ['Tớ đói quá! 🍚', 'Bụng tớ kêu ròi... 🍎'];
const CHAT_DIRTY  = ['Tắm cho tớ nhé! 🛁', 'Tớ hơi bẩn rồi...'];
const CHAT_TIRED  = ['Tớ buồn ngủ... 💤', 'Cho tớ ngủ chút nha 😴'];
const CHAT_SAD    = ['Chơi game với tớ đi! 🎮', 'Tớ buồn quá à...'];

/* ---------------- Trạng thái ---------------- */
const SAVE_KEY = 'thucungao_save_v1';
let S = null; // save state

function defaultState(species) {
  return {
    v: 1, species: species, level: 1, xp: 0, coins: 40,
    hunger: 80, happy: 80, energy: 90, clean: 90,
    egg: true, warm: 0,
    lastSeen: Date.now(), lastGift: ''
  };
}
function save() { if (S) { S.lastSeen = Date.now(); try { localStorage.setItem(SAVE_KEY, JSON.stringify(S)); } catch (e) {} } }
function load() {
  try {
    const raw = localStorage.getItem(SAVE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {}
  return null;
}
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
function stageOf(level) { return level >= 10 ? 2 : level >= 5 ? 1 : 0; }
function petName() { return SPECIES[S.species].stages[stageOf(S.level)]; }
function xpNeed(level) { return 40 + level * 25; }
function moodOf() {
  const avg = (S.hunger + S.happy + S.energy + S.clean) / 4;
  return avg >= 60 ? 'happy' : avg >= 32 ? 'ok' : 'sad';
}

/* ---------------- SVG trứng ---------------- */
function svgEgg(species, crack) {
  const c = SPECIES[species].c;
  const spots = `
    <circle cx="82" cy="100" r="9" fill="${c.body1}" opacity=".85"/>
    <circle cx="120" cy="82" r="6" fill="${c.body1}" opacity=".85"/>
    <circle cx="112" cy="128" r="11" fill="${c.body1}" opacity=".85"/>
    <circle cx="78" cy="140" r="5" fill="${c.body1}" opacity=".85"/>`;
  let cracks = '';
  if (crack >= 1) cracks += `<path d="M88 52 l8 10 l-7 8 l9 9" stroke="${c.dark}" stroke-width="3" fill="none" stroke-linecap="round"/>`;
  if (crack >= 2) cracks += `<path d="M118 60 l-6 11 l9 7 l-6 10" stroke="${c.dark}" stroke-width="3" fill="none" stroke-linecap="round"/>
    <path d="M70 90 l10 6 l-4 10" stroke="${c.dark}" stroke-width="3" fill="none" stroke-linecap="round"/>`;
  return `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
    <defs><radialGradient id="egg${species}" cx="38%" cy="28%" r="85%">
      <stop offset="0%" stop-color="#FFFDF2"/><stop offset="100%" stop-color="#F2E3C2"/>
    </radialGradient></defs>
    <ellipse cx="100" cy="172" rx="62" ry="16" fill="#C89B57"/>
    <ellipse cx="100" cy="166" rx="52" ry="12" fill="#E8C173"/>
    <path d="M100 34 C 138 34 152 86 152 118 a52 52 0 0 1 -104 0 C48 86 62 34 100 34 Z" fill="url(#egg${species})" stroke="#D9C49A" stroke-width="2"/>
    ${spots}${cracks}
    <ellipse cx="82" cy="66" rx="12" ry="18" fill="#fff" opacity=".55" transform="rotate(-20 82 66)"/>
  </svg>`;
}

/* ---------------- SVG thú cưng ---------------- */
function svgPet(species, stage, mood, sleeping) {
  const c = Object.assign({}, SPECIES[species].c);
  /* cấp cao hơn = màu đậm, "ngầu" hơn */
  if (stage === 2) c.body2 = c.accent;
  const gid = 'g' + species + stage;
  const grad = `<defs>
    <radialGradient id="${gid}" cx="38%" cy="30%" r="80%">
      <stop offset="0%" stop-color="${c.body1}"/><stop offset="100%" stop-color="${c.body2}"/>
    </radialGradient>
  </defs>`;

  /* mắt + miệng theo tâm trạng */
  let eyes, mouth, brows = '';
  if (sleeping) {
    eyes = `<path d="M72 106 q10 8 20 0" stroke="${c.dark}" stroke-width="4" fill="none" stroke-linecap="round"/>
            <path d="M108 106 q10 8 20 0" stroke="${c.dark}" stroke-width="4" fill="none" stroke-linecap="round"/>`;
    mouth = `<ellipse cx="100" cy="126" rx="6" ry="4.5" fill="${c.dark}" opacity=".85"/>`;
  } else if (mood === 'sad') {
    eyes = `<circle cx="82" cy="104" r="9" fill="#fff"/><circle cx="82" cy="106" r="5" fill="#33224E"/>
            <circle cx="118" cy="104" r="9" fill="#fff"/><circle cx="118" cy="106" r="5" fill="#33224E"/>
            <circle cx="84" cy="103" r="2" fill="#fff"/><circle cx="120" cy="103" r="2" fill="#fff"/>`;
    brows = `<path d="M72 96 l18 -5" stroke="${c.dark}" stroke-width="4" stroke-linecap="round"/>
             <path d="M128 96 l-18 -5" stroke="${c.dark}" stroke-width="4" stroke-linecap="round"/>`;
    mouth = `<path d="M90 132 q10 -9 20 0" stroke="${c.dark}" stroke-width="4.5" fill="none" stroke-linecap="round"/>`;
  } else if (mood === 'ok') {
    eyes = `<circle cx="82" cy="102" r="10" fill="#fff"/><circle cx="82" cy="103" r="5.5" fill="#33224E"/>
            <circle cx="118" cy="102" r="10" fill="#fff"/><circle cx="118" cy="103" r="5.5" fill="#33224E"/>
            <circle cx="85" cy="100" r="2.5" fill="#fff"/><circle cx="121" cy="100" r="2.5" fill="#fff"/>`;
    mouth = `<path d="M92 128 q8 4 16 0" stroke="${c.dark}" stroke-width="4.5" fill="none" stroke-linecap="round"/>`;
  } else {
    eyes = `<circle cx="82" cy="101" r="11" fill="#fff"/><circle cx="83" cy="102" r="6" fill="#33224E"/>
            <circle cx="118" cy="101" r="11" fill="#fff"/><circle cx="119" cy="102" r="6" fill="#33224E"/>
            <circle cx="86" cy="99" r="3" fill="#fff"/><circle cx="122" cy="99" r="3" fill="#fff"/>`;
    mouth = `<path d="M88 124 q12 12 24 0" stroke="${c.dark}" stroke-width="5" fill="none" stroke-linecap="round"/>
             <path d="M96 130 q4 4 8 0" stroke="${c.accent}" stroke-width="3" fill="none" stroke-linecap="round" opacity=".6"/>`;
  }
  const cheeks = `<ellipse cx="68" cy="118" rx="7" ry="5" fill="#FF9DB0" opacity=".7"/>
                  <ellipse cx="132" cy="118" rx="7" ry="5" fill="#FF9DB0" opacity=".7"/>`;

  /* phụ kiện theo hệ */
  let hat = '', tail = '', wings = '', horns = '';
  if (species === 'fire') {
    hat = `<path d="M100 40 q-8 14 0 22 q10 -6 6 -18 q8 8 4 20 q14 -10 6 -28 q-8 -12 -16 4" fill="#FF8A3D"/>
           <path d="M100 48 q-4 8 0 13 q6 -4 4 -12" fill="#FFD34D"/>`;
    if (stage >= 1) tail = `<path d="M158 138 q26 -6 24 -30 q-4 10 -14 10 q8 -16 -4 -26 q2 14 -10 20 q-6 16 4 26" fill="#FF8A3D"/>`;
  } else if (species === 'water') {
    hat = `<path d="M100 38 q-14 16 0 26 q14 -10 0 -26" fill="${c.accent}"/>
           <circle cx="100" cy="55" r="4" fill="#BFE9FF"/>`;
    if (stage >= 1) tail = `<path d="M156 142 q26 2 30 -18 q-8 6 -14 4 q10 -10 4 -22 q-4 12 -16 12 q-12 10 -4 24" fill="${c.accent}" opacity=".9"/>`;
  } else {
    hat = `<path d="M100 62 q-2 -22 -22 -26 q10 18 16 24 M100 62 q2 -22 22 -26 q-10 18 -16 24" stroke="${c.accent}" stroke-width="6" fill="none" stroke-linecap="round"/>
           <ellipse cx="78" cy="38" rx="10" ry="6" fill="${c.accent}" transform="rotate(-35 78 38)"/>
           <ellipse cx="122" cy="38" rx="10" ry="6" fill="${c.accent}" transform="rotate(35 122 38)"/>`;
    if (stage >= 1) tail = `<ellipse cx="168" cy="130" rx="16" ry="9" fill="${c.accent}" transform="rotate(-30 168 130)"/>`;
  }
  if (stage >= 2) {
    wings = `<path d="M34 96 q-26 -18 -22 -44 q14 8 18 22 q2 -14 12 -20 q-2 22 2 34 z" fill="${c.body1}" stroke="${c.dark}" stroke-width="2" opacity=".95"/>
             <path d="M166 96 q26 -18 22 -44 q-14 8 -18 22 q-2 -14 -12 -20 q2 22 -2 34 z" fill="${c.body1}" stroke="${c.dark}" stroke-width="2" opacity=".95"/>`;
    horns = `<path d="M76 52 q-6 -18 4 -26 q6 12 6 22 z" fill="#FFE59A" stroke="${c.dark}" stroke-width="2"/>
             <path d="M124 52 q6 -18 -4 -26 q-6 12 -6 22 z" fill="#FFE59A" stroke="${c.dark}" stroke-width="2"/>`;
  }

  /* tai theo cấp */
  const ears = stage === 0
    ? `<circle cx="66" cy="62" r="12" fill="url(#${gid})"/><circle cx="134" cy="62" r="12" fill="url(#${gid})"/>`
    : `<ellipse cx="62" cy="56" rx="12" ry="20" fill="url(#${gid})" transform="rotate(-18 62 56)"/>
       <ellipse cx="138" cy="56" rx="12" ry="20" fill="url(#${gid})" transform="rotate(18 138 56)"/>
       <ellipse cx="62" cy="58" rx="6" ry="11" fill="${c.belly}" transform="rotate(-18 62 58)"/>
       <ellipse cx="138" cy="58" rx="6" ry="11" fill="${c.belly}" transform="rotate(18 138 58)"/>`;

  const bodyR = stage === 0 ? 56 : stage === 1 ? 62 : 66;
  const feet = `<ellipse cx="74" cy="176" rx="15" ry="9" fill="${c.body2}"/>
                <ellipse cx="126" cy="176" rx="15" ry="9" fill="${c.body2}"/>`;
  const arms = stage >= 1
    ? `<ellipse cx="42" cy="132" rx="10" ry="16" fill="url(#${gid})" transform="rotate(20 42 132)"/>
       <ellipse cx="158" cy="132" rx="10" ry="16" fill="url(#${gid})" transform="rotate(-20 158 132)"/>`
    : '';

  const hatScale = stage === 0 ? 0.85 : stage === 1 ? 1.15 : 1.4;
  return `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
    ${grad}
    ${wings}${tail}
    ${feet}
    ${ears}${horns}<g transform="translate(100 52) scale(${hatScale}) translate(-100 -52)">${hat}</g>
    <circle cx="100" cy="118" r="${bodyR}" fill="url(#${gid})"/>
    <ellipse cx="100" cy="140" rx="${bodyR * 0.62}" ry="${bodyR * 0.5}" fill="${c.belly}"/>
    ${arms}
    ${brows}${eyes}${cheeks}${mouth}
  </svg>`;
}

/* ---------------- Nhập liệu (D-pad) ---------------- */
const KEYMAP = {
  ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
  Enter: 'ok', ' ': 'ok', Escape: 'back', Backspace: 'back', GoBack: 'back'
};
const modeStack = [];
function setMode(m) { modeStack.length = 0; modeStack.push(m); }
function pushMode(m) { modeStack.push(m); }
function popMode() { modeStack.pop(); }

window.addEventListener('keydown', (e) => {
  const k = KEYMAP[e.key];
  if (!k) return;
  e.preventDefault();
  Sfx.init();
  dispatch(k);
});
function dispatch(k) {
  const m = modeStack[modeStack.length - 1];
  if (!m) return false;
  return m.onKey(k) !== false;
}
/* Nút BACK trên remote (gọi từ Android) */
window.handleBack = function () {
  Sfx.init();
  return dispatch('back') === true;
};

/* Danh sách tiêu điểm 1 chiều */
function focusList(els, startIdx) {
  const st = { els, idx: clamp(startIdx || 0, 0, els.length - 1) };
  const apply = () => els.forEach((el, i) => el.classList.toggle('focus', i === st.idx));
  apply();
  st.move = (dir) => {
    const d = (dir === 'left' || dir === 'up') ? -1 : 1;
    let n = st.idx;
    do { n = clamp(n + d, 0, els.length - 1); } while (false);
    if (n !== st.idx) { st.idx = n; apply(); Sfx.play('move'); }
  };
  st.current = () => els[st.idx];
  return st;
}

/* ---------------- Toast + FX ---------------- */
let toastTimer = null;
function toast(msg, ms) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), ms || 2000);
}
function floatFx(txt) {
  const zone = document.querySelector('.petzone');
  if (!zone) return;
  const s = document.createElement('div');
  s.className = 'fx-float';
  s.textContent = txt;
  zone.appendChild(s);
  setTimeout(() => s.remove(), 1500);
}

/* ---------------- Hộp thoại ---------------- */
function showDialog(opt) {
  const ov = document.getElementById('overlay');
  ov.innerHTML = `<div class="dim"></div>
    <div class="dialog">
      ${opt.title ? `<h2>${opt.title}</h2>` : ''}
      ${opt.text ? `<p>${opt.text}</p>` : ''}
      ${opt.html || ''}
      <div class="row">${opt.buttons.map(b => `<button class="btn">${b.label}</button>`).join('')}</div>
    </div>`;
  ov.classList.add('show');
  const btns = Array.from(ov.querySelectorAll('.row .btn'));
  const fl = focusList(btns, opt.defaultIdx || 0);
  pushMode({
    name: 'dialog',
    onKey(k) {
      if (k === 'left' || k === 'right' || k === 'up' || k === 'down') fl.move(k);
      else if (k === 'ok') {
        Sfx.play('select');
        closeDialog();
        opt.buttons[fl.idx].cb && opt.buttons[fl.idx].cb();
      } else if (k === 'back') {
        closeDialog();
        if (opt.onBack) opt.onBack();
      }
      return true;
    }
  });
}
function closeDialog() {
  const ov = document.getElementById('overlay');
  ov.classList.remove('show');
  ov.innerHTML = '';
  popMode();
}

/* ============================================================
   MÀN HÌNH: TIÊU ĐỀ
   ============================================================ */
function showTitle() {
  const app = document.getElementById('app');
  const hasSave = !!load();
  app.innerHTML = `<div class="screen title-screen">
    <div class="title-logo">🐾 THÚ CƯNG ẢO 🐾</div>
    <div class="title-sub">Nuôi bé thú đáng yêu của riêng em!</div>
    <div class="title-pets">${['fire','water','leaf'].map(s =>
      `<span style="display:inline-block;width:9rem;height:9rem;vertical-align:middle">${svgPet(s, 0, 'happy', false)}</span>`).join('')}</div>
    <div class="menu">
      ${hasSave ? '<button class="btn" data-a="continue"><span class="ic">▶️</span> Chơi tiếp</button>' : ''}
      <button class="btn" data-a="new"><span class="ic">🐣</span> ${hasSave ? 'Nuôi thú mới' : 'Bắt đầu'}</button>
      <button class="btn" data-a="exit"><span class="ic">🚪</span> Thoát</button>
    </div>
    <div class="hint-bar">🔼 🔽 Chọn &nbsp;•&nbsp; OK Xác nhận</div>
  </div>`;
  const btns = Array.from(app.querySelectorAll('.menu .btn'));
  const fl = focusList(btns, 0);
  setMode({
    name: 'title',
    onKey(k) {
      if (k === 'up' || k === 'down' || k === 'left' || k === 'right') { fl.move(k); return true; }
      if (k === 'ok') {
        Sfx.play('select');
        const a = fl.current().dataset.a;
        if (a === 'continue') {
          S = load();
          if (S.egg) { showEggScreen(); }
          else { applyOfflineDecay(); showHome(); dailyGift(); }
        }
        else if (a === 'new') {
          if (hasSave) {
            showDialog({
              title: '🐣 Nuôi thú mới?',
              text: 'Bé thú hiện tại sẽ tạm biệt em đó! Em chắc chưa?',
              buttons: [
                { label: 'Quay lại', cb: () => {} },
                { label: 'Nuôi thú mới!', cb: () => showPick() }
              ]
            });
          } else showPick();
        }
        else if (a === 'exit') exitApp();
        return true;
      }
      if (k === 'back') return false; /* cho phép thoát app */
      return true;
    }
  });
}
function exitApp() {
  save();
  if (window.AndroidTV && window.AndroidTV.exitApp) window.AndroidTV.exitApp();
  else window.close();
}

/* ============================================================
   MÀN HÌNH: CHỌN THÚ
   ============================================================ */
function showPick() {
  const app = document.getElementById('app');
  const keys = ['fire', 'water', 'leaf'];
  const eggNames = { fire: 'Trứng Lửa', water: 'Trứng Nước', leaf: 'Trứng Lá' };
  app.innerHTML = `<div class="screen pick-screen">
    <div class="pick-title">🥚 Chọn quả trứng bí ẩn! 🥚</div>
    <div class="pick-cards">
      ${keys.map(s => `<div class="pick-card" data-s="${s}">
          <div class="petbox">${svgEgg(s, 0)}</div>
          <h3>${eggNames[s]}</h3>
          <div class="el">${SPECIES[s].el}</div>
          <p>Bé thú gì sẽ nở ra nhỉ?</p>
        </div>`).join('')}
    </div>
    <div class="hint-bar">◀ ▶ Chọn &nbsp;•&nbsp; OK Nhận trứng &nbsp;•&nbsp; BACK Quay lại</div>
  </div>`;
  const cards = Array.from(app.querySelectorAll('.pick-card'));
  const fl = focusList(cards, 1);
  setMode({
    name: 'pick',
    onKey(k) {
      if (k === 'left' || k === 'right') { fl.move(k); return true; }
      if (k === 'ok') {
        const sp = fl.current().dataset.s;
        Sfx.play('happy');
        showDialog({
          title: `🥚 ${eggNames[sp]}`,
          text: 'Hãy ấp và vuốt ve để trứng nở ra bé thú nhé!',
          html: `<div style="width:12rem;height:12rem;margin:0 auto 1rem">${svgEgg(sp, 0)}</div>`,
          buttons: [
            { label: '💖 Nhận trứng!', cb: () => { S = defaultState(sp); save(); Sfx.play('win'); showEggScreen(); } },
            { label: 'Chọn lại', cb: () => {} }
          ]
        });
        return true;
      }
      if (k === 'back') { showTitle(); return true; }
      return true;
    }
  });
}

/* ============================================================
   MÀN HÌNH: ẤP TRỨNG
   ============================================================ */
function crackOf(warm) { return warm >= 75 ? 2 : warm >= 40 ? 1 : 0; }

function showEggScreen() {
  clearInterval(homeTimer); clearTimeout(chatTimer);
  const app = document.getElementById('app');
  const hour = new Date().getHours();
  const night = hour >= 18 || hour < 6;
  app.innerHTML = `<div class="screen home-screen ${night ? 'night' : ''}">
    <div class="home-bg">
      <div class="sun"></div>
      <div class="cloud" style="top:10%;width:11rem;height:3.4rem;animation-duration:60s"></div>
      <div class="cloud" style="top:22%;width:8rem;height:2.6rem;animation-duration:85s;animation-delay:-30s"></div>
    </div>
    <div class="petzone">
      <div class="eggbox" id="eggbox">${svgEgg(S.species, crackOf(S.warm))}</div>
      <div class="warm-meter">
        <div class="lab">🔥 Ấp trứng: <span id="warmVal">${Math.round(S.warm)}</span>%</div>
        <div class="bar"><i id="warmBar" style="width:${S.warm}%;background:linear-gradient(90deg,#FFC94D,#FF8A3D)"></i></div>
      </div>
      <div class="egg-hint">Bấm <b>OK</b> thật nhiều để ấp và vuốt ve trứng! 🥚💖</div>
    </div>
    <div class="hint-bar" style="color:#fff">OK Ấp trứng &nbsp;•&nbsp; BACK Thoát</div>
  </div>`;
  setMode({
    name: 'egg',
    onKey(k) {
      if (k === 'ok') { warmEgg(); return true; }
      if (k === 'back') {
        showDialog({
          title: '🚪 Tạm biệt?',
          text: 'Quả trứng sẽ chờ em quay lại ấp tiếp đó!',
          buttons: [
            { label: '💖 Ấp tiếp', cb: () => {} },
            { label: '👋 Về màn hình chính', cb: () => { save(); showTitle(); } },
            { label: '🚪 Tắt game', cb: exitApp }
          ]
        });
        return true;
      }
      return true;
    }
  });
}

function warmEgg() {
  if (S.warm >= 100) return;
  const before = crackOf(S.warm);
  S.warm = clamp(S.warm + 7 + Math.random() * 5, 0, 100);
  const after = crackOf(S.warm);
  const box = document.getElementById('eggbox');
  document.getElementById('warmVal').textContent = Math.round(S.warm);
  document.getElementById('warmBar').style.width = S.warm + '%';
  if (box) {
    box.classList.remove('wob');
    void box.offsetWidth; /* restart animation */
    box.classList.add('wob');
    if (after !== before) {
      box.innerHTML = svgEgg(S.species, after);
      Sfx.play('crack');
      floatFx(after === 1 ? '✨ Có vết nứt rồi!' : '💥 Sắp nở rồi!!');
    } else {
      Sfx.play('warm');
      if (Math.random() < 0.3) floatFx(['💖', '🔥', '✨', '🥰'][Math.floor(Math.random() * 4)]);
    }
  }
  save();
  if (S.warm >= 100) {
    pushMode({ name: 'busy', onKey() { return true; } });
    setTimeout(showHatch, 800);
  }
}

function showHatch() {
  S.egg = false;
  S.coins += 20;
  save();
  Sfx.play('evolve');
  const app = document.getElementById('app');
  app.innerHTML = `<div class="screen evo-screen">
    <div class="evo-title">🎉 TRỨNG NỞ RỒI! 🎉</div>
    <div class="evo-pet">${svgPet(S.species, 0, 'happy', false)}</div>
    <div class="evo-name">Chào em, tớ là ${SPECIES[S.species].stages[0]}!</div>
    <div class="hint-bar" style="color:#fff">Bấm OK để bắt đầu chăm sóc bé 💖 (+20 xu)</div>
  </div>`;
  const scr = app.querySelector('.evo-screen');
  const emo = ['🎉', '⭐', '✨', '🎊', '💖', '🥚'];
  for (let i = 0; i < 26; i++) {
    const c = document.createElement('div');
    c.className = 'confetti';
    c.textContent = emo[i % emo.length];
    c.style.cssText = `left:${Math.random() * 100}%;animation-duration:${2.4 + Math.random() * 2.4}s;animation-delay:${Math.random() * 1.6}s`;
    scr.appendChild(c);
  }
  setMode({
    name: 'hatch',
    onKey(k) {
      if (k === 'ok' || k === 'back') { Sfx.play('select'); save(); showHome(); }
      return true;
    }
  });
}

/* ============================================================
   MÀN HÌNH: NHÀ (chính)
   ============================================================ */
let homeTimer = null;
let chatTimer = null;
let lastDecay = 0;

function applyOfflineDecay() {
  if (!S || S.egg) return;
  const mins = Math.min((Date.now() - (S.lastSeen || Date.now())) / 60000, 720);
  if (mins > 1) {
    S.hunger = clamp(S.hunger - mins * 0.5, 25, 100);
    S.happy  = clamp(S.happy  - mins * 0.35, 25, 100);
    S.energy = clamp(S.energy + mins * 0.8, 0, 100); /* nghỉ ngơi khi vắng */
    S.clean  = clamp(S.clean  - mins * 0.25, 25, 100);
  }
}

function dailyGift() {
  const today = new Date().toDateString();
  if (S.lastGift !== today) {
    S.lastGift = today;
    S.coins += 30;
    save();
    setTimeout(() => {
      Sfx.play('coin');
      showDialog({
        title: '🎁 Quà mỗi ngày!',
        text: 'Em nhận được <b style="color:#E8A200">+30 xu</b>! Hãy chăm sóc bé thú thật tốt nhé!',
        buttons: [{ label: '💛 Cảm ơn!', cb: () => {} }]
      });
    }, 600);
  }
}

const ACTIONS = [
  { id: 'feed',  ic: '🍎', label: 'Cho ăn' },
  { id: 'play',  ic: '🎮', label: 'Chơi game' },
  { id: 'wash',  ic: '🛁', label: 'Tắm' },
  { id: 'sleep', ic: '💤', label: 'Đi ngủ' }
];

function showHome() {
  if (S.egg) { showEggScreen(); return; }
  clearInterval(homeTimer); clearTimeout(chatTimer);
  const app = document.getElementById('app');
  const hour = new Date().getHours();
  const night = hour >= 18 || hour < 6;
  app.innerHTML = `<div class="screen home-screen ${night ? 'night' : ''}">
    <div class="home-bg">
      <div class="sun"></div>
      <div class="cloud" style="top:10%;width:11rem;height:3.4rem;animation-duration:60s"></div>
      <div class="cloud" style="top:22%;width:8rem;height:2.6rem;animation-duration:85s;animation-delay:-30s"></div>
    </div>
    <div class="hud">
      <div class="stats">
        ${[['hunger','🍗','No bụng'],['happy','😊','Vui vẻ'],['energy','⚡','Sức khỏe'],['clean','🫧','Sạch sẽ']].map(([id, ic, lb]) =>
          `<div class="stat" id="stat-${id}">
             <div class="lab"><span>${ic} ${lb}</span><span class="val"></span></div>
             <div class="bar"><i></i></div>
           </div>`).join('')}
      </div>
      <div class="hud-right">
        <div class="pill" id="coinPill">🪙 <span id="coinVal"></span> xu</div>
        <div class="pill" id="lvlPill">⭐ Cấp <span id="lvlVal"></span></div>
        <div class="pill xp-pill">XP <span class="bar"><i id="xpBar" style="background:#B77BFF"></i></span></div>
      </div>
    </div>
    <div class="petzone">
      <div class="speech" id="speech"></div>
      <div class="petbox" id="petbox">${svgPet(S.species, stageOf(S.level), moodOf(), false)}</div>
      <div class="petname" id="petname">${petName()}</div>
    </div>
    <div class="actions">
      ${ACTIONS.map(a => `<button class="action-btn" data-a="${a.id}"><span class="ic">${a.ic}</span>${a.label}</button>`).join('')}
    </div>
    <div class="hint-bar" style="color:#fff">◀ ▶ Chọn &nbsp;•&nbsp; OK Làm &nbsp;•&nbsp; BACK Thoát</div>
  </div>`;

  const btns = Array.from(app.querySelectorAll('.action-btn'));
  const fl = focusList(btns, 0);
  setMode({
    name: 'home',
    onKey(k) {
      if (k === 'left' || k === 'right') { fl.move(k); return true; }
      if (k === 'ok') { Sfx.play('select'); doAction(fl.current().dataset.a); return true; }
      if (k === 'back') {
        showDialog({
          title: '🚪 Tạm biệt?',
          text: 'Em muốn nghỉ chơi chưa? Bé thú sẽ chờ em quay lại đó!',
          buttons: [
            { label: '💖 Chơi tiếp', cb: () => {} },
            { label: '👋 Về màn hình chính', cb: () => { save(); showTitle(); } },
            { label: '🚪 Tắt game', cb: exitApp }
          ]
        });
        return true;
      }
      return true;
    }
  });

  lastDecay = Date.now();
  updateHomeUI();
  homeTimer = setInterval(homeTick, 1000);
  scheduleChat(4000);
}

function homeTick() {
  const now = Date.now();
  const mins = (now - lastDecay) / 60000;
  lastDecay = now;
  S.hunger = clamp(S.hunger - mins * 1.4, 0, 100);
  S.happy  = clamp(S.happy  - mins * 1.0, 0, 100);
  S.energy = clamp(S.energy - mins * 0.8, 0, 100);
  S.clean  = clamp(S.clean  - mins * 0.7, 0, 100);
  updateHomeUI();
  if (Math.random() < 0.02) save();
}

function barColor(v) { return v >= 60 ? '#58D68D' : v >= 30 ? '#FFC94D' : '#FF6B6B'; }

function updateHomeUI() {
  if (!document.getElementById('stat-hunger')) return;
  [['hunger'], ['happy'], ['energy'], ['clean']].forEach(([id]) => {
    const el = document.getElementById('stat-' + id);
    const v = Math.round(S[id]);
    el.querySelector('.val').textContent = v;
    const bar = el.querySelector('.bar i');
    bar.style.width = v + '%';
    bar.style.background = barColor(v);
    el.classList.toggle('low', v < 30);
  });
  document.getElementById('coinVal').textContent = S.coins;
  document.getElementById('lvlVal').textContent = S.level;
  document.getElementById('xpBar').style.width = clamp(S.xp / xpNeed(S.level) * 100, 0, 100) + '%';
  const box = document.getElementById('petbox');
  const stage = stageOf(S.level);
  const sig = S.species + stage + moodOf();
  if (box && box.dataset.sig !== sig) {
    box.dataset.sig = sig;
    box.innerHTML = svgPet(S.species, stage, moodOf(), false);
    /* thú lớn dần theo cấp tiến hóa */
    const sz = [22, 26, 30][stage];
    const svg = box.querySelector('svg');
    if (svg) { svg.style.width = sz + 'rem'; svg.style.height = sz + 'rem'; }
    document.getElementById('petname').textContent = petName();
  }
}

function scheduleChat(delay) {
  clearTimeout(chatTimer);
  chatTimer = setTimeout(() => {
    const el = document.getElementById('speech');
    if (el && modeStack[modeStack.length - 1] && modeStack[modeStack.length - 1].name === 'home') {
      let pool = CHAT_IDLE;
      if (S.hunger < 35) pool = CHAT_HUNGRY;
      else if (S.clean < 35) pool = CHAT_DIRTY;
      else if (S.energy < 35) pool = CHAT_TIRED;
      else if (S.happy < 35) pool = CHAT_SAD;
      el.textContent = pool[Math.floor(Math.random() * pool.length)];
      el.classList.add('show');
      setTimeout(() => el.classList.remove('show'), 3500);
    }
    scheduleChat(9000 + Math.random() * 8000);
  }, delay);
}

/* ---------------- Hành động ---------------- */
function doAction(a) {
  if (a === 'feed') showFoodMenu();
  else if (a === 'play') showPlayMenu();
  else if (a === 'wash') doWash();
  else if (a === 'sleep') doSleep();
}

let evolving = false;
function addXp(amount) {
  S.xp += amount;
  let leveled = false;
  while (S.xp >= xpNeed(S.level)) {
    S.xp -= xpNeed(S.level);
    const oldStage = stageOf(S.level);
    S.level += 1;
    S.coins += S.level * 5;
    leveled = true;
    const newStage = stageOf(S.level);
    if (newStage !== oldStage && !evolving) {
      evolving = true;
      save();
      setTimeout(() => showEvolution(newStage), 700);
      return;
    }
  }
  if (leveled) {
    Sfx.play('levelup');
    toast(`⭐ Lên cấp ${S.level}! Thưởng ${S.level * 5} xu!`, 2600);
  }
  save();
  updateHomeUI();
}

function showFoodMenu() {
  const cardsHtml = FOODS.map(f =>
    `<div class="card ${S.coins < f.cost ? 'locked' : ''}">
       <div class="big">${f.icon}</div>
       <div class="nm">${f.name}</div>
       <div class="sub">🪙 ${f.cost} xu</div>
       <div class="sub">🍗 +${f.hunger}</div>
     </div>`).join('');
  const ov = document.getElementById('overlay');
  ov.innerHTML = `<div class="dim"></div>
    <div class="dialog" style="min-width:70rem">
      <h2>🍽️ Cho bé ăn gì nào?</h2>
      <div class="cards">${cardsHtml}</div>
      <p style="margin-top:1.6rem;font-size:1.5rem;color:#8A7BAF">🪙 Em có ${S.coins} xu • BACK để quay lại</p>
    </div>`;
  ov.classList.add('show');
  const cards = Array.from(ov.querySelectorAll('.card'));
  const fl = focusList(cards, 0);
  pushMode({
    name: 'food',
    onKey(k) {
      if (k === 'left' || k === 'right') { fl.move(k); return true; }
      if (k === 'back') { closeDialog(); return true; }
      if (k === 'ok') {
        const f = FOODS[fl.idx];
        if (S.coins < f.cost) {
          Sfx.play('no');
          toast('Chưa đủ xu! Chơi mini game để kiếm xu nhé! 🎮', 2500);
          return true;
        }
        S.coins -= f.cost;
        closeDialog();
        feedPet(f);
        return true;
      }
      return true;
    }
  });
}

function feedPet(f) {
  Sfx.play('eat');
  S.hunger = clamp(S.hunger + f.hunger, 0, 100);
  S.happy = clamp(S.happy + f.happy, 0, 100);
  const box = document.getElementById('petbox');
  if (box) {
    box.classList.add('eat');
    setTimeout(() => box.classList.remove('eat'), 1100);
  }
  floatFx(`${f.icon} +${f.hunger} 🍗`);
  setTimeout(() => { Sfx.play('happy'); floatFx('😋 Ngon quá!'); }, 1200);
  addXp(6);
}

function doWash() {
  if (S.clean > 95) { toast('Bé thú đang sạch bong rồi! ✨', 2200); return; }
  const box = document.getElementById('petbox');
  const zone = document.querySelector('.petzone');
  if (!box || !zone) return;
  Sfx.play('wash');
  box.classList.add('washing');
  const bub = document.createElement('div');
  bub.className = 'bubbles';
  for (let i = 0; i < 14; i++) {
    const s = document.createElement('span');
    const sz = 1.2 + Math.random() * 2.2;
    s.style.cssText = `left:${20 + Math.random() * 60}%;bottom:${10 + Math.random() * 40}%;width:${sz}rem;height:${sz}rem;animation-delay:${Math.random() * 1.2}s`;
    bub.appendChild(s);
  }
  zone.appendChild(bub);
  pushMode({ name: 'busy', onKey() { return true; } });
  setTimeout(() => {
    popMode();
    bub.remove();
    if (box) box.classList.remove('washing');
    S.clean = 100;
    S.happy = clamp(S.happy + 5, 0, 100);
    Sfx.play('happy');
    floatFx('🫧 Sạch bong! ✨');
    addXp(5);
  }, 2600);
}

function doSleep() {
  if (S.energy > 95) { toast('Bé thú đang khỏe re, chưa buồn ngủ! ⚡', 2200); return; }
  const app = document.querySelector('.home-screen');
  const zone = document.querySelector('.petzone');
  const box = document.getElementById('petbox');
  if (!app || !zone || !box) return;
  Sfx.play('sleep');
  app.classList.add('night');
  const curSvg = box.querySelector('svg');
  const keepW = curSvg ? curSvg.style.width : '';
  box.innerHTML = svgPet(S.species, stageOf(S.level), 'happy', true);
  const newSvg = box.querySelector('svg');
  if (newSvg && keepW) { newSvg.style.width = keepW; newSvg.style.height = keepW; }
  const zs = [];
  for (let i = 0; i < 3; i++) {
    const z = document.createElement('div');
    z.className = 'zzz';
    z.textContent = '💤';
    z.style.cssText = `left:${60 + i * 8}%;top:${10 + i * 6}%;animation-delay:${i * 0.7}s`;
    zone.appendChild(z); zs.push(z);
  }
  toast('😴 Bé thú đang ngủ... (OK để đánh thức)', 2600);
  let slept = 0;
  const sleepInt = setInterval(() => {
    slept++;
    S.energy = clamp(S.energy + 7, 0, 100);
    updateHomeUI();
    if (S.energy >= 100 || slept > 16) wake();
  }, 900);
  let awake = false;
  function wake() {
    if (awake) return;
    awake = true;
    clearInterval(sleepInt);
    zs.forEach(z => z.remove());
    popMode();
    const hour = new Date().getHours();
    if (!(hour >= 18 || hour < 6)) app.classList.remove('night');
    box.dataset.sig = '';
    updateHomeUI();
    Sfx.play('happy');
    floatFx('⚡ Khỏe khoắn!');
    addXp(5);
  }
  pushMode({
    name: 'sleeping',
    onKey(k) { if (k === 'ok' || k === 'back') wake(); return true; }
  });
}

/* ---------------- Menu chọn mini game ---------------- */
function showPlayMenu() {
  const ov = document.getElementById('overlay');
  ov.innerHTML = `<div class="dim"></div>
    <div class="dialog" style="min-width:60rem">
      <h2>🎮 Chơi gì nào?</h2>
      <div class="cards">
        <div class="card" data-g="fruit" style="width:22rem">
          <div class="big">🍎🧺</div><div class="nm">Hứng Trái Cây</div>
          <div class="sub">◀ ▶ hứng trái cây rơi<br>Né hòn đá nhé!</div>
        </div>
        <div class="card" data-g="memory" style="width:22rem">
          <div class="big">🌈🧠</div><div class="nm">Bé Nhớ Giỏi</div>
          <div class="sub">Nhìn màu nhấp nháy<br>rồi bấm lại đúng thứ tự!</div>
        </div>
      </div>
      <p style="margin-top:1.6rem;font-size:1.5rem;color:#8A7BAF">Chơi game được XU và XP! • BACK quay lại</p>
    </div>`;
  ov.classList.add('show');
  const cards = Array.from(ov.querySelectorAll('.card'));
  const fl = focusList(cards, 0);
  pushMode({
    name: 'playmenu',
    onKey(k) {
      if (k === 'left' || k === 'right') { fl.move(k); return true; }
      if (k === 'back') { closeDialog(); return true; }
      if (k === 'ok') {
        Sfx.play('select');
        const g = fl.current().dataset.g;
        closeDialog();
        if (g === 'fruit') startFruitGame();
        else startMemoryGame();
        return true;
      }
      return true;
    }
  });
}

function endMiniGame(title, text, coins, xp, replayFn) {
  S.coins += coins;
  S.happy = clamp(S.happy + 12, 0, 100);
  S.energy = clamp(S.energy - 6, 0, 100);
  addXp(xp);
  save();
  showDialog({
    title,
    text: `${text}<br><b style="color:#E8A200">+${coins} xu</b> • <b style="color:#8B5CF6">+${xp} XP</b>`,
    buttons: [
      { label: '🔁 Chơi lại', cb: replayFn },
      { label: '🏠 Về nhà', cb: () => showHome() }
    ]
  });
}

/* ============================================================
   MINI GAME 1: HỨNG TRÁI CÂY
   ============================================================ */
function startFruitGame() {
  clearInterval(homeTimer); clearTimeout(chatTimer);
  const app = document.getElementById('app');
  app.innerHTML = `<div class="screen game-screen fruit-screen">
    <canvas id="fruitCanvas"></canvas>
    <div class="game-hud">
      <div class="pill">⏱️ <span id="fgTime">45</span>s</div>
      <div class="pill" style="font-size:2.4rem">🍎🧺 Hứng Trái Cây</div>
      <div class="pill">⭐ <span id="fgScore">0</span></div>
    </div>
    <div class="hint-bar" style="color:#5A4520">◀ ▶ Di chuyển • BACK Thoát</div>
  </div>`;

  const canvas = document.getElementById('fruitCanvas');
  /* toạ độ game theo sân khấu 1600x900; backing store theo pixel thật
     của màn hình (dpr x tỉ lệ sân khấu) để sắc nét trên TV 4K */
  const W = STAGE_W;
  const H = STAGE_H;
  const eff = Math.min((window.devicePixelRatio || 1) * (window.__stageScale || 1), 3) || 1;
  canvas.width = Math.round(W * eff);
  canvas.height = Math.round(H * eff);
  canvas.style.width = W + 'px';
  canvas.style.height = H + 'px';
  const ctx = canvas.getContext('2d');
  ctx.scale(eff, eff);

  const FRUITS = ['🍎', '🍌', '🍇', '🍓', '🍊', '🍉'];
  const petImg = new Image();
  petImg.src = 'data:image/svg+xml;charset=utf-8,' +
    encodeURIComponent(svgPet(S.species, stageOf(S.level), 'happy', false));

  const st = {
    x: W / 2, tx: W / 2, score: 0, time: 45, items: [], over: false,
    stun: 0, raf: 0, spawnAcc: 0, last: performance.now()
  };
  const petW = Math.min(H * 0.22, 240);
  const step = W * 0.09;

  const timerInt = setInterval(() => {
    if (st.over) return;
    st.time--;
    document.getElementById('fgTime').textContent = st.time;
    if (st.time <= 0) finish();
  }, 1000);

  function spawn() {
    const rock = Math.random() < 0.18;
    st.items.push({
      x: 60 + Math.random() * (W - 120),
      y: -60,
      v: (H * 0.22) + Math.random() * (H * 0.14),
      icon: rock ? '🪨' : FRUITS[Math.floor(Math.random() * FRUITS.length)],
      rock
    });
  }

  function frame(now) {
    if (st.over) return;
    const dt = Math.min((now - st.last) / 1000, 0.05);
    st.last = now;
    st.spawnAcc += dt;
    const rate = 0.75; /* giây / vật phẩm */
    if (st.spawnAcc > rate) { st.spawnAcc = 0; spawn(); }
    if (st.stun > 0) st.stun -= dt;

    st.x += (st.tx - st.x) * Math.min(dt * 10, 1);
    const py = H - petW * 1.12;

    for (let i = st.items.length - 1; i >= 0; i--) {
      const it = st.items[i];
      it.y += it.v * dt;
      if (it.y > H + 60) { st.items.splice(i, 1); continue; }
      if (Math.abs(it.x - st.x) < petW * 0.42 && Math.abs(it.y - (py + petW * 0.45)) < petW * 0.42) {
        st.items.splice(i, 1);
        if (it.rock) {
          st.stun = 1.2;
          st.score = Math.max(0, st.score - 1);
          Sfx.play('rock');
        } else if (st.stun <= 0) {
          st.score++;
          Sfx.play('catch');
        }
        document.getElementById('fgScore').textContent = st.score;
      }
    }

    ctx.clearRect(0, 0, W, H);
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.font = Math.round(H * 0.075) + 'px sans-serif';
    st.items.forEach(it => ctx.fillText(it.icon, it.x, it.y));
    /* thú + giỏ */
    const shake = st.stun > 0 ? Math.sin(now / 30) * 8 : 0;
    if (petImg.complete) ctx.drawImage(petImg, st.x - petW / 2 + shake, py, petW, petW);
    ctx.font = Math.round(petW * 0.55) + 'px sans-serif';
    ctx.fillText('🧺', st.x + shake, py + petW * 0.72);
    if (st.stun > 0) { ctx.font = Math.round(petW * 0.3) + 'px sans-serif'; ctx.fillText('💫', st.x + shake, py - petW * 0.12); }

    st.raf = requestAnimationFrame(frame);
  }

  function finish() {
    st.over = true;
    clearInterval(timerInt);
    cancelAnimationFrame(st.raf);
    popMode();
    const coins = st.score;
    const xp = 10 + Math.min(20, Math.floor(st.score / 2));
    Sfx.play('win');
    const praise = st.score >= 25 ? 'TUYỆT VỜI! 🏆' : st.score >= 12 ? 'Giỏi lắm! 🌟' : 'Cố lên nhé! 💪';
    endMiniGame('🍎 Hết giờ!', `${praise}<br>Em hứng được <b>${st.score}</b> trái cây!`, coins, xp, startFruitGame);
  }

  pushMode({
    name: 'fruit',
    onKey(k) {
      if (st.over) return true;
      if (k === 'left')  { st.tx = clamp(st.tx - step, petW / 2, W - petW / 2); return true; }
      if (k === 'right') { st.tx = clamp(st.tx + step, petW / 2, W - petW / 2); return true; }
      if (k === 'back') {
        showDialog({
          title: 'Thoát game?',
          text: 'Em muốn dừng chơi Hứng Trái Cây?',
          buttons: [
            { label: 'Chơi tiếp', cb: () => { st.last = performance.now(); } },
            { label: 'Thoát', cb: () => { finish(); } }
          ]
        });
        return true;
      }
      return true;
    }
  });
  st.raf = requestAnimationFrame(frame);
}

/* ============================================================
   MINI GAME 2: BÉ NHỚ GIỎI (Simon)
   ============================================================ */
function startMemoryGame() {
  clearInterval(homeTimer); clearTimeout(chatTimer);
  const app = document.getElementById('app');
  app.innerHTML = `<div class="screen game-screen memory-screen">
    <div class="game-hud">
      <div class="pill">🪙 +<span id="mgCoins">0</span></div>
      <div class="pill" style="font-size:2.4rem">🌈🧠 Bé Nhớ Giỏi</div>
      <div class="pill">Vòng <span id="mgRound">1</span>/8</div>
    </div>
    <div class="simon">
      <div class="pad up" data-d="0">☀️</div>
      <div class="pad right" data-d="1">🍓</div>
      <div class="pad down" data-d="2">🍀</div>
      <div class="pad left" data-d="3">💧</div>
      <div class="simon-center"><div class="rd" id="mgMsg">Nhìn nè!</div></div>
    </div>
    <div class="hint-bar">Bấm các nút ▲ ▶ ▼ ◀ theo đúng thứ tự nhấp nháy! • BACK Thoát</div>
  </div>`;

  const pads = ['up', 'right', 'down', 'left'].map(d => app.querySelector('.pad.' + d));
  const msg = document.getElementById('mgMsg');
  const st = { seq: [], input: 0, round: 1, coins: 0, showing: true, over: false, timers: [] };
  const MAX_ROUND = 8;
  const later = (fn, ms) => st.timers.push(setTimeout(fn, ms));

  function flash(d, dur) {
    pads[d].classList.add('lit');
    Sfx.play('pad' + d);
    later(() => pads[d].classList.remove('lit'), dur || 380);
  }

  function showSeq() {
    st.showing = true;
    msg.textContent = 'Nhìn nè!';
    st.seq.push(Math.floor(Math.random() * 4));
    const gap = Math.max(650 - st.round * 40, 420);
    st.seq.forEach((d, i) => later(() => flash(d, gap * 0.6), 800 + i * gap));
    later(() => {
      st.showing = false;
      st.input = 0;
      msg.textContent = 'Đến em!';
    }, 800 + st.seq.length * gap);
  }

  function cleanup() { st.timers.forEach(clearTimeout); st.timers = []; }

  function finish(winAll) {
    st.over = true;
    cleanup();
    popMode();
    const xp = 8 + st.coins;
    if (winAll) Sfx.play('win');
    const praise = winAll ? 'SIÊU TRÍ NHỚ! 🏆👑' :
      st.round >= 5 ? 'Trí nhớ tuyệt vời! 🌟' : st.round >= 3 ? 'Giỏi lắm! 💪' : 'Lần sau cố lên nhé! 🍀';
    endMiniGame('🌈 Kết quả!', `${praise}<br>Em nhớ được <b>${st.round - 1}</b> vòng!`,
      st.coins, xp, startMemoryGame);
  }

  pushMode({
    name: 'memory',
    onKey(k) {
      if (st.over) return true;
      if (k === 'back') {
        cleanup();
        showDialog({
          title: 'Thoát game?',
          text: 'Em muốn dừng chơi Bé Nhớ Giỏi?',
          buttons: [
            { label: 'Chơi tiếp', cb: () => { st.seq.pop(); showSeq(); } },
            { label: 'Thoát', cb: () => finish(false) }
          ]
        });
        return true;
      }
      if (st.showing) return true;
      const dir = { up: 0, right: 1, down: 2, left: 3 }[k];
      if (dir == null) return true;
      flash(dir, 260);
      if (dir === st.seq[st.input]) {
        st.input++;
        if (st.input >= st.seq.length) {
          st.coins += 3;
          document.getElementById('mgCoins').textContent = st.coins;
          Sfx.play('coin');
          if (st.round >= MAX_ROUND) { later(() => finish(true), 700); return true; }
          st.round++;
          document.getElementById('mgRound').textContent = st.round;
          msg.textContent = 'Đúng rồi! 🎉';
          st.showing = true;
          later(showSeq, 1000);
        }
      } else {
        Sfx.play('wrong');
        msg.textContent = 'Ôi sai rồi!';
        st.showing = true;
        later(() => flash(st.seq[st.input], 500), 400);
        later(() => finish(false), 1300);
      }
      return true;
    }
  });
  showSeq();
}

/* ============================================================
   TIẾN HÓA
   ============================================================ */
function showEvolution(newStage) {
  clearInterval(homeTimer); clearTimeout(chatTimer);
  /* đóng mọi hộp thoại đang mở */
  const ov = document.getElementById('overlay');
  ov.classList.remove('show');
  ov.innerHTML = '';
  Sfx.play('evolve');
  const app = document.getElementById('app');
  app.innerHTML = `<div class="screen evo-screen">
    <div class="evo-title">✨ TIẾN HÓA RỒI! ✨</div>
    <div class="evo-pet">${svgPet(S.species, newStage, 'happy', false)}</div>
    <div class="evo-name">${SPECIES[S.species].stages[newStage]}</div>
    <div class="hint-bar" style="color:#fff">Bấm OK để tiếp tục 🎉</div>
  </div>`;
  const scr = app.querySelector('.evo-screen');
  const emo = ['🎉', '⭐', '✨', '🎊', '💖', '🌟'];
  for (let i = 0; i < 26; i++) {
    const c = document.createElement('div');
    c.className = 'confetti';
    c.textContent = emo[i % emo.length];
    c.style.cssText = `left:${Math.random() * 100}%;animation-duration:${2.4 + Math.random() * 2.4}s;animation-delay:${Math.random() * 1.6}s`;
    scr.appendChild(c);
  }
  setMode({
    name: 'evolution',
    onKey(k) {
      if (k === 'ok' || k === 'back') { evolving = false; Sfx.play('select'); save(); showHome(); }
      return true;
    }
  });
}

/* ============================================================
   Vòng đời app
   ============================================================ */
window.gamePause = function () { save(); if (Sfx.ctx) Sfx.ctx.suspend(); };
window.gameResume = function () { if (Sfx.ctx) Sfx.ctx.resume(); };
window.addEventListener('beforeunload', save);

/* Khởi động */
showTitle();
