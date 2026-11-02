// Export generator: G34R·红弦 → Android adaptive icon resources
// 9 chords, 20° fan, tangent r12.5, half 23.7 on 100-grid; scale 1.08 → 108dp viewport
const fs = require('fs');
const OUT = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/outputs/DraftPeek-icon-export';
fs.mkdirSync(OUT + '/res', { recursive: true });
fs.mkdirSync(OUT + '/store-png', { recursive: true });

const K = 1.08, C = 54, R0 = 12.5 * K, HALF = 23.7 * K, SW = (4 * K).toFixed(2);
const INK = '#1C2B3A', RED = '#C41E3A', BG = '#F5F7FA';

let paths = '';
for (let k = 0; k < 9; k++) {
  const th = k * 20 * Math.PI / 180;
  const tx = C + R0 * Math.cos(th), ty = C + R0 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  const x1 = (tx + HALF * dx).toFixed(2), y1 = (ty + HALF * dy).toFixed(2);
  const x2 = (tx - HALF * dx).toFixed(2), y2 = (ty - HALF * dy).toFixed(2);
  const col = k === 4 ? RED : INK; // k=4 → 顶部水平弦(θ=80°,法向 80°→弦向 -10°≈水平)
  paths += `    <path android:pathData="M${x1},${y1}L${x2},${y2}" android:strokeColor="${col}" android:strokeWidth="${SW}" android:strokeLineCap="round"/>\n`;
}

const fgXml = `<?xml version="1.0" encoding="utf-8"?>
<!-- DraftPeek launcher foreground · G34R 红弦 (9-chord line envelope, 20° fan) -->
<!-- 内容包络半径 31.1dp,在 66dp 安全圆内;背景层建议 @color/ic_launcher_background = #F5F7FA -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
${paths}</vector>
`;

const monoXml = fgXml
  .replace(/android:strokeColor="[^"]*"/g, 'android:strokeColor="#FF000000"')
  .replace('G34R 红弦 (9-chord line envelope, 20° fan)', 'G34R 红弦 · 主题单色层 (system tints, color ignored)')
  .replace('背景层建议 @color/ic_launcher_background = #F5F7FA', 'single-tone: only geometry matters');

const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
`;

const bgXml = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#F5F7FA</color>
</resources>
`;

fs.writeFileSync(OUT + '/res/drawable/ic_launcher_foreground.xml', fgXml.replace('res/drawable', ''));
fs.writeFileSync(OUT + '/res/drawable/ic_launcher_monochrome.xml', monoXml);
fs.writeFileSync(OUT + '/res/mipmap-anydpi/ic_launcher.xml', adaptiveXml);
fs.writeFileSync(OUT + '/res/mipmap-anydpi/ic_launcher_round.xml', adaptiveXml);
fs.writeFileSync(OUT + '/res/values/ic_launcher_background.xml', bgXml);
fs.writeFileSync(OUT + '/res/values-night/ic_launcher_background.xml', bgXml);

// also dump geometry json for PNG generation
fs.writeFileSync(OUT + '/geometry.json', JSON.stringify({
  K, C, R0, HALF, SW: 4 * K, INK, RED, BG,
  chords: Array.from({ length: 9 }, (_, k) => {
    const th = k * 20 * Math.PI / 180;
    const tx = C + R0 * Math.cos(th), ty = C + R0 * Math.sin(th);
    const dx = -Math.sin(th), dy = Math.cos(th);
    return { x1: +(tx + HALF * dx).toFixed(2), y1: +(ty + HALF * dy).toFixed(2), x2: +(tx - HALF * dx).toFixed(2), y2: +(ty - HALF * dy).toFixed(2), red: k === 4 };
  })
}));
console.log('XML resources written to', OUT);
