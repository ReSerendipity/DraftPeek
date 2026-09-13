const fs = require('fs');
const h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html', 'utf8');
const num = parseFloat;
let allOk = true;
for (const id of ['mk-g4r','mk-g9r','mk-g9s','mk-g7r','mk-g1r']) {
  const re = new RegExp('<symbol id="' + id + '"[\\s\\S]*?</symbol>');
  const m = h.match(re);
  if (!m) { console.log(id, 'NOT FOUND'); allOk = false; continue; }
  const body = m[0]; let worst = 0, maxStroke = 0;
  for (const c of body.matchAll(/<circle[^>]*>/g)) {
    const t = c[0], g = k => { const x = t.match(new RegExp(k + '="(-?[\\d.]+)"')); return x ? num(x[1]) : 0; };
    worst = Math.max(worst, Math.hypot(g('cx') - 50, g('cy') - 50) + g('r'));
  }
  for (const s of body.matchAll(/stroke-width="([\d.]+)"/g)) maxStroke = Math.max(maxStroke, num(s[1]));
  for (const c of body.matchAll(/ d="([^"]+)"/g)) {
    const toks = c[1].match(/[MLAHVZ]|-?\d+\.?\d*/g) || []; let i = 0;
    while (i < toks.length) {
      const cmd = toks[i]; i++;
      if (cmd === 'M' || cmd === 'L') { worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50)); i += 2; }
      else if (cmd === 'A') { i += 5; worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50)); i += 2; }
      else if (cmd === 'V') { worst = Math.max(worst, Math.abs(num(toks[i]) - 50)); i++; }
      else if (cmd === 'H') { worst = Math.max(worst, Math.abs(num(toks[i]) - 50)); i++; }
    }
  }
  const total = worst + maxStroke / 2;
  const ok = total <= 30.6;
  if (!ok) allOk = false;
  console.log(id.padEnd(8), 'outer=' + total.toFixed(1), ok ? 'ok' : 'EXCEEDS');
}
console.log(allOk ? 'ALL REFINED MARKS WITHIN 66dp SAFE CIRCLE' : 'SOME EXCEED');
