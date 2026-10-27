// Renders the listing artwork in marketing/images/ from real in-game screenshots, the mod's own pixel art and the
// bundled OFL fonts.
// Usage (from the repository root): NODE_PATH="$(npm root -g)" node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build available.
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

// The release and the supported Minecraft versions. Adding a version is one line here
// (plus one row in the compatibility table of curseforge/description.md).
const RELEASE = { version: '2.0', name: 'Remnants | Fellowship' };
const VERSIONS = [
  { minecraft: '1.20.1', loaders: ['Fabric', 'Forge'] },
  { minecraft: '1.21.1', loaders: ['Fabric', 'NeoForge', 'Forge'] },
  { minecraft: '26.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.3', loaders: ['Fabric', 'NeoForge'] },
];
const LOADERS = ['Fabric', 'Forge', 'NeoForge'].filter((loader) => VERSIONS.some((v) => v.loaders.includes(loader)));

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));
const source = (name) => url(path.join(__dirname, name));
const icon = (name) => source(`icons/icon-${name}.png`);

// Class colours match the default class files (common/.../core/ClassCatalog.java).
const classes = [
  { id: 'bulwark', name: 'Bulwark', role: 'tank', color: '#4F8FE0' },
  { id: 'skirmisher', name: 'Skirmisher', role: 'melee', color: '#D9483B' },
  { id: 'ranger', name: 'Ranger', role: 'ranged', color: '#5DBB63' },
  { id: 'mender', name: 'Mender', role: 'healer', color: '#F2C94C' },
  { id: 'arcanist', name: 'Arcanist', role: 'caster', color: '#A06BE0' },
];

const css = `
@font-face { font-family: RsSerif; src: url('${url(path.join(fonts, 'eb-garamond.ttf'))}'); font-weight: 400 800; }
@font-face { font-family: RsDisplay; src: url('${url(path.join(fonts, 'cinzel.ttf'))}'); font-weight: 400 900; }
:root { --text:#dcd8e6; --dim:#9894a8; --header:#efe9d8; --gold:#e8c872; --gold-hi:#f6dc94; --bg:#07080f; --line:rgba(232,200,114,.35); }
* { box-sizing: border-box; }
html, body { margin: 0; background: var(--bg); color: var(--text); font-family: RsSerif, serif; }
img.px { image-rendering: pixelated; }
.stage { position: relative; overflow: hidden; background: radial-gradient(ellipse at 30% 40%, #151a2e 0%, #07080f 70%); }
.display { font-family: RsDisplay, serif; letter-spacing: .04em; }
.gold { background: linear-gradient(180deg, #fff4cf 0%, #f6dc94 35%, #e8c872 70%, #9a7a3c 100%);
        -webkit-background-clip: text; background-clip: text; color: transparent; }
.glow { filter: drop-shadow(0 0 18px rgba(232,200,114,.35)); }
.kicker { font-family: RsDisplay, serif; color: var(--gold); letter-spacing: .22em; font-size: 22px; }
.rule { height: 1px; background: linear-gradient(90deg, rgba(232,200,114,0), rgba(232,200,114,.9) 20%, rgba(232,200,114,.9) 70%, rgba(232,200,114,0)); }
.diamond { display: inline-block; width: 9px; height: 9px; background: var(--gold); transform: rotate(45deg); margin: 0 12px 3px 0; }
.frame { position: relative; border: 1px solid rgba(232,200,114,.45); box-shadow: 0 0 0 1px rgba(0,0,0,.6), 0 30px 80px rgba(0,0,0,.65), 0 0 60px rgba(255,190,90,.10); background: #000; }
.frame > img { display: block; width: 100%; }
canvas.stars { position: absolute; left: 0; top: 0; width: 100%; height: 100%; pointer-events: none; }
.vignette { position: absolute; inset: 0; background: radial-gradient(ellipse at center, rgba(0,0,0,0) 45%, rgba(0,0,0,.78) 100%); }
.backdrop { position: absolute; inset: -40px; background-size: cover; background-position: center; filter: blur(14px) brightness(.45) saturate(1.2); }
.pill { display: inline-block; border: 1px solid rgba(232,200,114,.55); padding: 6px 14px 4px; margin-right: 10px;
        font-family: RsDisplay, serif; font-size: 18px; letter-spacing: .12em; color: var(--header); background: rgba(0,0,0,.35); }
.label { position: absolute; font-family: RsDisplay, serif; font-size: 20px; letter-spacing: .14em; color: var(--header);
         background: rgba(8,9,16,.88); border: 1px solid rgba(232,200,114,.8); padding: 7px 14px 5px; white-space: nowrap;
         box-shadow: 0 8px 26px rgba(0,0,0,.6); z-index: 2; }
.label small { display: block; font-family: RsSerif, serif; letter-spacing: 0; font-size: 18px; color: var(--dim); margin-top: 1px; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 26px; margin: 0 0 14px; font-size: 27px; line-height: 1.32; color: var(--text); }
ul.points li::before { content: ''; position: absolute; left: 2px; top: 13px; width: 8px; height: 8px; background: var(--gold); transform: rotate(45deg); }
.stats { display: flex; margin-top: 22px; border: 1px solid rgba(232,200,114,.35); background: rgba(8,9,16,.85); box-shadow: 0 18px 50px rgba(0,0,0,.55); }
.stats > div { flex: 1; padding: 14px 24px 12px; border-left: 1px solid rgba(232,200,114,.2); }
.stats > div:first-child { border-left: 0; }
.stats .t { font-family: RsDisplay, serif; font-size: 16px; letter-spacing: .18em; color: var(--dim); }
.stats .v { font-family: RsDisplay, serif; font-size: 30px; color: var(--header); margin-top: 4px; white-space: nowrap; }
b.hi { color: var(--header); font-weight: normal; }
.emblem { position: absolute; transform: rotate(45deg); background: linear-gradient(160deg, #1d2338, #0a0b12); border: 3px solid; box-shadow: 0 0 22px rgba(0,0,0,.8); }
.emblem img { position: absolute; transform: rotate(-45deg); image-rendering: pixelated; }
`;

// A deterministic starfield with a few gold sparks, so renders are reproducible.
const stars = `<canvas class="stars"></canvas><script>
(() => { const c = document.querySelector('canvas.stars'); const r = c.getBoundingClientRect();
  c.width = r.width; c.height = r.height; const g = c.getContext('2d'); let s = 23;
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  for (let i = 0; i < 120; i++) { const x = rnd() * c.width, y = rnd() * c.height, a = .2 + rnd() * .6, z = .8 + rnd() * 1.8;
    const gold = rnd() < .35, col = gold ? '246,220,148' : '190,205,255';
    const h = g.createRadialGradient(x, y, 0, x, y, z * 5); h.addColorStop(0, 'rgba(' + col + ',' + a * .5 + ')'); h.addColorStop(1, 'rgba(' + col + ',0)');
    g.fillStyle = h; g.beginPath(); g.arc(x, y, z * 5, 0, Math.PI * 2); g.fill();
    g.fillStyle = 'rgba(' + col + ',' + a + ')'; g.fillRect(x - z / 2, y - z / 2, z, z); } })();
</script>`;

const page = (width, height, body, transparent = false) => `<!doctype html><html><head><meta charset="utf-8"><style>${css}
body { width: ${width}px; height: ${height}px; } .stage { width: ${width}px; height: ${height}px; }
${transparent ? 'html, body, .stage { background: transparent; }' : ''}</style></head>
<body><div class="stage">${body}</div></body></html>`;

const header = (title, long = title.length > 20) => page(1400, 120, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:22px;background:linear-gradient(90deg,rgba(0,0,0,0),rgba(14,12,20,.92) 26%,rgba(14,12,20,.92) 74%,rgba(0,0,0,0))">
    <div style="position:absolute;left:18%;right:18%;top:14px;height:1px;background:linear-gradient(90deg,rgba(232,200,114,0),rgba(232,200,114,.45),rgba(232,200,114,0))"></div>
    <div style="position:absolute;left:18%;right:18%;bottom:14px;height:1px;background:linear-gradient(90deg,rgba(232,200,114,0),rgba(232,200,114,.45),rgba(232,200,114,0))"></div>
    <div style="display:flex;flex-direction:column;gap:5px;transform:scaleX(-1)"><div class="rule" style="width:${long ? 190 : 290}px;height:2px"></div><div class="rule" style="width:${long ? 130 : 200}px;opacity:.6"></div></div>
    <span class="diamond" style="margin:0;width:12px;height:12px;box-shadow:0 0 12px rgba(246,220,148,.8)"></span>
    <div class="display gold glow" style="font-size:${long ? 42 : 50}px;font-weight:700;white-space:nowrap;letter-spacing:.06em;filter:drop-shadow(0 2px 0 #000) drop-shadow(0 0 18px rgba(232,200,114,.45))">${title}</div>
    <span class="diamond" style="margin:0;width:12px;height:12px;box-shadow:0 0 12px rgba(246,220,148,.8)"></span>
    <div style="display:flex;flex-direction:column;gap:5px"><div class="rule" style="width:${long ? 190 : 290}px;height:2px"></div><div class="rule" style="width:${long ? 130 : 200}px;opacity:.6"></div></div>
  </div>`, true);

// A class emblem: a dark diamond tile with the class colour and its pixel icon.
const emblem = (c, x, y, size) => `<div class="emblem" style="left:${x - size / 2}px;top:${y - size / 2}px;width:${size}px;height:${size}px;border-color:${c.color};box-shadow:0 0 18px ${c.color}66, 0 0 0 2px #05060b">
    <img src="${icon(c.role)}" style="left:${size * 0.14}px;top:${size * 0.14}px;width:${size * 0.72}px;height:${size * 0.72}px"></div>`;

// The four formations, slot for slot as common/.../core/Formation.java places them (+z the way the anchor faces,
// +x its left), for one ally of each class in role-rank order: Bulwark, Skirmisher, Mender, Ranger, Arcanist.
const SPACING = 1.6;
const RANK_ORDER = [0, 1, 3, 2, 4]; // indices into classes[]
const formationSlots = (name, n = 5) => {
  const out = [];
  if (name === 'Line') for (let i = 0; i < n; i++) out.push([(i - (n - 1) / 2) * SPACING, -2]);
  if (name === 'Wedge') for (let i = 0; i < n; i++) {
    const step = Math.floor((i + 1) / 2), side = i % 2 ? 1 : -1;
    out.push(i ? [side * step * SPACING * 0.9, -2 - step * SPACING * 0.9] : [0, -2]);
  }
  if (name === 'Shield Wall') { // tanks and melee in front, everyone else behind
    const front = [0, 1], back = [2, 3, 4];
    front.forEach((i, k) => { out[i] = [(k - (front.length - 1) / 2) * 1.2, 2.5]; });
    back.forEach((i, k) => { out[i] = [(k - (back.length - 1) / 2) * SPACING, -1.8]; });
  }
  if (name === 'Circle') {
    const r = Math.max(2.5, n * SPACING / (2 * Math.PI));
    for (let i = 0; i < n; i++) { const a = 2 * Math.PI * i / n; out.push([Math.sin(a) * r, Math.cos(a) * r]); }
  }
  return out;
};
// A top-down tile of one formation on a one-block grid: the anchor (you, or the planted Warbanner) faces up.
const formationPanel = (name, w = 233, h = 252, S = 21) => {
  const cx = w / 2, cy = 96;
  const allies = formationSlots(name).map(([x, z], i) => emblem(classes[RANK_ORDER[i]], cx - x * S, cy - z * S, 27)).join('');
  return `<div class="frame" style="position:relative;width:${w}px;height:${h}px;overflow:hidden;background:
      radial-gradient(ellipse at 50% 42%,rgba(232,200,114,.10),rgba(0,0,0,0) 62%),
      linear-gradient(rgba(232,200,114,.08) 1px,transparent 1px),linear-gradient(90deg,rgba(232,200,114,.08) 1px,transparent 1px),#0a0c16;
      background-size:auto,${S}px ${S}px,${S}px ${S}px,auto;background-position:0 0,${cx % S}px ${cy % S}px,${cx % S}px ${cy % S}px,0 0">
    <div style="position:absolute;left:${cx - 7}px;top:${cy - 29}px;width:0;height:0;border-left:7px solid transparent;border-right:7px solid transparent;border-bottom:10px solid var(--gold-hi);filter:drop-shadow(0 0 6px rgba(246,220,148,.8))"></div>
    <img class="px" src="${source('warbanner.png')}" style="position:absolute;left:${cx - 15}px;top:${cy - 15}px;width:30px;height:30px;filter:drop-shadow(0 0 8px rgba(246,220,148,.7)) drop-shadow(0 3px 3px #000)">
    ${allies}
    <div class="display" style="position:absolute;left:0;right:0;bottom:12px;text-align:center;font-size:20px;letter-spacing:.14em;color:var(--header)">${name.toUpperCase()}</div>
  </div>`;
};
const formations = () => `<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px">${['Line', 'Wedge', 'Shield Wall', 'Circle'].map((f) => formationPanel(f)).join('')}</div>`;

// A feature card: copy on the left, artwork on the right. Bullets never end on a one-word line.
const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 980, backdrop = 'backdrop.jpg' }) => page(width, height, `
  <div class="backdrop" style="background-image:url('${source(backdrop)}');opacity:.55"></div>
  <div class="vignette"></div>${stars}
  <div style="position:absolute;left:80px;top:0;bottom:0;width:${width - artWidth - 150}px;display:flex;flex-direction:column;justify-content:center">
    <div class="kicker"${kicker.length > 24 ? ' style="font-size:18px;letter-spacing:.12em"' : ''}><span class="diamond"></span>${kicker}</div>
    <div class="display gold glow" style="font-size:52px;line-height:1.1;margin:18px 0 22px">${title.replace(/ (\S+)$/, '&nbsp;$1')}</div>
    <div class="rule" style="width:80%;margin-bottom:30px"></div>
    <ul class="points">${points.map((point) => `<li>${point.replace(/ (\S+)$/, '&nbsp;$1')}</li>`).join('')}</ul>
  </div>
  <div style="position:absolute;right:60px;top:50%;transform:translateY(-50%);width:${artWidth}px">${art}</div>`);

// An exact region (x, y, w, h in 1920x1080 pixels) of a capture, scaled to W pixels wide.
const region = (file, x, y, w, h, W) => `<div style="position:relative;width:${W}px;height:${Math.round(W * h / w)}px;overflow:hidden">
    <img src="${shot(file)}" style="position:absolute;max-width:none;width:${W * 1920 / w}px;left:${-x * W / w}px;top:${-y * W / w}px"></div>`;
const framed = (inner, style = '', color = '') => `<div class="frame" style="${color ? `border-color:${color}aa;` : ''}${style}">${inner}</div>`;
const label = (text, style, small = '') => `<div class="label" style="${style}">${text}${small ? `<small>${small}</small>` : ''}</div>`;
const stats = (items, color) => `<div class="stats"${color ? ` style="border-top:2px solid ${color}"` : ''}>${items.map(([t, v, flex]) =>
  `<div${flex ? ` style="flex:${flex}"` : ''}><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;
// Two overlapping captures, each a whole screenshot or a region of one ({ file, x, y, w, h }, kept at 16:9).
const pic = (p, W) => (typeof p === 'string' ? `<img src="${shot(p)}" style="display:block;width:100%">` : region(p.file, p.x, p.y, p.w, p.h, W));
const pair = (first, second, firstLabel, secondLabel, firstSpot = 'left:20px;top:20px', height = 640, W = 652) => `<div style="position:relative;height:${height}px">
    <div class="frame" style="position:absolute;left:0;top:0;width:${W}px">${pic(first, W)}</div>
    <div class="frame" style="position:absolute;right:0;bottom:0;width:${W}px">${pic(second, W)}</div>
    ${label(firstLabel, firstSpot)}${label(secondLabel, 'right:20px;bottom:20px')}</div>`;
// A grid of labelled action shots (two columns unless told otherwise).
const grid = (items, columns = 2, width = 1000) => {
  const gap = columns > 2 ? 14 : 18, W = Math.floor((width - gap * (columns - 1)) / columns);
  return `<div style="display:grid;grid-template-columns:repeat(${columns},1fr);gap:${gap}px">${items.map(([file, name, sub, color]) =>
    framed(pic(file, W) + label(name, 'left:12px;bottom:12px;font-size:17px', sub), '', color)).join('')}</div>`;
};

const versionText = VERSIONS.map((v) => v.minecraft).join(' · ');

const assets = {
  // The project avatar: the Warbanner raised over the five classes, inside a gold diamond. No ring.
  icon: () => page(512, 512, `
    <div style="position:absolute;inset:0;background:radial-gradient(circle at 50% 44%,#3a2a2a 0%,#191726 40%,#07080f 78%)"></div>
    <div style="position:absolute;left:96px;top:70px;width:320px;height:320px;transform:rotate(45deg);border:3px solid var(--gold);
      box-shadow:0 0 44px rgba(232,200,114,.42), inset 0 0 60px rgba(232,200,114,.20)"></div>
    <div style="position:absolute;left:111px;top:85px;width:290px;height:290px;transform:rotate(45deg);border:1px solid rgba(232,200,114,.45)"></div>
    <div style="position:absolute;left:256px;top:214px;width:320px;height:320px;margin:-160px;background:radial-gradient(circle,rgba(255,214,140,.32),rgba(0,0,0,0) 60%)"></div>
    <img class="px" src="${source('warbanner.png')}" style="position:absolute;left:160px;top:110px;width:192px;height:192px;filter:drop-shadow(0 10px 16px rgba(0,0,0,.85))">
    ${[[0, 118, 322], [1, 190, 392], [2, 256, 446], [3, 322, 392], [4, 394, 322]].map(([i, x, y]) => emblem(classes[i], x, y, 54)).join('')}`),

  divider: () => page(1400, 40, `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:18px">
    <div class="rule" style="width:520px;transform:scaleX(-1)"></div><span class="diamond" style="margin:0"></span><div class="rule" style="width:520px"></div></div>`, true),
};

// A code panel for the configuration card: the real generated file, a few lines highlighted.
const code = (title, text, hot = []) => `<div class="frame" style="background:#0b0c14;padding:22px 26px;border-color:rgba(232,200,114,.55)">
    <div class="kicker" style="font-size:15px;margin-bottom:12px">${title}</div>
    <pre style="margin:0;font:19px/1.42 'DejaVu Sans Mono',monospace;color:#cfd3e4;white-space:pre">${text.split('\n').map((l) =>
      hot.some((h) => l.includes(h)) ? `<span style="color:#f6dc94;background:rgba(232,200,114,.12)">${l}</span>` : l).join('\n')}</pre></div>`;
const config = fs.readFileSync(path.join(root, 'docs', 'reference', 'fellowship.json'), 'utf8').trim();
const bulwark = JSON.parse(fs.readFileSync(path.join(root, 'docs', 'reference', 'classes', 'bulwark.json'), 'utf8'));

Object.assign(assets, {
  // The listing's top image: the warband at sunset, an Arc Storm on the raiders, the name and the promise.
  banner: () => page(1600, 520, `
    <div style="position:absolute;inset:0;background:radial-gradient(ellipse at 78% 40%,#2c1e14 0%,#0b0a10 72%)"></div>
    <img src="${shot('battle-storm.png')}" style="position:absolute;left:560px;top:-150px;width:1600px;
      -webkit-mask-image:linear-gradient(90deg,transparent 0,rgba(0,0,0,.4) 16%,#000 40%);mask-image:linear-gradient(90deg,transparent 0,rgba(0,0,0,.4) 16%,#000 40%)">
    <div style="position:absolute;inset:0;background:linear-gradient(90deg,rgba(7,8,15,.94) 0%,rgba(7,8,15,.86) 30%,rgba(7,8,15,.25) 58%,rgba(7,8,15,0) 74%)"></div>
    <div class="vignette" style="background:radial-gradient(ellipse at 72% 45%,rgba(0,0,0,0) 46%,rgba(0,0,0,.55) 100%)"></div>
    <img src="${url(path.join(out, 'icon.png'))}" style="position:absolute;left:70px;top:132px;width:256px;height:256px;border-radius:30px;box-shadow:0 18px 50px rgba(0,0,0,.7),0 0 50px rgba(232,200,114,.25)">
    <div style="position:absolute;left:366px;top:116px;width:780px">
      <div class="kicker" style="font-size:26px;letter-spacing:.32em;display:flex;align-items:center;gap:18px">REMNANTS<span style="width:2px;height:26px;background:var(--gold)"></span></div>
      <div class="display gold glow" style="font-size:104px;font-weight:900;line-height:1;margin:8px 0 18px;filter:drop-shadow(0 4px 0 #000) drop-shadow(0 0 24px rgba(232,200,114,.45))">FELLOWSHIP</div>
      <div class="rule" style="width:640px;height:2px;margin-bottom:20px"></div>
      <div style="font-size:38px;font-style:italic;font-weight:600;color:var(--header);text-shadow:0 3px 0 #000">Hire a warband of sellswords. Lead it.</div>
      <div style="margin-top:22px">${LOADERS.map((l) => `<span class="pill">${l}</span>`).join('')}<span class="pill">${versionText}</span></div>
    </div>`),

  'card-warband': () => card({ kicker: 'SELLSWORDS FOR HIRE', title: 'Hire your warband',
    points: ['Sellswords wait near village bells: name, class, level and price over their heads.',
             'Pay in gold ingots: 7 to 12 at level 1, plus 2 or 3 per level.',
             'Up to 6 allies by default, each with one or two of eleven traits.',
             'They level to 20 and earn titles: Veteran, Champion, Hero, Legend.'],
    art: pair({ file: 'hire-sellsword.png', x: 300, y: 110, w: 1400, h: 788 }, { file: 'warband.png', x: 160, y: 100, w: 1600, h: 900 },
      'For hire', 'Your warband') }),

  'card-classes': () => card({ kicker: 'FIVE CLASSES', title: 'Each fights its own way', artWidth: 960,
    points: ['<b class="hi">Bulwark</b>: Challenge taunts every enemy within 8 blocks.',
             '<b class="hi">Skirmisher</b>: Whirl hits everything within 3 blocks.',
             '<b class="hi">Ranger</b>: a three-arrow Volley every 10 seconds.',
             '<b class="hi">Arcanist</b>: Arc Storm strikes the whole pack.',
             '<b class="hi">Mender</b>: heals the most hurt ally and revives the downed.'],
    // Each tile a 16:9 region (1280x720) of its capture, centred on the ability.
    art: grid([[{ file: 'taunt.png', x: 640, y: 360, w: 1280, h: 720 }, 'Bulwark', 'Challenge', classes[0].color],
               [{ file: 'whirl.png', x: 560, y: 340, w: 1280, h: 720 }, 'Skirmisher', 'Whirl', classes[1].color],
               [{ file: 'volley.png', x: 480, y: 160, w: 1280, h: 720 }, 'Ranger', 'Volley', classes[2].color],
               [{ file: 'arc-storm.png', x: 320, y: 60, w: 1280, h: 720 }, 'Arcanist', 'Arc Storm', classes[4].color],
               [{ file: 'mend.png', x: 400, y: 20, w: 1280, h: 720 }, 'Mender', 'Mend', classes[3].color],
               [{ file: 'warband.png', x: 320, y: 60, w: 1280, h: 720 }, 'The warband', 'All five together', '']], 2, 960) }),

  'card-command': () => card({ kicker: 'HOLD G', title: 'Command the band',
    points: ['A command wheel: Follow, Charge, Focus, Hold, Fall back, Guard banner, Formation, Recall.',
             'Four formations around you or your planted Warbanner: Line, Wedge, Shield Wall and Circle.',
             'Party frames show every ally’s class, health and level.'],
    art: `${framed(`<img src="${shot('command-wheel.png')}">` + label('The command wheel', 'left:16px;bottom:16px'))}
      <div style="margin-top:18px">${formations()}</div>` }),

  'card-aggro': () => card({ kicker: 'AGGRO & TAUNTS', title: 'Aggro you can steer',
    points: ['Enemies attack whoever holds the most aggro, and keep their target until someone beats it by 10%.',
             'The Bulwark’s Challenge pulls them off everyone else.',
             'A label over every engaged enemy shows who it is after.'],
    art: pair({ file: 'aggro-labels.png', x: 420, y: 150, w: 1280, h: 720 }, 'taunt.png', 'Who it is after', 'Challenge') }),

  'card-downed': () => card({ kicker: 'DOWNED, NOT DEAD', title: 'Pull them back up',
    points: ['At zero health an ally collapses instead of dying and bleeds out over 30 seconds.',
             'Crouch beside it for 3 seconds to revive it. A Mender does it in 5.',
             'Bled out? It recovers at camp for 5 minutes. Permadeath is a switch.'],
    art: pair({ file: 'downed.png', x: 560, y: 405, w: 1200, h: 675 }, { file: 'revive.png', x: 480, y: 0, w: 1440, h: 810 },
      'Downed', 'Revived') }),

  'card-banner': () => card({ kicker: 'THE WARBANNER', title: 'Plant it, hold it',
    points: ['Craft it from 3 gold ingots, a black banner and a stick.',
             'Plant it and the band forms up around it in your formation and guards it.',
             'Lift it and they follow you again.'],
    art: pair({ file: 'warbanner-planted.png', x: 200, y: 0, w: 1600, h: 900 }, { file: 'warbanner-guard.png', x: 260, y: 40, w: 1400, h: 788 },
      'Planted', 'Guarded') }),

  'card-roster': () => card({ kicker: 'ROSTER & CHRONICLE', title: 'Every ally has a story', artWidth: 1040,
    points: ['Press U: live portraits, health, experience, morale, kills, bosses, revives and healing.',
             'The Chronicle is written from what actually happened.',
             'Send allies to camp, summon or dismiss them.'],
    art: framed(region('roster.png', 252, 102, 1416, 876, 1040)) }),

  'card-config': () => card({ kicker: 'SERVER OWNERS', title: 'Your rules', artWidth: 1000,
    points: ['Every number is in <span style="color:var(--header)">fellowship.json</span>, in the config/better_with_gold folder.',
             'Classes are JSON files: change them or add your own.',
             '/fellowship reload applies changes without a restart.'],
    art: `<div style="display:grid;grid-template-columns:1.05fr 1fr;gap:18px;align-items:start">
      ${code('fellowship.json', config, ['partySize', 'bleedOutSeconds', 'permadeath', 'currency', 'aggroLabels'])}
      ${code('classes/bulwark.json', JSON.stringify({ id: bulwark.id, name: bulwark.name, role: bulwark.role, baseHealth: bulwark.baseHealth,
        healthPerLevel: bulwark.healthPerLevel, armor: bulwark.armor, price: bulwark.price, abilityName: bulwark.abilityName,
        abilityRadius: bulwark.abilityRadius, abilityPower: bulwark.abilityPower, abilityCooldown: bulwark.abilityCooldown }, null, 2), ['abilityName', 'abilityRadius'])}
    </div>` }),
});

const headers = { 'header-warband': 'Your Warband', 'header-classes': 'Five Classes', 'header-command': 'Command',
  'header-aggro': 'Aggro & Taunts', 'header-downed': 'Downed, Not Dead', 'header-banner': 'The Warbanner',
  'header-roster': 'Roster & Chronicle', 'header-travel': 'Never Lost', 'header-gold': 'Gold & Gilded Regalia',
  'header-config': 'Server Owners', 'header-compatibility': 'Downloads & Compatibility', 'header-faq': 'Questions',
  'header-gallery': 'Gallery' };
for (const [name, title] of Object.entries(headers)) assets[name] = () => header(title);

module.exports = { assets, page, card, region, framed, label, stats, pair, grid, shot, source, icon, classes, emblem, stars, versionText, LOADERS, RELEASE };

if (require.main === module) {
  (async () => {
    const wanted = process.argv.slice(2);
    const browser = await chromium.launch();
    fs.mkdirSync(out, { recursive: true });
    for (const [name, build] of Object.entries(assets)) {
      if (wanted.length && !wanted.includes(name)) continue;
      const html = build();
      const size = html.match(/body \{ width: (\d+)px; height: (\d+)px; \}/);
      const tab = await browser.newPage({ viewport: { width: +size[1], height: +size[2] }, deviceScaleFactor: 1 });
      const file = path.join(__dirname, `.render-${name}.html`);
      fs.writeFileSync(file, html);
      await tab.goto(url(file));
      await tab.evaluate(() => document.fonts.ready);
      await tab.waitForLoadState('networkidle');
      await tab.screenshot({ path: path.join(out, `${name}.png`), omitBackground: name === 'divider' || name.startsWith('header-') });
      fs.unlinkSync(file);
      await tab.close();
      console.log('rendered', name);
    }
    await browser.close();
  })().catch((error) => { console.error(error); process.exit(1); });
}
