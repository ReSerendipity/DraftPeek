// v9.4: four challenger candidates — semantics-first (honeycomb / QR locator / text lines / ziggurat)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

const newSymbols = `
  <!-- G30 蜂窝三簇:三枚正六边形精确平铺咬合,缝隙 1.3u -->
  <symbol id="mk-g30" viewBox="0 0 100 100">
    <path d="M50 25.8L58.8 30.9V41.1L50 46.2L41.2 41.1V30.9Z" fill="var(--s3)"/>
    <path d="M40.5 42.3L49.3 47.4V57.6L40.5 62.7L31.7 57.6V47.4Z" fill="var(--s1)"/>
    <path d="M59.5 42.3L68.3 47.4V57.6L59.5 62.7L50.7 57.6V47.4Z" fill="var(--s2)"/>
  </symbol>

  <!-- G31 定位角标:三组嵌套方占三角,内芯红/蓝/mint,第四角留白 -->
  <symbol id="mk-g31" viewBox="0 0 100 100">
    <rect x="23" y="23" width="15" height="15" rx="3.5" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <rect x="57" y="23" width="15" height="15" rx="3.5" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <rect x="23" y="57" width="15" height="15" rx="3.5" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <rect x="28" y="28" width="5" height="5" rx="1.4" fill="var(--s3)"/>
    <rect x="62" y="28" width="5" height="5" rx="1.4" fill="var(--s2)"/>
    <rect x="28" y="62" width="5" height="5" rx="1.4" fill="var(--s4)"/>
  </symbol>

  <!-- G32 文本行阵:四条圆头行,第三行红色加长=正在预览的那一行 -->
  <symbol id="mk-g32" viewBox="0 0 100 100">
    <rect x="28" y="30" width="44" height="7" rx="3.5" fill="var(--s1)"/>
    <rect x="28" y="41" width="30" height="7" rx="3.5" fill="var(--s1)"/>
    <rect x="28" y="52" width="44" height="7" rx="3.5" fill="var(--s3)"/>
    <rect x="28" y="63" width="22" height="7" rx="3.5" fill="var(--s2)"/>
  </symbol>

  <!-- G33 阶梯塔:三层逐级收窄,墨/蓝/红 -->
  <symbol id="mk-g33" viewBox="0 0 100 100">
    <rect x="30" y="58" width="40" height="14" rx="4" fill="var(--s1)"/>
    <rect x="35" y="45" width="30" height="13" rx="4" fill="var(--s2)"/>
    <rect x="40" y="32" width="20" height="13" rx="4" fill="var(--s3)"/>
  </symbol>`;
r('  <symbol id="mk-old"', newSymbols + '\n\n  <symbol id="mk-old"');

r("{id:'g29', code:'G29', name:'黄金角扇', gene:'GOLDEN ANGLE FAN · 137.507° × 8 · 外刃渐长', c:'G9R 叶序点阵的放射版:8 枚圆头刃按黄金角递增排布,由于 137.507° 不可通约,任何两刃都不对齐——自然界的排布策略,红刃是最新的一片叶。'},",
`{id:'g29', code:'G29', name:'黄金角扇', gene:'GOLDEN ANGLE FAN · 137.507° × 8 · 外刃渐长', c:'G9R 叶序点阵的放射版:8 枚圆头刃按黄金角递增排布,由于 137.507° 不可通约,任何两刃都不对齐——自然界的排布策略,红刃是最新的一片叶。'},
  {id:'g30', code:'G30', name:'蜂窝三簇', gene:'HONEYCOMB · 正六边形平铺 · 缝隙 1.3u', c:'三枚正六边形以精确平铺关系咬合(夹角 120°、缝 1.3u),红/墨/蓝——模块与插件生态;蜂窝是效率工具的经典语汇,三簇咬合加结构色分配是自己的表达。'},
  {id:'g31', code:'G31', name:'定位角标', gene:'LOCATOR TRIO · 三组嵌套方 · 第四角留白', c:'二维码三个定位角的抽象:描边外框+实心内芯占据三角,内芯红/蓝/mint——「扫码即达」的编码语义;第四角留白是构图呼吸位。'},
  {id:'g32', code:'G32', name:'文本行阵', gene:'TEXT LINES · 四行圆头条 · 第三行红=预览中', c:'四条圆头文本行,第三行红色加长——「正在被预览的那一行」。DraftPeek 的本体就是文本行,这是最贴身的抽象,29px 下依然是一条条分明的行。'},
  {id:'g33', code:'G33', name:'阶梯塔', gene:'ZIGGURAT · 三层收窄 · 40/30/20u', c:'三层阶梯逐级收窄(墨/蓝/红)——从草稿到成稿的逐级构建,编译进度的纪念碑形态。'},`);

r("  ['G29','黄金角扇','黄金角放射',[4,4,5,4,4,5],0],",
`  ['G29','黄金角扇','黄金角放射',[4,4,5,4,4,5],0],
  ['G30','蜂窝三簇','模块集群',[4,4,4,4,5,4],0],
  ['G31','定位角标','扫码编码',[5,3,4,5,4,4],0],
  ['G32','文本行阵','文本本体',[4,5,3,5,5,4],0],
  ['G33','阶梯塔','逐级构建',[3,4,4,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.4 four challengers added');
