#!/usr/bin/env node
/*
 * 文档导出工具链冒烟：pptxgenjs 可加载 + 真生成一个含文本/图片的 pptx。
 *
 * 背景：pptxgenjs@4.0.1 的 package.json 声明依赖 image-size@^1.2.1（HIGH 漏洞
 * GHSA-5p2g-fcmc-qvqq / GHSA-w3rx-r6r6-pgpr），但其 dist 中唯一引用尺寸探测的
 * getSizeFromImage 整段是注释死代码（"currently unused"），根 package.json 用
 * overrides 把 image-size 钉到 2.x。本冒烟保证 override 后库仍可加载、pptx 可生成。
 */
const PptxGenJS = require("pptxgenjs");
const JSZip = require("jszip");

const png =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

const pptx = new PptxGenJS();
const slide = pptx.addSlide();
slide.addText("smoke", { x: 1, y: 1, w: 3, h: 1 });
// base64 data 路径，不触文件系统尺寸探测
slide.addImage({ data: "image/png;base64," + png, x: 1, y: 2, w: 1, h: 1 });

pptx
  .write({ outputType: "nodebuffer" })
  .then(async (buf) => {
    if (!Buffer.isBuffer(buf) || buf.length < 1000) {
      throw new Error("bad pptx buffer: " + (buf && buf.length));
    }
    const zip = await JSZip.loadAsync(buf);
    const slides = Object.keys(zip.files).filter((n) => n.includes("slide1.xml"));
    if (slides.length < 1) throw new Error("slide1.xml missing from generated pptx");
    console.log("SMOKE OK: pptx bytes=" + buf.length + " slide1 entries=" + slides.length);
  })
  .catch((err) => {
    console.error("SMOKE FAIL", err);
    process.exit(1);
  });
