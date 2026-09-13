// G34O final export: trim chords to r27 circle analytically, scale 100→108 viewport, emit XML + PNG geometry
const fs = require('fs');
const OUT = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/outputs/DraftPeek-icon-export-g34o';
['res/drawable', 'res/mipmap-anydpi', 'res/values', 'res/values-night', 'store-png'].forEach(d => fs.mkdirSync(OUT + '/' + d, { recursive: true }));

const C = 50, R = 27, K = 1.08;
const INK = '#1C2B3A', MIST = '#33475F', BLUE = '#3178C6', RED = '#C41E3A', BG = '#F5F7FA';
// raw chords (100-grid, from mk-g34o): [x1,y1,x2,y2,color]
const RAW = [
  [63.1, 78.3, 75.6, 7.4, MIST], [50.1, 77.4, 86.1, 15.1, MIST], [38.2, 72.2, 93.3, 25.9, MIST],
  [28.8, 63.2, 96.4, 38.6, BLUE], [23.0, 51.5, 95.0, 51.5, INK], [21.6, 38.6, 89.2, 63.2, INK],
  [24.7, 25.9, 79.8, 72.2, INK], [31.9, 15.1, 67.9, 77.4, INK], [42.4, 7.4, 54.9, 78.3, INK],
];
// trim segment to circle radius R about (C,C)
function trim(x1, y1, x2, y2) {
  const dx = x2 - x1, dy = y2 - y1;
  const fx = x1 - C, fy = y1 - C;
  const a = dx * dx + dy * dy, b = 2 * (fx * dx + fy * dy), cc = fx * fx + fy * fy - R * R;
  const disc = b * b - 4 * a * cc;
  if (disc < 0) return null;
  const sq = Math.sqrt(disc);
  let t0 = (-b - sq) / (2 * a), t1 = (-b + sq) / (2 * a);
  t0 = Math.max(0, t0); t1 = Math.min(1, t1);
  if (t1 <= t0) return null;
  return [x1 + t0 * dx, y1 + t0 * dy, x1 + t1 * dx, y1 + t1 * dy];
}
const chords = RAW.map(([x1, y1, x2, y2, col]) => {
  const t = trim(x1, y1, x2, y2);
  if (!t) { console.error('chord fully outside circle!'); process.exit(1); }
  return { x1: t[0] * K, y1: t[1] * K, x2: t[2] * K, y2: t[3] * K, col };
});
const SW = (3.6 * K).toFixed(2);
const EYE = { cx: 59 * K, cy: 41 * K, r: 4 * K };

function paths(colorOverride) {
  return chords.map(c => `    <path android:pathData="M${c.x1.toFixed(2)},${c.y1.toFixed(2)}L${c.x2.toFixed(2)},${c.y2.toFixed(2)}" android:strokeColor="${colorOverride || c.col}" android:strokeWidth="${SW}" android:strokeLineCap="round"/>`).join('\n');
}
const eyePath = colorOverride => `    <path android:pathData="M${(EYE.cx - EYE.r).toFixed(2)},${EYE.cy.toFixed(2)}a${EYE.r.toFixed(2)},${EYE.r.toFixed(2)} 0 1,0 ${(2 * EYE.r).toFixed(2)},0a${EYE.r.toFixed(2)},${EYE.r.toFixed(2)} 0 1,0 ${(-2 * EYE.r).toFixed(2)},0" android:fillColor="${colorOverride || RED}"/>`;

const header = (desc) => `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${desc} -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n`;
fs.writeFileSync(OUT + '/res/drawable/ic_launcher_foreground.xml',
  header('DraftPeek launcher foreground · G34O 偏心窥圆(定稿):九弦裁入 r27 圆、雾墨三级色阶 + 电光蓝强调弦 + 朱砂红眼') + paths() + '\n' + eyePath() + '\n</vector>\n');
fs.writeFileSync(OUT + '/res/drawable/ic_launcher_monochrome.xml',
  header('DraftPeek monochrome · G34O(系统着色,仅保留轮廓)') + paths('#FF000000') + '\n' + eyePath('#FF000000') + '\n</vector>\n');

// adaptive + background unchanged in project, but ship copies for completeness
const adaptive = `<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ic_launcher_background"/>\n    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n</adaptive-icon>\n`;
fs.writeFileSync(OUT + '/res/mipmap-anydpi/ic_launcher.xml', adaptive);
fs.writeFileSync(OUT + '/res/mipmap-anydpi/ic_launcher_round.xml', adaptive);
const bg = `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#F5F7FA</color>\n</resources>\n`;
fs.writeFileSync(OUT + '/res/values/ic_launcher_background.xml', bg);
fs.writeFileSync(OUT + '/res/values-night/ic_launcher_background.xml', bg);

fs.writeFileSync(OUT + '/geometry.json', JSON.stringify({
  size: 100, bg: BG, sw: 3.6,
  chords: chords.map(c => ({ x1: c.x1 / K, y1: c.y1 / K, x2: c.x2 / K, y2: c.y2 / K, col: c.col })),
  eye: { cx: 59, cy: 41, r: 4, col: RED }
}));
console.log('G34O XML written; chords trimmed to r27 (max endpoint radius):');
let mx = 0; chords.forEach(c => { for (const [x, y] of [[c.x1, c.y1], [c.x2, c.y2]]) mx = Math.max(mx, Math.hypot(x / K - 50, y / K - 50)); });
console.log('  =', mx.toFixed(2), 'u (limit 27 + stroke/2 1.8 = 28.8 ✓)');
