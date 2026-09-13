const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// clip defs for D and F
r('  <clipPath id="cp-g34e">', '  <clipPath id="cp-g34d"><circle cx="50" cy="50" r="27"/></clipPath>\n  <clipPath id="cp-g34f"><circle cx="50" cy="50" r="27"/></clipPath>\n  <clipPath id="cp-g34e">');

function wrapClip(symId, clipId) {
  const a = h.indexOf('<symbol id="' + symId + '" viewBox="0 0 100 100">');
  if (a < 0) { console.error(symId, 'MISS'); process.exit(1); }
  const b = h.indexOf('</symbol>', a);
  let seg = h.slice(a, b);
  if (seg.includes('clip-path')) { console.log(symId, 'already clipped'); return; }
  seg = seg.replace('>', '>\n    <g clip-path="url(#' + clipId + ')">');
  const lastClose = seg.lastIndexOf('</symbol>');
  seg = seg.slice(0, lastClose) + '    </g>\n  ' + seg.slice(lastClose);
  h = h.slice(0, a) + seg + h.slice(b);
}
wrapClip('mk-g34d', 'cp-g34d');
wrapClip('mk-g34f', 'cp-g34f');
fs.writeFileSync(P, h);
console.log('G34D/F clipped to main circle r27');
