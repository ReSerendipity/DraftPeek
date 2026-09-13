// v8: geometric precision system — strict grid, single stroke language, flat multi-color rhythm. Zero gradients, zero shadows.
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/index.html';

const DEFS = `
<defs>
  <pattern id="bpgrid" width="8" height="8" patternUnits="userSpaceOnUse"><path d="M8 0H0V8" fill="none" stroke="rgba(28,43,58,.10)" stroke-width=".5"/></pattern>
  <g id="guides" fill="none" stroke="rgba(46,111,242,.55)" stroke-width=".6">
    <circle cx="50" cy="50" r="30.6" stroke-dasharray="2.4 2"/>
    <rect x="16.7" y="16.7" width="66.6" height="66.6" rx="3" stroke-dasharray="2.4 2" opacity=".7"/>
    <path d="M50 12V88M12 50H88" opacity=".3"/>
  </g>
  <mask id="mx-g6c"><rect x="0" y="0" width="100" height="100" fill="white"/><rect x="44.5" y="44.5" width="11" height="11" rx="2.6" fill="black" transform="rotate(45 50 50)"/></mask>

  <!-- G1 三层信号环:同心 135° 弧 r10/18/26,逐层错开 45°,中心 mint 点 -->
  <symbol id="mk-g1" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M42.9 42.9A10 10 0 0 1 60 50" stroke="var(--s2)"/>
      <path d="M50 32A18 18 0 0 1 62.7 62.7" stroke="var(--s1)"/>
      <path d="M68.4 31.6A26 26 0 0 1 50 76" stroke="var(--s3)"/>
    </g>
    <circle cx="50" cy="50" r="4.4" fill="var(--s4)"/>
  </symbol>

  <!-- G2 斜向码栈:三根 -45° 圆头杆,长度 18/30/42 等差,间距 14 -->
  <symbol id="mk-g2" viewBox="0 0 100 100">
    <g transform="rotate(-45 50 50)" stroke-width="8" stroke-linecap="round">
      <path d="M36 41V59" stroke="var(--s3)"/>
      <path d="M50 35V65" stroke="var(--s2)"/>
      <path d="M64 29V71" stroke="var(--s1)"/>
    </g>
    <circle cx="67" cy="33" r="4" fill="var(--s4)"/>
  </symbol>

  <!-- G3 十二分段环:周长精确十二分,两段着色,中心留空 -->
  <symbol id="mk-g3" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="26" fill="none" stroke="var(--s1)" stroke-width="8" stroke-dasharray="9.66 3.95"/>
    <circle cx="50" cy="50" r="26" fill="none" stroke="var(--s3)" stroke-width="8" stroke-dasharray="9.66 153.68" stroke-dashoffset="-27.23"/>
    <circle cx="50" cy="50" r="26" fill="none" stroke="var(--s2)" stroke-width="8" stroke-dasharray="9.66 153.68" stroke-dashoffset="-108.95"/>
    <circle cx="50" cy="50" r="5" fill="var(--s4)"/>
  </symbol>

  <!-- G4 直刃光圈:六条正六边形隔角弦,精确 60° 旋转对称,红心六边形 -->
  <symbol id="mk-g4" viewBox="0 0 100 100">
    <g stroke-width="8" stroke-linecap="round">
      <path d="M50 24L42.3 43.6" stroke="var(--s1)"/>
      <path d="M27.5 37L40.6 53.4" stroke="var(--s1)"/>
      <path d="M27.5 63L48.3 59.8" stroke="var(--s2)"/>
      <path d="M50 76L57.7 56.4" stroke="var(--s1)"/>
      <path d="M72.5 63L59.4 46.6" stroke="var(--s1)"/>
      <path d="M72.5 37L51.7 40.2" stroke="var(--s1)"/>
    </g>
    <circle cx="50" cy="50" r="5.4" fill="var(--s3)"/>
  </symbol>

  <!-- G5 相切三圆:沿 45° 对角线精确外切 r15/r10/r6.5,空隙补 mint 切圆 -->
  <symbol id="mk-g5" viewBox="0 0 100 100">
    <circle cx="38" cy="62" r="15" fill="var(--s1)"/>
    <circle cx="55.7" cy="44.3" r="10" fill="var(--s2)"/>
    <circle cx="67" cy="33" r="6" fill="var(--s3)"/>
    <circle cx="36.5" cy="42.5" r="4.5" fill="var(--s4)"/>
  </symbol>

  <!-- G6 等距积木:30° 轴测,顶 mint/左墨/右蓝,红色小积木叠左肩 -->
  <symbol id="mk-g6" viewBox="0 0 100 100">
    <path d="M50 32L69 41.5L50 51L31 41.5Z" fill="var(--s4)"/>
    <path d="M31 41.5L50 51L50 69L31 59.5Z" fill="var(--s1)"/>
    <path d="M69 41.5L50 51L50 69L69 59.5Z" fill="var(--s2)"/>
    <path d="M40.5 34.7L47.5 38.2L40.5 41.7L33.5 38.2Z" fill="var(--s3)"/>
    <path d="M33.5 38.2L40.5 41.7L40.5 47.7L33.5 44.2Z" fill="var(--s3)"/>
    <path d="M47.5 38.2L40.5 41.7L40.5 47.7L47.5 44.2Z" fill="var(--s3)"/>
    <path d="M40.5 34.7L47.5 38.2M40.5 34.7L33.5 38.2" stroke="var(--pure-white)" stroke-width="1.1" opacity=".5"/>
  </symbol>

  <!-- G7 嵌套V形:三层 90° 尖角 V,深度 10/18/26 等差,内红中蓝外墨 -->
  <symbol id="mk-g7" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M28 44L50 66L72 44" stroke="var(--s1)"/>
      <path d="M36 40L50 54L64 40" stroke="var(--s2)"/>
      <path d="M44 36L50 42L56 36" stroke="var(--s3)"/>
    </g>
  </symbol>


  <!-- G8 渐细黄金螺旋:缎带宽 9→1.5,四心相切(50,50)(50,58)(56,58)(56,54) -->
  <symbol id="mk-g8" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M21.5 50A28.5 28.5 0 0 0 50 77.5A19.5 19.5 0 0 0 68.5 58A11.5 11.5 0 0 0 56 46.5A6.75 6.75 0 0 0 49.25 54L50.75 54A5.25 5.25 0 0 1 56 49.5A8.5 8.5 0 0 1 63.5 58A12.5 12.5 0 0 1 50 70.5A19.5 19.5 0 0 1 30.5 50Z"/>
    <circle cx="53.1" cy="53.8" r="2.8" fill="var(--s3)"/>
  </symbol>

  <!-- G9 叶序点阵:21 点按 137.5° 黄金角 + r=c√n 精确生成 -->
  <symbol id="mk-g9" viewBox="0 0 100 100">
    <circle cx="45.7" cy="54.0" r="2.6" fill="var(--s1)"/>
    <circle cx="50.7" cy="41.8" r="2.6" fill="var(--s1)"/>
    <circle cx="56.2" cy="58.0" r="2.7" fill="var(--s1)"/>
    <circle cx="38.5" cy="48.0" r="2.7" fill="var(--s1)"/>
    <circle cx="61.0" cy="43.0" r="2.8" fill="var(--s1)"/>
    <circle cx="46.3" cy="63.8" r="2.8" fill="var(--s1)"/>
    <circle cx="42.9" cy="36.3" r="2.9" fill="var(--s1)"/>
    <circle cx="65.5" cy="55.7" r="2.9" fill="var(--s4)"/>
    <circle cx="33.8" cy="56.7" r="3.0" fill="var(--s1)"/>
    <circle cx="57.8" cy="33.2" r="3.0" fill="var(--s1)"/>
    <circle cx="55.8" cy="68.5" r="3.1" fill="var(--s1)"/>
    <circle cx="32.5" cy="39.8" r="3.1" fill="var(--s1)"/>
    <circle cx="70.6" cy="45.5" r="3.2" fill="var(--s2)"/>
    <circle cx="37.4" cy="67.9" r="3.2" fill="var(--s1)"/>
    <circle cx="47.1" cy="27.5" r="3.3" fill="var(--s1)"/>
    <circle cx="67.9" cy="65.1" r="3.3" fill="var(--s1)"/>
    <circle cx="25.9" cy="51.0" r="3.4" fill="var(--s1)"/>
    <circle cx="67.6" cy="32.5" r="3.4" fill="var(--s1)"/>
    <circle cx="48.8" cy="75.5" r="3.5" fill="var(--s1)"/>
    <circle cx="33.2" cy="29.9" r="3.5" fill="var(--s1)"/>
    <circle cx="76.6" cy="53.6" r="3.6" fill="var(--s3)"/>
  </symbol>
  <!-- G10 黄金作图:1:1.618 矩形 + 四级分割制图线(18% 墨)+ 四段相切弧链(墨→墨→蓝→红) -->
  <symbol id="mk-g10" viewBox="0 0 100 100">
    <rect x="28" y="36.4" width="44" height="27.2" rx="2.5" fill="none" stroke="var(--s1)" stroke-width="1.5" opacity=".35"/>
    <path d="M55.2 36.4V63.6M55.2 53.2H72M65.6 53.2V63.6M65.6 59.6H72" stroke="var(--s1)" stroke-width="1.2" opacity=".22"/>
    <path d="M55.2 36.4A16.8 16.8 0 0 1 72 53.2" fill="none" stroke="var(--s2)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M72 53.2A10.4 10.4 0 0 1 61.6 63.6" fill="none" stroke="var(--s2)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M61.6 63.6A6.4 6.4 0 0 1 55.2 57.2" fill="none" stroke="var(--s3)" stroke-width="5" stroke-linecap="round"/>
    <path d="M28 63.6A27.2 27.2 0 0 1 55.2 36.4" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
  </symbol>


  <mask id="mx-g19a"><circle cx="50" cy="50" r="6" fill="white"/><circle cx="56" cy="44" r="5.5" fill="black"/></mask>
  <mask id="mx-g19b"><circle cx="68" cy="56" r="6" fill="white"/><circle cx="74" cy="50" r="6" fill="black"/></mask>

  <!-- G11 折角名牌:圆角方版切角,mint 折页盖缝,红点居中 -->

  <!-- G24R 透镜交集·精修:双环 r17 / 线宽 7 / 圆心距 20,红镜高 27.5u -->
  <symbol id="mk-g24r" viewBox="0 0 100 100">
    <circle cx="40" cy="50" r="17" fill="none" stroke="var(--s1)" stroke-width="7"/>
    <circle cx="60" cy="50" r="17" fill="none" stroke="var(--s2)" stroke-width="7"/>
    <path d="M50 36.25A17 17 0 0 1 50 63.75A17 17 0 0 1 50 36.25Z" fill="var(--s3)"/>
  </symbol>

  <symbol id="mk-g11" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M35 22H56L74 40V61Q74 74 61 74H35Q22 74 22 61V35Q22 22 35 22Z"/>
    <path fill="var(--s4)" d="M56 22V40H74Z"/>
    <circle cx="48" cy="49" r="4.2" fill="var(--s3)"/>
  </symbol>

  <!-- G16 层叠菱形:三枚 45° 菱形沿对角线每层错位 6u -->
  <symbol id="mk-g16" viewBox="0 0 100 100">
    <path d="M44 24L58 38L44 52L30 38Z" fill="var(--s1)"/>
    <path d="M50 30L64 44L50 58L36 44Z" fill="var(--s2)"/>
    <path d="M56 36L70 50L56 64L42 50Z" fill="var(--s3)"/>
  </symbol>

  <!-- G19 月相序列:全圆 → 盈月 → 红新月,沿对角线推进 -->
  <symbol id="mk-g19" viewBox="0 0 100 100">
    <circle cx="32" cy="44" r="6" fill="var(--s1)"/>
    <g mask="url(#mx-g19a)"><circle cx="50" cy="50" r="6" fill="var(--s1)"/></g>
    <g mask="url(#mx-g19b)"><circle cx="68" cy="56" r="6" fill="var(--s3)"/></g>
  </symbol>

  <!-- G24 透镜交集:双环 r16 圆心距 18,透镜形交集填红 -->
  <symbol id="mk-g24" viewBox="0 0 100 100">
    <circle cx="41" cy="50" r="16" fill="none" stroke="var(--s1)" stroke-width="6.5"/>
    <circle cx="59" cy="50" r="16" fill="none" stroke="var(--s2)" stroke-width="6.5"/>
    <path d="M50 36.8A16 16 0 0 1 50 63.2A16 16 0 0 1 50 36.8Z" fill="var(--s3)"/>
  </symbol>

  <!-- G25 分屏位移:双圆角板错位 ±4u,蓝色游标居间 -->
  <symbol id="mk-g25" viewBox="0 0 100 100">
    <rect x="28" y="30" width="17" height="36" rx="6" fill="var(--s1)"/>
    <rect x="55" y="34" width="17" height="36" rx="6" fill="var(--s3)"/>
    <path d="M50 42V58" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
  </symbol>


  <!-- G26 谢尔宾斯基三角:深度 2 递归,9 枚正三角,顶枚红 -->
  <symbol id="mk-g26" viewBox="0 0 100 100">
    <path d="M50.0 20.0L56.5 31.3L43.5 31.3Z" fill="var(--s3)"/>
    <path d="M56.5 31.3L63.0 42.5L50.0 42.5Z" fill="var(--s1)"/>
    <path d="M43.5 31.3L50.0 42.5L37.0 42.5Z" fill="var(--s1)"/>
    <path d="M63.0 42.5L69.5 53.8L56.5 53.8Z" fill="var(--s1)"/>
    <path d="M69.5 53.8L76.0 65.0L63.0 65.0Z" fill="var(--s1)"/>
    <path d="M56.5 53.8L63.0 65.0L50.0 65.0Z" fill="var(--s1)"/>
    <path d="M37.0 42.5L43.5 53.8L30.5 53.8Z" fill="var(--s1)"/>
    <path d="M43.5 53.8L50.0 65.0L37.0 65.0Z" fill="var(--s1)"/>
    <path d="M30.5 53.8L37.0 65.0L24.0 65.0Z" fill="var(--s1)"/>
  </symbol>

  <!-- G27 角无限环:双三角对顶,中缝 8u,红点悬于交叉点 -->
  <symbol id="mk-g27" viewBox="0 0 100 100">
    <path d="M26 36L46 50L26 64Z" fill="var(--s1)"/>
    <path d="M74 36L54 50L74 64Z" fill="var(--s2)"/>
    <circle cx="50" cy="50" r="4.2" fill="var(--s3)"/>
  </symbol>

  <!-- G28 利萨茹曲线:x:y 频率比 3:2 闭合曲线,A18.9/B21.3,起终点红标 -->
  <symbol id="mk-g28" viewBox="0 0 100 100">
    <path d="M68.9 50.0L68.7 52.2L68.0 54.4L66.8 56.6L65.3 58.7L63.4 60.6L61.1 62.5L58.6 64.3L55.8 65.8L53.0 67.2L50.0 68.4L47.0 69.5L44.2 70.3L41.4 70.8L38.9 71.2L36.6 71.3L34.7 71.2L33.2 70.8L32.0 70.3L31.3 69.5L31.1 68.4L31.3 67.2L32.0 65.8L33.2 64.3L34.7 62.5L36.6 60.6L38.9 58.7L41.4 56.6L44.2 54.4L47.0 52.2L50.0 50.0L53.0 47.8L55.8 45.6L58.6 43.4L61.1 41.3L63.4 39.3L65.3 37.5L66.8 35.7L68.0 34.2L68.7 32.8L68.9 31.6L68.7 30.5L68.0 29.7L66.8 29.2L65.3 28.8L63.4 28.7L61.1 28.8L58.6 29.2L55.8 29.7L53.0 30.5L50.0 31.6L47.0 32.8L44.2 34.2L41.4 35.7L38.9 37.5L36.6 39.3L34.7 41.3L33.2 43.4L32.0 45.6L31.3 47.8L31.1 50.0L31.3 52.2L32.0 54.4L33.2 56.6L34.7 58.7L36.6 60.6L38.9 62.5L41.4 64.3L44.2 65.8L47.0 67.2L50.0 68.4L53.0 69.5L55.8 70.3L58.6 70.8L61.1 71.2L63.4 71.3L65.3 71.2L66.8 70.8L68.0 70.3L68.7 69.5L68.9 68.4L68.7 67.2L68.0 65.8L66.8 64.3L65.3 62.5L63.4 60.6L61.1 58.7L58.6 56.6L55.8 54.4L53.0 52.2L50.0 50.0L47.0 47.8L44.2 45.6L41.4 43.4L38.9 41.3L36.6 39.4L34.7 37.5L33.2 35.7L32.0 34.2L31.3 32.8L31.1 31.6L31.3 30.5L32.0 29.7L33.2 29.2L34.7 28.8L36.6 28.7L38.9 28.8L41.4 29.2L44.2 29.7L47.0 30.5L50.0 31.6L53.0 32.8L55.8 34.2L58.6 35.7L61.1 37.5L63.4 39.4L65.3 41.3L66.8 43.4L68.0 45.6L68.7 47.8L68.9 50.0Z" fill="none" stroke="var(--s1)" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="69.3" cy="50" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G29 黄金角扇:8 枚放射刃按 137.507° 递增,外刃 17→26.6 渐长 -->
  <symbol id="mk-g29" viewBox="0 0 100 100">
    <path d="M43.9 43.4L37.7 36.6" stroke="var(--s1)" stroke-width="7" stroke-linecap="round"/>
    <path d="M59.0 50.8L69.3 51.7" stroke="var(--s1)" stroke-width="7" stroke-linecap="round"/>
    <path d="M42.9 55.5L33.7 62.5" stroke="var(--s1)" stroke-width="7" stroke-linecap="round"/>
    <path d="M51.6 41.1L53.8 28.5" stroke="var(--s1)" stroke-width="7" stroke-linecap="round"/>
    <path d="M54.8 57.6L62.3 69.4" stroke="var(--s2)" stroke-width="7" stroke-linecap="round"/>
    <path d="M41.3 47.7L26.6 43.7" stroke="var(--s1)" stroke-width="7" stroke-linecap="round"/>
    <path d="M58.0 45.9L72.5 38.3" stroke="var(--s1)" stroke-width="7" stroke-linecap="round"/>
    <path d="M46.9 58.5L40.9 75.0" stroke="var(--s3)" stroke-width="7" stroke-linecap="round"/>
  </symbol>


  <!-- G30 蜂窝三簇:三枚正六边形精确平铺咬合,缝隙 1.3u -->
  <symbol id="mk-g30" viewBox="0 0 100 100">
    <path d="M50 25.8L58.8 30.9V41.1L50 46.2L41.2 41.1V30.9Z" fill="var(--s3)"/>
    <path d="M40.5 42.3L49.3 47.4V57.6L40.5 62.7L31.7 57.6V47.4Z" fill="var(--s1)"/>
    <path d="M59.5 42.3L68.3 47.4V57.6L59.5 62.7L50.7 57.6V47.4Z" fill="var(--s2)"/>
  </symbol>

  <!-- G31 定位角标:三组嵌套方占三角,内芯红/蓝/mint,第四角留白 -->
  <symbol id="mk-g31" viewBox="0 0 100 100">
    <rect x="30.5" y="30.5" width="14" height="14" rx="3.2" fill="none" stroke="var(--s1)" stroke-width="4.5"/>
    <rect x="55.5" y="30.5" width="14" height="14" rx="3.2" fill="none" stroke="var(--s1)" stroke-width="4.5"/>
    <rect x="30.5" y="55.5" width="14" height="14" rx="3.2" fill="none" stroke="var(--s1)" stroke-width="4.5"/>
    <rect x="35.2" y="35.2" width="4.6" height="4.6" rx="1.3" fill="var(--s3)"/>
    <rect x="57.7" y="35.2" width="4.6" height="4.6" rx="1.3" fill="var(--s2)"/>
    <rect x="35.2" y="57.7" width="4.6" height="4.6" rx="1.3" fill="var(--s4)"/>
  </symbol>

  <!-- G32 文本行阵:四条圆头行,第三行红色加长=正在预览的那一行 -->
  <symbol id="mk-g32" viewBox="0 0 100 100">
    <rect x="28" y="30" width="44" height="7" rx="3.5" fill="var(--s1)"/>
    <rect x="28" y="41" width="30" height="7" rx="3.5" fill="var(--s1)"/>
    <rect x="28" y="52" width="44" height="7" rx="3.5" fill="var(--s3)"/>
    <rect x="28" y="63" width="22" height="7" rx="3.5" fill="var(--s2)"/>
  </symbol>

  <!-- G33 阶梯塔:三层逐级收窄,墨/蓝/红 -->
  <symbol id="mk-g33" viewBox="0 0 100 100">
    <rect x="30" y="58" width="40" height="14" rx="4" fill="var(--s1)"/>
    <rect x="35" y="45" width="30" height="13" rx="4" fill="var(--s2)"/>
    <rect x="40" y="32" width="20" height="13" rx="4" fill="var(--s3)"/>
  </symbol>


  <!-- G34 直线包络:7 条弦切于 r12 内圆(角距 20°),直线蓄出圆 -->
  <symbol id="mk-g34" viewBox="0 0 100 100">
    <path d="M62.0 74.2L62.0 25.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M53.0 76.8L69.5 31.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.6 76.2L74.7 39.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.1 72.5L76.9 48.3" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M28.3 66.0L75.9 57.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M24.1 57.6L71.7 66.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.1 48.3L64.9 72.5" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
  </symbol>





  <!-- G50 折纸燕:全直线多边形拼接,远翼蓝/体墨/燕尾叉红,无眼纯剪影 -->
  <symbol id="mk-g50" viewBox="0 0 100 100">
    <path d="M44 50L60 30L62 50Z" fill="var(--s2)"/>
    <path d="M24 64L34 53L56 52L64 58L50 66Z" fill="var(--s1)"/>
    <path d="M32 54L66 28L58 52L40 56Z" fill="var(--s1)"/>
    <path d="M62 56L78 48L66 62Z" fill="var(--s3)"/>
    <path d="M62 58L72 68L58 64Z" fill="var(--s3)"/>
  </symbol>

  <!-- G51 剪影燕:单路径流畅剪影,镰刀翼+深剪叉尾,下叉红 -->
  <symbol id="mk-g51" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M24 60Q30 54 36 54L44 50L64 30Q58 46 56 48L64 54L78 46L66 56L74 64Q60 62 52 60Q40 64 32 62Q26 62 24 60Z"/>
    <path d="M66 56L74 64L62 60Z" fill="var(--s3)"/>
  </symbol>

  <!-- G52 燕掠弦窗:九弦弦窗 + 红燕掠过( mini 折纸燕 0.5x) -->
  <symbol id="mk-g52" viewBox="0 0 100 100">
    <g stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round">
      <path d="M73.7 50L26.3 38.8" fill="none"/>
      <path d="M69.6 61.6L30.4 38.4" fill="none" stroke="var(--s2)"/>
      <path d="M61.6 69.6L38.4 30.4" fill="none"/>
      <path d="M50 73.7L50 26.3" fill="none"/>
      <path d="M38.4 69.6L69.6 30.4" fill="none"/>
      <path d="M26.3 61.6L73.7 38.4" fill="none"/>
      <path d="M30.4 50L73.7 50" fill="none" stroke="var(--s2)"/>
      <path d="M38.8 26.3L61.2 73.7" fill="none"/>
      <path d="M26.3 50L61.2 26.3" fill="none" stroke="var(--s2)"/>
    </g>
    <path d="M47 48.5L55 38.5L56 48.5Z" fill="var(--s3)"/>
    <path d="M37 57L42 51.5L53 51L57 54L50 58Z" fill="var(--s3)"/>
    <path d="M41 52L58 39L54 51L45 53Z" fill="var(--s3)"/>
    <path d="M56 53L64 49L58 56Z" fill="var(--s3)"/>
    <path d="M56 54L61 59L54 57Z" fill="var(--s3)"/>
  </symbol>


  <clipPath id="cp-g50b"><path d="M26.1 59.2Q29.2 49.7 34.8 49.7L42.3 46L60.9 27.6Q55.4 42.3 53.5 44.2L60.9 49.7L73.8 42.3L62.7 51.5L70.1 58.9Q57.2 57 49.8 55.2Q38.8 58.9 31.4 57Q26.1 57 26.1 59.2Z"/></clipPath>

  <!-- G50C 燕形线描:一根线走完燕的轮廓,剪叉两羽红 -->
  <symbol id="mk-g50c" viewBox="0 0 100 100">
    <path d="M24 64L34 53L66 28L58 52L64 58L78 48L66 62L72 68L50 66Z" fill="none" stroke="var(--s1)" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M64 58L78 48L66 62" fill="none" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M66 62L72 68" fill="none" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round"/>
  </symbol>

  <!-- G50B 横排纹燕:水平文字行裁入燕形,红线=预览行 -->
  <symbol id="mk-g50b" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g50b)">
    <path d="M21 27H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 32.5H79" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 38H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 43.5H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 49H79" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 54.5H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 60H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 65.5H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21 71H79" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    </g>
  </symbol>

  <!-- G50A 羽轴扇燕:12 线从喉部放射,线端包络出燕形(翼尖红) -->
  <symbol id="mk-g50a" viewBox="0 0 100 100">
    <path d="M46 52L51.2 32.7" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L61.5 29.9" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L67.7 35.1" stroke="var(--s3)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L69.2 42.6" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L67.8 48.9" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L71.9 54.3" stroke="var(--s2)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L60.6 55.6" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L68.3 61.0" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L59.8 63.6" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L52.9 63.0" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L48.3 60.7" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
    <path d="M46 52L24.2 62.1" stroke="var(--s1)" stroke-width="4.2" stroke-linecap="round"/>
  </symbol>


  <!-- G53 张翼巡弋(写生①):双翼全展浅 V 滑翔,面向左,喉部朱砂 -->
  <symbol id="mk-g53" viewBox="0 0 100 100">
    <path d="M44 48L24 36L34 44L48 52Z" fill="var(--s1)"/>
    <path d="M50 48L72 32L66 42L52 52Z" fill="var(--s1)"/>
    <path d="M34 50L52 46L62 52L56 58L38 56Z" fill="var(--s1)"/>
    <path d="M38 56L56 58L52 63L40 60Z" fill="var(--s6)"/>
    <path d="M30 52L36 52L34 57L29 55Z" fill="var(--s3)"/>
    <path d="M28 52L21 54L29 55Z" fill="var(--s1)"/>
    <path d="M62 52L76 56L64 60Z" fill="var(--s1)"/>
    <path d="M60 56L70 66L58 62Z" fill="var(--s1)"/>
  </symbol>

  <!-- G54 侧滑掠影(写生②):收翼水平滑翔,面向右,剪叉尾在后 -->
  <symbol id="mk-g54" viewBox="0 0 100 100">
    <path d="M60 42L34 28L42 40L50 48Z" fill="var(--s1)"/>
    <path d="M44 48L70 44L74 48L70 52L46 52Z" fill="var(--s1)"/>
    <path d="M46 52L70 52L68 57L48 56Z" fill="var(--s6)"/>
    <path d="M68 48L76 48L74 54L68 53Z" fill="var(--s3)"/>
    <path d="M70 44L79 47.5L71 51.5Z" fill="var(--s1)"/>
    <path d="M44 48L24 42L40 52Z" fill="var(--s1)"/>
    <path d="M44 52L26 58L42 56Z" fill="var(--s1)"/>
  </symbol>

  <!-- G55 归巢收羽(写生③):双翼上扬宽 V,尾羽下扇,面向左上 -->
  <symbol id="mk-g55" viewBox="0 0 100 100">
    <path d="M42 40L30 30L38 38L48 44Z" fill="var(--s1)"/>
    <path d="M52 42L70 28L64 38L50 44Z" fill="var(--s1)"/>
    <path d="M36 32L50 42L58 52L44 46L34 38Z" fill="var(--s1)"/>
    <path d="M40 38L46 40L44 46L38 44Z" fill="var(--s3)"/>
    <path d="M44 46L58 52L52 58L42 50Z" fill="var(--s6)"/>
    <path d="M56 52L62 58L58 72L54 56Z" fill="var(--s1)"/>
    <path d="M58 54L66 56L66 72L56 56Z" fill="var(--s1)"/>
    <path d="M60 52L72 54L72 66L58 54Z" fill="var(--s1)" opacity=".85"/>
  </symbol>


  

  

  

  

  

  


  <!-- G53P/G53C 扫描线象形燕:每行端点精确落在姿态轮廓上,零裁剪 -->
  <symbol id="mk-g53p" viewBox="0 0 100 100">
    <path d="M64.1 37.7L68.6 37.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M59.7 40.9L66.6 40.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.3 44.2L37.6 44.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.3 44.2L63.0 44.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.9 47.4L50.8 47.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.3 47.4L58.5 47.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.4 50.6L59.7 50.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21.6 53.8L68.4 53.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.5 57.1L72.8 57.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.1 60.3L54.2 60.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M58.6 60.3L64.3 60.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M62.5 63.5L67.5 63.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g53q" viewBox="0 0 100 100">
    <path d="M64.1 37.7L68.6 37.7" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M59.7 40.9L66.6 40.9" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.3 44.2L37.6 44.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.3 44.2L63.0 44.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.9 47.4L50.8 47.4" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.3 47.4L58.5 47.4" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.4 50.6L59.7 50.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21.6 53.8L68.4 53.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.5 57.1L72.8 57.1" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.1 60.3L54.2 60.3" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M58.6 60.3L64.3 60.3" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M62.5 63.5L67.5 63.5" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
  </symbol>

  <!-- G54P/G54C -->
  <symbol id="mk-g54p" viewBox="0 0 100 100">
    <path d="M35.7 30.5L38.6 30.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.5 33.3L43.8 33.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.4 36.1L49.0 36.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.2 38.8L54.1 38.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.6 41.6L59.3 41.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.8 44.4L32.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M46.4 44.4L56.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M67.5 44.4L71.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M32.3 47.2L41.2 47.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.2 47.2L78.1 47.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M36.7 49.9L75.4 49.9" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.8 52.7L74.4 52.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M33.5 55.5L42.3 55.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M47.8 55.5L68.6 55.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g54q" viewBox="0 0 100 100">
    <path d="M35.7 30.5L38.6 30.5" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.5 33.3L43.8 33.3" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.4 36.1L49.0 36.1" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.2 38.8L54.1 38.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.6 41.6L59.3 41.6" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.8 44.4L32.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M46.4 44.4L56.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M67.5 44.4L71.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M32.3 47.2L41.2 47.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.2 47.2L78.1 47.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M36.7 49.9L75.4 49.9" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.8 52.7L74.4 52.7" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M33.5 55.5L42.3 55.5" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M47.8 55.5L68.6 55.5" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
  </symbol>

  <!-- G55P/G55C -->
  <symbol id="mk-g55p" viewBox="0 0 100 100">
    <path d="M34.8 34.8L40.0 34.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M61.2 34.8L65.9 34.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.5 39.2L46.0 39.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.6 39.2L61.3 39.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.2 43.5L40.9 43.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M44.8 43.5L51.2 43.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.1 47.8L54.7 47.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M44.7 52.2L61.0 52.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M50.1 56.5L56.3 56.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M66.0 56.5L72.0 56.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.2 60.8L72.0 60.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M56.3 65.2L66.0 65.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g55q" viewBox="0 0 100 100">
    <path d="M34.8 34.8L40.0 34.8" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M61.2 34.8L65.9 34.8" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.5 39.2L46.0 39.2" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.6 39.2L61.3 39.2" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.2 43.5L40.9 43.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M44.8 43.5L51.2 43.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.1 47.8L54.7 47.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M44.7 52.2L61.0 52.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M50.1 56.5L56.3 56.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M66.0 56.5L72.0 56.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.2 60.8L72.0 60.8" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
    <path d="M56.3 65.2L66.0 65.2" stroke="var(--s6)" stroke-width="4" stroke-linecap="round"/>
  </symbol>


  <!-- G50D 羽轴包络燕:弦线族介于体芯圆 r9 与燕形轮廓之间,15° 扇 —— G34 构造的燕形移植 -->
  <symbol id="mk-g50d" viewBox="0 0 100 100">
    <path d="M46.8 43.0L47.0 41.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.1 43.5L49.8 41.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M51.2 44.6L52.3 43.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M52.9 46.2L55.8 43.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.2 48.2L63.3 43.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.9 50.4L58.4 49.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.0 52.8L69.1 54.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.5 55.1L66.1 59.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M53.4 57.2L62.4 63.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M51.8 58.9L53.6 61.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.8 60.2L51.0 62.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M47.6 60.9L47.9 62.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M45.2 61.0L45.0 63.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.1 57.8L37.6 59.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.7 55.4L33.8 56.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>

  <!-- G50E 羽轴包络燕·羽色:同构,弦线按翼/尾/头分区配色 -->
  <symbol id="mk-g50e" viewBox="0 0 100 100">
    <path d="M46.8 43.0L47.0 41.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.1 43.5L49.8 41.7" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M51.2 44.6L52.3 43.0" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M52.9 46.2L55.8 43.8" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.2 48.2L63.3 43.9" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.9 50.4L58.4 49.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.0 52.8L69.1 54.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.5 55.1L66.1 59.3" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M53.4 57.2L62.4 63.5" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M51.8 58.9L53.6 61.0" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.8 60.2L51.0 62.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M47.6 60.9L47.9 62.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M45.2 61.0L45.0 63.0" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.1 57.8L37.6 59.1" stroke="var(--s5)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.7 55.4L33.8 56.9" stroke="var(--s5)" stroke-width="4" stroke-linecap="round"/>
  </symbol>


  <!-- G56 燕出于弦:燕形轮廓的 8 条边各自延长成整弦,燕子=纯负空间,由自己的边缘线围出 -->
  <symbol id="mk-g56" viewBox="0 0 100 100">
    <path d="M29.6 67.7L65.7 28.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M27.6 65.0L70.0 31.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M63.6 26.7L46.9 76.8" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M32.2 29.7L70.3 67.8" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M34.6 72.2L76.0 42.7" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M70.4 32.4L57.4 76.0" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M74.4 61.6L23.9 57.0" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M74.5 61.3L24.0 57.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
  </symbol>


  <!-- G57 燕蓄:九弦玫瑰结原样保留,仅孔轮廓由圆改为燕——每弦垂足落在燕形轮廓上,燕由弦自己蓄出 -->
  <symbol id="mk-g57" viewBox="0 0 100 100">
    <path d="M55.3 76.0L55.3 24.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M46.7 76.3L64.4 27.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.7 74.4L72.3 35.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.1 73.1L76.5 50.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M25.3 59.5L76.5 50.5" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.5 50.5L74.7 59.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.5 50.4L62.9 73.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.7 35.6L60.3 74.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.6 27.8L53.3 76.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>


  <!-- 三姿态 × 三线式:每行端点落在姿态轮廓上,零填充零裁剪 -->

  <symbol id="mk-g53h1" viewBox="0 0 100 100">
    
    <path d="M64.1 37.7L68.6 37.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M59.7 40.9L66.6 40.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.3 44.2L37.6 44.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M55.3 44.2L63.0 44.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.9 47.4L50.8 47.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M54.3 47.4L58.5 47.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.4 50.6L59.7 50.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21.6 53.8L68.4 53.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.5 57.1L72.8 57.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.1 60.3L54.2 60.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M58.6 60.3L64.3 60.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M62.5 63.5L67.5 63.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g53h2" viewBox="0 0 100 100">
    
    <path d="M60.4 31.7L63.3 31.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M57.9 35.4L62.6 35.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.4 39.1L60.9 39.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M52.9 42.8L58.3 42.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    
    <path d="M34.7 50.1L72.2 50.1" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.6 53.8L67.6 53.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.2 57.5L57.7 57.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M61.6 57.5L72.8 57.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.6 61.2L37.4 61.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M42.3 61.2L56.4 61.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g53h3" viewBox="0 0 100 100">
    <path d="M34.0 50.0L52.0 46.0L62.0 52.0L56.0 58.0L38.0 56.0Z" fill="none" stroke="var(--s1)" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/><path d="M34.0 50.0L52.0 46.0" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g54h1" viewBox="0 0 100 100">
    <path d="M35.7 30.5L38.6 30.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.5 33.3L43.8 33.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M39.4 36.1L49.0 36.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.2 38.8L54.1 38.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.6 41.6L59.3 41.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.8 44.4L32.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M46.4 44.4L56.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M67.5 44.4L71.0 44.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M32.3 47.2L41.2 47.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M49.2 47.2L78.1 47.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M36.7 49.9L75.4 49.9" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.8 52.7L74.4 52.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M33.5 55.5L42.3 55.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M47.8 55.5L68.6 55.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g54h2" viewBox="0 0 100 100">
    <path d="M31.7 35.4L39.4 35.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.0 38.3L51.7 38.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.3 41.2L56.1 41.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M62.9 41.2L76.1 41.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M42.6 44.1L74.4 44.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M47.6 47.0L74.3 47.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M25.6 49.9L69.5 49.9" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.1 52.8L41.9 52.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M46.5 52.8L66.2 52.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M41.4 55.7L44.3 55.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M49.0 55.7L52.7 55.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.1 58.5L42.2 58.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    
  </symbol>
  <symbol id="mk-g54h3" viewBox="0 0 100 100">
    <path d="M44.0 48.0L70.0 44.0L74.0 48.0L70.0 52.0L46.0 52.0Z" fill="none" stroke="var(--s1)" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/><path d="M44.0 48.0L70.0 44.0" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round"/>
  </symbol>
  <symbol id="mk-g55h1" viewBox="0 0 100 100">
    
    <path d="M34.8 34.8L40.0 34.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M61.2 34.8L65.9 34.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.5 39.2L46.0 39.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M55.6 39.2L61.3 39.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M38.2 43.5L40.9 43.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M44.8 43.5L51.2 43.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.1 47.8L54.7 47.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M44.7 52.2L61.0 52.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M50.1 56.5L56.3 56.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M66.0 56.5L72.0 56.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M55.2 60.8L72.0 60.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M56.3 65.2L66.0 65.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    
  </symbol>
  <symbol id="mk-g55h2" viewBox="0 0 100 100">
    
    <path d="M51.7 27.2L54.2 27.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M50.4 31.4L54.5 31.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M49.1 35.6L52.4 35.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    
    <path d="M28.4 44.1L47.6 44.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M63.2 44.1L72.7 44.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M29.7 48.3L35.6 48.3" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/><path d="M42.2 48.3L59.9 48.3" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/><path d="M68.3 48.3L75.7 48.3" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.1 52.5L60.8 52.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M65.4 52.5L71.3 52.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M61.4 56.7L66.9 56.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/><path d="M70.7 56.7L74.2 56.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    
  </symbol>
  <symbol id="mk-g55h3" viewBox="0 0 100 100">
    <path d="M36.0 32.0L50.0 42.0L58.0 52.0L44.0 46.0L34.0 38.0Z" fill="none" stroke="var(--s1)" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/><path d="M36.0 32.0L50.0 42.0" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round"/>
  </symbol>


  <clipPath id="cp-g58"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g59"><circle cx="50" cy="50" r="27"/></clipPath>
  <mask id="mx-g58h"><path d="M29 49L41 49.5L39.5 57.5L30 56Z" fill="white"/><circle cx="35" cy="53.2" r="5" fill="black"/></mask>
  <mask id="mx-g59h"><path d="M59 49.5L71 49L70 56.5L60.5 57.5Z" fill="white"/><circle cx="65" cy="53.2" r="5" fill="black"/></mask>

  <!-- G58 窗中燕:G34W 方孔弦窗为底,G53 燕身穿窗,红点=眼睛(点睛) -->
  <symbol id="mk-g58" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g58)">
    <path d="M64.5 70.0L64.5 18.0M55.6 71.5L73.3 22.6M45.8 69.4L79.2 29.5M36.9 63.0L82.0 37.0M31.7 56.2L83.0 47.2M28.9 48.2L80.1 57.2M29.1 38.7L74.1 64.7M33.3 29.1L66.7 68.9M40.6 21.9L58.4 70.8" fill="none" stroke="var(--s1)" stroke-width="3.2" stroke-linecap="round"/>
    </g>
    <path d="M45.2 48.4L29.2 38.8L37.2 45.2L48.4 51.6Z" fill="var(--s2)"/>
    <path d="M59.6 51.6L70.8 54.8L61.2 58Z" fill="var(--s1)"/>
    <path d="M58 54.8L66 62.8L56.4 59.6Z" fill="var(--s1)"/>
    <path d="M37.2 50L51.6 46.8L59.6 51.6L54.8 56.4L40.4 54.8Z" fill="var(--s1)"/>
    <path d="M50 48.4L67.6 35.6L62.8 43.6L51.6 51.6Z" fill="var(--s1)"/>
    <path d="M40.4 54.8L54.8 56.4L51.6 60.4L42 58Z" fill="var(--s6)"/>
    <path mask="url(#mx-g58h)" d="M29 49L41 49.5L39.5 57.5L30 56Z" fill="var(--s1)"/>
    <path d="M29 51.5L23 53.2L29.5 55.5Z" fill="var(--s1)"/>
    <circle cx="35" cy="53.2" r="2.2" fill="var(--s3)"/>
  </symbol>

  <!-- G59 咬边燕:G34B 咬边弦窗为底,燕身向缺口飞去,红点=眼睛 -->
  <symbol id="mk-g59" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g59)">
    <path d="M75.0 70.0L75.0 18.0M65.4 72.2L83.2 23.3M55.7 71.0L89.1 31.2M47.0 66.5L92.0 40.5M40.3 59.3L91.5 50.3M36.5 50.3L87.7 59.3M36.0 40.5L81.0 66.5M38.9 31.2L72.3 71.0M44.8 23.3L62.6 72.2" fill="none" stroke="var(--s1)" stroke-width="3.2" stroke-linecap="round"/>
    </g>
    <path d="M54.8 48.4L70.8 38.8L62.8 45.2L51.6 51.6Z" fill="var(--s2)"/>
    <path d="M40.4 51.6L29.2 54.8L38.8 58Z" fill="var(--s1)"/>
    <path d="M42 54.8L34 62.8L43.6 59.6Z" fill="var(--s1)"/>
    <path d="M62.8 50L48.4 46.8L40.4 51.6L45.2 56.4L59.6 54.8Z" fill="var(--s1)"/>
    <path d="M50 48.4L32.4 35.6L37.2 43.6L48.4 51.6Z" fill="var(--s1)"/>
    <path d="M59.6 54.8L45.2 56.4L48.4 60.4L58 58Z" fill="var(--s6)"/>
    <path mask="url(#mx-g59h)" d="M59 49.5L71 49L70 56.5L60.5 57.5Z" fill="var(--s1)"/>
    <path d="M71 51L77 53.2L70.5 55.5Z" fill="var(--s1)"/>
    <circle cx="65" cy="53.2" r="2.2" fill="var(--s3)"/>
  </symbol>


  <!-- G34SW 燕孔弦窗:G34 九弦构造,内包络=燕形凸壳(头/双翼尖/尾角),红点=眼睛悬浮头叶 -->
  <symbol id="mk-g34sw" viewBox="0 0 100 100">
    <path d="M64.0 72.5L64.0 27.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M52.8 76.4L69.1 31.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M46.1 76.2L75.1 41.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M37.9 73.6L76.5 51.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M29.7 67.0L74.9 59.0" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M25.1 59.0L70.3 67.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.5 51.3L62.1 73.6" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M24.9 41.6L53.9 76.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M30.9 31.6L47.2 76.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <circle cx="50" cy="42" r="2.2" fill="var(--s3)"/>
  </symbol>

  <!-- G34SB 燕孔咬边:燕孔下移,尾尖咬破外圆(G34B 惯例),红眼悬浮 -->
  <symbol id="mk-g34sb" viewBox="0 0 100 100">
    <path d="M64.0 72.5L64.0 27.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M58.3 75.2L72.5 36.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M58.9 75.0L76.1 54.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M61.5 74.0L65.0 72.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M52.7 76.9L56.7 76.2" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M43.3 76.2L47.3 76.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M35.0 72.0L38.5 74.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.9 54.4L41.1 75.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.5 36.1L41.7 75.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <circle cx="50" cy="56" r="2.2" fill="var(--s3)"/>
  </symbol>

  <!-- G34SO 燕孔偏心:燕孔偏左上(G34O 惯例),窥视有方位,红眼在头叶 -->
  <symbol id="mk-g34so" viewBox="0 0 100 100">
    <path d="M56.0 75.8L56.0 24.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M44.8 76.0L62.7 26.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M36.2 72.6L69.9 32.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M30.1 67.5L75.1 41.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M25.6 60.3L76.5 51.3" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.8 54.2L73.1 62.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.5 49.9L63.3 72.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M24.1 44.2L51.2 76.5" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.0 36.8L40.9 74.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <circle cx="42" cy="36" r="2.2" fill="var(--s3)"/>
  </symbol>

  <!-- G43 错位环:双圆心相距 8u 的两半弧,拼成被轻微掰弯的圆 -->
  <symbol id="mk-g43" viewBox="0 0 100 100">
    <path d="M42.0 72.7A23 23 0 0 1 42.0 27.3" fill="none" stroke="var(--s1)" stroke-width="6.5" stroke-linecap="round"/>
    <path d="M58.0 27.3A23 23 0 0 1 58.0 72.7" fill="none" stroke="var(--s2)" stroke-width="6.5" stroke-linecap="round"/>
    <circle cx="50" cy="50" r="3.8" fill="var(--s3)"/>
  </symbol>

  <!-- G44 心形线:r=a(1+cos t) 参数曲线,尖点红标 -->
  <symbol id="mk-g44" viewBox="0 0 100 100">
    <path d="M77.0 50.0L76.9 51.9L76.6 53.7L76.1 55.6L75.5 57.3L74.6 59.0L73.6 60.5L72.4 61.9L71.2 63.2L69.8 64.4L68.3 65.3L66.7 66.1L65.1 66.7L63.4 67.2L61.8 67.5L60.1 67.5L58.5 67.5L57.0 67.2L55.5 66.8L54.1 66.3L52.8 65.6L51.6 64.8L50.5 64.0L49.5 63.0L48.7 62.0L48.1 61.0L47.5 59.9L47.1 58.9L46.8 57.8L46.7 56.8L46.6 55.8L46.7 54.9L46.8 54.1L47.0 53.3L47.3 52.6L47.6 52.0L47.9 51.5L48.3 51.1L48.6 50.7L48.9 50.5L49.2 50.3L49.5 50.1L49.7 50.1L49.9 50.0L50.0 50.0L50.0 50.0L50.0 50.0L49.9 50.0L49.7 49.9L49.5 49.9L49.2 49.7L48.9 49.5L48.6 49.3L48.3 48.9L47.9 48.5L47.6 48.0L47.3 47.4L47.0 46.7L46.8 45.9L46.7 45.1L46.6 44.2L46.7 43.2L46.8 42.2L47.1 41.1L47.5 40.1L48.1 39.0L48.7 38.0L49.5 37.0L50.5 36.0L51.6 35.2L52.8 34.4L54.1 33.7L55.5 33.2L57.0 32.8L58.5 32.5L60.1 32.5L61.8 32.5L63.4 32.8L65.1 33.3L66.7 33.9L68.3 34.7L69.8 35.6L71.2 36.8L72.4 38.1L73.6 39.5L74.6 41.0L75.5 42.7L76.1 44.4L76.6 46.3L76.9 48.1L77.0 50.0Z" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linejoin="round"/>
    <circle cx="50" cy="50" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G45 渐细S带:双半圆 S 路径,缎带宽 2→8→2,法向偏移烘焙 -->
  <symbol id="mk-g45" viewBox="0 0 100 100">
    <path d="MM29.8 38.8L30.0 37.8L30.3 36.8L30.6 35.9L31.1 35.1L31.6 34.3L32.2 33.6L32.8 33.0L33.5 32.4L34.3 32.0L35.1 31.6L35.9 31.3L36.7 31.1L37.5 30.9L38.4 30.9L39.2 31.0L40.0 31.1L40.8 31.3L41.5 31.5L42.2 31.9L42.9 32.3L43.5 32.8L44.1 33.3L44.6 33.9L45.0 34.5L45.4 35.1L45.7 35.8L46.0 36.5L46.1 37.2L46.2 38.0L47.1 40.9L68.7 63.5L67.9 62.0L67.8 62.8L67.6 63.5L67.3 64.2L67.0 64.9L66.6 65.5L66.2 66.1L65.7 66.7L65.1 67.2L64.5 67.7L63.8 68.1L63.1 68.5L62.4 68.7L61.6 68.9L60.8 69.0L60.0 69.1L59.2 69.1L58.3 68.9L57.5 68.7L56.7 68.4L55.9 68.0L55.2 67.6L54.5 67.0L53.8 66.4L53.2 65.7L52.7 64.9L52.3 64.1L51.9 63.2L51.6 62.2L51.5 61.2L49.5 61.3L49.4 62.5L49.5 63.7L49.7 64.9L50.0 66.1L50.5 67.2L51.1 68.3L51.8 69.4L52.6 70.4L53.6 71.3L54.6 72.1L55.7 72.7L56.9 73.3L58.2 73.7L59.5 74.0L60.8 74.2L62.2 74.2L63.5 74.1L64.9 73.8L66.2 73.3L67.4 72.8L68.6 72.1L69.8 71.2L70.8 70.3L71.7 69.2L72.5 68.0L73.2 66.8L73.7 65.5L74.1 64.1L74.3 62.7L73.6 59.1L51.9 36.5L52.7 37.3L52.5 35.9L52.1 34.5L51.6 33.2L50.9 32.0L50.1 30.8L49.2 29.7L48.1 28.8L47.0 27.9L45.8 27.2L44.6 26.7L43.3 26.2L41.9 25.9L40.5 25.8L39.2 25.8L37.8 26.0L36.5 26.3L35.3 26.7L34.1 27.3L33.0 27.9L31.9 28.7L31.0 29.6L30.2 30.6L29.5 31.7L28.9 32.8L28.4 33.9L28.1 35.1L27.9 36.3L27.8 37.5L27.9 38.7Z" fill="var(--s1)"/>
    <circle cx="53" cy="76" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G46 偏心涟漪:三环逐级缩小且环心逐级漂移 -->
  <symbol id="mk-g46" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="26" fill="none" stroke="var(--s1)" stroke-width="5"/>
    <circle cx="53" cy="47" r="19" fill="none" stroke="var(--s2)" stroke-width="4"/>
    <circle cx="56" cy="44" r="12" fill="none" stroke="var(--s3)" stroke-width="3"/>
  </symbol>


  <mask id="mx-g48"><rect x="22" y="22" width="56" height="56" rx="13" fill="white"/>
    <path d="M38 38Q50 45 62 38Q55 50 62 62Q50 55 38 62Q45 50 38 38Z" fill="black"/>
  </mask>

  <!-- G47 曲直同体:一笔从水平直线无缝卷成整圆,渐细缎带 -->
  <symbol id="mk-g47" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M30 57H58A8.6 8.6 0 0 1 49 48A7.9 7.9 0 0 1 58 39A7.1 7.1 0 0 1 67 48A6.4 6.4 0 0 1 58 60L58 60.5A12.5 12.5 0 0 0 70.5 48A14 14 0 0 0 58 34A15.5 15.5 0 0 0 42.5 48A17 17 0 0 0 58 65L30 65Z"/>
    <circle cx="58" cy="48" r="3.5" fill="var(--s3)"/>
  </symbol>

  <!-- G48 四弧成方:墨版上以四段内凹弧蓄出方形负空间,红点悬于孔心 -->
  <symbol id="mk-g48" viewBox="0 0 100 100">
    <g mask="url(#mx-g48)"><rect x="22" y="22" width="56" height="56" rx="13" fill="var(--s1)"/></g>
    <rect x="22" y="22" width="56" height="56" rx="13" fill="none" stroke="var(--mk-edge)" stroke-width="1.4"/>
    <circle cx="50" cy="50" r="4.5" fill="var(--s3)"/>
  </symbol>

  <!-- G49 弓弦张力:120° 朱砂弓弧 + 墨色弦线,弦上红点=拉满的箭待发 -->
  <symbol id="mk-g49" viewBox="0 0 100 100">
    <path d="M26 58A24.3 24.3 0 0 1 74 58" fill="none" stroke="var(--s3)" stroke-width="7" stroke-linecap="round"/>
    <path d="M26 58H74" stroke="var(--s1)" stroke-width="4.5" stroke-linecap="round"/>
    <circle cx="50" cy="44" r="4.5" fill="var(--s3)"/>
  </symbol>


  <clipPath id="cp-g34d"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g34f"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g34e"><rect x="21" y="21" width="58" height="58" rx="14"/></clipPath>
  <clipPath id="cp-g34h"><circle cx="50" cy="50" r="27"/></clipPath>

  <!-- G34D 双孔包络:弦线交替切于两枚 r7.5 钉圆,蓄出双孔 -->
  <symbol id="mk-g34d" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34d)">
    <path d="M47.5 75.9L47.5 24.1" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M59.1 76.9L75.1 27.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.8 75.4L61.3 33.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M43.5 71.3L85.4 40.8" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M17.7 65.1L66.9 49.1" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M34.1 57.5L85.9 57.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M13.1 49.1L62.3 65.1" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M34.6 40.8L76.5 71.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M18.7 33.5L49.2 75.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.9 27.7L60.9 76.9" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
     </g>
   </symbol>

  <!-- G34E 方幅包络:切线弦族裁入圆角方幅,页面上鼓出圆 -->
  <symbol id="mk-g34e" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34e)">
    <path d="M62.5 86.0L62.5 14.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M49.4 88.1L74.1 20.4" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M36.4 85.6L82.7 30.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.1 78.8L87.4 42.8" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M16.7 68.6L87.6 56.1" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M12.4 56.1L83.3 68.6" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M12.6 42.8L74.9 78.8" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M17.3 30.5L63.6 85.6" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.9 20.4L50.6 88.1" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    </g>
  </symbol>

  <!-- G34F 漂移孔包络:每弦切于逐级漂移的 r8 圆,包络孔滑动 -->
  <symbol id="mk-g34f" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34f)">
    <path d="M46.0 68.0L46.0 16.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M39.6 71.2L57.4 22.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M33.4 71.1L66.8 31.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.5 67.9L73.5 41.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.8 62.4L77.0 53.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M26.0 55.4L77.2 64.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M29.5 47.9L74.5 73.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M36.2 41.2L69.6 81.1" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M45.6 36.3L63.4 85.2" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
     </g>
   </symbol>

  <!-- G34H 对撞双扇:左右两扇相向,主圆裁切,中带对撞 -->
  <symbol id="mk-g34h" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34h)">
    <path d="M46.0 82.0L46.0 18.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M33.3 83.4L57.3 24.1" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M21.0 80.0L65.4 33.9" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M10.8 72.2L69.3 46.1" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M30.7 46.1L89.2 72.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M34.6 33.9L79.0 80.0" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
    <path d="M42.7 24.1L66.7 83.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M54.0 18.0L54.0 82.0" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    </g>
  </symbol>


  <clipPath id="cp-g34l"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g34w"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g34b"><circle cx="50" cy="50" r="27"/></clipPath>

  <!-- G34L 椭圆窥孔:支撑函数蓄出倾斜椭圆孔,红点在焦点 -->
  <symbol id="mk-g34l" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34l)">
    <path d="M67.9 70.0L67.9 18.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M56.8 72.0L74.6 23.1" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M46.0 69.5L79.4 29.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M37.5 63.9L82.5 37.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M31.9 57.1L83.1 48.1" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.6 49.7L79.8 58.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M27.5 41.3L72.5 67.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M29.4 32.4L62.8 72.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.0 24.0L52.8 72.8" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    </g>
    <circle cx="64.9" cy="38.9" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G34W 方孔窗:弦族蓄出旋转 20° 的方形孔,红点居窗心 -->
  <symbol id="mk-g34w" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34w)">
    <path d="M64.5 70.0L64.5 18.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M55.6 71.5L73.3 22.6" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M45.8 69.4L79.2 29.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M36.9 63.0L82.0 37.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M31.7 56.2L83.0 47.2" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.9 48.2L80.1 57.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M29.1 38.7L74.1 64.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M33.3 29.1L66.7 68.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M40.6 21.9L58.4 70.8" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    </g>
    <circle cx="56" cy="44" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G34B 半孔咬边:孔在边缘被主圆裁掉一半,成月牙缺口 -->
  <symbol id="mk-g34b" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34b)">
    <path d="M75.0 70.0L75.0 18.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M65.4 72.2L83.2 23.3" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M55.7 71.0L89.1 31.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M47.0 66.5L92.0 40.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M40.3 59.3L91.5 50.3" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M36.5 50.3L87.7 59.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M36.0 40.5L81.0 66.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M38.9 31.2L72.3 71.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.8 23.3L62.6 72.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    </g>
    <circle cx="64" cy="44" r="3.6" fill="var(--s3)"/>
  </symbol>


  <!-- C1 空窗:负空间自己当主角 -->
  <symbol id="mk-c1" viewBox="0 0 100 100">
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M53.6 76.5L69.9 32.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.3 76.2L74.8 39.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.7 72.7L76.8 49.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.8 66.4L75.5 58.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M24.5 58.2L71.2 66.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.2 39.9L55.7 76.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.1 32.0L46.4 76.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
  </symbol>
  <!-- C2 块光标:编辑器方块光标 -->
  <symbol id="mk-c2" viewBox="0 0 100 100">
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M53.6 76.5L69.9 32.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.3 76.2L74.8 39.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.7 72.7L76.8 49.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.8 66.4L75.5 58.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M24.5 58.2L71.2 66.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.2 39.9L55.7 76.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.1 32.0L46.4 76.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <rect x="46" y="46" width="8" height="8" rx="2" fill="var(--s3)"/>
  </symbol>
  <!-- C3 光标条:文本插入符 -->
  <symbol id="mk-c3" viewBox="0 0 100 100">
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M53.6 76.5L69.9 32.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.3 76.2L74.8 39.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.7 72.7L76.8 49.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.8 66.4L75.5 58.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M24.5 58.2L71.2 66.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.2 39.9L55.7 76.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.1 32.0L46.4 76.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <rect x="47.6" y="41.5" width="4.8" height="17" rx="2.4" fill="var(--s3)"/>
  </symbol>
  <!-- C4 红弦:红色并入线系统 -->
  <symbol id="mk-c4" viewBox="0 0 100 100">
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M53.6 76.5L69.9 32.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.3 76.2L74.8 39.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.7 72.7L76.8 49.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.8 66.4L75.5 58.2" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M24.5 58.2L71.2 66.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.2 39.9L55.7 76.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.1 32.0L46.4 76.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
  </symbol>
  <!-- C5 红窗:孔本身点亮 -->
  <symbol id="mk-c5" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="8.6" fill="var(--s3)"/>
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M53.6 76.5L69.9 32.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.3 76.2L74.8 39.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.7 72.7L76.8 49.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.8 66.4L75.5 58.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M24.5 58.2L71.2 66.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.2 39.9L55.7 76.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.1 32.0L46.4 76.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
  </symbol>

  <!-- G40 波瓣环:8 段外凸弧蓄成波浪圆,顶瓣红=预览中 -->
  <symbol id="mk-g40" viewBox="0 0 100 100">
    <path d="M50 74A10.0 10.0 0 0 1 33 67" fill="none" stroke="var(--s3)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M33 67A10.0 10.0 0 0 1 26 50" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M26 50A10.0 10.0 0 0 1 33 33" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M33 33A10.0 10.0 0 0 1 50 26" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M50 26A10.0 10.0 0 0 1 67 33" fill="none" stroke="var(--s2)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M67 33A10.0 10.0 0 0 1 74 50" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M74 50A10.0 10.0 0 0 1 67 67" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M67 67A10.0 10.0 0 0 1 50 74" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
  </symbol>

  <!-- G41 渐细新月:单条缎带弧,宽 9→1.5 跨 200°,开口朝左下 -->
  <symbol id="mk-g41" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M24.9 35.5A28.3 28.3 0 0 1 59.5 24A27.0 27.0 0 0 1 75.9 54.6A25.6 25.6 0 0 1 66.1 69.2L65.4 68.4A23.4 23.4 0 0 0 72.4 53.9A22.0 22.0 0 0 0 57.3 29.9A20.7 20.7 0 0 0 32.7 40Z"/>
    <circle cx="45.5" cy="47.5" r="4" fill="var(--s3)"/>
  </symbol>

  <!-- G42 涡旋三弧:三段 100° 弧半径 26/20/14 逐级内旋,错位 120° -->
  <symbol id="mk-g42" viewBox="0 0 100 100">
    <path d="M75.1 56.7A26 26 0 0 1 39.0 73.6" fill="none" stroke="var(--s1)" stroke-width="8" stroke-linecap="round"/>
    <path d="M35.9 64.1A20 20 0 0 1 38.5 33.6" fill="none" stroke="var(--s2)" stroke-width="8" stroke-linecap="round"/>
    <path d="M46.4 36.5A14 14 0 0 1 63.9 48.8" fill="none" stroke="var(--s3)" stroke-width="8" stroke-linecap="round"/>
  </symbol>

  <!-- G34R 匀密九弦:9 弦均布 20° 全扇,顶部横线红=预览中 -->
  <symbol id="mk-g34r" viewBox="0 0 100 100">
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M53.6 76.5L69.9 32.0" stroke="var(--s2)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M44.3 76.2L74.8 39.9" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M35.7 72.7L76.8 49.0" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M28.8 66.4L75.5 58.2" stroke="var(--s3)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M24.5 58.2L71.2 66.4" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M25.2 39.9L55.7 76.2" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
    <path d="M30.1 32.0L46.4 76.5" stroke="var(--s1)" stroke-width="3.8" stroke-linecap="round"/>
  </symbol>

  <!-- G34X 开角疏密:角距 12→30 渐扩,疏端红=览的出口 -->
  <symbol id="mk-g34x" viewBox="0 0 100 100">
    <path d="M62.5 73.7L62.5 26.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M57.3 75.8L67.2 29.4" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M50.8 76.8L71.6 34.2" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M42.5 75.7L75.5 41.6" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
    <path d="M33.4 71.1L76.7 51.8" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M25.9 61.7L73.2 63.3" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M23.2 49.0L64.3 72.7" stroke="var(--s1)" stroke-width="4" stroke-linecap="round"/>
    <path d="M27.3 35.7L51.0 76.8" stroke="var(--s3)" stroke-width="4" stroke-linecap="round"/>
  </symbol>

  <!-- G34O 偏心窥圆:弦切于 (59,41) r10.5 偏心圆,外缘裁入主圆 r27 -->
  <symbol id="mk-g34o" viewBox="0 0 100 100">
    <clipPath id="cp-g34o"><circle cx="50" cy="50" r="27"/></clipPath>
    <g clip-path="url(#cp-g34o)">
    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s2)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M23.0 51.5L95.0 51.5" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M21.6 38.6L89.2 63.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M24.7 25.9L79.8 72.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M31.9 15.1L67.9 77.4" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M42.4 7.4L54.9 78.3" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    </g>
    <circle cx="59" cy="41" r="4" fill="var(--s3)"/>
  </symbol>
<symbol id="mk-g34o-amber" viewBox="0 0 100 100">
    <clipPath id="cp-mk-g34o-amber"><circle cx="50" cy="50" r="27"/></clipPath>
    <g clip-path="url(#cp-g34o)">
    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s5)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M23.0 51.5L95.0 51.5" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M21.6 38.6L89.2 63.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M24.7 25.9L79.8 72.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M31.9 15.1L67.9 77.4" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M42.4 7.4L54.9 78.3" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    </g>
    <circle cx="59" cy="41" r="4" fill="var(--s3)"/>
  </symbol>
<symbol id="mk-g34o-mint" viewBox="0 0 100 100">
    <clipPath id="cp-mk-g34o-mint"><circle cx="50" cy="50" r="27"/></clipPath>
    <g clip-path="url(#cp-g34o)">
    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s4)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M23.0 51.5L95.0 51.5" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M21.6 38.6L89.2 63.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M24.7 25.9L79.8 72.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M31.9 15.1L67.9 77.4" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M42.4 7.4L54.9 78.3" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    </g>
    <circle cx="59" cy="41" r="4" fill="var(--s3)"/>
  </symbol>
<symbol id="mk-g34o-crimson" viewBox="0 0 100 100">
    <clipPath id="cp-mk-g34o-crimson"><circle cx="50" cy="50" r="27"/></clipPath>
    <g clip-path="url(#cp-g34o)">
    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s7)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M23.0 51.5L95.0 51.5" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M21.6 38.6L89.2 63.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M24.7 25.9L79.8 72.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M31.9 15.1L67.9 77.4" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M42.4 7.4L54.9 78.3" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    </g>
    <circle cx="59" cy="41" r="4" fill="var(--s3)"/>
  </symbol>
<symbol id="mk-g34o-mist" viewBox="0 0 100 100">
    <clipPath id="cp-mk-g34o-mist"><circle cx="50" cy="50" r="27"/></clipPath>
    <g clip-path="url(#cp-g34o)">
    <path d="M63.1 78.3L75.6 7.4" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M50.1 77.4L86.1 15.1" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M38.2 72.2L93.3 25.9" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M28.8 63.2L96.4 38.6" stroke="var(--s1b)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M23.0 51.5L95.0 51.5" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M21.6 38.6L89.2 63.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M24.7 25.9L79.8 72.2" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M31.9 15.1L67.9 77.4" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    <path d="M42.4 7.4L54.9 78.3" stroke="var(--s1)" stroke-width="3.6" stroke-linecap="round"/>
    </g>
    <circle cx="59" cy="41" r="4" fill="var(--s3)"/>
  </symbol>

  <!-- G35 分支合并:主干 + 右支弧线,三节点(出发蓝/分支墨/合并红) -->
  <symbol id="mk-g35" viewBox="0 0 100 100">
    <path d="M50 74V26" stroke="var(--s1)" stroke-width="6.5" stroke-linecap="round"/>
    <path d="M50 58H60Q68 58 68 50V42Q68 34 60 34H50" fill="none" stroke="var(--s2)" stroke-width="6.5" stroke-linecap="round"/>
    <circle cx="50" cy="58" r="5" fill="var(--s2)"/>
    <circle cx="68" cy="50" r="4" fill="var(--s1)"/>
    <circle cx="50" cy="34" r="5.5" fill="var(--s3)"/>
  </symbol>

  <!-- G36 双缝干涉:左右两组弧波相向推进,中心红点=叠加最亮处 -->
  <symbol id="mk-g36" viewBox="0 0 100 100">
    <g fill="none" stroke="var(--s1)" stroke-width="4">
      <path d="M36.4 42.3A10 10 0 0 1 36.4 57.7"/>
      <path d="M40.9 37A17 17 0 0 1 40.9 63"/>
      <path d="M45.4 31.6A24 24 0 0 1 45.4 68.4"/>
    </g>
    <g fill="none" stroke="var(--s2)" stroke-width="4">
      <path d="M63.6 42.3A10 10 0 0 0 63.6 57.7"/>
      <path d="M59.1 37A17 17 0 0 0 59.1 63"/>
      <path d="M54.6 31.6A24 24 0 0 0 54.6 68.4"/>
    </g>
    <circle cx="50" cy="50" r="4.5" fill="var(--s3)"/>
  </symbol>

  <!-- G37 希尔伯特曲线:order-2 递归一笔遍历 4x4 全格 -->
  <symbol id="mk-g37" viewBox="0 0 100 100">
    <path d="M33.5 66.5L33.5 63.5L36.5 63.5L36.5 66.5L39.5 66.5L42.5 66.5L42.5 63.5L39.5 63.5L39.5 60.5L42.5 60.5L42.5 57.5L39.5 57.5L36.5 57.5L36.5 60.5L33.5 60.5L33.5 57.5" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="33.5" cy="66.5" r="3.4" fill="var(--s3)"/>
    <circle cx="33.5" cy="57.5" r="3.4" fill="var(--s2)"/>
  </symbol>

  <!-- G38 摩尔斯节奏:点·点·划·划·点,信号本身的线性韵律 -->
  <symbol id="mk-g38" viewBox="0 0 100 100">
    <circle cx="25" cy="50" r="3.2" fill="var(--s1)"/>
    <circle cx="34" cy="50" r="3.2" fill="var(--s1)"/>
    <rect x="40" y="47" width="11" height="6" rx="3" fill="var(--s1)"/>
    <rect x="54" y="47" width="11" height="6" rx="3" fill="var(--s1)"/>
    <circle cx="71" cy="50" r="3.2" fill="var(--s3)"/>
  </symbol>

  <symbol id="mk-old" viewBox="0 0 100 100">
    <rect x="22" y="22" width="26" height="40" rx="3" fill="var(--old-panel)"/>
    <g fill="var(--blue)"><rect x="26" y="27" width="12" height="2.4" rx="1.2"/><rect x="26" y="41" width="14" height="2.4" rx="1.2"/><rect x="26" y="53" width="10" height="2.4" rx="1.2"/></g>
    <g fill="var(--old-green)"><rect x="26" y="32" width="16" height="2.4" rx="1.2"/><rect x="26" y="46" width="12" height="2.4" rx="1.2"/></g>
    <g fill="var(--old-orange)"><rect x="26" y="37" width="9" height="2.4" rx="1.2"/><rect x="26" y="58" width="15" height="2.4" rx="1.2"/></g>
    <rect x="52" y="22" width="26" height="40" rx="3" fill="var(--old-paper)" stroke="var(--old-paper-line)" stroke-width="1.4"/>
    <rect x="55" y="26" width="20" height="5" rx="1.5" fill="var(--old-head)"/>
    <rect x="55" y="34" width="20" height="8" rx="1.5" fill="var(--old-paper-line)"/>
    <g><rect x="57" y="38" width="2.4" height="3" fill="var(--old-bar-blue)"/><rect x="61" y="37" width="2.4" height="4" fill="var(--old-green)"/><rect x="65" y="38" width="2.4" height="3" fill="var(--old-orange)"/><rect x="69" y="36.5" width="2.4" height="4.5" fill="var(--old-bar-red)"/></g>
    <rect x="55" y="46" width="20" height="6" rx="1.5" fill="var(--old-paper-line)"/>
    <rect x="22" y="66" width="56" height="2.6" fill="var(--red)"/>
    <rect x="30" y="73" width="40" height="5" rx="2.5" fill="var(--old-word)"/>
  </symbol>

  <!-- G4R 直刃光圈·精修:外 r26.5 / 内 r11.5 / lead 55°,线宽 7.5,红瞳 r6 与光瞳同心 -->
  <symbol id="mk-g4r" viewBox="0 0 100 100">
    <g stroke-width="7.5" stroke-linecap="round">
      <path d="M50 23.5L59.4 43.4" stroke="var(--s1)"/>
      <path d="M73 36.75L60.4 54.9" stroke="var(--s1)"/>
      <path d="M73 63.25L51 61.45" stroke="var(--s2)"/>
      <path d="M50 76.5L40.6 56.6" stroke="var(--s1)"/>
      <path d="M27 63.25L39.6 45.1" stroke="var(--s1)"/>
      <path d="M27 36.75L49 38.55" stroke="var(--s1)"/>
    </g>
    <circle cx="50" cy="50" r="6" fill="var(--s3)"/>
  </symbol>

  <!-- G9R 叶序点阵·精修:生长前沿三色(i21/20/19),点径 ∝ √i,整体旋转 20° -->
  <symbol id="mk-g9r" viewBox="0 0 100 100">
    <circle cx="44.6" cy="52.2" r="2.55" fill="var(--s1)"/>
    <circle cx="53.5" cy="42.5" r="2.69" fill="var(--s1)"/>
    <circle cx="53.0" cy="59.7" r="2.80" fill="var(--s1)"/>
    <circle cx="39.9" cy="44.1" r="2.90" fill="var(--s1)"/>
    <circle cx="62.8" cy="47.2" r="2.98" fill="var(--s1)"/>
    <circle cx="41.8" cy="61.7" r="3.06" fill="var(--s1)"/>
    <circle cx="48.0" cy="34.7" r="3.12" fill="var(--s1)"/>
    <circle cx="62.7" cy="60.7" r="3.19" fill="var(--s1)"/>
    <circle cx="32.5" cy="50.7" r="3.25" fill="var(--s1)"/>
    <circle cx="63.1" cy="36.9" r="3.30" fill="var(--s1)"/>
    <circle cx="49.1" cy="69.4" r="3.36" fill="var(--s1)"/>
    <circle cx="37.0" cy="34.4" r="3.41" fill="var(--s1)"/>
    <circle cx="70.9" cy="52.8" r="3.46" fill="var(--s1)"/>
    <circle cx="32.0" cy="62.5" r="3.51" fill="var(--s1)"/>
    <circle cx="55.0" cy="27.9" r="3.55" fill="var(--s1)"/>
    <circle cx="61.6" cy="70.3" r="3.60" fill="var(--s1)"/>
    <circle cx="27.0" cy="42.7" r="3.64" fill="var(--s1)"/>
    <circle cx="72.5" cy="39.6" r="3.68" fill="var(--s1)"/>
    <circle cx="40.2" cy="73.5" r="3.72" fill="var(--s4)"/>
    <circle cx="41.1" cy="25.4" r="3.76" fill="var(--s2)"/>
    <circle cx="73.7" cy="62.5" r="3.80" fill="var(--s3)"/>
  </symbol>

  <!-- G9S 叶序·小尺寸简化:8 点均匀大径,仅生长点着红 -->
  <symbol id="mk-g9s" viewBox="0 0 100 100">
    <circle cx="43.3" cy="52.7" r="4.2" fill="var(--s1)"/>
    <circle cx="54.3" cy="40.8" r="4.2" fill="var(--s1)"/>
    <circle cx="53.7" cy="61.9" r="4.2" fill="var(--s1)"/>
    <circle cx="37.5" cy="42.8" r="4.2" fill="var(--s1)"/>
    <circle cx="65.7" cy="46.5" r="4.2" fill="var(--s1)"/>
    <circle cx="39.9" cy="64.4" r="4.2" fill="var(--s1)"/>
    <circle cx="47.5" cy="31.1" r="4.2" fill="var(--s1)"/>
    <circle cx="65.6" cy="63.1" r="4.2" fill="var(--s3)"/>
  </symbol>

  <!-- G7R 嵌套V形·精修:顶点间距 15u(垂直净隙 4.9u),深度 14/12/10 递减 -->
  <symbol id="mk-g7r" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M28 52L50 66L72 52" stroke="var(--s1)"/>
      <path d="M36 39L50 51L64 39" stroke="var(--s2)"/>
      <path d="M44 26L50 36L56 26" stroke="var(--s3)"/>
    </g>
  </symbol>

  <!-- G1R 旋转连续环·精修:内→中→外三段 120° 弧角度首尾相接,同一旋转运动 -->
  <symbol id="mk-g1r" viewBox="0 0 100 100">
    <g fill="none" stroke-width="8" stroke-linecap="round">
      <path d="M60 50A10 10 0 0 1 45 58.7" stroke="var(--s3)"/>
      <path d="M41 65.6A18 18 0 0 1 41 34.4" stroke="var(--s2)"/>
      <path d="M37 27.5A26 26 0 0 1 76 50" stroke="var(--s1)"/>
    </g>
    <circle cx="50" cy="50" r="4.2" fill="var(--s4)"/>
  </symbol>
</defs>`;

const CSS = `
:root{
  --seed-bg:#F5F6F8; --seed-surface:#FFFFFF; --seed-fg:#16222E;
  --seed-primary:#E23C4D; --seed-accent:#2E6FF2; --seed-night:#22303F;
  --bg:var(--seed-bg); --surface:var(--seed-surface); --fg:var(--seed-fg);
  --muted:color-mix(in srgb, var(--seed-fg) 56%, var(--seed-bg));
  --faint:color-mix(in srgb, var(--seed-fg) 40%, var(--seed-bg));
  --border:color-mix(in srgb, var(--seed-fg) 10%, var(--seed-bg));
  --border-strong:color-mix(in srgb, var(--seed-fg) 18%, var(--seed-bg));
  --code-bg:color-mix(in srgb, var(--seed-fg) 6%, var(--seed-bg));
  --red:var(--seed-primary); --red-deep:color-mix(in srgb, var(--seed-primary) 70%, black);
  --blue:var(--seed-accent); --night:var(--seed-night);
  --pure-white:#FFFFFF;
  /* mark slots: flat structural colors, single stroke language */
  --s1:#1C2B3A; --s2:var(--seed-accent); --s3:var(--seed-primary); --s4:#17B98C; --s5:#F2A93B; --s6:#F2E7D2; --s1b:#33475F; --s7:#A81832;
  --ctx-shape:#E7ECF4; --ctx-blue:#7FA9FF; --ctx-red:#FF7A85; --ctx-mint:#4FD4A9; --ctx-amber:#F7C46B;
  --ctx-ink-a:#2A3846; --ctx-ink-b:#1A2530;
  --chip-a:#FFFFFF; --chip-b:#EDF0F5;
  --bp-surface:#FBFCFE;
  --show-a:#EDF0F5; --show-b:#DFE4EC;
  --wall-a:#F3F6FA; --wall-b:#E2E8F2; --wall-c:#EBEFF6;
  --mono-fg:#F2F5F9;
  /* fixed asset-domain constants: faithful reproduction of the CURRENT shipped icon (diagnostic only) */
  --old-panel:#2A3B52; --old-green:#10B981; --old-orange:#D97706; --old-paper:#F8FAFC; --old-paper-line:#E2E8F0; --old-head:#C7D2E0; --old-bar-blue:#3B82F6; --old-bar-red:#DC2626; --old-word:#475569;
  --r:12px; --chip-r:22%;
  --mono:"Cascadia Mono","JetBrains Mono",Consolas,monospace;
  --sans:"Bahnschrift","Segoe UI Variable Text","Microsoft YaHei UI","PingFang SC",sans-serif;
}
*{box-sizing:border-box;margin:0;padding:0}
html{scroll-behavior:smooth}
body{background:var(--bg);color:var(--fg);font-family:var(--sans);font-size:15px;line-height:1.55;-webkit-font-smoothing:antialiased}
.wrap{max-width:1180px;margin:0 auto;padding:40px 24px 96px}
.mono{font-family:var(--mono)}
.caps{text-transform:uppercase;letter-spacing:.08em;font-size:11px}
.eyebrow{display:inline-flex;align-items:center;gap:8px;color:var(--red);font-weight:600}
.eyebrow::before{content:"";width:22px;height:2px;background:var(--red)}
h1{font-size:clamp(30px,4.5vw,44px);letter-spacing:-.02em;line-height:1.12;margin:10px 0 8px;font-weight:650}
.lede{max-width:70ch;color:var(--muted)}
.metarow{display:flex;flex-wrap:wrap;gap:10px;margin-top:18px}
.pill{border:1px solid var(--border-strong);border-radius:999px;padding:4px 12px;font-size:12px;color:var(--muted);background:var(--surface)}
.pill b{color:var(--fg);font-weight:600}
.discipline{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;margin-top:22px}
.dz{background:var(--surface);border:1px solid var(--border);border-radius:12px;padding:12px 14px}
.dz b{display:block;font-family:var(--mono);font-size:13px}
.dz span{font-size:11px;color:var(--muted)}
.hardcons{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:10px;margin-top:12px}
.hc{border-radius:12px;padding:12px 14px;font-size:13px;background:var(--surface);border:1px solid var(--border)}
.hc b{display:block;font-size:12px}
.hc.no{border-color:color-mix(in srgb, var(--red) 40%, transparent);background:color-mix(in srgb, var(--red) 4%, var(--surface))}
.hc.no b{color:var(--red-deep)}
.palette{display:flex;gap:8px;margin-top:16px;flex-wrap:wrap}
.pal{display:flex;align-items:center;gap:8px;border:1px solid var(--border);border-radius:10px;background:var(--surface);padding:6px 10px;font-size:12px}
.pal i{width:26px;height:18px;border-radius:4px;border:1px solid color-mix(in srgb, var(--fg) 12%, transparent)}
.pal .q1{background:var(--s1)}.pal .q2{background:var(--s2)}.pal .q3{background:var(--s3)}.pal .q4{background:var(--s4)}.pal .q5{background:var(--s5)}
section{margin-top:56px}
.sec-hd{display:flex;align-items:baseline;gap:14px;border-bottom:2px solid var(--fg);padding-bottom:10px;margin-bottom:22px}
.sec-hd .no{font-family:var(--mono);font-size:12px;color:var(--red);font-weight:700}
.sec-hd h2{font-size:22px;letter-spacing:-.01em;font-weight:650}
.sec-hd .sub{margin-left:auto;color:var(--faint);font-size:12px}
.note{color:var(--muted);max-width:78ch;font-size:14px}
.chip{display:grid;place-items:center;flex:none;position:relative;background:linear-gradient(180deg,var(--chip-a),var(--chip-b));border:1px solid color-mix(in srgb, var(--fg) 8%, transparent)}
.chip>svg{width:68%;height:68%}
.chip.on-ink{background:linear-gradient(160deg,var(--ctx-ink-a),var(--ctx-ink-b));border-color:transparent;--s1:var(--ctx-shape);--s1b:var(--ctx-shape);--s7:var(--ctx-red);--s2:var(--ctx-blue);--s3:var(--ctx-red);--s4:var(--ctx-mint);--s5:var(--ctx-amber)}
.mono-layer{--s1:currentColor;--s1b:currentColor;--s7:currentColor;--s2:currentColor;--s3:currentColor;--s4:currentColor;--s5:currentColor}
.chip.mono-layer{color:var(--mono-fg);background:linear-gradient(160deg,var(--ctx-ink-a),var(--ctx-ink-b));border-color:transparent}
.chip.mono-layer.on-white{color:var(--fg);background:var(--pure-white)}
.float{display:grid;place-items:center;background:var(--surface);border:1px solid var(--border);border-radius:var(--r)}
.float>svg{width:70%;height:70%}
.float.on-dark{background:linear-gradient(160deg,var(--ctx-ink-a),var(--ctx-ink-b));--s1:var(--ctx-shape);--s1b:var(--ctx-shape);--s7:var(--ctx-red);--s2:var(--ctx-blue);--s3:var(--ctx-red);--s4:var(--ctx-mint);--s5:var(--ctx-amber);border-color:transparent}
.wall{display:grid;grid-template-columns:repeat(auto-fill,minmax(128px,1fr));gap:12px}
.wall a{text-decoration:none;background:var(--surface);border:1px solid var(--border);border-radius:var(--r);padding:14px 10px 10px;display:grid;justify-items:center;gap:8px;transition:border-color .15s,transform .15s}
.wall a:hover{border-color:var(--fg);transform:translateY(-2px)}
.wall .stage{width:76px;height:76px}
.wall .stage>svg{width:100%;height:100%}
.wall .code{font-family:var(--mono);font-size:11px;font-weight:700}
.wall .nm{font-size:11px;color:var(--faint);margin-top:-6px}
.spec{background:var(--surface);border:1px solid var(--border);border-radius:var(--r);padding:20px;margin-bottom:18px}
.spec-hd{display:flex;gap:14px;align-items:flex-start;flex-wrap:wrap;margin-bottom:16px}
.spec-hd .code{font-family:var(--mono);font-size:13px;font-weight:700;color:var(--pure-white);background:var(--fg);border-radius:8px;padding:3px 9px;flex:none}
.spec-hd h4{font-size:16px;letter-spacing:-.01em}
.spec-hd .gene{font-size:11px;color:var(--faint);font-family:var(--mono)}
.spec-hd .concept{flex:1;min-width:240px;font-size:13px;color:var(--muted);max-width:58ch}
.spec-grid{display:grid;grid-template-columns:220px 1fr;gap:18px}
@media (max-width:760px){.spec-grid{grid-template-columns:1fr}}
.bp{background:var(--bp-surface);border-radius:10px;padding:8px;border:1px solid color-mix(in srgb, var(--fg) 10%, transparent)}
.bp svg{display:block;width:100%;height:auto}
.bp .gd{opacity:.5;transition:opacity .2s}
.spec:hover .bp .gd{opacity:1}
.bp figcaption{color:var(--muted);font-family:var(--mono);font-size:10px;padding:4px 6px 2px;display:flex;justify-content:space-between}
.simrows{display:grid;gap:14px;align-content:start}
.sim{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.sim .lab{font-family:var(--mono);font-size:10px;color:var(--faint);width:86px;flex:none}
.sim .chip{width:58px;height:58px}
.sizewrap{display:flex;align-items:flex-end;gap:14px;flex-wrap:wrap}
.sizewrap figure{display:grid;justify-items:center;gap:4px}
.sizewrap figcaption{font-family:var(--mono);font-size:10px;color:var(--faint)}
.showcase{background:linear-gradient(180deg,var(--show-a),var(--show-b));border-radius:20px;padding:34px 24px;color:var(--fg);position:relative}
.showcase h3{font-size:16px;margin-bottom:4px;position:relative}
.showcase .note{color:var(--muted);position:relative;margin-bottom:20px}
.showcols{display:grid;grid-template-columns:1fr 340px 1fr;gap:24px;align-items:start;position:relative}
@media (max-width:980px){.showcols{grid-template-columns:1fr}}
.floatstrip{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}
.floatstrip .cell{display:grid;justify-items:center;gap:6px}
.floatstrip .stage{width:84px;height:84px}
.floatstrip .stage>svg{width:100%;height:100%}
.floatstrip .code{font-family:var(--mono);font-size:10px;color:var(--faint)}
.phone{position:relative;width:340px;margin:0 auto;background:linear-gradient(165deg,var(--wall-a) 0%,var(--wall-b) 55%,var(--wall-c) 100%);border:1px solid color-mix(in srgb, var(--fg) 12%, transparent);border-radius:34px;padding:18px 16px 26px;box-shadow:0 24px 60px color-mix(in srgb, var(--fg) 16%, transparent)}
.phone .status{display:flex;justify-content:space-between;align-items:center;padding:2px 8px 14px;font-family:var(--mono);font-size:10px;color:var(--muted)}
.homerow{display:grid;grid-template-columns:repeat(3,1fr);gap:16px 10px}
.homerow .cell{display:grid;justify-items:center;gap:5px}
.homerow .chip{width:56px;height:56px;border-radius:24%}
.homerow .code{font-family:var(--mono);font-size:9px;color:var(--faint)}
.notif{margin-top:20px;position:relative;background:color-mix(in srgb, white 85%, transparent);border:1px solid color-mix(in srgb, var(--fg) 8%, transparent);border-radius:14px;padding:12px 14px;display:flex;align-items:center;gap:12px}
.notif .stage{width:22px;height:22px;flex:none}
.notif .stage>svg{width:100%;height:100%}
.notif .lines{flex:1;display:grid;gap:6px}
.notif .lines i{display:block;height:8px;border-radius:4px;background:color-mix(in srgb, var(--fg) 15%, transparent)}
.notif .lines i:last-child{width:55%}
.notif .t{font-family:var(--mono);font-size:9px;color:var(--faint)}
.matrix{width:100%;border-collapse:collapse;background:var(--surface);border:1px solid var(--border);border-radius:var(--r);overflow:hidden;font-size:13px}
.matrix th{font-size:11px;color:var(--muted);font-weight:600;text-align:left;padding:12px;border-bottom:2px solid var(--fg)}
.matrix td{padding:10px 12px;border-bottom:1px solid var(--border);vertical-align:middle}
.matrix tr:last-child td{border-bottom:none}
.matrix .fam{font-weight:650;white-space:nowrap}
.matrix .fam small{display:block;font-weight:400;color:var(--faint);font-size:11px}
.dots{display:inline-flex;gap:3px}
.dots i{width:7px;height:7px;border-radius:50%;border:1.5px solid var(--border-strong)}
.dots i.on{background:var(--fg);border-color:var(--fg)}
tr.top .fam{color:var(--red-deep)}
tr.top .dots i.on{background:var(--red);border-color:var(--red)}
.rank{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--red)}
.top3{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:14px;margin-top:18px}
.top3 .card{background:var(--surface);border:1px solid var(--border);border-left:3px solid var(--red);border-radius:var(--r);padding:16px;display:flex;gap:14px}
.top3 .stage{width:56px;height:56px;flex:none}
.top3 .stage>svg{width:100%;height:100%}
.top3 h5{font-size:14px;margin-bottom:4px}
.top3 p{font-size:12px;color:var(--muted)}
.steps{counter-reset:st;display:grid;gap:12px}
.step{background:var(--surface);border:1px solid var(--border);border-radius:var(--r);padding:14px 18px 14px 54px;position:relative}
.step::before{counter-increment:st;content:counter(st);position:absolute;left:16px;top:14px;width:24px;height:24px;border-radius:8px;background:var(--fg);color:var(--pure-white);font-family:var(--mono);font-size:12px;font-weight:700;display:grid;place-items:center}
.step b{font-size:14px}
.step p{font-size:13px;color:var(--muted);margin-top:2px}
.step code,.mcode{font-family:var(--mono);font-size:12px;background:var(--code-bg);border:1px solid var(--border);border-radius:5px;padding:1px 6px}
footer{margin-top:64px;padding-top:18px;border-top:1px solid var(--border-strong);display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;font-size:12px;color:var(--faint)}
`;

const marks = [
  {id:'g1', code:'G1', name:'三弧信号', gene:'SIGNAL RINGS · 同心 135° 弧 r10/18/26 · 层间错角 45°', c:'三根 135° 弧共享记号中心,半径 10/18/26 等差、逐层错开 45°,mint 点锚定圆心。语义:从核心向外一圈圈扩散的「览」。'},
  {id:'g2', code:'G2', name:'斜向码栈', gene:'DIAGONAL BARS · 长度 18/30/42 · -45°', c:'三根 -45° 圆头杆以 14u 间距平行,长度等差 12u,红蓝墨按序排布,mint 点在长杆尽头。语义:代码行的堆叠与速度感。'},
  {id:'g3', code:'G3', name:'十二分段环', gene:'SEGMENTED RING · 周长 12 等分 · 段 9.66/隙 3.95', c:'一个圆被精确十二等分,仅两段着色(红/蓝),中心一枚 mint 焦点。语义:进度、聚焦、循环——最克制的数据感。'},
  {id:'g4', code:'G4', name:'直刃光圈', gene:'APERTURE BLADES · outer r26 → inner r10 · lead 50° · 60° 旋转对称', c:'六条直刃以 60° 旋转对称排布,外端落在 r26 圆上、内端切于 r10 光瞳,自然围出六边形快门孔;一刃换蓝做「破序」,红瞳居中。语义:窥视的机械精确版。'},
  {id:'g5', code:'G5', name:'相切三圆', gene:'TANGENT CIRCLES · r15/r10/r6 · 45° 对角 · 全链 ≤ r30', c:'三个圆沿对角线精确外切,尺寸按黄金比递减,空隙补进第四枚 mint 切圆。语义:编辑器/预览/终端三工具相切共生。'},
  {id:'g6', code:'G6', name:'等距积木', gene:'ISOMETRIC BLOCKS · 30° 轴测 · 三面三色', c:'标准 30° 轴测积木:顶面 mint、左面墨、右面蓝,一枚红色小积木叠在左肩并带白色棱线高光——编辑器「搭建」的体块隐喻,所有斜面角度由轴测数学推出。'},
  {id:'g7', code:'G7', name:'嵌套V形', gene:'NESTED CHEVRONS · 顶点间距 12u · 90° 尖角', c:'三层 90° V 形同心嵌套,深度等差 8u,内红中蓝外墨。语义:向下滚动即预览——「轻览」的方向性动作。'},
  {id:'g8', code:'G8', name:'渐细黄金螺旋', gene:'GOLDEN SPIRAL RIBBON · 宽 9→1.5 渐细 · 四心相切', c:'螺旋第一次有了体积:实心缎带宽度从外端 9u 连续渐细到旋心 1.5u,四段圆弧切线连续无缝;红点钉在涡心。渐细的速率本身就是斐波那契的「生长」被画出来。'},
  {id:'g9', code:'G9', name:'叶序点阵', gene:'PHYLOTAXIS · 137.5° 黄金角 · r=c√n · n=21', c:'向日葵种子排布的数学直译:21 个点按黄金角与平方根半径精确分布,点径由内向外渐增,红/蓝/mint 标记生长前沿的三个「最新」点。斐波那契最自然、最不像图表的表达。'},
  {id:'g10', code:'G10', name:'黄金作图', gene:'GOLDEN CONSTRUCTION · 1:1.618 · 制图线 + 四段相切弧链', c:'把黄金矩形的完整作图过程画进图标:四级递减方格线以 22% 墨作底稿,四段相切弧链(墨→蓝→红)贯穿其上,弧链本身构成一条完整的螺旋。工程制图的仪式感。'},
  {id:'g11', code:'G11', name:'折角名牌', gene:'DOG-EAR PLATE · 切角 18u · 折页盖缝', c:'圆角方版右上折角,mint 折页盖在切缝上,红点钉在版心——「草稿折了一角」。名牌本身就是应用图标的原生形态,是所有系统里装裱成本最低的。'},
  {id:'g16', code:'G16', name:'层叠菱形', gene:'DIAMOND STACK · 45° 菱形 · 层间错位 6u', c:'三枚 45° 菱形沿对角线逐层错位 6u:墨、蓝、红——编辑、中间态、预览三层堆叠。零曲线,纯角度几何。'},
  {id:'g19', code:'G19', name:'月相序列', gene:'PHASE SEQUENCE · 全圆 → 盈月 → 红新月', c:'三个圆盘讲一个消减的过程:全圆是草稿,被咬掉的盈月是修改,红色新月是定稿。沿对角线推进,像时间轴被折叠进一个记号。'},
  {id:'g24', code:'G24', name:'透镜交集', gene:'VESICA OVERLAP · 双环 r16 · 圆心距 18 · 交叠红镜', c:'墨环与蓝环相交,透镜形交集填红——编辑与预览的重合处,正是 DraftPeek 站的位置。diff/merge 语义的直译,数学上是一个完美的 vesica piscis。'},
  {id:'g25', code:'G25', name:'分屏位移', gene:'SPLIT SHIFT · 双板错位 ±4u · 游标居间', c:'两块圆角板错位排布(左墨右红),中间一枚蓝色游标——左写右览的分屏对照。构成元素最少的一个候选。'},
  {id:'g26', code:'G26', name:'谢尔宾斯基三角', gene:'SIERPINSKI · 深度 2 递归 · 9 枚正三角', c:'递归分形的图标化:大正三角按谢尔宾斯基规则挖两轮,剩 9 枚小三角,顶枚红——「递归」是程序员一眼认出的数学,缺口本身构成倒三角负形。'},
  {id:'g27', code:'G27', name:'角无限环', gene:'ANGULAR LEMNISCATE · 双三角对顶 · 缝 8u', c:'莫比乌斯/无限环的角量化:两枚对顶三角中缝 8u,红点悬于交叉点——两条边、一个面、没有尽头的循环。'},
  {id:'g28', code:'G28', name:'利萨茹曲线', gene:'LISSAJOUS · 频率比 3:2 · 120 段折线逼近', c:'参数数学之美:x 与 y 以 3:2 频率振动,闭合曲线三次自交却一气呵成;红点标在起终点——所有振荡最终回到原点。'},
  {id:'g29', code:'G29', name:'黄金角扇', gene:'GOLDEN ANGLE FAN · 137.507° × 8 · 外刃渐长', c:'G9R 叶序点阵的放射版:8 枚圆头刃按黄金角递增排布,由于 137.507° 不可通约,任何两刃都不对齐——自然界的排布策略,红刃是最新的一片叶。'},
  {id:'g30', code:'G30', name:'蜂窝三簇', gene:'HONEYCOMB · 正六边形平铺 · 缝隙 1.3u', c:'三枚正六边形以精确平铺关系咬合(夹角 120°、缝 1.3u),红/墨/蓝——模块与插件生态;蜂窝是效率工具的经典语汇,三簇咬合加结构色分配是自己的表达。'},
  {id:'g31', code:'G31', name:'定位角标', gene:'LOCATOR TRIO · 三组嵌套方 14u · 第四角留白', c:'二维码三个定位角的抽象:描边外框+实心内芯占据三角,内芯红/蓝/mint——「扫码即达」的编码语义;第四角留白是构图呼吸位。'},
  {id:'g32', code:'G32', name:'文本行阵', gene:'TEXT LINES · 四行圆头条 · 第三行红=预览中', c:'四条圆头文本行,第三行红色加长——「正在被预览的那一行」。DraftPeek 的本体就是文本行,这是最贴身的抽象,29px 下依然是一条条分明的行。'},
  {id:'g33', code:'G33', name:'阶梯塔', gene:'ZIGGURAT · 三层收窄 · 40/30/20u', c:'三层阶梯逐级收窄(墨/蓝/红)——从草稿到成稿的逐级构建,编译进度的纪念碑形态。'},
  {id:'g34', code:'G34', name:'直线包络', gene:'LINE ENVELOPE · 7 弦切于 r12 · 角距 20°', c:'九条等宽直线只因角度逐根旋变,就蓄出一枚圆——「纯文本渲染出圆滑预览」的产品逻辑被几何直接证明;蓝线破序,红线点睛。'},
  {id:'g34r', code:'G34R', name:'匀密九弦', gene:'LINE ENVELOPE · 9 弦均布 20° · 全扇覆盖', c:'G34 的密度精修:覆盖补全、红线归位到顶部横线,中孔 29px 成圆。'},
  {id:'g34x', code:'G34X', name:'开角疏密', gene:'LINE ENVELOPE · 角距 12→30 渐扩', c:'G34 的动势精修:疏密渐扩如折扇缓开,密处撰、疏端览。'},
  {id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有方位。强调弦恢复电光蓝(与朱砂眼冷暖对撞,红弦版经对比落选);近眼三弦雾墨提亮的两级色阶保留——向眼睛旋转汇聚。强调弦五候选见下方对比条。'},
  {id:'g34sw', code:'G34SW', name:'燕孔弦窗', gene:'G34 FAMILY · 内包络=燕形凸壳 · 红点=眼', c:'G34 家族本尊的燕形化:九弦构造原样(20° 扇/端点落外圆 r26.5/八墨一红),只把内包络从圆换成燕形凸壳——弦线切于头、双翼尖、尾角;红点=眼睛悬浮头叶,四周留白即眼白。'},
  {id:'g34sb', code:'G34SB', name:'燕孔咬边', gene:'G34 FAMILY · 燕孔下移 · 尾尖咬破外圆', c:'G34B 惯例的燕孔版:孔下移 14u,尾尖咬破外圆——剪叉尾在边界上完成;红眼悬浮孔心。'},
  {id:'g34so', code:'G34SO', name:'燕孔偏心', gene:'G34 FAMILY · 燕孔偏左上 · 窥视有方位', c:'G34O 惯例的燕孔版:孔偏左上,燕向巢外张望;红眼在头叶,偏心让「窥」有了方向。'},
  {id:'g34d', code:'G34D', name:'双孔包络', gene:'TWO-PIN ENVELOPE · 弦线交替切于双钉 r7.5', c:'包络几何的经典玩法:弦线交替切于两枚「图钉」,内缘蓄出两个小孔——编辑与预览两枚钉,缝线缠绕其间。'},
  {id:'g34e', name:'方幅包络', code:'G34E', gene:'PAGE-CROPPED ENVELOPE · 弦族裁入圆角方幅', c:'同样的切线弦族,裁进圆角方幅而非圆——「一页纸」上鼓出一个圆:装裱方式的改变彻底改变气质,方与圆互相成全。'},
  {id:'g34f', code:'G34F', name:'漂移孔包络', gene:'MIGRATING ENVELOPE · 每弦切于漂移 r8 圆', c:'九根弦各切于一颗逐级漂移的「图钉」,包络孔从左上滑向右下——窥视是移动的,一串连续动作被叠进一个静帧。'},
  {id:'g34h', code:'G34H', name:'对撞双扇', gene:'CLASHING FANS · 左右双扇相向 · 主圆裁切', c:'两组切线扇从左右相向推进,在对撞带交错出莫尔条纹般的张力——密度冲突产生能量,圆是它们的竞技场。'},
  {id:'g34l', code:'G34L', name:'椭圆窥孔', gene:'ELLIPSE HOLE · 支撑函数蓄椭圆 · 红点在焦点', c:'G34O 孔形替代一:同一套切线构造,把孔换成倾斜椭圆(半轴 13/8),红点不放在中心而放在焦点——「焦点」双关注意力的焦点,光学语义最强。'},
  {id:'g34w', code:'G34W', name:'方孔窗', gene:'SQUARE HOLE · 蓄出旋转 20° 的方孔', c:'孔形替代二:弦族蓄出一个旋转 20° 的正方形孔——「窥孔」升级为「窥窗」,窗就是预览窗,产品语义最准。'},
  {id:'g34b', code:'G34B', name:'半孔咬边', gene:'EDGE-BITEN HOLE · 孔在边缘被裁成月牙', c:'孔形替代三:把孔推到主圆边缘,被裁掉一半,留下一个月牙缺口——窥视发生在边界上,最大胆的不对称。'},
  {id:'g40', code:'G40', name:'波瓣环', gene:'SCALLOP RING · 8 段外凸弧 · 蓬高 6u', c:'曲线版「弧蓄成圆」:8 段圆弧各自外凸成瓣,连接处收尖,整体是一朵波浪圆;顶瓣红。'},
  {id:'g41', code:'G41', name:'渐细新月', gene:'TAPERED CRESCENT · 缎带宽 9→1.5 · 200°', c:'一条渐细缎带弧扫过 200°,开口悬红点——「未完待续」的生成中。'},
  {id:'g42', code:'G42', name:'涡旋三弧', gene:'VORTEX THIRDS · 100° × 3 · r26/20/14 内旋', c:'三段弧半径逐级内收、角位前移,向心阶梯涡旋;内弧红。'},
  {id:'g43', code:'G43', name:'错位环', gene:'OFFSET RING · 双圆心差 8u · 两半弧', c:'两个圆心相距 8u 的半圆弧拼成被轻微掰弯的圆——预览视角的微小偏移画进轮廓;交叉点红点=两视线交点。'},
  {id:'g44', code:'G44', name:'心形线', gene:'CARDIOID · r=a(1+cos t) · 90 段逼近', c:'一动点绕等圆滚动留下的轨迹,尖点在圆心、腹部向右;尖点红标。参数数学里最流畅的弧线。'},
  {id:'g45', code:'G45', name:'渐细S带', gene:'TAPERED S · 双半圆路径 · 宽 2→8→2', c:'一条缎带走 S 形,中段最宽两端收尖——视线在文档里游走的轨迹;末端红点。'},
  {id:'g46', code:'G46', name:'偏心涟漪', gene:'DRIFTING RIPPLES · r26/19/12 · 环心漂移 3u', c:'年轮式三环,环心逐级向右上漂移——生长有方向的涟漪;最内环红。'},
  {id:'g47', code:'G47', name:'曲直同体', gene:'STRAIGHT→COIL · 一笔 · 宽 8→1.5 渐细', c:'一 Stroke 两个世界:前半是水平的直线,滑到切点后无缝卷成一整圈渐细圆环——直线与曲线不再是两种元素,而是同一笔的前后两口气。红点钉在圆心。'},
  {id:'g48', code:'G48', name:'四弧成方', gene:'ARCS→SQUARE · 四段内凹弧蓄出方形负空间', c:'G34 的倒置悖论:直线蓄出圆,这里让曲线蓄出「方」——墨版中央的方形孔,四边全部内凹成弧,方与曲同时成立;红点悬于孔心。'},
  {id:'g49', code:'G49', name:'弓弦张力', gene:'BOW & STRING · 120° 弓弧 + 弦线 · 矢待发', c:'朱砂弓弧与墨色弦线互相较劲,弓形弦上的红点是一支拉满的箭——曲线的张力被弦线「拉直」的那一刻,就是预览诞生的瞬间。'},
  {id:'g50', code:'G50', name:'折纸燕', gene:'ORIGAMI SWALLOW · 全直线多边形 · 燕尾叉红', c:'G34 直线基因的动物化:燕子完全由直线多边形折出——远翼蓝、体墨、深剪叉燕尾红。轻、快、掠过,正是「轻览」的气质;无眼纯剪影。'},
  {id:'g51', code:'G51', name:'剪影燕', gene:'SILHOUETTE SWALLOW · 单路径流畅剪影', c:'镰刀翼 + 深剪叉尾的流畅剪影,滑翔姿态一次成形;下叉尾羽红。与圆鸽类(Twitter 系)靠燕尾叉与折角翼缘区隔。'},
  {id:'g52', code:'G52', name:'燕掠弦窗', gene:'SWALLOW × ENVELOPE · 九弦窗 + 红燕掠过', c:'两个母题的嵌合:九弦弦窗是「窗」,红燕掠窗而过是「览」——轻览的全部动作被一个瞬间收拢。'},
  {id:'g50c', code:'G50C', name:'燕形线描', gene:'CONTOUR LINEART · 一根线走完整只燕', c:'燕子本身由一根连续的线画出:喙→头→翼尖→back→剪叉两羽→腹,回到喙。没有任何填充——线就是燕。剪叉两羽红。'},
  {id:'g50b', code:'G50B', name:'横排纹燕', gene:'RASTER SWALLOW · 水平文字行裁入燕形', c:'最贴题的答案:燕子由九行水平「文字行」排成——文本行本身就是燕子的羽毛;红线=正在被预览的那一行。G34 横线基因与动物母题的直接合并。'},
  {id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},
  {id:'g50d', code:'G50D', name:'羽轴包络燕', gene:'CHORD FAMILY · 内包络体芯 r9 / 外包络燕形轮廓 · 15° 扇', c:'G34 构造对燕形的忠实移植:弦线族介于体芯圆与燕形轮廓两层包络之间,15° 均匀扇——翼尖、剪叉缺口、腹线全由弦线端点落在轮廓上自然表达,零裁剪零填充;翼尖羽红。'},
  {id:'g50e', code:'G50E', name:'羽轴包络·羽色', gene:'CHORD FAMILY · 同构 · 按翼/尾/头分区配色', c:'同构配色版:翼区弦线蓝、尾区弦线红、头喙弦线琥珀、体弦墨——羽色按弦线所属身体分区映射。'},
  {id:'g56', code:'G56', name:'燕出于弦', gene:'THE SWALLOW AS ENVELOPE · 8 条边线延长成弦', c:'G34 几何表达燕形的终极形态:燕形轮廓的每条边本身就是一根弦线,向两端延长穿越整个圆盘——燕子作为纯负空间,被自己的边缘线围出来;剪叉尾的三条边全红。没有一个多余的元素:线即燕,燕即线。'},
  {id:'g57', code:'G57', name:'燕蓄', gene:'SINGLE-VARIABLE · 九弦玫瑰结原样 · 仅孔轮廓圆→燕', c:'单变量迭代的答案:G34R 的九弦玫瑰结原样保留(9 弦/20° 扇/端点落外圆 r26.5/八墨一红),只把中心孔的轮廓从圆换成燕——每根弦的垂足精确落在燕形轮廓上,燕形由弦自己蓄出。与 G34R 的唯一差别,就是孔的形状。'},
  {id:'g58', code:'G58', name:'窗中燕', gene:'COMPOSITE · G34W 方孔弦窗 × G53 燕身 · 红点睛', c:'G34W 方孔弦窗为底,燕身穿窗而过。眼部已按「眼-窗-隙」修正:12u 燕头挖出 r5 圆窗(窗缘即眼眶),红点眼睛(r2.2)悬浮窗心、四周 2.8u 间隙即眼白——像 G34L/W/B/O 那样因留白而一眼可辨。'},
  {id:'g59', code:'G59', name:'咬边燕', gene:'COMPOSITE · G34B 咬边弦窗 × G53 燕身(右飞) · 红点睛', c:'G34B 咬边弦窗为底,燕身向缺口飞去。眼部同构修正:12u 头挖 r5 圆窗,红点悬浮窗心、2.8u 眼白间隙——「从缺口窥入」的叙事闭环。'},
  {id:'g53h1', code:'G53·S1', name:'巡弋·水平行', gene:'POSE g53 × 水平行 · 端点即轮廓', c:'写生①姿态的水平行扫描:行行端点落在展翼剪影上,最长行红。'},
  {id:'g53h2', code:'G53·S2', name:'巡弋·斜行束', gene:'POSE g53 × 斜行束 -20°', c:'线沿飞行轴平行排布,速度感;端点仍落在轮廓上。'},
  {id:'g53h3', code:'G53·S3', name:'巡弋·轮廓线描', gene:'POSE g53 × 轮廓线描', c:'一根线勾出展翼燕形,最长边(翼)红。'},
  {id:'g54h1', code:'G54·S1', name:'掠影·水平行', gene:'POSE g54 × 水平行 · 端点即轮廓', c:'写生②姿态的水平行扫描,最修长剪影。'},
  {id:'g54h2', code:'G54·S2', name:'掠影·斜行束', gene:'POSE g54 × 斜行束 -15°', c:'线沿飞行轴平行排布。'},
  {id:'g54h3', code:'G54·S3', name:'掠影·轮廓线描', gene:'POSE g54 × 轮廓线描', c:'一根线勾出侧滑燕形,最长边红。'},
  {id:'g55h1', code:'G55·S1', name:'归巢·水平行', gene:'POSE g55 × 水平行 · 端点即轮廓', c:'写生③姿态的水平行扫描,扇尾分层自然呈现。'},
  {id:'g55h2', code:'G55·S2', name:'归巢·斜行束', gene:'POSE g55 × 斜行束 -35°', c:'线沿俯冲轴平行排布,俯冲感。'},
  {id:'g55h3', code:'G55·S3', name:'归巢·轮廓线描', gene:'POSE g55 × 轮廓线描', c:'一根线勾出归巢燕形,最长边红。'},
  {id:'g53', code:'G53', name:'张翼巡弋', gene:'PHOTO STUDY ① · 双翼全展浅 V · 面向左', c:'写生①的姿态:双翼全展成浅 V 滑翔,喉部朱砂、奶油腹、剪叉尾在右下——家燕巡田的标准瞬间,平涂几何化。'},
  {id:'g54', code:'G54', name:'侧滑掠影', gene:'PHOTO STUDY ② · 收翼水平滑翔 · 面向右', c:'写生②的姿态:收翼水平滑翔,剪叉尾在后,橙喙前指——全场最修长的一版,速度感来自水平轴线。'},
  {id:'g55', code:'G55', name:'归巢收羽', gene:'PHOTO STUDY ③ · 翼上扬宽 V · 尾羽下扇', c:'写生③的姿态:双翼上扬宽 V、尾羽下扇,面向左上巢口——「归巢」的叙事;尾羽透明度分层制造扇面深度。'},
  {id:'g53p', code:'G53P', name:'巡弋·扫描线', gene:'SCANLINE PICTOGRAPH · 端点即轮廓 · 零裁剪', c:'G34 构造的忠实移植:每行横线的起止点由扫描线算法精确求得,落在姿态轮廓上——燕形由线的长短「长」出来,无任何裁切;最长行红。'},
  {id:'g53q', code:'G53Q', name:'巡弋·羽色扫描', gene:'SCANLINE PICTOGRAPH · 行级羽色', c:'同构配色版:背翼行蓝、腹行奶油、喉行朱砂——羽色按行映射。'},
  {id:'g54p', code:'G54P', name:'掠影·扫描线', gene:'SCANLINE PICTOGRAPH · 端点即轮廓', c:'最修长姿态的扫描线象形:剪叉尾与收翼全由行的伸缩表达。'},
  {id:'g54q', code:'G54Q', name:'掠影·羽色扫描', gene:'SCANLINE PICTOGRAPH · 行级羽色', c:'同构配色版。'},
  {id:'g55p', code:'G55P', name:'归巢·扫描线', gene:'SCANLINE PICTOGRAPH · 端点即轮廓', c:'上扬宽 V 与下扇尾羽的扫描线象形;扇面深度由行间距自然呈现。'},
  {id:'g55q', code:'G55Q', name:'归巢·羽色扫描', gene:'SCANLINE PICTOGRAPH · 行级羽色', c:'同构配色版。'},
  {id:'g35', code:'G35', name:'分支合并', gene:'GIT GRAPH · 主干 + 右支弧线 · 三节点', c:'主干、分支、合并:三枚节点两段线构成 Git 的最小语法树,合并点红。DraftPeek 自带 Git 功能,语义零翻译成本。'},
  {id:'g36', code:'G36', name:'双缝干涉', gene:'INTERFERENCE · 左右弧波相向 · 叠加最亮', c:'物理学的双缝实验抽象:左右两组弧波相向推进,叠加中心红点最亮——两份内容对比之处,正是洞察产生之地。'},
  {id:'g37', code:'G37', name:'希尔伯特曲线', gene:'HILBERT · order-2 递归 · 一笔遍历 4x4', c:'一条线按希尔伯特次序遍历全部 16 格,不重复、不遗漏、一笔画完——「轻览全部」的空间填满;红点起点、蓝点终点。'},
  {id:'g38', code:'G38', name:'摩尔斯节奏', gene:'MORSE RHYTHM · 点点划划点 · 信号原点', c:'点、点、划、划、点——摩尔斯是「编码」一词最古老的形态;删掉字母,只留信号本身的线性韵律。收尾红点=信号到达。'},
];

const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;

function card(m){
  const tiles = ['','on-ink','mono-layer'].map(c=>`<span class="chip ${c}">${U(m.id)}</span>`).join('');
  const sizes = [96,48,29].map(s=>`<figure><span class="chip" style="width:${s}px;height:${s}px;border-radius:var(--chip-r)">${U(m.id)}</span><figcaption>${s}px</figcaption></figure>`).join('');
  const floats = `<span class="float stage" style="width:120px;height:120px">${U(m.id)}</span><span class="float on-dark stage" style="width:120px;height:120px">${U(m.id)}</span>`;
  return `<article class="spec" id="s-${m.id}" data-component="icon-spec">
  <div class="spec-hd"><span class="code">${m.code}</span><div><h4>${m.name}</h4><p class="gene">${m.gene}</p></div><p class="concept">${m.c}</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/>${U(m.id)}<use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>STROKE 8U</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span>${floats}</div>
      <div class="sim"><span class="lab">TILES</span>${tiles}</div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap">${sizes}</div></div>
    </div>
  </div>
</article>
`;
}

const wall = marks.map(m=>`<a href="#s-${m.id}"><span class="stage float">${U(m.id)}</span><span class="code">${m.code}</span><span class="nm">${m.name}</span></a>`).join('\n    ');
const floatStrip = marks.map(m=>`<div class="cell"><span class="stage">${U(m.id)}</span><span class="code">${m.code}</span></div>`).join('\n        ');
const homeCells = marks.map(m=>`<div class="cell"><span class="chip">${U(m.id)}</span><span class="code">${m.code}</span></div>`).join('\n        ') + `\n        <div class="cell"><span class="chip" style="border-style:dashed;border-color:color-mix(in srgb, var(--red) 60%, transparent)"><svg viewBox="0 0 100 100"><use href="#mk-old"/></svg></span><span class="code">现状</span></div>`;
const inkStrip = marks.map(m=>`<div class="cell"><span class="chip on-ink stage" style="width:84px;height:84px;border-radius:24%">${U(m.id)}</span><span class="code">${m.code}</span></div>`).join('\n        ');

const rows = [
  ['G1','三弧信号','同心递进',[4,4,5,5,5,5],2],
  ['G2','斜向码栈','平行节奏',[4,5,5,4,4,4],0],
  ['G3','十二分段环','数据克制',[3,3,5,4,5,4],0],
  ['G4','直刃光圈','旋转对称',[5,5,4,5,4,5],1],
  ['G5','相切三圆','相切数学',[4,4,5,3,4,4],0],
  ['G6','等距积木','轴测搭建',[4,4,5,4,4,4],0],
  ['G7','嵌套V形','方向动作',[5,5,4,5,3,4],3],
  ['G8','连续黄金螺旋','一笔生长',[4,4,5,4,5,5],0],
  ['G9','叶序点阵','自然数学',[5,5,5,3,4,5],4],
  ['G10','黄金作图','制图过程',[3,4,5,4,3,4],0],
  ['G11','折角名牌','页签折角',[4,4,4,4,5,3],0],
  ['G16','层叠菱形','层级堆叠',[4,4,4,4,4,4],0],
  ['G19','月相序列','阶段过程',[3,4,5,3,3,4],0],
  ['G24','透镜交集','对比合并',[5,5,4,4,4,5],2],
  ['G25','分屏位移','分屏对照',[4,5,3,4,5,4],0],
  ['G26','谢尔宾斯基','递归分形',[4,4,5,3,4,5],0],
  ['G27','角无限环','拓扑循环',[4,4,5,5,4,4],0],
  ['G28','利萨茹曲线','参数数学',[3,3,5,4,3,4],0],
  ['G29','黄金角扇','黄金角放射',[4,4,5,4,4,5],0],
  ['G30','蜂窝三簇','模块集群',[4,4,4,4,5,4],0],
  ['G31','定位角标','扫码编码',[5,3,4,5,4,4],0],
  ['G32','文本行阵','文本本体',[4,5,3,5,5,4],0],
  ['G33','阶梯塔','逐级构建',[3,4,4,4,4,4],0],
  ['G34','直线包络','切线成圆',[5,5,5,4,4,5],0],
  ['G34R','匀密九弦','包络精修',[5,5,4,5,4,5],2],
  ['G34X','开角疏密','动势精修',[4,4,5,4,4,4],0],
  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],
  ['G34SW','燕孔弦窗','燕形点睛',[5,5,5,4,4,5],0],
  ['G34SB','燕孔咬边','尾咬边界',[4,5,4,4,4,4],0],
  ['G34SO','燕孔偏心','方位窥视',[4,5,4,4,4,4],0],
  ['G34D','双孔包络','双钉缠线',[4,5,5,4,4,5],0],
  ['G34E','方幅包络','页面装裱',[4,4,4,5,4,4],0],
  ['G34F','漂移孔包络','动势包络',[4,4,5,4,4,5],0],
  ['G34H','对撞双扇','密度冲突',[4,4,4,5,3,4],0],
  ['G34L','椭圆窥孔','焦点双关',[5,5,5,4,4,5],2],
  ['G34W','方孔窗','窗=预览',[4,5,4,4,4,4],0],
  ['G34B','半孔咬边','边界缺口',[4,4,5,4,3,4],0],
  ['G40','波瓣环','弧蓄成圆',[5,4,4,5,4,4],0],
  ['G41','渐细新月','渐细动势',[4,4,5,4,4,4],0],
  ['G42','涡旋三弧','内旋构图',[4,4,5,4,4,4],0],
  ['G43','错位环','视角偏移',[4,4,4,4,4,4],0],
  ['G44','心形线','旋轮轨迹',[4,3,5,4,3,4],0],
  ['G45','渐细S带','视线轨迹',[4,4,5,4,4,4],0],
  ['G46','偏心涟漪','生长漂移',[4,4,4,4,4,4],0],
  ['G47','曲直同体','一笔两气',[5,5,5,4,4,5],2],
  ['G48','四弧成方','曲蓄成方',[5,4,5,4,4,4],0],
  ['G49','弓弦张力','拉满待发',[4,4,5,4,4,4],0],
  ['G50','折纸燕','直线动物',[5,4,5,4,4,4],2],
  ['G51','剪影燕','流畅剪影',[5,4,4,4,4,3],0],
  ['G52','燕掠弦窗','双母题嵌合',[4,5,4,4,4,4],0],
  ['G50C','燕形线描','一根线成燕',[4,4,5,4,4,4],0],
  ['G50B','横排纹燕','文字行成燕',[5,5,4,4,4,4],2],
  ['G50A','羽轴扇燕','包络成燕',[4,4,5,4,3,5],0],
  ['G50D','羽轴包络燕','双层包络',[5,5,5,4,4,5],2],
  ['G50E','羽轴包络·羽色','分区配色',[5,4,4,5,4,4],0],
  ['G56','燕出于弦','负空间极致',[5,5,5,4,4,5],0],
  ['G57','燕蓄','单变量迭代',[5,5,5,4,4,5],0],
  ['G58','窗中燕','合成·点睛',[5,5,4,4,4,5],0],
  ['G59','咬边燕','合成·点睛',[4,5,4,4,4,4],0],
  ['G53S1','巡弋水平行','横线象形',[4,4,4,4,4,4],0],
  ['G53S2','巡弋斜行束','斜线速度',[4,4,4,4,4,4],0],
  ['G53S3','巡弋轮廓','线描燕形',[4,4,4,4,4,4],0],
  ['G54S1','掠影水平行','横线象形',[4,4,4,4,4,4],0],
  ['G54S2','掠影斜行束','斜线速度',[4,4,4,4,4,4],0],
  ['G54S3','掠影轮廓','线描燕形',[4,4,4,4,4,4],0],
  ['G55S1','归巢水平行','横线象形',[4,4,4,4,4,4],0],
  ['G55S2','归巢斜行束','斜线俯冲',[4,4,4,4,4,4],0],
  ['G55S3','归巢轮廓','线描燕形',[4,4,4,4,4,4],0],
  ['G53','张翼巡弋','写生①滑翔',[5,4,4,4,4,4],0],
  ['G54','侧滑掠影','写生②速度',[4,4,4,4,5,4],0],
  ['G55','归巢收羽','写生③归巢',[4,5,4,4,4,4],0],
  ['G35','分支合并','版本图谱',[4,5,4,4,4,5],0],
  ['G36','双缝干涉','对比成亮',[4,4,5,4,3,4],0],
  ['G37','希尔伯特','遍历全文',[4,4,5,4,4,5],0],
  ['G38','摩尔斯节奏','编码原点',[4,4,4,4,4,4],0],
];
const dots = n => `<span class="dots">${[0,1,2,3,4].map(i=>`<i class="${i<n?'on':''}"></i>`).join('')}</span>`;
const rankMap = {G4:'TOP 1',G7:'TOP 2',G1:'TOP 3'};
const matrixRows = rows.map(rw=>{
  const top = rw[4]?'class="top"':'';
  const rank = rankMap[rw[0]]?`<span class="rank">${rankMap[rw[0]]}</span>`:'';
  return `<tr ${top}><td class="fam">${rw[0]} ${rw[1]}<small>${rw[2]}</small></td><td>${dots(rw[3][0])}</td><td>${dots(rw[3][1])}</td><td>${dots(rw[3][2])}</td><td>${dots(rw[3][3])}</td><td>${dots(rw[3][4])}</td><td>${dots(rw[3][5])}</td><td>${rank}</td></tr>`;
}).join('\n      ');
const top3cards = `
<div class="top3" data-component="top3">
  <div class="card"><span class="stage">${U('g4r')}</span><div><h5>主推 · G4R 直刃光圈</h5><p>「窥视」的精确版,六轮中唯一从未被点名批评的方向;全形态无短板。定稿默认选择。</p></div></div>
  <div class="card"><span class="stage">${U('g7r')}</span><div><h5>决赛 · G7R 嵌套V形</h5><p>动作性最强:三层等差嵌套给出「向下即预览」的方向直觉;净隙已修正至 4.9u。</p></div></div>
  <div class="card"><span class="stage">${U('g24r')}</span><div><h5>强推 · G24R 透镜交集</h5><p>概念密度最高:「编辑∩预览的重合处」就是产品定义本身;diff/merge 语义直译,参考集零重合。</p></div></div>
</div>`;


const VARIANTS = [
  ['G34 包络精修变体', 'ENVELOPE VARIANTS · 排布 / 密度 / 构图', ['g34r', 'g34x', 'g34o', 'g34d', 'g34e', 'g34f', 'g34h', 'g34l', 'g34w', 'g34b', 'g34sw', 'g34sb', 'g34so']],
  ['曲线家族 · 三种弯线美化', 'CURVE FAMILY · 蓬瓣 / 渐细 / 内旋', ['g40', 'g41', 'g42']],
  ['燕形线稿 · G34 基因', 'SWALLOW LINEART · 线描 / 排纹 / 包络', ['g50c', 'g50b', 'g50a', 'g50d', 'g50e', 'g56', 'g57']],
  ['家燕写生三连', 'PHOTO STUDY · 一图一态', ['g53', 'g54', 'g55']],
  ['弦窗飞燕 · 点睛', 'COMPOSITE · G34 弦窗 × G53 燕身', ['g58', 'g59']],
  ['燕形横线象形 · 纯扫描线', 'SCANLINE PICTOGRAPH · 端点即轮廓 · 零裁剪', ['g53p', 'g53q', 'g54p', 'g54q', 'g55p', 'g55q']],
  ['三姿态 × 三线式', 'POSE × LINESTYLE · 水平行 / 斜行束 / 轮廓线描', ['g53h1', 'g53h2', 'g53h3', 'g54h1', 'g54h2', 'g54h3', 'g55h1', 'g55h2', 'g55h3']],
];
const stripHtml = `
  <div class="family-hd" data-component="family-header"><h3>G34O · 强调弦色对比</h3><span class="caps mono">ACCENT CHORD SHOOTOUT · 五选一</span></div>
  <div class="discipline" style="grid-template-columns:repeat(auto-fit,minmax(150px,1fr))">
    <div class="dz" style="display:grid;justify-items:center;gap:8px">
      <span class="chip" style="width:110px;height:110px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g34o"/></svg></span>
      <span class="mono" style="font-size:11px;font-weight:700">电光蓝(原版)</span>
      <span class="mono" style="font-size:10px;color:var(--faint)">#3178C6</span>
    </div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px">
      <span class="chip" style="width:110px;height:110px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g34o-amber"/></svg></span>
      <span class="mono" style="font-size:11px;font-weight:700">琥珀</span>
      <span class="mono" style="font-size:10px;color:var(--faint)">#F2A93B</span>
    </div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px">
      <span class="chip" style="width:110px;height:110px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g34o-mint"/></svg></span>
      <span class="mono" style="font-size:11px;font-weight:700">薄荷绿</span>
      <span class="mono" style="font-size:10px;color:var(--faint)">#17B98C</span>
    </div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px">
      <span class="chip" style="width:110px;height:110px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g34o-crimson"/></svg></span>
      <span class="mono" style="font-size:11px;font-weight:700">绯红(深朱砂)</span>
      <span class="mono" style="font-size:10px;color:var(--faint)">#A81832</span>
    </div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px">
      <span class="chip" style="width:110px;height:110px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g34o-mist"/></svg></span>
      <span class="mono" style="font-size:11px;font-weight:700">雾墨(无彩)</span>
      <span class="mono" style="font-size:10px;color:var(--faint)">#33475F</span>
    </div>
  </div>
`;;
const specsHtml = marks.filter(m => !VARIANTS.some(v => v[2].includes(m.id))).map(card).join('\n')
  + VARIANTS.map(([title, sub, ids], vi) => '<div class="family-hd" data-component="family-header"><h3>' + title + '</h3><span class="caps mono">' + sub + '</span></div>'
    + ids.map(id => card(marks.find(m => m.id === id)) + (id === 'g34o' ? stripHtml : '')).join('\n')).join('\n');
const html = `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>DraftPeek 图标重构 · v10 决赛圈</title>
<style>${CSS}</style>
</head>
<body>
<svg width="0" height="0" style="position:absolute" aria-hidden="true">${DEFS}</svg>
<div class="wrap">
<main>
<header data-component="page-header">
  <span class="eyebrow caps">Icon Redesign · Review Board v8 · Geometric Precision</span>
  <h1>DraftPeek 图标 v10 · 决赛圈:五选一</h1>
  <p class="lede">v7 的教训:高级感不来自装饰(渐变/高光/投影恰恰是幼稚感来源),而来自数学精确。v8 先立构造纪律,再在纪律内生成 8 个记号:所有尺寸落在 8u 网格,线宽恒 8u,角度只取 0/45/60/90°,半径等差递进,平涂色按「结构位」分配而非涂装。色彩不再绑定朱砂——石墨墨、电光蓝、珊瑚红、薄荷绿、琥珀五色按节奏出现。</p>
  <div class="metarow">
    <span class="pill">平台 <b>Android Adaptive Icon · 108dp</b></span>
    <span class="pill">网格 <b>8u 制 · 记号内零自由曲线</b></span>
    <span class="pill">装裱 <b>悬浮 + 瓦片双形态</b></span>
    <span class="pill">日期 <b>2026-09-06</b></span>
  </div>
  <div class="discipline" data-component="construction-rules">
    <div class="dz"><b>GRID 8u</b><span>viewBox 100 = 12.5u 格</span></div>
    <div class="dz"><b>STROKE 8u</b><span>全图唯一线宽,圆头端点</span></div>
    <div class="dz"><b>ANGLES 0/45/60/90</b><span>无任意角度</span></div>
    <div class="dz"><b>PROGRESSION Δ8-12u</b><span>半径/长度等差递进</span></div>
    <div class="dz"><b>COLORS ≤4 FLAT</b><span>色块按结构位分配</span></div>
    <div class="dz"><b>FX: NONE</b><span>零渐变/零阴影/零高光</span></div>
  </div>
  <div class="hardcons" data-component="hard-constraints">
    <div class="hc no"><b>硬约束 1 · 无文字</b>绝对不含字母/数字/wordmark(符号内零文本节点,已自动校验)。</div>
    <div class="hc no"><b>硬约束 2 · 无眼睛</b>瞳孔/眼睑/凝视母题全部禁用。</div>
    <div class="hc no"><b>硬约束 3 · 不借形</b>母题与参考集品牌轮廓零重合,只继承工艺纪律。</div>
  </div>
  <div class="palette" data-component="palette-card">
    <span class="pal"><i class="q1"></i>石墨墨</span>
    <span class="pal"><i class="q2"></i>电光蓝</span>
    <span class="pal"><i class="q3"></i>珊瑚红</span>
    <span class="pal"><i class="q4"></i>薄荷绿</span>
    <span class="pal"><i class="q5"></i>琥珀</span>
  </div>
</header>

<section id="finals" data-component="finals-round">
  <div class="sec-hd"><span class="no">00</span><h2>决赛圈 · 五个最终候选</h2><span class="sub mono">FINAL FIVE · 以下过程档案仅供参考</span></div>
  <p class="note">经六轮迭代与两轮淘汰,19 个方向收敛为 5 个决赛候选。淘汰名单与理由见页尾「过程档案」。先看这五个,选一个即进入资源导出。</p>
  <div class="wall" data-component="finals-wall" style="margin-bottom:20px">
    <a href="#s-g4r"><span class="stage float"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span><span class="code">G4R</span><span class="nm">直刃光圈 · 主推</span></a>
    <a href="#s-g24r"><span class="stage float"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="code">G24R</span><span class="nm">透镜交集 · 强推</span></a>
    <a href="#s-g9r"><span class="stage float"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span><span class="code">G9R</span><span class="nm">叶序点阵 · 签名</span></a>
    <a href="#s-g7r"><span class="stage float"><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span><span class="code">G7R</span><span class="nm">嵌套V形 · 轻盈</span></a>
    <a href="#s-g1r"><span class="stage float"><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg></span><span class="code">G1R</span><span class="nm">旋转连续环 · 耐看</span></a>
  </div>
  <div class="top3" data-component="final-verdict">
    <div class="card"><span class="stage"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span><div><h5>裁定 · G4R 直刃光圈(主推)</h5><p>六轮中唯一从未被点名批评的方向:窥视语义、机械精确、全形态无短板。如果不想再纠结,选它,直接进资源导出。</p></div></div>
    <div class="card"><span class="stage"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><div><h5>裁定 · G24R 透镜交集(并列强推)</h5><p>概念密度最高——「编辑∩预览的重合处」就是产品定义本身;墨蓝红三色结构最清晰。想要品牌有「一句话故事」,选它。</p></div></div>
    <div class="card"><span class="stage"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span><div><h5>裁定 · G9R 叶序点阵(签名备选)</h5><p>最独特、延展面最广(动画/插画同源);代价是 29px 依赖简化形态。想要「别人没有的」,选它。</p></div></div>
  </div>
</section>

<section id="wall" data-component="overview-wall">
  <div class="sec-hd"><span class="no">01</span><h2>候选总览 · 8 系统记号(悬浮态)</h2><span class="sub mono">点击跳转规格卡</span></div>
  <div class="wall">
    ${wall}
  </div>
</section>

<section id="specs" data-component="spec-cards">
  <div class="sec-hd"><span class="no">02</span><h2>规格详评 · 网格上验证每一个数字</h2><span class="sub mono">悬停规格卡点亮构造线</span></div>
  <p class="note">每卡:8u 网格构造图(虚线圆 = 66dp 遮罩安全区,卡头标注精确几何参数)→ 悬浮态(浅/深底)→ 瓦片(浅/石墨/主题单色)→ 96/48/29px 实测。</p>
  ${specsHtml}
  
  
</section>


<div class="family-hd" data-component="retired-header"><h3>已淘汰(12)</h3><span class="caps mono">RETIRED WITH REASONS · 仅存档</span></div>
<p class="note" style="margin-bottom:14px">两轮淘汰留档,防复盘时重蹈覆辙:G2 斜向码栈(与加载条同形)、G3 十二分段环(进度环是 SaaS 通用词)、G5 相切三圆(语义靠解释)、G6 等距积木(等距方块是新粗野主义流行元素,不具独占性)、G8 渐细黄金螺旋(螺旋偏装饰,与编辑器身份最远)、G10 黄金作图(制图线 29px 必糊)、G11 折角名牌(折角是笔记类通用角标)、G16 层叠菱形(通用 layers 图标)、G19 月相序列(易读作暗色模式/天气)、G25 分屏位移(与系统分屏 glyph 同形)、G1/G4/G7 原始版(被 R 版替代)、G9 保留为 G9R 的附属简化形态。</p>

<section id="center-alts" data-component="center-alternatives">
  <div class="sec-hd"><span class="no">03+</span><h2>心元素替代 · 红点的五种命运</h2><span class="sub mono">同一具身体,只换心脏</span></div>
  <div class="discipline" style="grid-template-columns:repeat(auto-fit,minmax(180px,1fr))">
    <div class="dz" style="display:grid;justify-items:center;gap:8px;padding:14px 10px">
  <span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-c1"/></svg></span>
  <span class="mono" style="font-size:11px;font-weight:700">C1 · 空窗</span>
  <span style="display:flex;gap:8px;align-items:flex-end"><span class="chip" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c1"/></svg></span><span class="chip" style="width:29px;height:29px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c1"/></svg></span><span class="chip mono-layer" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c1"/></svg></span></span>
  <p style="font-size:11px;color:var(--muted);text-align:left">零中心元素,负空间自己当主角;最克制,但 29px 下孔可能读作"空洞"而非"窥"。</p>
</div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px;padding:14px 10px">
  <span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-c2"/></svg></span>
  <span class="mono" style="font-size:11px;font-weight:700">C2 · 块光标</span>
  <span style="display:flex;gap:8px;align-items:flex-end"><span class="chip" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c2"/></svg></span><span class="chip" style="width:29px;height:29px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c2"/></svg></span><span class="chip mono-layer" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c2"/></svg></span></span>
  <p style="font-size:11px;color:var(--muted);text-align:left">红色方块=编辑器方块光标 ▮;身份语义最正,几何与九弦同族(直角对圆头)。</p>
</div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px;padding:14px 10px">
  <span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-c3"/></svg></span>
  <span class="mono" style="font-size:11px;font-weight:700">C3 · 光标条</span>
  <span style="display:flex;gap:8px;align-items:flex-end"><span class="chip" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c3"/></svg></span><span class="chip" style="width:29px;height:29px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c3"/></svg></span><span class="chip mono-layer" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c3"/></svg></span></span>
  <p style="font-size:11px;color:var(--muted);text-align:left">红色竖条=文本插入符;全池最"编辑器"的一笔,细长在 29px 会弱化。</p>
</div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px;padding:14px 10px">
  <span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-c4"/></svg></span>
  <span class="mono" style="font-size:11px;font-weight:700">C4 · 红弦</span>
  <span style="display:flex;gap:8px;align-items:flex-end"><span class="chip" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c4"/></svg></span><span class="chip" style="width:29px;height:29px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c4"/></svg></span><span class="chip mono-layer" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c4"/></svg></span></span>
  <p style="font-size:11px;color:var(--muted);text-align:left">取消中心元素,让第四根弦变红——红色并入线系统,构图最纯粹,但焦点感稍弱。</p>
</div>
    <div class="dz" style="display:grid;justify-items:center;gap:8px;padding:14px 10px">
  <span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-c5"/></svg></span>
  <span class="mono" style="font-size:11px;font-weight:700">C5 · 红窗</span>
  <span style="display:flex;gap:8px;align-items:flex-end"><span class="chip" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c5"/></svg></span><span class="chip" style="width:29px;height:29px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c5"/></svg></span><span class="chip mono-layer" style="width:48px;height:48px;border-radius:24%"><svg viewBox="0 0 100 100"><use href="#mk-c5"/></svg></span></span>
  <p style="font-size:11px;color:var(--muted);text-align:left">孔本身填红发光——"窗后有人";色块最重,小尺寸最稳,气质最大胆。</p>
</div>
  </div>
</section>

<section id="showcase" data-component="showcase">
  <div class="sec-hd"><span class="no">03</span><h2>同框对比 · 悬浮 / 桌面 / 石墨瓦片</h2><span class="sub mono">FLOATING / HOME / INK TILE</span></div>
  <div class="showcase">
    <h3>左:悬浮记号 · 中:启动器桌面 · 右:石墨瓦片</h3>
    <p class="note">桌面区含现状图标对照(虚线框);通知栏演示 G4/G7 的 22px 单色形态。</p>
    <div class="showcols">
      <div class="floatstrip" data-component="floating-strip">
        ${floatStrip}
      </div>
      <div class="phone">
        <div class="status"><span>09:06</span><span>撰码轻览 · 桌面模拟</span></div>
        <div class="homerow">
        ${homeCells}
        </div>
        <div class="notif" data-component="notification-demo">
          <span class="stage mono-layer" style="color:var(--fg)">${U('g4')}</span>
          <div class="lines"><i></i><i></i></div>
          <span class="t">22px · MONO</span>
        </div>
        <div class="notif">
          <span class="stage mono-layer" style="color:var(--fg)">${U('g7')}</span>
          <div class="lines"><i></i><i></i></div>
          <span class="t">22px · MONO</span>
        </div>
      </div>
      <div class="floatstrip" data-component="tile-strip">
        ${inkStrip}
      </div>
    </div>
  </div>
</section>

<section id="matrix" data-component="matrix">
  <div class="sec-hd"><span class="no">04</span><h2>六维对比矩阵 · Top3</h2><span class="sub mono">SCORE 1–5 · DESIGN JUDGMENT</span></div>
  <table class="matrix">
    <thead><tr><th>记号</th><th>辨识度</th><th>概念贴合</th><th>几何严谨度</th><th>小尺寸表现</th><th>延展性</th><th>原创安全度</th><th>结论</th></tr></thead>
    <tbody>
      ${matrixRows}
    </tbody>
  </table>
  ${top3cards}
</section>


<section id="refined" data-component="refined-round">
  <div class="sec-hd"><span class="no">05</span><h2>精修轮 · 四强候选的光学级修正</h2><span class="sub mono">TOP-4 REFINED · G4R / G9R / G7R / G1R</span></div>
  <p class="note">从 10 个系统记号中收敛出 4 个最强方向,逐项做光学级修正:间隙均匀化、切点落位、着色语义归位、小尺寸简化形态。以下为最终定稿池。</p>
  <article class="spec" id="s-g4r" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">G4R</span><div><h4>直刃光圈·精修</h4><p class="gene">APERTURE · 外 r26.5 / 光瞳 r11.5 / lead 55° / 线宽 7.5</p></div><p class="concept">机械光圈的最终形态:六刃内外端分别落在 r26.5 与 r11.5 的同心圆上,55° 导程让刃与孔的比例接近真实快门;线宽从 8 收到 7.5,红瞳与光瞳同心悬浮。</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg><use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span><span class="float stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span><span class="float on-dark stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span></div>
      <div class="sim"><span class="lab">TILES</span><span class="chip "><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span><span class="chip on-ink"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span><span class="chip mono-layer"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span></div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap"></div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ①线宽 8→7.5,刃/孔比例对齐真实快门;②刃端精确落在双同心圆上,消除 v8 的松散感;③瞳孔加大至 r6,任何遮罩下孔心不闭。</div>
</article>
<article class="spec" id="s-g9r" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">G9R</span><div><h4>叶序点阵·精修</h4><p class="gene">PHYLOTAXIS · 137.5° 黄金角 · 前沿三色</p></div><p class="concept">修正 v8 的着色逻辑:三色不再散落内圈(读作噪音),而是标在外缘相邻的三颗「最新种子」上——生长前沿的故事一眼可读;整体旋转 20° 避开正上方的呆板;点径按 √i 几何增长。</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg><use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span><span class="float stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span><span class="float on-dark stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span></div>
      <div class="sim"><span class="lab">TILES</span><span class="chip "><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span><span class="chip on-ink"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span><span class="chip mono-layer"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span></div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap"><figure><span class="chip" style="width:29px;height:29px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g9s"/></svg></span><figcaption>29px·简化</figcaption></figure><figure><span class="chip" style="width:48px;height:48px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g9s"/></svg></span><figcaption>48px·简化</figcaption></figure></div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ①红/蓝/mint 从散点改为外缘相邻三连——「新叶」语义成立;②点径改 √i 增长,外圈更饱满;③整体相位旋转 20°。29px 使用 8 点简化形态(见 SIZES 行)。</div>
</article>
<article class="spec" id="s-g7r" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">G7R</span><div><h4>嵌套V形·精修</h4><p class="gene">NESTED CHEVRONS · 顶点距 15u · 深度 14/12/10</p></div><p class="concept">修正 v8 的致命间隙问题:顶点距 15u 后,平行臂间垂直净隙达 4.9u,三层 V 形终于「呼吸」;深度改为 14/12/10 递减,收束感更强。</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg><use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span><span class="float stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span><span class="float on-dark stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span></div>
      <div class="sim"><span class="lab">TILES</span><span class="chip "><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span><span class="chip on-ink"><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span><span class="chip mono-layer"><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span></div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap"></div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ①顶点距 12→15u,净隙 0.5→4.9u,三层不再粘连;②深度递减 14/12/10,视线被引向内层红尖;③蓝色中层的破序位保持。</div>
</article>
<article class="spec" id="s-g1r" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">G1R</span><div><h4>旋转连续环·精修</h4><p class="gene">ROTATIONAL CONTINUITY · r10/18/26 · 三段 120° 首尾相接</p></div><p class="concept">v8 的 135° 错角弧改为三段 120° 弧在角度上首尾相接(内段终点角=中段起点角),三个半径读作同一个旋转运动的三个瞬间——比错角排布更有「动势被冻结」的叙事。</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg><use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span><span class="float stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg></span><span class="float on-dark stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg></span></div>
      <div class="sim"><span class="lab">TILES</span><span class="chip "><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg></span><span class="chip on-ink"><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg></span><span class="chip mono-layer"><svg viewBox="0 0 100 100"><use href="#mk-g1r"/></svg></span></div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap"></div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ①三段弧角度连续(0-120-240-360),旋转运动感成立;②半径 10/18/26 等差;③内红中蓝外墨,mint 心点居中。</div>
</article>

  <article class="spec" id="s-g24r" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">G24R</span><div><h4>透镜交集·精修</h4><p class="gene">VESICA OVERLAP · 双环 r17 / 线宽 7 / 圆心距 20</p></div><p class="concept">墨环与蓝环相切级相交,透镜形交集填红——编辑与预览的重合处正是 DraftPeek 站的位置。精修后环径加大到 r17、线宽 7,红镜高 27.5u,交叠关系在 29px 依然一读即懂。</p></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg><use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span><span class="float stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="float on-dark stage" style="width:120px;height:120px"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span></div>
      <div class="sim"><span class="lab">TILES</span><span class="chip"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="chip on-ink"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><span class="chip mono-layer"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span></div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap"><figure><span class="chip" style="width:96px;height:96px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><figcaption>96px</figcaption></figure><figure><span class="chip" style="width:48px;height:48px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><figcaption>48px</figcaption></figure><figure><span class="chip" style="width:29px;height:29px;border-radius:var(--chip-r)"><svg viewBox="0 0 100 100"><use href="#mk-g24r"/></svg></span><figcaption>29px</figcaption></figure></div></div>
    </div>
  </div>
  <div class="orig"><b>精修注记</b> · ①环径 r16→r17、线宽 6.5→7,对比度拉满;②圆心距 18→20,红镜高度 26.4→27.5u,交集存在感更强;③左墨右蓝的结构色与「左写右览」方位一致。</div>
</article>
  <div class="top3" data-component="final-reco">
    <div class="card"><span class="stage"><svg viewBox="0 0 100 100"><use href="#mk-g4r"/></svg></span><div><h5>主推 · G4R 直刃光圈</h5><p>「窥视」语义、机械精确、瓦片/单色/29px 全形态无短板;工业感与 DraftPeek 的编辑器身份最契合。定稿风险最低。</p></div></div>
    <div class="card"><span class="stage"><svg viewBox="0 0 100 100"><use href="#mk-g9r"/></svg></span><div><h5>签名备选 · G9R 叶序点阵</h5><p>全场最独特:程序化生成的生长母题,同套公式可延展加载动画/启动画面/空状态;小尺寸依赖简化形态是唯一代价。</p></div></div>
    <div class="card"><span class="stage"><svg viewBox="0 0 100 100"><use href="#mk-g7r"/></svg></span><div><h5>动作备选 · G7R 嵌套V形</h5><p>方向性最强、最轻;若想要「工具感」弱一点、更轻盈的品牌气质,选它。</p></div></div>
  </div>
</section>
<section id="guide" data-component="export-guide">
  <div class="sec-hd"><span class="no">06</span><h2>定稿后 · Android 资源导出与替换</h2><span class="sub mono">NEXT ROUND</span></div>
  <p class="note" style="margin-bottom:16px">圈定方向(可含参数级微调,如「G4 弦宽改 6u」)后,下一轮生成矢量资源并精确替换:</p>
  <div class="steps">
    <div class="step"><b>生成前景矢量 <code>ic_launcher_foreground.xml</code></b><p>纯几何记号可直接转 vector path,无渐变转换损失;替换现状位图前景。</p></div>
    <div class="step"><b>生成单色层 <code>ic_launcher_monochrome.xml</code></b><p>同轮廓单色版,补齐主题图标(现状缺失)。</p></div>
    <div class="step"><b>更新 adaptive icon 定义</b><p><code>mipmap-anydpi/ic_launcher.xml</code> 与 <code>ic_launcher_round.xml</code> 加入 <code>&lt;monochrome&gt;</code>;背景色建议浅底(记号自带深色)或石墨 <code>#22303F</code>。</p></div>
    <div class="step"><b>导出商店位图</b><p>512/192/144/96/72/48 PNG(含圆角母版),供 Google Play、README 与文档站。</p></div>
    <div class="step"><b>近似检索 + 构建验证</b><p>对最终图形做商标近似检索;执行 <span class="mcode">gradlew.bat :app:assembleDebug</span> 核对遮罩、29px、主题单色。</p></div>
  </div>
</section>
</main>

<footer data-component="page-footer">
  <span>DraftPeek 图标评审板 v9 · v8 十系统 + 四强精修(G4R/G9R/G7R/G1R)· 定稿池已就绪</span>
  <span class="mono">下一步:圈定方向 → 近似检索 → 导出 Android 矢量资源</span>
</footer>
</div>
</body>
</html>`;

fs.writeFileSync(P, html);
console.log('v8 written, bytes=', html.length);
