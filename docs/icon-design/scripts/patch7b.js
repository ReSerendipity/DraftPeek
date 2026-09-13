const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen7.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// 1) add gradient/context tokens to :root
r('  --ctx-ink-a:#33475F; --ctx-ink-b:#1C2838;',
  `  --ctx-ink-a:#33475F; --ctx-ink-b:#1C2838;
  --ctx-ink-shape:#DCE4EF; --ctx-ink-sheet:#E9EEF6; --ctx-ink-sheet2:#DCE6F4; --ctx-screen-lift:#22334A; --ctx-green-lift:#34D399; --ctx-amber-lift:#F0B24A;
  --ctx-red-sheet:#FFF3F5; --ctx-red-sheet2:#FFE3E8; --ctx-red-blue:#FFEDEE; --ctx-red-green:#1E7A57; --ctx-red-amber:#8E1526;
  --g-ink-a:#3E5573; --g-ink-b:#16202E;
  --g-paper-a:#FFFFFF; --g-paper-b:#DDE5EF;
  --g-paper2-a:#F2F6FB; --g-paper2-b:#CFDCEE;
  --g-screen-a:#1B2839; --g-screen-b:#0D141F;
  --g-flame-hi:#F6C445; --g-spec-amber:#D97706; --g-spec-green:#10B981; --g-teal:#2DD4BF;
  --f-shadow:#141E2D; --chip-b:#EDF1F7; --mono-fg:#F2F5F9;`);

// 2) gradient stops -> tokens
r('<stop offset="0" stop-color="#3E5573"/><stop offset="1" stop-color="#16202E"/>',
  '<stop offset="0" stop-color="var(--g-ink-a)"/><stop offset="1" stop-color="var(--g-ink-b)"/>');
r('<stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="#DDE5EF"/>',
  '<stop offset="0" stop-color="var(--g-paper-a)"/><stop offset="1" stop-color="var(--g-paper-b)"/>');
r('<stop offset="0" stop-color="#F2F6FB"/><stop offset="1" stop-color="#CFDCEE"/>',
  '<stop offset="0" stop-color="var(--g-paper2-a)"/><stop offset="1" stop-color="var(--g-paper2-b)"/>');
r('<stop offset="0" stop-color="#1B2839"/><stop offset="1" stop-color="#0D141F"/>',
  '<stop offset="0" stop-color="var(--g-screen-a)"/><stop offset="1" stop-color="var(--g-screen-b)"/>');
r('<stop offset="0" stop-color="var(--seed-primary)"/><stop offset=".35" stop-color="#D97706"/><stop offset=".68" stop-color="#10B981"/><stop offset="1" stop-color="var(--seed-accent)"/>',
  '<stop offset="0" stop-color="var(--seed-primary)"/><stop offset=".35" stop-color="var(--g-spec-amber)"/><stop offset=".68" stop-color="var(--g-spec-green)"/><stop offset="1" stop-color="var(--seed-accent)"/>');
r('<stop offset="1" stop-color="#F6C445"/>', '<stop offset="1" stop-color="var(--g-flame-hi)"/>');
r('<stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="#F6C445"/>',
  '<stop offset="0" stop-color="var(--g-paper-a)"/><stop offset="1" stop-color="var(--g-flame-hi)"/>');
r('<stop offset="0" stop-color="#F6C445" stop-opacity=".55"/><stop offset="1" stop-color="#F6C445" stop-opacity="0"/>',
  '<stop offset="0" stop-color="var(--g-flame-hi)" stop-opacity=".55"/><stop offset="1" stop-color="var(--g-flame-hi)" stop-opacity="0"/>');
r('<stop offset="0" stop-color="#FFFFFF" stop-opacity=".8"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>',
  '<stop offset="0" stop-color="var(--g-paper-a)" stop-opacity=".8"/><stop offset="1" stop-color="var(--g-paper-a)" stop-opacity="0"/>');
r('flood-color="#141E2D"', 'flood-color="var(--f-shadow)"');

// 3) in-symbol literal colors -> tokens
r('<circle cx="33" cy="33" r="2.2" fill="#2DD4BF"/>', '<circle cx="33" cy="33" r="2.2" fill="var(--g-teal)"/>');

// 4) CSS context overrides -> tokens
r('--m2:#DCE4EF;--m6:#E9EEF6;--m6b:#DCE6F4;--m8:#22334A;--m4:#34D399;--m5:#F0B24A;',
  '--m2:var(--ctx-ink-shape);--m6:var(--ctx-ink-sheet);--m6b:var(--ctx-ink-sheet2);--m8:var(--ctx-screen-lift);--m4:var(--ctx-green-lift);--m5:var(--ctx-amber-lift);');
r('--m3:#FFEDEE;--m4:#1E7A57;--m5:#8E1526;--m6:#FFF3F5;--m6b:#FFE3E8;',
  '--m3:var(--ctx-red-blue);--m4:var(--ctx-red-green);--m5:var(--ctx-red-amber);--m6:var(--ctx-red-sheet);--m6b:var(--ctx-red-sheet2);');
r('.chip.mono-layer{color:#F2F5F9;', '.chip.mono-layer{color:var(--mono-fg);');
r('background:linear-gradient(180deg,var(--pure-white),#EDF1F7);', 'background:linear-gradient(180deg,var(--pure-white),var(--chip-b));');

fs.writeFileSync(P, h);
console.log('gen7 tokenized');
