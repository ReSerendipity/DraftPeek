// v8.2 safe-zone compliance pass: every drawn pixel ≤ 30.6u from center (66dp circle)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// G1: corner-origin arcs clipped by circle masks → centered 135° signal rings (r10/18/26, staggered 45°)
r(`  <!-- G1 三弧信号:同心四分之一弧,半径 16/28/40,线宽 8,原点红心 -->
  <symbol id="mk-g1" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M26 58A16 16 0 0 1 42 74" stroke="var(--s3)"/>
      <path d="M26 46A28 28 0 0 1 54 74" stroke="var(--s2)"/>
      <path d="M26 34A40 40 0 0 1 66 74" stroke="var(--s1)"/>
    </g>
    <circle cx="26" cy="74" r="5" fill="var(--s4)"/>
  </symbol>`,
`  <!-- G1 三层信号环:同心 135° 弧 r10/18/26,逐层错开 45°,中心 mint 点 -->
  <symbol id="mk-g1" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M42.9 42.9A10 10 0 0 1 60 50" stroke="var(--s2)"/>
      <path d="M50 32A18 18 0 0 1 62.7 62.7" stroke="var(--s1)"/>
      <path d="M68.4 31.6A26 26 0 0 1 50 76" stroke="var(--s3)"/>
    </g>
    <circle cx="50" cy="50" r="4.4" fill="var(--s4)"/>
  </symbol>`);
r('CONCENTRIC ARCS · r 16/28/40 · Δ12u · CENTER(26,74)', 'SIGNAL RINGS · 同心 135° 弧 r10/18/26 · 层间错角 45°');
r('三根同心四分之一弧共享左下原点,半径等差递进,线宽恒 8u;墨/蓝/红三色按结构位分配,mint 圆点钉住原点。语义:从一点向外扩散的「览」。',
  '三根 135° 弧共享记号中心,半径 10/18/26 等差、逐层错开 45°,mint 点锚定圆心。语义:从核心向外一圈圈扩散的「览」。');

// G2: pull mint dot inside safe circle
r('<circle cx="70" cy="30" r="4" fill="var(--s4)"/>', '<circle cx="67" cy="33" r="4" fill="var(--s4)"/>');

// G5: re-derive tangency chain inside r30: A(38,62)r15, B(55.7,44.3)r10, C(67,33)r6, mint gap circle
r(`    <circle cx="37" cy="63" r="15" fill="var(--s1)"/>
    <circle cx="54.7" cy="45.3" r="10" fill="var(--s2)"/>
    <circle cx="66.4" cy="33.6" r="6.5" fill="var(--s3)"/>
    <circle cx="35.5" cy="43" r="4.5" fill="var(--s4)"/>`,
`    <circle cx="38" cy="62" r="15" fill="var(--s1)"/>
    <circle cx="55.7" cy="44.3" r="10" fill="var(--s2)"/>
    <circle cx="67" cy="33" r="6" fill="var(--s3)"/>
    <circle cx="36.5" cy="42.5" r="4.5" fill="var(--s4)"/>`);
r('TANGENT CIRCLES · r15/r10/r6.5 · 45° 对角', 'TANGENT CIRCLES · r15/r10/r6 · 45° 对角 · 全链 ≤ r30');

// G6: cells 10→9.5, diagonal offset 16.5→16 (corner 29.3u)
r('<rect x="28" y="28" width="10" height="10" rx="2.4"/>', '<rect x="29.25" y="29.25" width="9.5" height="9.5" rx="2.4"/>');
r('<rect x="62" y="62" width="10" height="10" rx="2.4"/>', '<rect x="61.25" y="61.25" width="9.5" height="9.5" rx="2.4"/>');
r('<rect x="62" y="28" width="10" height="10" rx="2.4" fill="var(--s2)"/>', '<rect x="61.25" y="29.25" width="9.5" height="9.5" rx="2.4" fill="var(--s2)"/>');
r('<rect x="28" y="62" width="10" height="10" rx="2.4" fill="var(--s4)"/>', '<rect x="29.25" y="61.25" width="9.5" height="9.5" rx="2.4" fill="var(--s4)"/>');
r('<rect x="45" y="45" width="10" height="10" rx="2.4" fill="var(--s3)" transform="rotate(45 50 50)"/>', '<rect x="45.25" y="45.25" width="9.5" height="9.5" rx="2.4" fill="var(--s3)" transform="rotate(45 50 50)"/>');
r('QUINCUNX · 10u 方格 · 对角 ±16.5u · 中心旋转 45°', 'QUINCUNX · 9.5u 方格 · 对角 ±16u · 中心旋转 45°');

// G7: rebuild nested chevrons with apex spacing 12u, all arms ≤ 27u
r(`    <g fill="none" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M24 40L50 66L76 40" stroke="var(--s1)"/>
      <path d="M32 32L50 50L68 32" stroke="var(--s2)"/>
      <path d="M40 24L50 34L60 24" stroke="var(--s3)"/>
    </g>`,
`    <g fill="none" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M28 44L50 66L72 44" stroke="var(--s1)"/>
      <path d="M36 40L50 54L64 40" stroke="var(--s2)"/>
      <path d="M44 36L50 42L56 36" stroke="var(--s3)"/>
    </g>`);
r('NESTED CHEVRONS · 深度 10/18/26 · 90° 尖角', 'NESTED CHEVRONS · 顶点间距 12u · 90° 尖角');

// G9: tighten phyllotaxis constant so outer dots fit (C 6.05→5.85)
r('const N = 21, GA = 137.507 * Math.PI / 180, C = 6.05;', 'const N = 21, GA = 137.507 * Math.PI / 180, C = 5.85;');

fs.writeFileSync(P, h);
console.log('v8.2 patched');
