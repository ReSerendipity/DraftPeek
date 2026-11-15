# JNI 符号按「包名 + 类名 + 方法名」查找，R8 改名会让 native 方法绑定失败。
# TerminalPtyNative 的任何混淆都会导致 UnsatisfiedLinkError，必须保留原名与 native 方法。
-keep class com.draftpeek.feature.terminal.emulator.TerminalPtyNative {
    native <methods>;
}
