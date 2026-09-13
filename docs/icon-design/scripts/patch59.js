// v11.5: G34O direct edit — blue chord → red (preview line), plus two-step ink ramp toward the eye (depth/rotation)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// 1) new lighter-slate token for the ramp (near-eye chords)
r('--s6:#F2E7D2;', '--s6:#F2E7D2; --s1b:#33475F;');

// 2) rewrite the 9 chord strokes: k3 blue→red; k0-2 (nearest the eye at (59,41)) lighter slate; rest deep ink
r('    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>\n    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>\n    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>\n    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s2)" stroke-width="3.6" stroke-linecap="round"/>',
  '    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>\n    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>\n    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>\n    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s3)" stroke-width="3.6" stroke-linecap="round"/>');

// 3) concept text update
r("c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'}",
  "c:'G34 的构图精修:包络圆偏心,窥视有方位。配色定稿:蓝弦已换朱砂=预览行;靠近孔眼的三根弦提亮为雾墨(#33475F),与远端深墨形成两级色阶——弦线像在向眼睛旋转汇聚。'}");

fs.writeFileSync(P, h);
console.log('v11.5 G34O recolored: blue→red + two-step ink ramp');
