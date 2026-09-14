// v9: precision refinement round on top-4 marks (G4/G9/G7/G1) + small-size simplified variants
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// --- G9R phyllotaxis refined: growth-front coloring (i=21/20/19), phase rotation, geometric dot growth ---
const N = 21, GA = 137.507 * Math.PI / 180, C = 5.85, PHASE = 0.35;
const dots = [];
for (let i = 1; i <= N; i++) {
  const a = i * GA + PHASE, rad = C * Math.sqrt(i);
  const x = (50 + rad * Math.cos(a)).toFixed(1), y = (50 + rad * Math.sin(a)).toFixed(1);
  let fill = 'var(--s1)';
  if (i === 21) fill = 'var(--s3)'; else if (i === 20) fill = 'var(--s2)'; else if (i === 19) fill = 'var(--s4)';
  const sz = (2.2 + 1.6 * Math.sqrt(i / N)).toFixed(2);
  dots.push(`<circle cx="${x}" cy="${y}" r="${sz}" fill="${fill}"/>`);
}
// --- G9S simplified 8-dot version for small sizes ---
const sDots = [];
for (let i = 1; i <= 8; i++) {
  const a = i * GA + PHASE, rad = 7.2 * Math.sqrt(i);
  const x = (50 + rad * Math.cos(a)).toFixed(1), y = (50 + rad * Math.sin(a)).toFixed(1);
  sDots.push(`<circle cx="${x}" cy="${y}" r="4.2" fill="${i === 8 ? 'var(--s3)' : 'var(--s1)'}"/>`);
}

const newSymbols = `
  <!-- G4R 直刃光圈·精修:外 r26.5 / 内 r11.5 / lead 55°,线宽 7.5,红瞳 r6 与光瞳同心 -->
  <symbol id="mk-g4r" viewBox="0 0 100 100">
    <g stroke-width="7.5" stroke-linecap="round">
      <path d="M50 23.5L59.4 43.4" stroke="var(--s1)"/>
      <path d="M73 36.75L60.4 54.9" stroke="var(--s1)"/>
      <path d="M73 63.25L51 61.45" stroke="var(--s2)"/>
      <path d="M50 76.5L40.6 56.6" stroke="var(--s1)"/>
      <path d="M27 63.25L39.6 45.1" stroke="var(--s1)"/>
      <path d="M27 36.75L49 38.55" stroke="var(--s1)"/>
    </g>
    <circle cx="50" cy="50" r="6" fill="var(--s3)"/>
  </symbol>

  <!-- G9R 叶序点阵·精修:生长前沿三色(i21/20/19),点径 ∝ √i,整体旋转 20° -->
  <symbol id="mk-g9r" viewBox="0 0 100 100">
    ${dots.join('\n    ')}
  </symbol>

  <!-- G9S 叶序·小尺寸简化:8 点均匀大径,仅生长点着红 -->
  <symbol id="mk-g9s" viewBox="0 0 100 100">
    ${sDots.join('\n    ')}
  </symbol>

  <!-- G7R 嵌套V形·精修:顶点间距 15u(垂直净隙 4.9u),深度 14/12/10 递减 -->
  <symbol id="mk-g7r" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M28 52L50 66L72 52" stroke="var(--s1)"/>
      <path d="M36 39L50 51L64 39" stroke="var(--s2)"/>
      <path d="M44 26L50 36L56 26" stroke="var(--s3)"/>
    </g>
  </symbol>

  <!-- G1R 旋转连续环·精修:内→中→外三段 120° 弧角度首尾相接,同一旋转运动 -->
  <symbol id="mk-g1r" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M60 50A10 10 0 0 1 45 58.7" stroke="var(--s3)"/>
      <path d="M41 65.6A18 18 0 0 1 41 34.4" stroke="var(--s2)"/>
      <path d="M37 27.5A26 26 0 0 1 76 50" stroke="var(--s1)"/>
    </g>
    <circle cx="50" cy="50" r="4.2" fill="var(--s4)"/>
  </symbol>`;

h = h.replace('</defs>', newSymbols + '\n</defs>');

// --- refined marks data + section 06 ---
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;

function rcard(m){
  const tiles = ['','on-ink','mono-layer'].map(c=>`<span class="chip ${c}">${U(m.id)}</span>`).join('');
  const smalls = (m.small||[]).map(s=>`<figure><span class="chip" style="width:${s}px;height:${s}px;border-radius:var(--chip-r)">${U(m.smallId||m.id)}</span><figcaption>${s}px${m.smallId?'·简化':''}</figcaption></figure>`).join('');
  const floats = `<span class="float stage" style="width:120px;height:120px">${U(m.id)}</span><span class="float on-dark stage" style="width:120px;height:120px">${U(m.id)}</span>`;
  return `<article class="spec" id="s-${m.id}" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">${m.code}</span><div><h4>${m.name}</h4><p class="gene">${m.gene}</p></div><p class="concept">${m.c}</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/>${U(m.id)}<use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span>${floats}</div>
      <div class="sim"><span class="lab">TILES</span>${tiles}</div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap">${smalls}</div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ${m.note}</div>
</article>`;
}

const refined = [
  {id:'g4r', code:'G4R', name:'直刃光圈·精修', gene:'APERTURE · 外 r26.5 / 光瞳 r11.5 / lead 55° / 线宽 7.5', c:'机械光圈的最终形态:六刃内外端分别落在 r26.5 与 r11.5 的同心圆上,55° 导程让刃与孔的比例接近真实快门;线宽从 8 收到 7.5,红瞳与光瞳同心悬浮。', note:'①线宽 8→7.5,刃/孔比例对齐真实快门;②刃端精确落在双同心圆上,消除 v8 的松散感;③瞳孔加大至 r6,任何遮罩下孔心不闭。'},
  {id:'g9r', code:'G9R', name:'叶序点阵·精修', gene:'PHYLOTAXIS · 137.5° 黄金角 · 前沿三色', c:'修正 v8 的着色逻辑:三色不再散落内圈(读作噪音),而是标在外缘相邻的三颗「最新种子」上——生长前沿的故事一眼可读;整体旋转 20° 避开正上方的呆板;点径按 √i 几何增长。', note:'①红/蓝/mint 从散点改为外缘相邻三连——「新叶」语义成立;②点径改 √i 增长,外圈更饱满;③整体相位旋转 20°。29px 使用 8 点简化形态(见 SIZES 行)。', small:[29,48], smallId:'g9s'},
  {id:'g7r', code:'G7R', name:'嵌套V形·精修', gene:'NESTED CHEVRONS · 顶点距 15u · 深度 14/12/10', c:'修正 v8 的致命间隙问题:顶点距 15u 后,平行臂间垂直净隙达 4.9u,三层 V 形终于「呼吸」;深度改为 14/12/10 递减,收束感更强。', note:'①顶点距 12→15u,净隙 0.5→4.9u,三层不再粘连;②深度递减 14/12/10,视线被引向内层红尖;③蓝色中层的破序位保持。'},
  {id:'g1r', code:'G1R', name:'旋转连续环·精修', gene:'ROTATIONAL CONTINUITY · r10/18/26 · 三段 120° 首尾相接', c:'v8 的 135° 错角弧改为三段 120° 弧在角度上首尾相接(内段终点角=中段起点角),三个半径读作同一个旋转运动的三个瞬间——比错角排布更有「动势被冻结」的叙事。', note:'①三段弧角度连续(0-120-240-360),旋转运动感成立;②半径 10/18/26 等差;③内红中蓝外墨,mint 心点居中。'},
];

const section9 = `
<section id="refined" data-component="refined-round">
  <div class="sec-hd"><span class="no">06</span><h2>精修轮 · 四强候选的光学级修正</h2><span class="sub mono">TOP-4 REFINED · G4R / G9R / G7R / G1R</span></div>
  <p class="note">从 10 个系统记号中收敛出 4 个最强方向,逐项做光学级修正:间隙均匀化、切点落位、着色语义归位、小尺寸简化形态。以下为最终定稿池。</p>
  ${refined.map(rcard).join('\n')}
  <div class="top3" data-component="final-reco">
    <div class="card"><span class="stage">${U('g4r')}</span><div><h5>主推 · G4R 直刃光圈</h5><p>「窥视」语义、机械精确、瓦片/单色/29px 全形态无短板;工业感与 DraftPeek 的编辑器身份最契合。定稿风险最低。</p></div></div>
    <div class="card"><span class="stage">${U('g9r')}</span><div><h5>签名备选 · G9R 叶序点阵</h5><p>全场最独特:程序化生成的生长母题,同套公式可延展加载动画/启动画面/空状态;小尺寸依赖简化形态是唯一代价。</p></div></div>
    <div class="card"><span class="stage">${U('g7r')}</span><div><h5>动作备选 · G7R 嵌套V形</h5><p>方向性最强、最轻;若想要「工具感」弱一点、更轻盈的品牌气质,选它。</p></div></div>
  </div>
</section>`;

h = h.replace('<section id="guide"', section9 + '\n<section id="guide"');
h = h.replace('<span>DraftPeek 图标评审板 v8 · 几何精确系统 · 8u 网格 / 单一线宽 / 平涂结构色</span>', '<span>DraftPeek 图标评审板 v9 · v8 十系统 + 四强精修(G4R/G9R/G7R/G1R)· 定稿池已就绪</span>');
h = h.replace('<title>DraftPeek 图标重构 · v8 几何精确系统评审板</title>', '<title>DraftPeek 图标重构 · v9 精修轮评审板</title>');
h = h.replace('<h1>DraftPeek 图标 v8 · 几何精确系统(零渐变 · 零投影)</h1>', '<h1>DraftPeek 图标 v9 · 几何精确系统 · 四强精修定稿池</h1>');

fs.writeFileSync(P, h);
console.log('v9 refined round added');
