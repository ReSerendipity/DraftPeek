const fs = require('fs');
const h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html', 'utf8');
const num = parseFloat;
let allOk = true;
const ids = [];
for (const k of ['g53','g54','g55']) for (const s of ['h1','h2','h3']) ids.push('mk-' + k + s);
for (const id of ids) {
  const a = h.indexOf('<symbol id="' + id + '"');
  const b = h.indexOf('</symbol>', a);
  const body = h.slice(a, b);
  let worst = 0, worstPair = null;
  for (const c of body.matchAll(/ d="([^"]+)"/g)) {
    const toks = c[1].match(/[MLQZ]|-?\d+\.?\d*/g) || []; let i = 0;
    while (i < toks.length) {
      const cmd = toks[i]; i++;
      if (cmd === 'M' || cmd === 'L') { { const dd = Math.hypot(num(toks[i]) - 50, num(toks[i+1]) - 50); if (dd > worst) { worst = dd; worstPair = [cmd, toks[i], toks[i+1]]; } } i += 2; }
      else if (cmd === 'Q') { i += 4; }
    }
  }
  for (const s of body.matchAll(/stroke-width="([\d.]+)"/g)) worst = Math.max(worst, num(s[1]) / 2);
  const ok = worst <= 30.6;
  if (!ok) allOk = false;
  if (!ok) console.log('   worstPair=', JSON.stringify(worstPair || null));
  console.log(id.padEnd(10), 'outer=' + worst.toFixed(1), ok ? 'ok' : 'EXCEEDS');
}
console.log(allOk ? 'ALL NINE WITHIN 66dp SAFE CIRCLE' : 'SOME EXCEED');
