/**
 * Regenerates assets/samples/welcome.pdf with proper CJK font support.
 *
 * The original PDF used WinAnsiEncoding (Helvetica) for Chinese text encoded
 * as UTF-16BE, causing PdfRenderer to display garbled characters.
 *
 * Usage: node scripts/generate-welcome-pdf.js
 */
const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

const OUTPUT = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'samples', 'welcome.pdf');

// Use SimHei (黑体) – available on all Chinese Windows systems; TTF format supported by pdfkit
const FONT_PATH = 'C:\\Windows\\Fonts\\simhei.ttf';

const doc = new PDFDocument({
  size: 'A4',
  margins: { top: 50, bottom: 50, left: 50, right: 50 },
  info: {
    Title: 'Welcome to DraftPeek',
    Author: 'DraftPeek',
    Creator: 'DraftPeek',
    Producer: 'DraftPeek',
  },
});

const stream = fs.createWriteStream(OUTPUT);
doc.pipe(stream);

// ── Page 1: Title ─────────────────────────────────────────────────────
doc.moveDown(6);
doc.font(FONT_PATH).fontSize(36).fillColor('#2563EB')
   .text('欢迎使用 DraftPeek', { align: 'center' });
doc.moveDown(1);
doc.fontSize(20).fillColor('#666666')
   .text('一款轻量级 Android 代码查看与编辑器', { align: 'center' });

// ── Page 2: Code Examples ─────────────────────────────────────────────
doc.addPage();
doc.font(FONT_PATH).fontSize(24).fillColor('#2563EB')
   .text('一、代码示例');
doc.moveDown(0.5);

const codeSamples = [
  { lang: 'Java', code: 'System.out.println("你好，欢迎使用 DraftPeek！");' },
  { lang: 'Python', code: 'print("你好，欢迎使用 DraftPeek！")' },
  { lang: 'JavaScript', code: 'console.log("你好，欢迎使用 DraftPeek！");' },
  { lang: 'Kotlin', code: 'println("你好，欢迎使用 DraftPeek！")' },
  { lang: 'C', code: 'printf("你好，欢迎使用 DraftPeek！\\n");' },
  { lang: 'C++', code: 'std::cout << "你好，欢迎使用 DraftPeek！" << std::endl;' },
  { lang: 'Go', code: 'fmt.Println("你好，欢迎使用 DraftPeek！")' },
  { lang: 'Rust', code: 'println!("你好，欢迎使用 DraftPeek！");' },
  { lang: 'Ruby', code: 'puts "你好，欢迎使用 DraftPeek！"' },
  { lang: 'Dart', code: "print('你好，欢迎使用 DraftPeek！');" },
  { lang: 'Lua', code: 'print("你好，欢迎使用 DraftPeek！")' },
  { lang: 'Scala', code: 'println("你好，欢迎使用 DraftPeek！")' },
];

for (const { lang, code } of codeSamples) {
  doc.font(FONT_PATH).fontSize(11).fillColor('#333333')
     .text(`${lang}: ${code}`);
}

// ── Page 3: Features ──────────────────────────────────────────────────
doc.addPage();
doc.font(FONT_PATH).fontSize(24).fillColor('#2563EB')
   .text('二、功能特色');
doc.moveDown(0.5);

const features = [
  '• 支持 50+ 编程语言的语法高亮显示',
  '• 安全可靠：SQLCipher 加密数据库存储',
  '• 基于 Material 3 设计规范的现代界面',
  '• 支持文件浏览与管理功能',
  '• 支持查看与编辑 Office 文档',
  '• 内置多种实用工具与设置选项',
  '• 完善的文件浏览历史记录功能',
  '• 便捷的代码片段管理功能',
];

doc.font(FONT_PATH).fontSize(14).fillColor('#333333');
for (const feature of features) {
  doc.text(feature);
  doc.moveDown(0.3);
}

// ── Page 4: Thank You ─────────────────────────────────────────────────
doc.addPage();
doc.moveDown(6);
doc.font(FONT_PATH).fontSize(28).fillColor('#10B981')
   .text('感谢使用 DraftPeek！', { align: 'center' });
doc.moveDown(1);
doc.fontSize(18).fillColor('#333333')
   .text('你好，欢迎使用我们的程序', { align: 'center' });
doc.moveDown(1);
doc.fontSize(14).fillColor('#999999')
   .text('© 2026 DraftPeek Team', { align: 'center' });

doc.end();

stream.on('finish', () => {
  const stats = fs.statSync(OUTPUT);
  console.log(`✓ Generated ${OUTPUT} (${stats.size} bytes)`);
});

stream.on('error', (err) => {
  console.error('Failed to write PDF:', err);
  process.exit(1);
});
