// v11.6: revert G34O accent chord to blue; add accent-chord color comparison strip (blue / amber / mint / crimson / mist-ink)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// 1) revert blue chord in original mk-g34o
r('<path d="M28.8 63.2L96.4 38.6" stroke="var(--s3)" stroke-width="3.6" stroke-linecap="round"/>',
  '<path d="M28.8 63.2L96.4 38.6" stroke="var(--s2)" stroke-width="3.6" stroke-linecap="round"/>');

// 2) new crimson token + context coverage
r('--s1b:#33475F;', '--s1b:#33475F; --s7:#A81832;');
r('--s1:currentColor;--s1b:currentColor;', '--s1:currentColor;--s1b:currentColor;--s7:currentColor;');
r('--s1:var(--ctx-shape);--s1b:var(--ctx-shape);', '--s1:var(--ctx-shape);--s1b:var(--ctx-shape);--s7:var(--ctx-red);');

// 3) clone mk-g34o with different accent chords
const a0 = h.indexOf('<symbol id="mk-g34o"');
const b0 = h.indexOf('</symbol>', a0) + 9;
const orig = h.slice(a0, b0);
const BLUE = '<path d="M28.8 63.2L96.4 38.6" stroke="var(--s2)" stroke-width="3.6" stroke-linecap="round"/>';
function clone(id, comment, accent) {
  return orig.replace('id="mk-g34o"', 'id="' + id + '"')
    .replace('cp-g34o', 'cp-' + id)
    .replace('url(#cp-' + id + ')', 'url(#' + 'cp-' + id + ')')
    .replace(BLUE, accent)
    .replace('<!-- G34O 偏心窥圆:弦切于 (59,41) r10.5 偏心圆,外缘裁入主圆 r27 -->', comment);
}
const clones = [
  clone('mk-g34o-amber', '<!-- G34O·琥珀 -->', '<path d="M28.8 63.2L96.4 38.6" stroke="var(--s5)" stroke-width="3.6" stroke-linecap="round"/>'),
  clone('mk-g34o-mint', '<!-- G34O·薄荷 -->', '<path d="M28.8 63.2L96.4 38.6" stroke="var(--s4)" stroke-width="3.6" stroke-linecap="round"/>'),
  clone('mk-g34o-crimson', '<!-- G34O·绯红 -->', '<path d="M28.8 63.2L96.4 38.6" stroke="var(--s7)" stroke-width="3.6" stroke-linecap="round"/>'),
  clone('mk-g34o-mist', '<!-- G34O·雾墨 -->', '<path d="M28.8 63.2L96.4 38.6" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>'),
].join('\n');
h = h.slice(0, b0) + '\n' + clones + h.slice(b0);

// 4) comparison strip after VARIANTS families in specsHtml
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;
const strip = `
  <div class="family-hd" data-component="family-header"><h3>G34O · 强调弦色对比</h3><span class="caps mono">ACCENT CHORD SHOOTOUT · 五选一</span></div>
  <div class="discipline" style="grid-template-columns:repeat(auto-fit,minmax(150px,1fr))">
    ${[['g34o', '电光蓝(原版)', '#3178C6'], ['g34o-amber', '琥珀', '#F2A93B'], ['g34o-mint', '薄荷绿', '#17B98C'], ['g34o-crimson', '绯红(深朱砂)', '#A81832'], ['g34o-mist', '雾墨(无彩)', '#33475F']]
      .map(([id, name, hex]) => `<div class="dz" style="display:grid;justify-items:center;gap:8px">
      <span class="chip" style="width:110px;height:110px;border-radius:var(--chip-r)">${U(id)}</span>
      <span class="mono" style="font-size:11px;font-weight:700">${name}</span>
      <span class="mono" style="font-size:10px;color:var(--faint)">${hex}</span>
    </div>`).join('\n    ')}
  </div>`;
const specsEnd = "  + VARIANTS.map(([title, sub, ids]) =>";
if (!h.includes(specsEnd)) { console.error('specsHtml anchor MISS'); process.exit(1); }
// append strip after the VARIANTS join within specsHtml
r("+ VARIANTS.map(([title, sub, ids]) => '<div class=\"family-hd\" data-component=\"family-header\"><h3>' + title + '</h3><span class=\"caps mono\">' + sub + '</span></div>'\n    + ids.map(id => card(marks.find(m => m.id === id))).join('\\n')).join('\\n');",
  "+ VARIANTS.map(([title, sub, ids]) => '<div class=\"family-hd\" data-component=\"family-header\"><h3>' + title + '</h3><span class=\"caps mono\">' + sub + '</span></div>'\n    + ids.map(id => card(marks.find(m => m.id === id))).join('\\n')).join('\\n') + `" + strip.replace(/`/g, '') + ";");

// 5) concept text
r("c:'G34 的构图精修:包络圆偏心,窥视有方位。配色定稿:蓝弦已换朱砂=预览行;靠近孔眼的三根弦提亮为雾墨(#33475F),与远端深墨形成两级色阶——弦线像在向眼睛旋转汇聚。'}",
  "c:'G34 的构图精修:包络圆偏心,窥视有方位。强调弦恢复电光蓝(与朱砂眼冷暖对撞,红弦版经对比落选);近眼三弦雾墨提亮的两级色阶保留——向眼睛旋转汇聚。强调弦五候选见下方对比条。'}");

fs.writeFileSync(P, h);
console.log('v11.6 done: blue restored + accent shootout strip');
