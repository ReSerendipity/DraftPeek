#!/usr/bin/env bash
# A' 方案：在 benchmark 的 macro 用例跑起来之前，把两道"首启门"预置成已通过与真实用户一致的状态。
#
#   门 1 引导页：DataStore(name="onboarding") 的 booleanPreferencesKey("completed")=true
#         —— 唯一读点 app/.../SplashActivity.kt:141-143，决定 :87-89 跳 Onboarding 还是 Main。
#   门 2 协议弹窗：SharedPreferences("agreement") 的 "accepted_v1"=true
#         —— 唯一读点 app/.../MainActivity.kt:173-175，未同意则常驻 AgreementGateDialog（:319-348）。
#         这道门是本轮新查实的：只绕引导的话，进了 MainActivity 也点不到「新建文件」。
#
# 为什么走 root+预置而不是 UI 点过或直启：被测包是 production release（非 debuggable），
# run-as 不可用；`am start -n …/.MainActivity` 直启在 run 36029759144 实测被接受但未落地
# （无 MainActivity record、焦点在桌面）。预置让应用走自己的 LAUNCHER 路径，与真实用户一致。
#
# 失败口径（统一 fail-closed）：任一步不成立就打印 id / ls -lZ / crash 缓冲三件套后非零退出，
# 绝不允许"预置没成功也继续跑、然后把红归因给测试断言"。
#
# 本脚本还做第三件事（步骤②d）：往应用内部存储的 user_files 里播一个 aaa_bench_seed.kt，
# macro 用例点它的列表行进 CodeEditor —— 让性能腿只测"打开并输入"，不再背负新建文件对话框
# 的输入依赖。包名从 app/build.gradle.kts 解析并与设备已安装包三方核对，不单方面硬编码。
set -euo pipefail

PKG=com.draftpeek
DATA="/data/data/$PKG"
DS_DIR="$DATA/files/datastore"
SP_DIR="$DATA/shared_prefs"
DS_FILE="$DS_DIR/onboarding.preferences_pb"
SP_FILE="$SP_DIR/agreement.xml"

# 载荷本地生成、base64 穿透写入。不用 printf + 单引号包裹：本机 sh -c 同层穿透实测会把
# <?xml version='1.0' ...?> 的单引号吃掉变成畸形 XML（SharedPreferences 解析不出来），
# 而只查布尔属性的 grep 自证照样通过 —— 属于会静默骗过自检的坑。
#
# DataStore wire 字节：PreferenceMap{ map<string,Value> preferences=1 } →
# MapEntry{ key(1)="completed", value(2)=Value{ bool(1)=true } } = 0A 0F 0A 09 completed 12 02 08 01
# （字段号取自 datastore-preferences-core-jvm 1.1.1 的 PreferenceMap.PREFERENCES_FIELD_NUMBER=1
#   与 Value.BOOLEAN_FIELD_NUMBER=1）
DS_BYTES=17
DS_B64=$(printf '\012\017\012\011completed\022\002\010\001' | base64 -w0)
SP_XML='<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><boolean name="accepted_v1" value="true" /></map>'
SP_B64=$(printf '%s' "$SP_XML" | base64 -w0)

diag() {
  echo "::error::A' 预置失败，以下为定死原因所需的三件套"
  echo "--- adb shell id（非 root 时这行就是结论） ---"
  adb shell id 2>&1 || true
  echo "--- getprop ro.build.type ---"
  adb shell getprop ro.build.type 2>&1 || true
  echo "--- ls -lZ 目标路径 ---"
  adb shell "ls -lZd '$DS_DIR' '$SP_DIR' '$DS_FILE' '$SP_FILE' 2>&1" || true
  echo "--- crash 缓冲（末 60 行） ---"
  adb shell "logcat -d -b crash -t 60 2>&1" || true
  echo "--- 当前 resumed / focus ---"
  adb shell "dumpsys activity activities 2>&1 | grep -E 'topResumedActivity|mResumed' | head -5" || true
  adb shell "dumpsys window 2>&1 | grep mCurrentFocus | head -3" || true
}
trap 'diag; exit 1' ERR

echo "== 步骤① root 断言 =="
adb wait-for-device
adb root >/dev/null 2>&1 || true
adb wait-for-device
IDU="$(adb shell id -u | tr -d '\r')"
if [ "$IDU" != "0" ]; then
  echo "id -u=$IDU（期望 0）⇒ 本镜像 root 不可用，A/A' 全废，按裁决转 B1 议题"
  adb shell id
  adb shell getprop ro.build.type
  exit 1
fi
echo "root 可用：uid=$IDU，build=$(adb shell getprop ro.build.type | tr -d '\r')"

echo "== 步骤② 基线探针（预置前的界面状态，用来对比） =="
adb shell "dumpsys activity activities 2>&1 | grep -E 'topResumedActivity|mResumed' | head -3" || true
adb shell "dumpsys window 2>&1 | grep -E 'mCurrentFocus|mFocusedApp' | head -3" || true

APP_UID="$(adb shell "stat -c %u $DATA" | tr -d '\r')"
[ -n "$APP_UID" ] || { echo "取不到 $DATA 的 uid"; exit 1; }
echo "app uid=$APP_UID"

# force-stop 再写：避免既有进程已经把 DataStore 的空实例建在内存里（写盘也读不到）
adb shell "am force-stop $PKG"
adb shell "mkdir -p '$DS_DIR' '$SP_DIR'"
adb shell "echo $DS_B64 | base64 -d > '$DS_FILE'"
adb shell "echo $SP_B64 | base64 -d > '$SP_FILE'"
adb shell "chown -R $APP_UID:$APP_UID '$DS_DIR' '$SP_DIR'"
adb shell "restorecon -R '$DS_DIR' '$SP_DIR'" 2>/dev/null || echo "(restorecon 不可用，继续；回读断言兜底)"
adb shell "am force-stop $PKG"

echo "== 步骤②b 写完回读自证（字节级） =="
DS_SIZE="$(adb shell "wc -c < '$DS_FILE'" | tr -d '\r')"
if [ "$DS_SIZE" != "$DS_BYTES" ]; then
  echo "DataStore 文件字节数=$DS_SIZE，期望=$DS_BYTES ⇒ 手写 wire 字节没写对"
  adb shell "od -c '$DS_FILE'" || true
  exit 1
fi
adb shell "ls -lZ '$DS_FILE' '$SP_FILE'"
adb shell "od -c '$DS_FILE'"
# agreement.xml：既查布尔属性，也查 XML 声明的引号有没有被 shell 吃掉（畸形则解析不出）
SP_READ="$(adb shell "cat '$SP_FILE'" | tr -d '\r')"
SP_WANT="$(printf '%s' "$SP_XML" | wc -c)"
SP_GOT="$(printf '%s' "$SP_READ" | wc -c)"
if ! printf '%s' "$SP_READ" | grep -q 'version="1.0"'; then
  echo "agreement.xml 畸形（声明引号被吃）：$SP_READ"; exit 1
fi
if ! printf '%s' "$SP_READ" | grep -q 'accepted_v1" value="true"'; then
  echo "agreement.xml 不含 accepted_v1=true：$SP_READ"; exit 1
fi
if [ "$SP_GOT" != "$SP_WANT" ]; then
  echo "agreement.xml 字节数不符：got=$SP_GOT want=$SP_WANT"; exit 1
fi
# 属主必须是被测 app 的 uid，否则 app 进程读不到（root 写出来的常见坑）。
# 注意：stat -c %U 返回的是 u0_aNNN 而不是包名，所以只能按 uid 数值比对。
FILE_UID="$(adb shell "stat -c %u '$DS_FILE'" | tr -d '\r')"
SP_UID="$(adb shell "stat -c %u '$SP_FILE'" | tr -d '\r')"
if [ "$FILE_UID" != "$APP_UID" ] || [ "$SP_UID" != "$APP_UID" ]; then
  echo "属主 uid 不符（期望 app uid=$APP_UID）：DS=$FILE_UID SP=$SP_UID"; exit 1
fi

echo "== 步骤②c 包名自证：从构建配置取，不接受脚本单方面硬编码 =="
# applicationId 是 production flavor 的包名（该 flavor 无 applicationIdSuffix）。
# 脚本里的 PKG 与构建配置不一致时立即失败 —— 否则会出现"预置写进了一个没人装的包"。
BUILD_PKG="$(sed -n 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)"
if [ -z "$BUILD_PKG" ]; then
  echo "没能从 app/build.gradle.kts 解析出 applicationId（改过构建脚本？先核对再跑）"
  exit 1
fi
if [ "$BUILD_PKG" != "$PKG" ]; then
  echo "构建配置 applicationId=$BUILD_PKG ≠ 脚本 PKG=$PKG ⇒ 停，别把载荷写到错包里"
  exit 1
fi
adb shell "pm list packages" | tr -d '\r' | grep -qx "package:$PKG" || { echo "设备上没装 $PKG（预置要求先安装被测包）"; exit 1; }
echo "包名三方一致：构建配置=$BUILD_PKG / 脚本=$PKG / 设备已安装"

echo "== 步骤②d 播一个可直接打开的种子文件（macro 靠它进 CodeEditor）=="
# 为什么走文件而不走「新建文件」UI：CreateFileDialog 的主按钮 enabled = filename.isNotBlank()
# （CreateFileDialog.kt:198），无头档还要背 IME 注入的不确定性 —— 那是功能测试的活（见
# feature/browser/src/androidTest/.../CreateFileDialogFlowTest.kt），不该压给性能基线。
# 落盘路径的生产依据：AppFileManager.kt:23 USER_FILES_DIR="user_files" 与 :33-37
# File(context.filesDir, "user_files")；读盘侧 EditorRepositoryImpl.kt:446-459 要求
# isInternalUri + exists() && isFile。列表默认根就是它（FileBrowserScreen.kt:1560 internalFiles），
# 所以从 MainActivity 到该文件只需 1 次点击。
SEED_SUBDIR=user_files
# 默认排序 NAME_ASC（FileBrowserViewModel.kt:176 / FileSortOption.kt:44-46 按 name.lowercase()），
# "aaa" 前缀让它落在首屏第一行，省掉滚动的不确定性。别用 .jenc 后缀（会转去解密对话框）。
SEED_NAME=aaa_bench_seed.kt
SEED_TEXT='// DraftPeek macrobenchmark seed file. Safe to delete; CI recreates it.
fun benchmarkSeedEntry(): Int = 42
'
SEED_BYTES=$(printf '%s' "$SEED_TEXT" | wc -c)
SEED_B64=$(printf '%s' "$SEED_TEXT" | base64 -w0)

# 内部存储的真实位置在设备上问，不在脚本里假定（/data/data 是 /data/user/0 的符号链接，
# 某些多用户/镜像配置下只有后者可解析）。
RESOLVED_DIR="$(adb shell "readlink -f '$DATA'" | tr -d '\r')"
[ -n "$RESOLVED_DIR" ] || { echo "readlink -f $DATA 取不到真实路径"; exit 1; }
SEED_DIR="$RESOLVED_DIR/files/$SEED_SUBDIR"
SEED_FILE="$SEED_DIR/$SEED_NAME"
echo "设备实测数据目录：$DATA → $RESOLVED_DIR；种子落 $SEED_FILE"

adb shell "am force-stop $PKG"
adb shell "mkdir -p '$SEED_DIR'"
adb shell "echo $SEED_B64 | base64 -d > '$SEED_FILE'"
adb shell "chown $APP_UID:$APP_UID '$SEED_DIR' '$SEED_FILE'"
adb shell "restorecon -R '$SEED_DIR'" 2>/dev/null || echo "(restorecon 不可用，继续；下面回读兜底)"
adb shell "am force-stop $PKG"

echo "== 步骤②e 种子回读自证 =="
SEED_SIZE="$(adb shell "wc -c < '$SEED_FILE'" | tr -d '\r')"
if [ "$SEED_SIZE" != "$SEED_BYTES" ]; then
  echo "种子字节数=$SEED_SIZE，期望=$SEED_BYTES"; adb shell "od -c '$SEED_FILE'" || true; exit 1
fi
SEED_OWN="$(adb shell "stat -c %u '$SEED_FILE'" | tr -d '\r')"
[ "$SEED_OWN" = "$APP_UID" ] || { echo "种子属主 uid=$SEED_OWN，期望 $APP_UID"; exit 1; }
# 唯一定位的前提：该目录里 .kt 只有种子这一个，用例才能按文件名单击命中它。
KT_COUNT="$(adb shell "ls '$SEED_DIR' | grep -c '\.kt$'" | tr -d '\r')"
if [ "$KT_COUNT" != "1" ]; then
  echo "user_files 下有 $KT_COUNT 个 .kt，用例按名字定位会有歧义"; adb shell "ls -l '$SEED_DIR'" || true; exit 1
fi
adb shell "ls -lZ '$SEED_FILE'"
echo "种子就绪：$SEED_NAME（$SEED_SIZE 字节，属主 uid=$SEED_OWN，目录内 .kt 唯一）"

echo "== 步骤③ 交给 LAUNCHER 路径：本脚本不直启任何 Activity =="
echo "预置完成，随后 connectedDebugAndroidTest 会 pressHome + startActivityAndWait，"
echo "由 SplashActivity 自己读 completed=true 导航到 MainActivity。"
trap - ERR
echo "A' 预置成功"
