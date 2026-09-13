const fs = require('fs');
const h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html', 'utf8');
const num = parseFloat;
let allOk = true;
for (const id of ['mk-g34','mk-g35','mk-g36','mk-g37','mk-g38']) {
  const a = h.indexOf('<symbol id="' + id + '"');
  const b = h.indexOf('</symbol>', a);
  const body = h.slice(a, b);
  let worst = 0, maxStroke = 0;
  for (const c of body.matchAll(/<circle[^>]*>/g)) {
    const t = c[0], g = k => { const x = t.match(new RegExp(k + '="(-?[\\d.]+)"')); return x ? num(x[1]) : 0; };
    worst = Math.max(worst, Math.hypot(g('cx') - 50, g('cy') - 50) + g('r'));
  }
  for (const c of body.matchAll(/<rect[^>]*>/g)) {
    const t = c[0], g = k => { const x = t.match(new RegExp(k + '="(-?[\\d.]+)"')); return x ? num(x[1]) : 0; };
    const x = g('x'), y = g('y'), w = g('width'), hh = g('height');
    for (const [px, py] of [[x,y],[x+w,y],[x,y+hh],[x+w,y+hh]]) worst = Math.max(worst, Math.hypot(px - 50, py - 50));
  }
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
  for (const s of body.matchAll(/stroke-width="([\d.]+)"/g)) maxStroke = Math.max(maxStroke, num(s[1]));
  const total = worst + maxStroke / 2;
  const ok = total <= 30.6;
  if (!ok) allOk = false;
  console.log(id.padEnd(8), 'outer=' + total.toFixed(1), ok ? 'ok' : 'EXCEEDS');
}
console.log(allOk ? 'ALL FIVE WITHIN 66dp SAFE CIRCLE' : 'SOME EXCEED');
