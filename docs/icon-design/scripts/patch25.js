const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// 1) remove injected cards from card() return template (they sit right before the closing backtick)
const bad = /<article class="spec" id="s-g34r"[\s\S]*?(?=`;)/;
const cnt = (h.match(bad) || []).length;
h = h.replace(bad, '');
console.log('removed blocks inside card() template:', cnt);
if (cnt !== 1) { console.error('expected exactly 1'); process.exit(1); }

// 2) build the 3 cards (rendered) and insert right after the specs map placeholder
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;
function card(id, code, name, gene, concept, note) {
  const tiles = ['','on-ink','mono-layer'].map(c => `<span class="chip ${c}">${U(id)}</span>`).join('');
  const sizes = [96,48,29].map(s => `<figure><span class="chip" style="width:${s}px;height:${s}px;border-radius:var(--chip-r)">${U(id)}</span><figcaption>${s}px</figcaption></figure>`).join('');
  const floats = `<span class="float stage" style="width:120px;height:120px">${U(id)}</span><span class="float on-dark stage" style="width:120px;height:120px">${U(id)}</span>`;
  return `<article class="spec" id="s-${id}" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">${code}</span><div><h4>${name}</h4><p class="gene">${gene}</p></div><p class="concept">${concept}</p><div class="orig"><b>精修注记</b> · ${note}</div></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/>${U(id)}<use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span>${floats}</div>
      <div class="sim"><span class="lab">TILES</span>${tiles}</div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap">${sizes}</div></div>
    </div>
  </div>
</article>`;
}
const cards = [
  card('g34r', 'G34R', '匀密九弦', 'LINE ENVELOPE · 9 弦均布 20° · 全扇覆盖', '把 7 弦 120° 的覆盖补全为 9 弦 160°,包络圆不再有暗缺角;顶部横线改为红色——「正在被预览的那一行」,蓝色第二弦保持破序。密度拉满后,中孔在 29px 依然成圆。', '①7→9 弦,角距 20° 全扇;②红色从端部移到顶部横线,语义归位;③线宽 4→3.8 配合密度。'),
  card('g34x', 'G34X', '开角疏密', 'LINE ENVELOPE · 角距 12→30 渐扩 · 开放动势', '角距按 12/14/18/22/26/28/30 渐扩:左密右疏,像一把缓缓打开的折扇——密处是「撰」,疏端红弦是「览」的出口。同样弦长下,不均匀节奏带来动势。', '①角度序列非均匀化(12→30 渐扩);②红色移至最疏端,「打开」的方向叙事;③其余保持纯墨,只留一枚蓝。'),
  card('g34o', 'G34O', '偏心窥圆', 'LINE ENVELOPE · 弦切偏心圆 · 裁入主圆 r27', '包络圆偏移到右上 (59,41),直线族裁入主圆——圆孔不在正中,窥视有了方位感;红点悬在偏心孔心。三种变体里最现代、最不对称的一版。', '①包络圆偏心至 (59,41) r10.5;②整族直线裁入主圆 r27,外缘成干净的圆;③红点=窥视焦点,悬于孔心。'),
];
const anchor = "marks.map(card).join('\\n')}";
if (!h.includes(anchor)) { console.error('specs map anchor MISS'); process.exit(1); }
const block = `
  <div class="family-hd" data-component="family-header"><h3>G34 包络精修变体</h3><span class="caps mono">ENVELOPE VARIANTS · 排布 / 密度 / 构图</span></div>
  ${cards.join('\n')}`;
h = h.replace(anchor, anchor + block);

fs.writeFileSync(P, h);
console.log('fixed: template restored, cards placed after specs map');
