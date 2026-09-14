// v9.9: three paradox-curve marks — straight-becomes-coil / arcs implying square / bow & string tension
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

const newSymbols = `
  <mask id="mx-g48"><rect x="22" y="22" width="56" height="56" rx="13" fill="white"/>
    <path d="M38 38Q50 45 62 38Q55 50 62 62Q50 55 38 62Q45 50 38 38Z" fill="black"/>
  </mask>

  <!-- G47 曲直同体:一笔从水平直线无缝卷成整圆,渐细缎带 -->
  <symbol id="mk-g47" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M30 57H58A8.6 8.6 0 0 1 49 48A7.9 7.9 0 0 1 58 39A7.1 7.1 0 0 1 67 48A6.4 6.4 0 0 1 58 60L58 60.5A12.5 12.5 0 0 0 70.5 48A14 14 0 0 0 58 34A15.5 15.5 0 0 0 42.5 48A17 17 0 0 0 58 65L30 65Z"/>
    <circle cx="58" cy="48" r="3.5" fill="var(--s3)"/>
  </symbol>

  <!-- G48 四弧成方:墨版上以四段内凹弧蓄出方形负空间,红点悬于孔心 -->
  <symbol id="mk-g48" viewBox="0 0 100 100">
    <g mask="url(#mx-g48)"><rect x="22" y="22" width="56" height="56" rx="13" fill="var(--s1)"/></g>
    <rect x="22" y="22" width="56" height="56" rx="13" fill="none" stroke="var(--mk-edge)" stroke-width="1.4"/>
    <circle cx="50" cy="50" r="4.5" fill="var(--s3)"/>
  </symbol>

  <!-- G49 弓弦张力:120° 朱砂弓弧 + 墨色弦线,弦上红点=拉满的箭待发 -->
  <symbol id="mk-g49" viewBox="0 0 100 100">
    <path d="M26 58A24.3 24.3 0 0 1 74 58" fill="none" stroke="var(--s3)" stroke-width="7" stroke-linecap="round"/>
    <path d="M26 58H74" stroke="var(--s1)" stroke-width="4.5" stroke-linecap="round"/>
    <circle cx="50" cy="44" r="4.5" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <!-- G40 波瓣环', newSymbols + '\n\n  <!-- G40 波瓣环');

// --- marks entries + matrix rows ---
r("{id:'g46', code:'G46', name:'偏心涟漪', gene:'DRIFTING RIPPLES · r26/19/12 · 环心漂移 3u', c:'年轮式三环,环心逐级向右上漂移——生长有方向的涟漪;最内环红。'},",
`{id:'g46', code:'G46', name:'偏心涟漪', gene:'DRIFTING RIPPLES · r26/19/12 · 环心漂移 3u', c:'年轮式三环,环心逐级向右上漂移——生长有方向的涟漪;最内环红。'},
  {id:'g47', code:'G47', name:'曲直同体', gene:'STRAIGHT→COIL · 一笔 · 宽 8→1.5 渐细', c:'一 Stroke 两个世界:前半是水平的直线,滑到切点后无缝卷成一整圈渐细圆环——直线与曲线不再是两种元素,而是同一笔的前后两口气。红点钉在圆心。'},
  {id:'g48', code:'G48', name:'四弧成方', gene:'ARCS→SQUARE · 四段内凹弧蓄出方形负空间', c:'G34 的倒置悖论:直线蓄出圆,这里让曲线蓄出「方」——墨版中央的方形孔,四边全部内凹成弧,方与曲同时成立;红点悬于孔心。'},
  {id:'g49', code:'G49', name:'弓弦张力', gene:'BOW & STRING · 120° 弓弧 + 弦线 · 矢待发', c:'朱砂弓弧与墨色弦线互相较劲,弓形弦上的红点是一支拉满的箭——曲线的张力被弦线「拉直」的那一刻,就是预览诞生的瞬间。'},`);

r("  ['G46','偏心涟漪','生长漂移',[4,4,4,4,4,4],0],",
`  ['G46','偏心涟漪','生长漂移',[4,4,4,4,4,4],0],
  ['G47','曲直同体','一笔两气',[5,5,5,4,4,5],2],
  ['G48','四弧成方','曲蓄成方',[5,4,5,4,4,4],0],
  ['G49','弓弦张力','拉满待发',[4,4,5,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.9 three paradox-curve marks added');
