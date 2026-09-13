const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;

// regenerate tapered S ribbon with r=11, centers (38.5,38)/(61.5,62)
const samp = [];
for (let i = 0; i <= 30; i++) { const a = 180 - (i / 30) * 180; samp.push([38.5 + 11.5 * Math.cos(rad(a)), 38 - 11.5 * Math.sin(rad(a))]); }
for (let i = 0; i <= 30; i++) { const a = 0 + (i / 30) * 180; samp.push([61.5 + 11.5 * Math.cos(rad(a)), 62 + 11.5 * Math.sin(rad(a))]); }
const n = samp.length;
const wid = samp.map((_, i) => 2 + 4.5 * Math.sin(Math.PI * i / (n - 1)));
const L = [], R = [];
for (let i = 0; i < n; i++) {
  const p0 = samp[Math.max(0, i - 1)], p1 = samp[Math.min(n - 1, i + 1)];
  let tx = p1[0] - p0[0], ty = p1[1] - p0[1];
  const len = Math.hypot(tx, ty) || 1; tx /= len; ty /= len;
  L.push([samp[i][0] - ty * wid[i] / 2, samp[i][1] + tx * wid[i] / 2]);
  R.push([samp[i][0] + ty * wid[i] / 2, samp[i][1] - tx * wid[i] / 2]);
}
samp.forEach((p,i)=>{samp[i]=[50+(p[0]-50)*0.95,50+(p[1]-50)*0.95];});
const poly = pts => 'M' + pts.map(p => p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join('L');
const path = poly(L) + poly(R.slice().reverse()).replace('M', 'L') + 'Z';

// verify max extent
let worst = 0;
for (const p of L.concat(R)) worst = Math.max(worst, Math.hypot(p[0] - 50, p[1] - 50));
console.log('G45 ribbon max extent =', worst.toFixed(1), '(limit 30.6)');

const a = h.indexOf('<symbol id="mk-g45"');
const dS = h.indexOf(' d="', a) + 5;
const dE = h.indexOf('"', dS);
h = h.slice(0, dS) + path + h.slice(dE);
fs.writeFileSync(P, h);
console.log('G45 ribbon re-baked');
