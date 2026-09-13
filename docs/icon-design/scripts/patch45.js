const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const anchor = "{id:'g52', code:'G52', name:'燕掠弦窗', gene:'SWALLOW × ENVELOPE · 九弦窗 + 红燕掠过', c:'两个母题的嵌合:九弦弦窗是「窗」,红燕掠窗而过是「览」——轻览的全部动作被一个瞬间收拢。'},";
if (!h.includes(anchor)) { console.error('anchor MISS'); process.exit(1); }
if (h.includes("id:'g50c'")) { console.log('already added'); process.exit(0); }
const add = `
  {id:'g50c', code:'G50C', name:'燕形线描', gene:'CONTOUR LINEART · 一根线走完整只燕', c:'燕子本身由一根连续的线画出:喙→头→翼尖→back→剪叉两羽→腹,回到喙。没有任何填充——线就是燕。剪叉两羽红。'},
  {id:'g50b', code:'G50B', name:'横排纹燕', gene:'RASTER SWALLOW · 水平文字行裁入燕形', c:'最贴题的答案:燕子由九行水平「文字行」排成——文本行本身就是燕子的羽毛;红线=正在被预览的那一行。G34 横线基因与动物母题的直接合并。'},
  {id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},`;
h = h.replace(anchor, anchor + add);
fs.writeFileSync(P, h);
console.log('swallow marks entries added');
