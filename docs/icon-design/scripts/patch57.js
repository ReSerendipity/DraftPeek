// v11.3: eye-window-gap fix for G58/G59 — head enlarged to 12u, r5 circular window cut via mask,
// red eye dot r2.2 floating at window center with 2.8u gap (eye white). Single-variable: nothing else changes.
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// 1) add head-window masks right after the composite clipPaths
r('  <clipPath id="cp-g59"><circle cx="50" cy="50" r="27"/></clipPath>',
`  <clipPath id="cp-g59"><circle cx="50" cy="50" r="27"/></clipPath>
  <mask id="mx-g58h"><path d="M29 49L41 49.5L39.5 57.5L30 56Z" fill="white"/><circle cx="35" cy="53.2" r="5" fill="black"/></mask>
  <mask id="mx-g59h"><path d="M59 49.5L71 49L70 56.5L60.5 57.5Z" fill="white"/><circle cx="65" cy="53.2" r="5" fill="black"/></mask>`);

// 2) G58 head: enlarge + window mask; beak reattach; eye dot r2.2 at window center
r('    <path d="M34 51.6L38.8 51.6L37.2 55.6L33.2 54Z" fill="var(--s1)"/>\n    <path d="M32.4 51.6L26.8 53.2L33.2 54Z" fill="var(--s1)"/>\n    <circle cx="35.5" cy="53.3" r="2.6" fill="var(--s3)"/>',
  '    <path mask="url(#mx-g58h)" d="M29 49L41 49.5L39.5 57.5L30 56Z" fill="var(--s1)"/>\n    <path d="M29 51.5L23 53.2L29.5 55.5Z" fill="var(--s1)"/>\n    <circle cx="35" cy="53.2" r="2.2" fill="var(--s3)"/>');

// 3) G59 head (mirrored): same treatment
r('    <path d="M66 51.6L61.2 51.6L62.8 55.6L66.8 54Z" fill="var(--s1)"/>\n    <path d="M67.6 51.6L73.2 53.2L66.8 54Z" fill="var(--s1)"/>\n    <circle cx="65.5" cy="53.3" r="2.6" fill="var(--s3)"/>',
  '    <path mask="url(#mx-g59h)" d="M59 49.5L71 49L70 56.5L60.5 57.5Z" fill="var(--s1)"/>\n    <path d="M71 51L77 53.2L70.5 55.5Z" fill="var(--s1)"/>\n    <circle cx="65" cy="53.2" r="2.2" fill="var(--s3)"/>');

// 4) concept text updates
r("c:'按您的配方合成:G34W 的方孔弦窗为底,写生燕(远翼蓝/体墨/腹奶油/喉朱砂)穿窗而过,头部一枚红点=眼睛的点睛之笔。弦线细至 3.2 让位给燕身。'",
  "c:'G34W 方孔弦窗为底,燕身穿窗而过。眼部已按「眼-窗-隙」修正:12u 燕头挖出 r5 圆窗(窗缘即眼眶),红点眼睛(r2.2)悬浮窗心、四周 2.8u 间隙即眼白——像 G34L/W/B/O 那样因留白而一眼可辨。'");
r("c:'咬边版合成:燕身向缺口飞去,红点眼睛在头部——「从缺口窥入」的叙事闭环;弦线同样让位。'",
  "c:'G34B 咬边弦窗为底,燕身向缺口飞去。眼部同构修正:12u 头挖 r5 圆窗,红点悬浮窗心、2.8u 眼白间隙——「从缺口窥入」的叙事闭环。'");

fs.writeFileSync(P, h);
console.log('v11.3 eye-window-gap applied to G58/G59');
