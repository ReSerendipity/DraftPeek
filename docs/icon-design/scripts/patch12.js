// v10: curation round — cut weak marks, promote finals, refine G24, definitive recommendation
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- G24R refined vesica: bigger rings (r17, stroke 7), lens 27.5u tall ---
const g24r = `
  <!-- G24R 透镜交集·精修:双环 r17 / 线宽 7 / 圆心距 20,红镜高 27.5u -->
  <symbol id="mk-g24r" viewBox="0 0 100 100">
    <circle cx="40" cy="50" r="17" fill="none" stroke="var(--s1)" stroke-width="7"/>
    <circle cx="60" cy="50" r="17" fill="none" stroke="var(--s2)" stroke-width="7"/>
    <path d="M50 36.25A17 17 0 0 1 50 63.75A17 17 0 0 1 50 36.25Z" fill="var(--s3)"/>
  </symbol>`;
r('  <symbol id="mk-g11"', g24r + '\n\n  <symbol id="mk-g11"');

// --- add G24R to refined array (after g1r entry) ---
const g1rTail = `③内红中蓝外墨,mint 心点居中。'},`;
r(g1rTail, g1rTail + `
  {id:'g24r', code:'G24R', name:'透镜交集·精修', gene:'VESICA OVERLAP · 双环 r17 / 线宽 7 / 圆心距 20', c:'墨环与蓝环相切级相交,透镜形交集填红——编辑与预览的重合处正是 DraftPeek。精修后环径加大到 r17、线宽 7,红镜高 27.5u,交叠关系在 29px 依然一读即懂。', note:'①环径 r16→r17、线宽 6.5→7,对比度拉满;②圆心距 18→20,红镜高度 26.4→27.5u,交集存在感更强;③左墨右蓝的结构色与「左写右览」方位一致。'}`);

// --- finals section right after header ---
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;
const finals = `
<section id="finals" data-component="finals-round">
  <div class="sec-hd"><span class="no">00</span><h2>决赛圈 · 五个最终候选</h2><span class="sub mono">FINAL FIVE · 以下过程档案仅供参考</span></div>
  <p class="note">经六轮迭代与两轮淘汰,19 个方向收敛为 5 个决赛候选。淘汰名单与理由见页尾「过程档案」。先看这五个,选一个即进入资源导出。</p>
  <div class="wall" data-component="finals-wall" style="margin-bottom:20px">
    <a href="#s-g4r"><span class="stage float">${U('g4r')}</span><span class="code">G4R</span><span class="nm">直刃光圈 · 主推</span></a>
    <a href="#s-g24r"><span class="stage float">${U('g24r')}</span><span class="code">G24R</span><span class="nm">透镜交集 · 强推</span></a>
    <a href="#s-g9r"><span class="stage float">${U('g9r')}</span><span class="code">G9R</span><span class="nm">叶序点阵 · 签名</span></a>
    <a href="#s-g7r"><span class="stage float">${U('g7r')}</span><span class="code">G7R</span><span class="nm">嵌套V形 · 轻盈</span></a>
    <a href="#s-g1r"><span class="stage float">${U('g1r')}</span><span class="code">G1R</span><span class="nm">旋转连续环 · 耐看</span></a>
  </div>
  <div class="top3" data-component="final-verdict">
    <div class="card"><span class="stage">${U('g4r')}</span><div><h5>裁定 · G4R 直刃光圈(主推)</h5><p>六轮中唯一从未被点名批评的方向:窥视语义、机械精确、全形态无短板。如果不想再纠结,选它,直接进资源导出。</p></div></div>
    <div class="card"><span class="stage">${U('g24r')}</span><div><h5>裁定 · G24R 透镜交集(并列强推)</h5><p>概念密度最高——「编辑∩预览的重合处」就是产品定义本身;墨蓝红三色结构最清晰。想要品牌有「一句话故事」,选它。</p></div></div>
    <div class="card"><span class="stage">${U('g9r')}</span><div><h5>裁定 · G9R 叶序点阵(签名备选)</h5><p>最独特、延展面最广(动画/插画同源);代价是 29px 依赖简化形态。想要「别人没有的」,选它。</p></div></div>
  </div>
</section>`;

r('</header>', '</header>\n' + finals);

// --- retired strip at the very end of specs section (before showcase) ---
const retired = `
<div class="family-hd" data-component="retired-header"><h3>已淘汰(12)</h3><span class="caps mono">RETIRED WITH REASONS · 仅存档</span></div>
<p class="note" style="margin-bottom:14px">两轮淘汰留档,防复盘时重蹈覆辙:G2 斜向码栈(与加载条同形)、G3 十二分段环(进度环是 SaaS 通用词)、G5 相切三圆(语义靠解释)、G6 等距积木(等距方块是新粗野主义流行元素,不具独占性)、G8 渐细黄金螺旋(螺旋偏装饰,与编辑器身份最远)、G10 黄金作图(制图线 29px 必糊)、G11 折角名牌(折角是笔记类通用角标)、G16 层叠菱形(通用 layers 图标)、G19 月相序列(易读作暗色模式/天气)、G25 分屏位移(与系统分屏 glyph 同形)、G1/G4/G7 原始版(被 R 版替代)、G9 保留为 G9R 的附属简化形态。</p>`;
r('<section id="showcase"', retired + '\n<section id="showcase"');

// --- verdict text in refined section: replace old top3 with pointer to finals ---
r(`<div class="card"><span class="stage">${U('g4r')}</span><div><h5>TOP 1 · G4R 直刃光圈</h5><p>最贴近您给的市场样本(Claude/Perplexity 语言):一个元素、一色到底、节奏感强;朱砂红品牌感直接,29px 与单色层几乎零损耗。</p></div></div>`,
`<div class="card"><span class="stage">${U('g4r')}</span><div><h5>主推 · G4R 直刃光圈</h5><p>六轮中唯一从未被点名批评的方向:窥视语义、机械精确、全形态无短板;与 G24R 并列第一,见顶部「决赛圈」。</p></div></div>`);
r(`<div class="card"><span class="stage">${U('g7')}</span><div><h5>TOP 2 · G7 嵌套V形</h5>`, `<div class="card"><span class="stage">${U('g7r')}</span><div><h5>决赛 · G7R 嵌套V形</h5>`);
r(`<div class="card"><span class="stage">${U('g1')}</span><div><h5>TOP 3 · G1 三弧信号</h5><p>最耐看:半径等差 + 原点锚定,延展性最高(加载动画、进度语义都能接)。</p></div></div>`,
`<div class="card"><span class="stage">${U('g1r')}</span><div><h5>决赛 · G1R 旋转连续环</h5><p>半径等差 + 角度连续,延展性最高(加载动画、进度语义都能接);见顶部「决赛圈」。</p></div></div>`);

// page title/version bump
r('<title>DraftPeek 图标重构 · v9 精修轮评审板</title>', '<title>DraftPeek 图标重构 · v10 决赛圈</title>');
r('<h1>DraftPeek 图标 v9 · 几何精确系统 · 四强精修定稿池</h1>', '<h1>DraftPeek 图标 v10 · 决赛圈:五选一</h1>');

fs.writeFileSync(P, h);
console.log('v10 curation done');
