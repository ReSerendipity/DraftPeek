// Apply the G34O final export to DraftPeek app resources — G34O ONLY.
// Do NOT point SRC at docs/icon-design/archive/g34r/: that is the historical G34R package
// and would regress the launcher icon.
const fs = require('fs');
const os = require('os');
const path = require('path');

const ROOT = path.resolve(__dirname, '../../..');
const SRC = path.join(ROOT, 'docs/icon-design/icon-export-g34o/res');
const RES = path.join(ROOT, 'app/src/main/res');
// Runtime safety backup outside the repo; not part of the historical archive.
const BK = path.join(os.tmpdir(), 'draftpeek-icon-backup-g34o');

if (!fs.existsSync(SRC)) {
  throw new Error(`G34O export not found: ${SRC}`);
}
fs.mkdirSync(BK, { recursive: true });

function backupAndCopy(rel, srcFile) {
  const dst = path.join(RES, rel);
  if (fs.existsSync(dst)) {
    const bk = path.join(BK, rel.replace(/[\\/]/g, '__'));
    fs.copyFileSync(dst, bk);
    console.log('backed up:', rel);
  } else {
    console.log('new file:', rel);
  }
  fs.mkdirSync(path.dirname(dst), { recursive: true });
  fs.copyFileSync(srcFile, dst);
}

// 1) adaptive icon XMLs (overwrite)
backupAndCopy('mipmap-anydpi/ic_launcher.xml', path.join(SRC, 'mipmap-anydpi/ic_launcher.xml'));
backupAndCopy('mipmap-anydpi/ic_launcher_round.xml', path.join(SRC, 'mipmap-anydpi/ic_launcher_round.xml'));
// 2) background colors (overwrite day + night)
backupAndCopy('values/ic_launcher_background.xml', path.join(SRC, 'values/ic_launcher_background.xml'));
backupAndCopy('values-night/ic_launcher_background.xml', path.join(SRC, 'values-night/ic_launcher_background.xml'));
// 3) G34O drawables (foreground vector + monochrome)
backupAndCopy('drawable/ic_launcher_foreground.xml', path.join(SRC, 'drawable/ic_launcher_foreground.xml'));
backupAndCopy('drawable/ic_launcher_monochrome.xml', path.join(SRC, 'drawable/ic_launcher_monochrome.xml'));
// 4) legacy cleanup: the initial PNG should already be absent from current res
const legacyPng = path.join(RES, 'drawable-nodpi/ic_launcher_foreground.png');
if (fs.existsSync(legacyPng)) {
  fs.copyFileSync(legacyPng, path.join(BK, 'drawable-nodpi__ic_launcher_foreground.png'));
  fs.unlinkSync(legacyPng);
  console.log('moved legacy PNG out of res (backed up): drawable-nodpi/ic_launcher_foreground.png');
}
console.log('DONE — G34O resources applied to app/src/main/res');
