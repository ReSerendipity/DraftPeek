const fs = require('fs');
const h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html', 'utf8');
const num = parseFloat;
for (const id of ['mk-c1','mk-c2','mk-c3','mk-c4','mk-c5']) {
  const a = h.indexOf('<symbol id="' + id + '"');
  const b = h.indexOf('</symbol>', a);
  const body = h.slice(a, b);
  let worst = 0, ms = 0;
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
    const toks = c[1].match(/[MLZ]|-?\d+\.?\d*/g) || []; let i = 0;
    while (i < toks.length) {
      const cmd = toks[i]; i++;
      if (cmd === 'M' || cmd === 'L') { worst = Math.max(worst, Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50)); i += 2; }
    }
  }
  for (const s of body.matchAll(/stroke-width="([\d.]+)"/g)) ms = Math.max(ms, num(s[1]));
  const t = worst + ms / 2;
  console.log(id.padEnd(7), 'outer=' + t.toFixed(1), t <= 30.6 ? 'ok' : 'EXCEEDS');
}
