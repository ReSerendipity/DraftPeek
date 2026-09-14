// v9.8: four more curve variants — offset ring, cardioid, tapered S ribbon, drifting ripples
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const px = (cx, cy, r, a) => (cx + r * Math.cos(rad(a))).toFixed(1);
const py = (cx, cy, r, a) => (cy + r * Math.sin(rad(a))).toFixed(1);

// --- G44 cardioid: r = a(1+cos t), a=13.5, cusp at left center, 90 samples ---
let cPts = [];
for (let i = 0; i <= 90; i++) {
  const t = (i / 90) * 2 * Math.PI;
  const rr = 13.5 * (1 + Math.cos(t));
  cPts.push(`${(50 + rr * Math.cos(t - rad(0))).toFixed(1)} ${(50 + rr * Math.sin(t)).toFixed(1)}`);
}
const g44path = 'M' + cPts.join('L') + 'Z';

// --- G45 tapered S ribbon: two semicircle halves, width 2→8→2, offset polygon baked ---
const sp = []; // sample S path: top semicircle (38,37) r13 from 90→270 going CCW? build: right half down
// S path: start (51,24) arc center (38,37) r13 sweep CW to (38,50); then arc center (62,63) r13 CW to (49,76)?? simpler: two semicircles:
// seg1: center (38,37), r13, from a=0 to a=180 (through top): points
const samp = [];
for (let i = 0; i <= 30; i++) { const a = 180 - (i / 30) * 180; samp.push([38 + 13 * Math.cos(rad(a)), 37 - 13 * Math.sin(rad(a))]); }
for (let i = 0; i <= 30; i++) { const a = 0 + (i / 30) * 180; samp.push([62 + 13 * Math.cos(rad(a)), 63 + 13 * Math.sin(rad(a))]); }
// width profile: 2 at ends, 8 at middle (total samples 62; middle ~ index 31)
const n = samp.length;
const wid = samp.map((_, i) => 2 + 6 * Math.sin(Math.PI * i / (n - 1)));
// normals: tangent from neighbors, normal = perpendicular
const L = [], R = [];
for (let i = 0; i < n; i++) {
  const p0 = samp[Math.max(0, i - 1)], p1 = samp[Math.min(n - 1, i + 1)];
  let tx = p1[0] - p0[0], ty = p1[1] - p0[1];
  const len = Math.hypot(tx, ty) || 1; tx /= len; ty /= len;
  L.push([samp[i][0] - ty * wid[i] / 2, samp[i][1] + tx * wid[i] / 2]);
  R.push([samp[i][0] + ty * wid[i] / 2, samp[i][1] - tx * wid[i] / 2]);
}
const poly = pts => 'M' + pts.map(p => p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join('L');
const g45path = poly(L) + poly(R.slice().reverse()).replace('M', 'L') + 'Z';

const newSymbols = `
  <!-- G43 错位环:双圆心相距 8u 的两半弧,拼成被轻微掰弯的圆 -->
  <symbol id="mk-g43" viewBox="0 0 100 100">
    <path d="M${px(46, 50, 23, 100)} ${py(46, 50, 23, 100)}A23 23 0 0 1 ${px(46, 50, 23, 260)} ${py(46, 50, 23, 260)}" fill="none" stroke="var(--s1)" stroke-width="6.5" stroke-linecap="round"/>
    <path d="M${px(54, 50, 23, 280)} ${py(54, 50, 23, 280)}A23 23 0 0 1 ${px(54, 50, 23, 440)} ${py(54, 50, 23, 440)}" fill="none" stroke="var(--s2)" stroke-width="6.5" stroke-linecap="round"/>
    <circle cx="50" cy="50" r="3.8" fill="var(--s3)"/>
  </symbol>

  <!-- G44 心形线:r=a(1+cos t) 参数曲线,尖点红标 -->
  <symbol id="mk-g44" viewBox="0 0 100 100">
    <path d="${g44path}" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linejoin="round"/>
    <circle cx="50" cy="50" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G45 渐细S带:双半圆 S 路径,缎带宽 2→8→2,法向偏移烘焙 -->
  <symbol id="mk-g45" viewBox="0 0 100 100">
    <path d="${g45path}" fill="var(--s1)"/>
    <circle cx="53" cy="76" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G46 偏心涟漪:三环逐级缩小且环心逐级漂移 -->
  <symbol id="mk-g46" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="26" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <circle cx="53" cy="47" r="19" fill="none" stroke="var(--s2)" stroke-width="4"/>
    <circle cx="56" cy="44" r="12" fill="none" stroke="var(--s3)" stroke-width="3"/>
  </symbol>`;
h = h.replace('  <!-- G40 波瓣环', newSymbols + '\n\n  <!-- G40 波瓣环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- spec cards after G42 card ---
// --- marks entries + matrix rows ---
r("{id:'g42', code:'G42', name:'涡旋三弧', gene:'VORTEX THIRDS · 100° × 3 · r26/20/14 内旋', c:'三段弧半径逐级内收、角位前移,向心阶梯涡旋;内弧红。'},",
`{id:'g42', code:'G42', name:'涡旋三弧', gene:'VORTEX THIRDS · 100° × 3 · r26/20/14 内旋', c:'三段弧半径逐级内收、角位前移,向心阶梯涡旋;内弧红。'},
  {id:'g43', code:'G43', name:'错位环', gene:'OFFSET RING · 双圆心差 8u · 两半弧', c:'两个圆心相距 8u 的半圆弧拼成被轻微掰弯的圆——预览视角的微小偏移画进轮廓;交叉点红点=两视线交点。'},
  {id:'g44', code:'G44', name:'心形线', gene:'CARDIOID · r=a(1+cos t) · 90 段逼近', c:'一动点绕等圆滚动留下的轨迹,尖点在圆心、腹部向右;尖点红标。参数数学里最流畅的弧线。'},
  {id:'g45', code:'G45', name:'渐细S带', gene:'TAPERED S · 双半圆路径 · 宽 2→8→2', c:'一条缎带走 S 形,中段最宽两端收尖——视线在文档里游走的轨迹;末端红点。'},
  {id:'g46', code:'G46', name:'偏心涟漪', gene:'DRIFTING RIPPLES · r26/19/12 · 环心漂移 3u', c:'年轮式三环,环心逐级向右上漂移——生长有方向的涟漪;最内环红。'},`);

r("  ['G42','涡旋三弧','内旋构图',[4,4,5,4,4,4],0],",
`  ['G42','涡旋三弧','内旋构图',[4,4,5,4,4,4],0],
  ['G43','错位环','视角偏移',[4,4,4,4,4,4],0],
  ['G44','心形线','旋轮轨迹',[4,3,5,4,3,4],0],
  ['G45','渐细S带','视线轨迹',[4,4,5,4,4,4],0],
  ['G46','偏心涟漪','生长漂移',[4,4,4,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.8 four more curve variants added');
