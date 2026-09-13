const fs = require('fs');
const h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html', 'utf8');
const num = parseFloat;
let allOk = true;
for (const id of ['mk-g34sw', 'mk-g34sb', 'mk-g34so']) {
  const a = h.indexOf('<symbol id="' + id + '"');
  const b = h.indexOf('</symbol>', a);
  const body = h.slice(a, b);
  let worst = 0, ms = 0;
  for (const s of body.matchAll(/stroke-width="([\d.]+)"/g)) ms = Math.max(ms, num(s[1]));
  for (const c of body.matchAll(/ d="([^"]+)"/g)) {
    const toks = c[1].match(/[MLZ]|-?\d+\.?\d*/g) || []; let i = 0;
    while (i < toks.length) {
      const cmd = toks[i]; i++;
      if (cmd === 'M' || cmd === 'L') { worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50)); i += 2; }
    }
  }
  for (const c of body.matchAll(/<circle[^>]*>/g)) {
    const t = c[0], g = k => { const x = t.match(new RegExp(k + '="(-?[\\d.]+)"')); return x ? num(x[1]) : 0; };
    worst = Math.max(worst, Math.hypot(g('cx') - 50, g('cy') - 50) + g('r'));
  }
  const total = worst + ms / 2;
  const ok = total <= 30.6;
  if (!ok) allOk = false;
  console.log(id.padEnd(9), 'outer=' + total.toFixed(1), ok ? 'ok' : 'EXCEEDS');
}
console.log(allOk ? 'ALL THREE G34-SWALLOW VARIANTS WITHIN 66dp SAFE CIRCLE' : 'SOME EXCEED');
