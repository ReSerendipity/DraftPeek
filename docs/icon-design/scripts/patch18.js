const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// G31: shrink trio so outer corners sit within r30.6 (marker 14u, stroke 4.5, centers at ±19.5 diagonal)
r(`    <rect x="23" y="23" width="15" height="15" rx="3.5" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <rect x="57" y="23" width="15" height="15" rx="3.5" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <rect x="23" y="57" width="15" height="15" rx="3.5" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <rect x="28" y="28" width="5" height="5" rx="1.4" fill="var(--s3)"/>
    <rect x="62" y="28" width="5" height="5" rx="1.4" fill="var(--s2)"/>
    <rect x="28" y="62" width="5" height="5" rx="1.4" fill="var(--s4)"/>`,
`    <rect x="30.5" y="30.5" width="14" height="14" rx="3.2" fill="none" stroke="var(--s1)" stroke-width="4.5"/>
    <rect x="55.5" y="30.5" width="14" height="14" rx="3.2" fill="none" stroke="var(--s1)" stroke-width="4.5"/>
    <rect x="30.5" y="55.5" width="14" height="14" rx="3.2" fill="none" stroke="var(--s1)" stroke-width="4.5"/>
    <rect x="35.2" y="35.2" width="4.6" height="4.6" rx="1.3" fill="var(--s3)"/>
    <rect x="57.7" y="35.2" width="4.6" height="4.6" rx="1.3" fill="var(--s2)"/>
    <rect x="35.2" y="57.7" width="4.6" height="4.6" rx="1.3" fill="var(--s4)"/>`);
r('LOCATOR TRIO · 三组嵌套方 · 第四角留白', 'LOCATOR TRIO · 三组嵌套方 14u · 第四角留白');

fs.writeFileSync(P, h);
console.log('G31 fixed');
