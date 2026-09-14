const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// G35: shorten trunk so top/bottom stay within safe circle
r('<path d="M50 76V22" stroke="var(--s1)" stroke-width="6.5" stroke-linecap="round"/>',
  '<path d="M50 74V26" stroke="var(--s1)" stroke-width="6.5" stroke-linecap="round"/>');

// G37: re-generate Hilbert path with inset grid (S=12, origin 32,68) and reposition endpoint dots
const hp = [];
(function hilbert(x0, y0, xi, xj, yi, yj, n) {
  if (n <= 0) { hp.push([x0 + (xi + yi) / 2, y0 + (xj + yj) / 2]); return; }
  hilbert(x0, y0, yi / 2, yj / 2, xi / 2, xj / 2, n - 1);
  hilbert(x0 + xi / 2, y0 + xj / 2, xi / 2, xj / 2, yi / 2, yj / 2, n - 1);
  hilbert(x0 + xi / 2 + yi / 2, y0 + xj / 2 + yj / 2, xi / 2, xj / 2, yi / 2, yj / 2, n - 1);
  hilbert(x0 + xi / 2 + yi, y0 + xj / 2 + yj, -yi / 2, -yj / 2, -xi / 2, -xj / 2, n - 1);
})(0, 0, 1, 0, 0, 1, 2);
const S = 12, OX = 32, OY = 68;
const hpts = hp.map(p => [(OX + p[0] * S).toFixed(1), (OY - p[1] * S).toFixed(1)]);
const d = 'M' + hpts.map(p => p[0] + ' ' + p[1]).join('L');

const a = h.indexOf('<symbol id="mk-g37"');
const dS = h.indexOf('d="', h.indexOf('<path', a)) + 3;
const dE = h.indexOf('"', dS);
// patch safely: replace the path data attribute only (verify anchor contains 'M' first)
const oldD = h.slice(dS, dE);
if (!oldD.startsWith('M')) { console.error('G37 anchor sanity failed:', oldD.slice(0, 40)); process.exit(1); }
h = h.slice(0, dS) + d + h.slice(dE);
// reposition endpoint dots to first/last path points
const first = hpts[0], last = hpts[hpts.length - 1];
h = h.replace(/<circle cx="[\d.]+" cy="[\d.]+" r="3\.8" fill="var\(--s3\)"/, '<circle cx="' + first[0] + '" cy="' + first[1] + '" r="3.4" fill="var(--s3)"');
h = h.replace(/<circle cx="[\d.]+" cy="[\d.]+" r="3\.8" fill="var\(--s2\)"/, '<circle cx="' + last[0] + '" cy="' + last[1] + '" r="3.4" fill="var(--s2)"');

fs.writeFileSync(P, h);
console.log('G35/G37 fixed; hilbert extent', hpts[0], '→', hpts[hpts.length - 1]);
