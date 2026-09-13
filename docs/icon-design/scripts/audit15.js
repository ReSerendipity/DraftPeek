const fs = require('fs');
const h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html', 'utf8');
const num = parseFloat;
let allOk = true;
for (const id of ['mk-g53','mk-g54','mk-g55']) {
  const a = h.indexOf('<symbol id="' + id + '"');
  const b = h.indexOf('</symbol>', a);
  const body = h.slice(a, b);
  let worst = 0;
  for (const c of body.matchAll(/ d="([^"]+)"/g)) {
    const toks = c[1].match(/[MLQZVHA]|-?\d+\.?\d*/g) || []; let i = 0;
    while (i < toks.length) {
      const cmd = toks[i]; i++;
      if (cmd === 'M' || cmd === 'L') { worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50)); i += 2; }
      else if (cmd === 'Q') { worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50), Math.hypot(num(toks[i+2]) - 50, num(toks[i+3]) - 50)); i += 4; }
      else if (cmd === 'A') { i += 5; worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50)); i += 2; }
      else if (cmd === 'V') { worst = Math.max(worst, Math.abs(num(toks[i]) - 50)); i++; }
      else if (cmd === 'H') { worst = Math.max(worst, Math.abs(num(toks[i]) - 50)); i++; }
    }
  }
  // fills only (no strokes in these symbols)
  const ok = worst <= 30.6;
  if (!ok) allOk = false;
  console.log(id.padEnd(8), 'outer=' + worst.toFixed(1), ok ? 'ok' : 'EXCEEDS');
}
console.log(allOk ? 'ALL THREE WITHIN 66dp SAFE CIRCLE' : 'SOME EXCEED');
