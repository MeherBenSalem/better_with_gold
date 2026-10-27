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
@font-face { font-family: RsSerif; src: url('${url(path.join(fonts, 'eb-garamond.ttf'))}'); }
@font-face { font-family: RsDisplay; src: url('${url(path.join(fonts, 'cinzel.ttf'))}'); }
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

const header = (title) => page(1400, 120, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:28px;background:linear-gradient(90deg,rgba(0,0,0,0),rgba(12,14,26,.9) 30%,rgba(12,14,26,.9) 70%,rgba(0,0,0,0))">
    <div class="rule" style="width:300px;transform:scaleX(-1)"></div>
    <span class="diamond" style="margin:0"></span>
    <div class="display gold glow" style="font-size:44px;white-space:nowrap">${title}</div>
    <span class="diamond" style="margin:0"></span>
    <div class="rule" style="width:300px"></div>
  </div>`, true);

// A class emblem: a dark diamond tile with the class colour and its pixel icon.
const emblem = (c, x, y, size) => `<div class="emblem" style="left:${x - size / 2}px;top:${y - size / 2}px;width:${size}px;height:${size}px;border-color:${c.color};box-shadow:0 0 18px ${c.color}66, 0 0 0 2px #05060b">
    <img src="${icon(c.role)}" style="left:${size * 0.14}px;top:${size * 0.14}px;width:${size * 0.72}px;height:${size * 0.72}px"></div>`;

// A feature card: copy on the left, artwork on the right. Bullets never end on a one-word line.
const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 980, backdrop = 'backdrop.jpg' }) => page(width, height, `
  <div class="backdrop" style="background-image:url('${source(backdrop)}');opacity:.55"></div>
  <div class="vignette"></div>${stars}
  <div style="position:absolute;left:80px;top:0;bottom:0;width:${width - artWidth - 150}px;display:flex;flex-direction:column;justify-content:center">
    <div class="kicker"${kicker.length > 24 ? ' style="font-size:18px;letter-spacing:.12em"' : ''}><span class="diamond"></span>${kicker}</div>
    <div class="display gold glow" style="font-size:58px;line-height:1.08;margin:18px 0 22px">${title}</div>
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
// Two overlapping captures.
const pair = (first, second, firstLabel, secondLabel, firstSpot = 'left:20px;top:20px', height = 700) => `<div style="position:relative;height:${height}px">
    <div class="frame" style="position:absolute;left:0;top:0;width:74%"><img src="${shot(first)}" style="display:block;width:100%"></div>
    <div class="frame" style="position:absolute;right:0;bottom:0;width:74%"><img src="${shot(second)}" style="display:block;width:100%"></div>
    ${label(firstLabel, firstSpot)}${label(secondLabel, 'right:20px;bottom:20px')}</div>`;
// A 2x2 grid of labelled action shots.
const grid = (items) => `<div style="display:grid;grid-template-columns:1fr 1fr;gap:18px">${items.map(([file, name, sub, color]) =>
  framed(`<img src="${shot(file)}">` + label(name, 'left:12px;bottom:12px;font-size:17px', sub), '', color)).join('')}</div>`;

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
