// Apply export package to the DraftPeek project — with automatic backup of originals
const fs = require('fs');
const path = require('path');
const BK = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/outputs/DraftPeek-icon-export/backup-originals';
const SRC = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/outputs/DraftPeek-icon-export/res';
const RES = 'C:/Users/Doro/DraftPeek/app/src/main/res';
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
// 3) new drawables (foreground vector + monochrome)
backupAndCopy('drawable/ic_launcher_foreground.xml', path.join(SRC, 'drawable/ic_launcher_foreground.xml'));
backupAndCopy('drawable/ic_launcher_monochrome.xml', path.join(SRC, 'drawable/ic_launcher_monochrome.xml'));
// 4) move old PNG foreground out of res (avoid resource-name collision) — backup, then remove from res
const oldPng = path.join(RES, 'drawable-nodpi/ic_launcher_foreground.png');
if (fs.existsSync(oldPng)) {
  fs.copyFileSync(oldPng, path.join(BK, 'drawable-nodpi__ic_launcher_foreground.png'));
  fs.unlinkSync(oldPng);
  console.log('moved old PNG out of res (backed up): drawable-nodpi/ic_launcher_foreground.png');
}
console.log('DONE — project res updated');
