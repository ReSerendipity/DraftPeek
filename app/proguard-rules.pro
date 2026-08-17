# === PLAN-C FUTURE ===
# 方案 C 启用 OLLVM 时的 keep 规则：
# 当 ollvm-ndk 工具链启用控制流平坦化/虚假控制流/指令替换时，
# 以下规则保护 ollvm 生成的辅助类不被 R8 重命名/删除：
#   -keep class com.draftpeek.security.decoy.** { *; }
#   -keepclasseswithmembernames class * {
#       native <methods>;
#   }
#   -keep class * extends com.draftpeek.core.common.security.SecureStringResolver { *; }
# === END PLAN-C ===

# ===== R8 Standard Mode 混淆配置 =====
# 注意：android.enableR8.fullMode=false（见 gradle.properties）。
# Full Mode 会破坏 Apache POI / JGit 的反射链（详见 AGENTS.md），
# 因此本工程使用 Standard Mode。
#
# SECURITY VULN-002: 移除全局 -dontoptimize，恢复 R8 字节码优化。
# 仅禁用 field/marking/final 优化，该优化会错误地将 sora-editor
# DirectAccessProps 的非 final 字段标记为 final，导致运行时
# IllegalAccessError: Final field cannot be written to。
# 其他优化（method/inlining, code/dead/code, code/simplification 等）正常启用，
# 显著增加逆向工程难度（方法内联、死代码删除、控制流简化）。
-optimizations !field/marking/final

# ===== Release 日志移除（VULN-011: 移除所有级别日志） =====
# SECURITY: Release 构建中移除所有 Log 级别（d/v/i/w/e），
# 防止 logcat 泄露应用内部信息。仅保留 Crashlytics/崩溃收集机制。
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ===== 增强混淆 =====
# 注意：-repackageclasses ''（移至默认包）在 Kotlin 2.2+ 与多轮优化组合时
# 会破坏协程 SuspendLambda 状态机的 final 字段访问，导致 IllegalAccessError 崩溃。
# 改用 -flattenpackagehierarchy 将混淆类放入单一 obf 包，仍有混淆效果但不破坏访问权限。
# -repackageclasses ''
-flattenpackagehierarchy com.draftpeek.obf

# 保留堆栈信息用于崩溃分析（不泄露源文件名）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== Kotlin 协程保护 =====
# Kotlin 协程生成 SuspendLambda/ContinuationImpl 子类，内部使用 final 字段保存状态机。
# R8 激进优化（尤其是-repackageclasses+多轮优化）会错误地修改这些字段的访问标志，
# 导致 "Final field cannot be written to" IllegalAccessError 崩溃。
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# 保留所有协程 continuation/suspend lambda 类，禁止修改字段访问标志
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.coroutines.jvm.internal.**
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.coroutines.**

# 关键修复：保留所有 SuspendLambda 子类的字段访问权限（不要改变 final 标志）
# 使用 allowoptimization 允许代码优化但禁止访问标志修改
-keepclassmembers,allowoptimization,allowobfuscation class * extends kotlin.coroutines.jvm.internal.SuspendLambda {
    <fields>;
}
-keepclassmembers,allowoptimization,allowobfuscation class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    <fields>;
}
-keepclassmembers,allowoptimization,allowobfuscation class * extends kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <fields>;
}
-keepclassmembers,allowoptimization,allowobfuscation class * implements kotlin.coroutines.Continuation {
    <fields>;
}

# 禁止修改访问权限（不声明 -allowaccessmodification 即默认禁用，防止R8修改final字段访问标志）
# 关键修复：保留所有 SuspendLambda 子类的字段访问权限（不要改变 final 标志）

# ===== 保留规则 =====

# Hilt —— 仅保留 R8 Full Mode 无法自动处理的最小集合
# R8 Full Mode 已内置 Dagger/Hilt 优化，无需全量保留
-keep class dagger.hilt.android.internal.managers.* { *; }
-keep class dagger.hilt.internal.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.http.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Compose —— R8 Full Mode 已内置 Compose 优化，仅保留反射必需的类
# 移除全量 keep runtime/ui，改为最小化保留
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.snapshots.** { *; }
-dontwarn androidx.compose.**

# Compose snapshot 锁验证修复 — 防止 R8 Full Mode 剥离验证信息导致性能降级
-keep class androidx.compose.runtime.snapshots.SnapshotStateList {
    *** conditionalUpdate(...);
    *** conditionalUpdate$default(...);
    *** mutate(...);
    *** update(...);
    *** update$default(...);
}

# ===== Native 方法保护（tree-sitter / oniguruma 等 JNI 库必需） =====
# R8 默认可能重命名或移除 native 方法，导致 UnsatisfiedLinkError。
# 保留所有含 native 方法的类及其 native 方法名，确保 JNI 注册能找到对应方法。
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== LGPL-2.1 合规注记 =====
# sora-editor / language-textmate / language-treesitter（io.github.rosemoe.*，LGPL-2.1）与
# android-tree-sitter / tree-sitter-java（com.itsaky.androidide.treesitter.*，LGPL-2.1）为
# LGPL-2.1 许可组件。以下 -keep 规则保留其类名与成员、禁止重命名与裁剪，依据为
# LGPL-2.1 第 6 条：再分发方须允许用户以修改后的库版本替换随包版本并重新链接。
# 请勿删除或弱化以下 keep 规则；若调整混淆策略，须保证上述包名与成员可被替换重新链接。
# 详见 docs/开源合规说明.md 与 THIRD_PARTY_NOTICES.md 第 5 节。
# ===== END LGPL-2.1 合规注记 =====

# ===== sora-editor (code editor) =====
# 正确包名 io.github.rosemoe.sora，旧包名 com.blacksquircle.ui 仅为历史引用。
# sora-editor 内部使用：
#   - 反射：ColorScheme、Language 工厂、diagnostic 类通过 Class 引用访问
#   - 匿名内部类：IGrammarSource/IThemeSource 的匿名实现（fromFile/fromInputStream/fromResource）
#   - 序列化模型：ThemeModel/GrammarDefinition/DefaultGrammarDefinition 等被持久化引用
#   - DirectAccessProps：编辑器属性类，所有字段必须保持非 final 状态，应用代码和库内部代码
#     都会直接写入这些字段（如 editor.props.overrideSymbolPairs、editor.props.stickyScroll 等）。
#     R8 优化（包括 field/marking/final）可能错误地将非 final 字段标记为 final，
#     导致运行时 IllegalAccessError: Final field cannot be written to，进而破坏编辑器状态
#     （内容在显示一瞬间后消失）。
# 必须保留所有类和成员，禁止任何优化、混淆、访问标志修改，否则语法高亮、主题加载、属性设置会崩溃。
# 包覆盖范围：
#   io.github.rosemoe.sora.**        - 编辑器核心（widget, lang, text, event, graphics, util, annotations）
#   io.github.rosemoe.sora.langs.**  - 内置语言支持（textmate, java 等）
#   io.github.rosemoe.sora.editor.** - TreeSitter 语言支持（ts 子包）
#   io.github.rosemoe.oniguruma.**   - Oniguruma 原生正则绑定（可选组件，dontwarn 处理缺失情况）
-keep class io.github.rosemoe.** { *; }
-keepclassmembers class io.github.rosemoe.** {
    <init>(...);
    <fields>;
    <methods>;
}
# 特别保护 DirectAccessProps 及其相关类，所有 props 字段必须保持可写状态
-keep class io.github.rosemoe.sora.widget.DirectAccessProps { *; }
-keepclassmembers class io.github.rosemoe.sora.widget.DirectAccessProps {
    <init>(...);
    <fields>;
    <methods>;
}
# 保护 DirectAccessProps 的字段不要被移除、重命名或修改访问标志
-keepclassmembers,includedescriptorclasses class io.github.rosemoe.sora.widget.DirectAccessProps {
    <fields>;
}
-dontwarn io.github.rosemoe.**
-dontwarn io.github.rosemoe.oniguruma.OnigNative

# ===== sora-editor SPI / 反射注册保护 =====
# 虽然 sora-editor 0.24.6 不通过 META-INF/services 注册 IGrammarSource 实现，
# 但 IRegistryOptions 等接口可能被用户代码实现；同时 tm4e 内部通过接口回调
# 访问 grammar/theme provider，需保留接口实现类及其方法签名。
-keep class * implements org.eclipse.tm4e.core.registry.IGrammarSource { *; }
-keep class * implements org.eclipse.tm4e.core.registry.IThemeSource { *; }
-keep class * implements org.eclipse.tm4e.core.registry.IRegistryOptions { *; }
-keep class * implements org.eclipse.tm4e.core.internal.registry.IGrammarRepository { *; }
-keep class * implements org.eclipse.tm4e.core.internal.registry.IThemeProvider { *; }
-keep class * implements io.github.rosemoe.sora.lang.Language { *; }
-keep class * extends io.github.rosemoe.sora.widget.schemes.EditorColorScheme { *; }

# ===== TextMate 语法高亮引擎 (tm4e) =====
# org.eclipse.tm4e 内部包含大量 internal 包：
#   core/internal/grammar (raw, dependencies, matcher, oniguruma, parser, rule, theme, utils)
#   core/internal/registry (SyncRegistry, IThemeProvider, IGrammarRepository)
#   languageconfiguration/internal (model, supports, utils)
# 这些包被 Oniguruma 正则引擎、RawGrammarReader/RawThemeReader 反射调用，
# 模型类（AutoClosingPair, CommentRule, LanguageConfiguration 等）被 JSON 反序列化构造。
# 通配符 org.eclipse.tm4e.** 已覆盖上述所有包。
-keep class org.eclipse.tm4e.** { *; }
-keepclassmembers class org.eclipse.tm4e.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn org.eclipse.tm4e.**

# ===== Joni / JCodings (tm4e 使用的 Oniguruma 正则 Java 移植版) =====
# joni 是 org.eclipse.tm4e.core.internal.oniguruma.impl.joni 包的底层正则引擎，
# 内部大量使用 AST 节点反射和字符编码查找。sora-editor 官方 consumer-rules 已特别
# 保留 org.joni.ast.QuantifierNode，但 R8 full/standard mode 仍可能错误剥离
# 其他 AST 节点（NameNode, ListNode, StringNode 等）和编码类。
-keep class org.joni.** { *; }
-keepclassmembers class org.joni.** {
    <init>(...);
    <fields>;
    <methods>;
}
-keep class org.jcodings.** { *; }
-keepclassmembers class org.jcodings.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn org.joni.**
-dontwarn org.jcodings.**

# 本段 tree-sitter 相关 keep 规则同属 LGPL-2.1 可重链接义务的一部分（见上方 LGPL-2.1 合规注记）。
# ===== tree-sitter 语法解析 (sora-language-treesitter + android-tree-sitter) =====
# com.itsaky.androidide.treesitter 是 android-tree-sitter 原生绑定层，
# 通过 JNI 调用 C 库 libtree-sitter.so，所有类均含 native 方法。
# 包结构：
#   com.itsaky.androidide.treesitter.**       - 核心绑定（TSLanguage, TSTree, TSParser, TSQuery 等）
#   com.itsaky.androidide.treesitter.java.**  - Java 语言预编译 grammar
-keep class com.itsaky.androidide.treesitter.** { *; }
-keepclassmembers class com.itsaky.androidide.treesitter.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn com.itsaky.androidide.treesitter.**

# io.github.treesitter.** 为 tree-sitter 官方 Java 绑定（可能作为传递依赖引入），
# 同样使用 JNI，保留以防类找不到。
-keep class io.github.treesitter.** { *; }
-keepclassmembers class io.github.treesitter.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn io.github.treesitter.**

# ===== 旧包名兼容规则 =====
# com.blacksquircle.ui 是 sora-editor 0.x 早期版本的旧包名（fork 自 Squircle-CE），
# 0.22+ 已全面迁移至 io.github.rosemoe.sora。保留此规则防止历史引用崩溃。
-keep class com.blacksquircle.ui.** { *; }
-keep class com.blacksquircle.ui.editorextensions.** { *; }
-dontwarn com.blacksquircle.**

# WebView JavaScript Interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# DataStore
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ===== Apache POI (Office document parsing) =====
# POI 大量使用 Class.newInstance()、ServiceLoader、SPI 反射机制
# R8 Full Mode + overloadaggressively 会合并方法签名并删除构造器，
# 导致 InstantiationException / NoSuchMethodException。
# 必须保留所有构造器和类定义，禁止优化。

# 核心 POI 包 —— 完整保留（类名 + 构造器 + 所有成员）
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }
-keep class com.microsoft.schemas.** { *; }

# 显式保留所有构造器 —— Class.newInstance() 依赖无参构造器
-keepclassmembers class org.apache.poi.** {
    <init>(...);
}
-keepclassmembers class org.apache.xmlbeans.** {
    <init>(...);
}
-keepclassmembers class org.openxmlformats.schemas.** {
    <init>(...);
}
-keepclassmembers class com.microsoft.schemas.** {
    <init>(...);
}

# XMLBeans 生成的 XmlObject 子类 —— 通过 parse()/newInstance() 反射创建
-keepclassmembers class * extends org.apache.xmlbeans.XmlObject {
    public static ** parse(...);
    public static ** newInstance(...);
    public ** get*(...);
    public void set*(...);
    <init>(...);
}

# POI ServiceLoader / SPI —— 保留 META-INF 配置引用的类
-keep class org.apache.poi.ss.usermodel.WorkbookFactory { *; }
-keep class org.apache.poi.ss.usermodel.WorkbookProvider { *; }
-keep class org.apache.poi.util.IOUtils { *; }

# poi-scratchpad (legacy OLE2: HWPF .doc / HSLF .ppt) —— 同样走 Class.newInstance() 反射
-keepclassmembers class org.apache.poi.hwpf.** { <init>(...); }
-keepclassmembers class org.apache.poi.hslf.** { <init>(...); }
-keep class org.apache.poi.hwpf.HWPFDocument { *; }
-keep class org.apache.poi.hslf.usermodel.HSLFSlideShow { *; }

# 抑制所有 POI 相关的编译期警告
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.**
-dontwarn org.apache.xmlbeans.xml.stream.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.logging.slf4j.**

# ===== JGit (pure Java Git implementation) =====
# JGit 使用 getDeclaredConstructor() 反射实例化内部类（如 SSH transport、SessionFactory）。
# R8 Full Mode 删除无参构造器导致 NoSuchMethodException: hb0.<init> [] 崩溃。
# 必须完整保留所有类和构造器。
-keep class org.eclipse.jgit.** { *; }
-keepclassmembers class org.eclipse.jgit.** {
    <init>(...);
}
# JGit 通过 Class.forName() 加载的内部服务实现
-keep class org.eclipse.jgit.internal.JGitText { *; }
-keep class org.eclipse.jgit.lib.*Store* { *; }
-keep class org.eclipse.jgit.transport.* { *; }
-keep class org.eclipse.jgit.ssh.apache.* { *; }

-dontwarn org.eclipse.jgit.**
-dontwarn java.lang.ProcessHandle
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-dontwarn java.awt.**
-dontwarn com.graphbuilder.curve.**

# ===== OkHttp (network requests for GitHub API) =====
# OkHttp 4.x is fully compatible with R8 standard mode.
# Only keep the platform-specific classes that use reflection to load
# Android platform support and the logging interceptor.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Keep OkHttp platform classes that use reflection to detect TLS support
-keep class okhttp3.internal.platform.Android10Platform { *; }
-keep class okhttp3.internal.platform.AndroidPlatform { *; }
# Keep okio's ByteString (used in serialization)
-keep class okio.ByteString { *; }

# ===== Log4j (transitive from JGit/POI) =====
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.impl.**
-dontwarn aQute.bnd.annotation.**
-dontwarn org.osgi.framework.**

# ===== Compose Rich Editor (richeditor-compose + ksoup) =====
# richeditor-compose uses ksoup for HTML parsing and org.jetbrains.markdown for
# Markdown parsing. Keep public API to avoid reflection issues in standard R8 mode.
-keep class com.mohamedrejeb.richeditor.** { public *; }
-keep class com.mohamedrejeb.ksoup.** { public *; }
-dontwarn com.mohamedrejeb.ksoup.**

# ===== jsoup HTML sanitizer (VULN-004) =====
# jsoup uses reflection for some parser features. Keep public API.
-keep class org.jsoup.** { public *; }
-keepclassmembers class org.jsoup.** {
    public <init>(...);
    public <methods>;
}
-dontwarn org.jsoup.**

# ===== Google Play Integrity API (VULN-009) =====
# Play Integrity uses Google Play services, keep its API classes.
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.google.android.play.core.** { public *; }
-dontwarn com.google.android.play.core.**

# ===== Native Security Library (VULN-005) =====
# JNI methods in NativeSecurityChecker are already covered by the security class keep rules
# (com.draftpeek.security.** is kept with allowobfuscation).
# Ensure native method names are preserved for JNI registration.
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== 输出映射文件 =====
-printmapping mapping.txt

# ===== 混淆字典 =====
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# ===== 安全校验类（VULN-003: 允许类名混淆，保留方法签名供内部反射） =====
# SECURITY VULN-003: 安全类使用 allowobfuscation 允许类名被混淆，
# 使 Frida 无法通过类名（如 "com.draftpeek.security.AntiDebug"）定位安全检测代码。
# 方法名和字段名保留不变，因为 SecurityIntegrityChecker 通过反射验证方法签名。
# 枚举常量名（SAFE, SUSPICIOUS, HOSTILE）也保留，供枚举完整性验证使用。
# allowobfuscation 仅允许类名重命名，不影响成员保留。
-keep,allowobfuscation class com.draftpeek.security.** { *; }
-keepclassmembers class com.draftpeek.security.** {
    <init>(...);
    <fields>;
    <methods>;
}
-keep,allowobfuscation class com.draftpeek.core.common.security.SecurityGate { *; }
-keepclassmembers class com.draftpeek.core.common.security.SecurityGate {
    <init>(...);
    <fields>;
    <methods>;
}

# ===== juniversalchardet (encoding detection) =====
# juniversalchardet uses internal prober classes loaded by name for charset detection.
# Keep the core detector and prober classes to ensure encoding detection works after obfuscation.
-keep class org.mozilla.universalchardet.** { *; }
-keepclassmembers class org.mozilla.universalchardet.** {
    <init>(...);
}
-dontwarn org.mozilla.universalchardet.**

# ===== SQLCipher (net.zetetic:sqlcipher-android) =====
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# ===== java-diff-utils (P0: diff library) =====
# java-diff-utils uses reflection for Delta type instantiation
-keep class com.github.difflib.** { *; }
-keepclassmembers class com.github.difflib.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn com.github.difflib.**

# ===== Eclipse LSP4J (P1: LSP protocol layer) =====
# LSP4J uses Gson reflection for JSON-RPC serialization
-keep class org.eclipse.lsp4j.** { *; }
-keepclassmembers class org.eclipse.lsp4j.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn org.eclipse.lsp4j.**

# ===== PF4J (P1: Plugin framework) =====
# PF4J uses Class.forName() and reflection for plugin loading
-keep class org.pf4j.** { *; }
-keepclassmembers class org.pf4j.** {
    <init>(...);
    <fields>;
    <methods>;
}
-dontwarn org.pf4j.**

# ===== AI 对抗安全防护类（A+B 第二阶段） =====
# AI 检测/响应/威慑类允许混淆类名但保留方法签名
-keep,allowobfuscation class com.draftpeek.security.AiDetector { *; }
-keep,allowobfuscation class com.draftpeek.security.AiDetector$* { *; }
-keep,allowobfuscation class com.draftpeek.security.AiProtectionStateHolder { *; }
-keep,allowobfuscation class com.draftpeek.security.AiResponseExecutor { *; }
-keep,allowobfuscation class com.draftpeek.security.LegalDeterrence { *; }
-keep,allowobfuscation class com.draftpeek.security.LegalDeterrence$* { *; }
-keep,allowobfuscation class com.draftpeek.DraftPeekAppGlobals { *; }

# core/common AI 安全类
-keep,allowobfuscation class com.draftpeek.core.common.security.AiProtectionState { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.AiProtectionState$* { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.AiThreatLevel { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.AiDetectionSignal { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.AiEthicalNotice { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.SecurityEventRecorder { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.SecureStringResolver { *; }
-keep,allowobfuscation class com.draftpeek.core.common.security.SecureStringResolver$EncryptedString { *; }

# SecurityEvent 实体（Room 反射需要）
-keep class com.draftpeek.core.data.entity.SecurityEventEntity { *; }

# 虚假类包 — 不 keep（让 R8 完全混淆，增加逆向难度）
# 但也不让 R8 删除（虚假类需存在于 APK 中以迷惑 AI）
-keep,allowobfuscation class com.draftpeek.security.decoy.** { *; }

# ===== [REMOVED] Jetpack Security (security-crypto) =====
# SECURITY VULN-016: security-crypto (1.1.0-alpha06) fully removed.
# Replaced by SecureFileStorage + SecurePreferences (self-built Keystore AES-256-GCM).
# Keep rules no longer needed. Do NOT re-add.
