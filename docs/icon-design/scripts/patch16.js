const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// regenerate correctly scaled Lissajous (A 18.9, B 21.3 → reach 26.4 + 3 stroke ≈ 29.4)
let pts = [];
for (let i = 0; i <= 120; i++) {
  const t = (i / 120) * 2 * Math.PI;
  const x = 50 + 18.9 * Math.sin(3 * t + Math.PI / 2);
  const y = 50 + 21.3 * Math.sin(2 * t);
  pts.push(`${x.toFixed(1)} ${y.toFixed(1)}`);
}
const path = 'M' + pts.join('L') + 'Z';

let worst = 0;
const nums = path.match(/-?\d+\.?\d*/g);
for (let i = 0; i < nums.length; i += 2) worst = Math.max(worst, Math.hypot(parseFloat(nums[i]) - 50, parseFloat(nums[i + 1]) - 50));
console.log('new reach', worst.toFixed(1), '+3 =', (worst + 3).toFixed(1));

// replace the whole corrupted G28 symbol block (from its comment to closing tag)
const start = h.indexOf('  <!-- G28');
const symStart = h.indexOf('<symbol', start);
const symEnd = h.indexOf('</symbol>', symStart) + '</symbol>'.length;
if (start < 0 || symEnd < 0) { console.error('G28 block not found'); process.exit(1); }
const g28new = `  <!-- G28 利萨茹曲线:x:y 频率比 3:2 闭合曲线,A18.9/B21.3,起终点红标 -->
  <symbol id="mk-g28" viewBox="0 0 100 100">
    <path d="${path}" fill="none" stroke="var(--s1)" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="69.3" cy="50" r="3.6" fill="var(--s3)"/>
  </symbol>`;
h = h.slice(0, start) + g28new + h.slice(symEnd);
// verify unique symbol id restored
if (!h.includes('<symbol id="mk-g28" viewBox')) { console.error('still broken'); process.exit(1); }
fs.writeFileSync(P, h);
console.log('G28 rebuilt');
