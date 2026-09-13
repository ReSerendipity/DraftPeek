// fix G28 Lissajous scale: A 23→19.6, B 26→22.1 so max reach+stroke ≤ 30.6
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
let pts = [];
for (let i = 0; i <= 120; i++) {
  const t = (i / 120) * 2 * Math.PI;
  const x = 50 + 19.6 * Math.sin(3 * t + Math.PI / 2);
  const y = 50 + 22.1 * Math.sin(2 * t);
  pts.push(`${x.toFixed(1)} ${y.toFixed(1)}`);
}
const path = 'M' + pts.join('L') + 'Z';
const start = h.indexOf('<symbol id="mk-g28"');
const dStart = h.indexOf('d="', start) + 3;
const dEnd = h.indexOf('"', dStart);
h = h.slice(0, dStart) + path + h.slice(dEnd);
// also move the start-point red dot to the new start (73→69.6, 50)
h = h.replace('<circle cx="73" cy="50" r="3.6" fill="var(--s3)"/>', '<circle cx="69.6" cy="50" r="3.6" fill="var(--s3)"/>');
fs.writeFileSync(P, h);
// verify max reach
let worst = 0;
for (const m of path.match(/-?\d+\.?\d*/g).reduce((acc, v, i, arr) => { if (i % 2 === 0) acc.push([parseFloat(v), parseFloat(arr[i + 1])]); return acc; }, [])) {
  worst = Math.max(worst, Math.hypot(m[0] - 50, m[1] - 50));
}
console.log('G28 new max reach =', worst.toFixed(1), '+3 stroke =', (worst + 3).toFixed(1), '(limit 30.6)');
