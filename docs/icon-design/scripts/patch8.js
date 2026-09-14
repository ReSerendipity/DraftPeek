const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// G1: recenter origin to (26,74) so all bounds fit the 66dp safe circle
r(`<g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M24 60A16 16 0 0 1 40 76" stroke="var(--s3)"/>
      <path d="M24 48A28 28 0 0 1 52 76" stroke="var(--s2)"/>
      <path d="M24 36A40 40 0 0 1 64 76" stroke="var(--s1)"/>
    </g>
    <circle cx="24" cy="76" r="5" fill="var(--s4)"/>`,
`<g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M26 58A16 16 0 0 1 42 74" stroke="var(--s3)"/>
      <path d="M26 46A28 28 0 0 1 54 74" stroke="var(--s2)"/>
      <path d="M26 34A40 40 0 0 1 66 74" stroke="var(--s1)"/>
    </g>
    <circle cx="26" cy="74" r="5" fill="var(--s4)"/>`);
r('CONCENTRIC ARCS · r 16/28/40 · Δ12u', 'CONCENTRIC ARCS · r 16/28/40 · Δ12u · CENTER(26,74)');

// G4: replace hexagon-edge outline with true rotational aperture blades (outer r26 → inner r10, +50° lead)
r(`<g stroke-width="8" stroke-linecap="round">
      <path d="M50 24L72.5 37" stroke="var(--s1)"/>
      <path d="M72.5 37V63" stroke="var(--s1)" transform="translate(0 0)"/>
      <path d="M72.5 63L50 76" stroke="var(--s2)"/>
      <path d="M50 76L27.5 63" stroke="var(--s1)"/>
      <path d="M27.5 63V37" stroke="var(--s1)"/>
      <path d="M27.5 37L50 24" stroke="var(--s1)"/>
    </g>
    <path d="M50 41L57.8 45.5V54.5L50 59L42.2 54.5V45.5Z" fill="var(--s3)"/>`,
`<g stroke-width="8" stroke-linecap="round">
      <path d="M50 24L42.3 43.6" stroke="var(--s1)"/>
      <path d="M27.5 37L40.6 53.4" stroke="var(--s1)"/>
      <path d="M27.5 63L48.3 59.8" stroke="var(--s2)"/>
      <path d="M50 76L57.7 56.4" stroke="var(--s1)"/>
      <path d="M72.5 63L59.4 46.6" stroke="var(--s1)"/>
      <path d="M72.5 37L51.7 40.2" stroke="var(--s1)"/>
    </g>
    <circle cx="50" cy="50" r="5.4" fill="var(--s3)"/>`);
r('IRIS CHORDS · 六边形隔角弦 · 60° 对称', 'APERTURE BLADES · outer r26 → inner r10 · lead 50° · 60° 旋转对称');
r('六条正六边形隔角弦围出机械光圈,一条弦换蓝做「破序」,中心红六边形是孔也是心。语义:快门/窥视的精确版。',
  '六条直刃以 60° 旋转对称排布,外端落在 r26 圆上、内端切于 r10 光瞳,自然围出六边形快门孔;一刃换蓝做「破序」,红瞳居中。语义:窥视的机械精确版。');

// G6: shrink cells 11→10 so diagonal corners fit safe circle
r('<rect x="27.5" y="27.5" width="11" height="11" rx="2.6"/>', '<rect x="28" y="28" width="10" height="10" rx="2.4"/>');
r('<rect x="61.5" y="61.5" width="11" height="11" rx="2.6"/>', '<rect x="62" y="62" width="10" height="10" rx="2.4"/>');
r('<rect x="61.5" y="27.5" width="11" height="11" rx="2.6" fill="var(--s2)"/>', '<rect x="62" y="28" width="10" height="10" rx="2.4" fill="var(--s2)"/>');
r('<rect x="27.5" y="61.5" width="11" height="11" rx="2.6" fill="var(--s4)"/>', '<rect x="28" y="62" width="10" height="10" rx="2.4" fill="var(--s4)"/>');
r('<rect x="44.5" y="44.5" width="11" height="11" rx="2.6" fill="var(--s3)" transform="rotate(45 50 50)"/>', '<rect x="45" y="45" width="10" height="10" rx="2.4" fill="var(--s3)" transform="rotate(45 50 50)"/>');
r('QUINCUNX · 11u 方格 · 中心旋转 45°', 'QUINCUNX · 10u 方格 · 对角 ±16.5u · 中心旋转 45°');

// G8: rescale Fibonacci tiling to fit safe circle (34/21/13 → 32/20/12)
r('<rect x="17" y="33" width="34" height="34" rx="3" fill="var(--s1)"/>', '<rect x="19" y="35" width="32" height="32" rx="3" fill="var(--s1)"/>');
r('<rect x="51" y="33" width="21" height="21" rx="2.6" fill="var(--s2)"/>', '<rect x="51" y="35" width="20" height="20" rx="2.6" fill="var(--s2)"/>');
r('<rect x="51" y="54" width="13" height="13" rx="2" fill="var(--s3)"/>', '<rect x="51" y="55" width="12" height="12" rx="2" fill="var(--s3)"/>');
r('<path d="M17 67A34 34 0 0 1 51 33" fill="none" stroke="var(--s4)" stroke-width="7" stroke-linecap="round"/>', '<path d="M19 67A32 32 0 0 1 51 35" fill="none" stroke="var(--s4)" stroke-width="7" stroke-linecap="round"/>');
r('<circle cx="51" cy="67" r="3.4" fill="var(--s5)"/>', '<circle cx="51" cy="67" r="3.4" fill="var(--s5)"/>');
r('FIBONACCI TILING · 34/21/13 · 内接弧', 'FIBONACCI TILING · 32/20/12 · 内接四分之一弧');

fs.writeFileSync(P, h);
console.log('gen8 geometry patched');
