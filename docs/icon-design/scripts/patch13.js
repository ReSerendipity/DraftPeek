const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

const g24rCard = `
  <article class="spec" id="s-g24r" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">G24R</span><div><h4>透镜交集·精修</h4><p class="gene">VESICA OVERLAP · 双环 r17 / 线宽 7 / 圆心距 20</p></div><p class="concept">墨环与蓝环相切级相交,透镜形交集填红——编辑与预览的重合处正是 DraftPeek 站的位置。精修后环径加大到 r17、线宽 7,红镜高 27.5u,交叠关系在 29px 依然一读即懂。</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg><use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span><span class="float stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="float on-dark stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span></div>
      <div class="sim"><span class="lab">TILES</span><span class="chip"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="chip on-ink"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="chip mono-layer"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span></div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap"><figure><span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><figcaption>96px</figcaption></figure><figure><span class="chip" style="width:48px;height:48px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><figcaption>48px</figcaption></figure><figure><span class="chip" style="width:29px;height:29px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><figcaption>29px</figcaption></figure></div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ①环径 r16→r17、线宽 6.5→7,对比度拉满;②圆心距 18→20,红镜高度 26.4→27.5u,交集存在感更强;③左墨右蓝的结构色与「左写右览」方位一致。</div>
</article>`;

// insert after the s-g1r article's closing tag
const anchor = h.indexOf('<article class="spec" id="s-g1r"');
if (anchor < 0) { console.error('s-g1r article not found'); process.exit(1); }
const close = h.indexOf('</article>', anchor) + '</article>'.length;
h = h.slice(0, close) + '\n' + g24rCard + h.slice(close);

fs.writeFileSync(P, h);
console.log('G24R spec card inserted');
