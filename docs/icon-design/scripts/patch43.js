// v10.4: swallow variants on the G34 gene — origami / silhouette / swallow-over-strings
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

const newSymbols = `
  <!-- G50 折纸燕:全直线多边形拼接,远翼蓝/体墨/燕尾叉红,无眼纯剪影 -->
  <symbol id="mk-g50" viewBox="0 0 100 100">
    <path d="M44 50L60 30L62 50Z" fill="var(--s2)"/>
    <path d="M24 64L34 53L56 52L64 58L50 66Z" fill="var(--s1)"/>
    <path d="M32 54L66 28L58 52L40 56Z" fill="var(--s1)"/>
    <path d="M62 56L78 48L66 62Z" fill="var(--s3)"/>
    <path d="M62 58L72 68L58 64Z" fill="var(--s3)"/>
  </symbol>

  <!-- G51 剪影燕:单路径流畅剪影,镰刀翼+深剪叉尾,下叉红 -->
  <symbol id="mk-g51" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M24 60Q30 54 36 54L44 50L64 30Q58 46 56 48L64 54L78 46L66 56L74 64Q60 62 52 60Q40 64 32 62Q26 62 24 60Z"/>
    <path d="M66 56L74 64L62 60Z" fill="var(--s3)"/>
  </symbol>

  <!-- G52 燕掠弦窗:九弦弦窗 + 红燕掠过( mini 折纸燕 0.5x) -->
  <symbol id="mk-g52" viewBox="0 0 100 100">
    <g stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round">
      <path d="M73.7 50L26.3 38.8" fill="none"/>
      <path d="M69.6 61.6L30.4 38.4" fill="none" stroke="var(--s2)"/>
      <path d="M61.6 69.6L38.4 30.4" fill="none"/>
      <path d="M50 73.7L50 26.3" fill="none"/>
      <path d="M38.4 69.6L69.6 30.4" fill="none"/>
      <path d="M26.3 61.6L73.7 38.4" fill="none"/>
      <path d="M30.4 50L73.7 50" fill="none" stroke="var(--s2)"/>
      <path d="M38.8 26.3L61.2 73.7" fill="none"/>
      <path d="M26.3 50L61.2 26.3" fill="none" stroke="var(--s2)"/>
    </g>
    <path d="M47 48.5L55 38.5L56 48.5Z" fill="var(--s3)"/>
    <path d="M37 57L42 51.5L53 51L57 54L50 58Z" fill="var(--s3)"/>
    <path d="M41 52L58 39L54 51L45 53Z" fill="var(--s3)"/>
    <path d="M56 53L64 49L58 56Z" fill="var(--s3)"/>
    <path d="M56 54L61 59L54 57Z" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

// --- marks entries + matrix rows ---
r("{id:'g49', code:'G49', name:'弓弦张力', gene:'BOW & STRING · 120° 弓弧 + 弦线 · 矢待发', c:'朱砂弓弧与墨色弦线互相较劲,弓形弦上的红点是一支拉满的箭——曲线的张力被弦线「拉直」的那一刻,就是预览诞生的瞬间。'},",
`{id:'g49', code:'G49', name:'弓弦张力', gene:'BOW & STRING · 120° 弓弧 + 弦线 · 矢待发', c:'朱砂弓弧与墨色弦线互相较劲,弓形弦上的红点是一支拉满的箭——曲线的张力被弦线「拉直」的那一刻,就是预览诞生的瞬间。'},
  {id:'g50', code:'G50', name:'折纸燕', gene:'ORIGAMI SWALLOW · 全直线多边形 · 燕尾叉红', c:'G34 直线基因的动物化:燕子完全由直线多边形折出——远翼蓝、体墨、深剪叉燕尾红。轻、快、掠过,正是「轻览」的气质;无眼纯剪影。'},
  {id:'g51', code:'G51', name:'剪影燕', gene:'SILHOUETTE SWALLOW · 单路径流畅剪影', c:'镰刀翼 + 深剪叉尾的流畅剪影,滑翔姿态一次成形;下叉尾羽红。与圆鸽类(Twitter 系)靠燕尾叉与折角翼缘区隔。'},
  {id:'g52', code:'G52', name:'燕掠弦窗', gene:'SWALLOW × ENVELOPE · 九弦窗 + 红燕掠过', c:'两个母题的嵌合:九弦弦窗是「窗」,红燕掠窗而过是「览」——轻览的全部动作被一个瞬间收拢。'},`);

r("  ['G49','弓弦张力','拉满待发',[4,4,5,4,4,4],0],",
`  ['G49','弓弦张力','拉满待发',[4,4,5,4,4,4],0],
  ['G50','折纸燕','直线动物',[5,4,5,4,4,4],2],
  ['G51','剪影燕','流畅剪影',[5,4,4,4,4,3],0],
  ['G52','燕掠弦窗','双母题嵌合',[4,5,4,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v10.4 three swallow variants added');
