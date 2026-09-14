// v10.3: "center element" alternatives for the G34 hole — empty / block cursor / caret bar / red chord / red window
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const fx = v => v.toFixed(1);

function chords(colorAt) {
  const out = [];
  for (let k = 0; k < 9; k++) {
    const th = k * 20 * Math.PI / 180;
    const tx = 50 + 12.5 * Math.cos(th), ty = 50 + 12.5 * Math.sin(th);
    const dx = -Math.sin(th), dy = Math.cos(th);
    const col = colorAt(k);
    out.push(`<path d="M${fx(tx + 23.7 * dx)} ${fx(ty + 23.7 * dy)}L${fx(tx - 23.7 * dx)} ${fx(ty - 23.7 * dy)}" stroke="${col}" stroke-width="3.8" stroke-linecap="round"/>`);
  }
  return out.join('\n    ');
}
const inkBlue = k => k === 1 ? 'var(--s2)' : 'var(--s1)';
const inkBlueRed = k => k === 4 ? 'var(--s3)' : (k === 1 ? 'var(--s2)' : 'var(--s1)');

const newSymbols = `
  <!-- C1 空窗:负空间自己当主角 -->
  <symbol id="mk-c1" viewBox="0 0 100 100">
    ${chords(inkBlue)}
  </symbol>
  <!-- C2 块光标:编辑器方块光标 -->
  <symbol id="mk-c2" viewBox="0 0 100 100">
    ${chords(inkBlue)}
    <rect x="46" y="46" width="8" height="8" rx="2" fill="var(--s3)"/>
  </symbol>
  <!-- C3 光标条:文本插入符 -->
  <symbol id="mk-c3" viewBox="0 0 100 100">
    ${chords(inkBlue)}
    <rect x="47.6" y="41.5" width="4.8" height="17" rx="2.4" fill="var(--s3)"/>
  </symbol>
  <!-- C4 红弦:红色并入线系统 -->
  <symbol id="mk-c4" viewBox="0 0 100 100">
    ${chords(inkBlueRed)}
  </symbol>
  <!-- C5 红窗:孔本身点亮 -->
  <symbol id="mk-c5" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="8.6" fill="var(--s3)"/>
    ${chords(inkBlue)}
  </symbol>`;
h = h.replace('  <!-- G40 波瓣环', newSymbols + '\n\n  <!-- G40 波瓣环');

// lightweight comparison section before showcase
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;
const alts = [
  ['c1', 'C1 · 空窗', '零中心元素,负空间自己当主角;最克制,但 29px 下孔可能读作"空洞"而非"窥"。'],
  ['c2', 'C2 · 块光标', '红色方块=编辑器方块光标 ▮;身份语义最正,几何与九弦同族(直角对圆头)。'],
  ['c3', 'C3 · 光标条', '红色竖条=文本插入符;全池最"编辑器"的一笔,细长在 29px 会弱化。'],
  ['c4', 'C4 · 红弦', '取消中心元素,让第四根弦变红——红色并入线系统,构图最纯粹,但焦点感稍弱。'],
  ['c5', 'C5 · 红窗', '孔本身填红发光——"窗后有人";色块最重,小尺寸最稳,气质最大胆。'],
];
const cols = alts.map(([id, name, desc]) => `<div class="dz" style="display:grid;justify-items:center;gap:8px;padding:14px 10px">
  <span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)">${U(id)}</span>
  <span class="mono" style="font-size:11px;font-weight:700">${name}</span>
  <span style="display:flex;gap:8px;align-items:flex-end"><span class="chip" style="width:48px;height:48px;border-radius:24%">${U(id)}</span><span class="chip" style="width:29px;height:29px;border-radius:24%">${U(id)}</span><span class="chip mono-layer" style="width:48px;height:48px;border-radius:24%">${U(id)}</span></span>
  <p style="font-size:11px;color:var(--muted);text-align:left">${desc}</p>
</div>`).join('\n    ');
const section = `
<section id="center-alts" data-component="center-alternatives">
  <div class="sec-hd"><span class="no">05</span><h2>心元素替代 · 红点的五种命运</h2><span class="sub mono">同一具身体,只换心脏</span></div>
  <div class="discipline" style="grid-template-columns:repeat(auto-fit,minmax(180px,1fr))">
    ${cols}
  </div>
</section>

<section id="showcase"`;
h = h.replace('<section id="showcase"', section.replace('<section id="showcase"', '<section id="showcase"'));
// fix: replace() above is identity; do direct replacement
if (h.includes('<section id="center-alts"')) { console.log('already inserted'); } else {
  const i = h.indexOf('<section id="showcase"');
  h = h.slice(0, i) + section.slice(0, section.length - '<section id="showcase"'.length) + h.slice(i);
}
fs.writeFileSync(P, h);
console.log('v10.3 five center-element alternatives added');
