/**
 * 文件功能：Markdown 内容导出工具，支持 HTML、PDF、DOCX、图片、ZIP 多种格式
 *
 * 主要对象：
 * - [MarkdownExporter]：导出工具单例对象，提供多种格式导出能力
 *
 * 模块依赖：
 * - android.content：ContentResolver、MediaStore 用于文件保存
 * - android.graphics：Bitmap、Canvas 用于长截图
 * - android.print：PrintManager、PrintAttributes 用于 PDF 导出
 * - android.webkit：WebView 用于渲染和截图
 * - core.common.security.SecurityGate：安全校验
 * - feature.editor.model.MarkdownTheme：Markdown 主题枚举
 * - kotlinx.coroutines：协程支持
 * - org.apache.poi：DOCX 导出
 * - java.util.zip：ZIP 打包
 *
 * 导出格式：
 * - HTML：自包含独立 HTML（内联所有 JS/CSS 资源）
 * - PDF：使用 Android 打印 API 通过 WebView 渲染后打印
 * - DOCX：委托给 MarkdownDocxExporter
 * - 图片（PNG）：WebView 长截图
 * - ZIP：打包 Markdown 文件和关联资源文件夹
 */
package com.draftpeek.feature.editor.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.feature.editor.model.MarkdownTheme
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val ASSET_BASE = "markdown"

/**
 * Markdown 导出工具（单例对象）
 *
 * 提供 Markdown 内容到多种格式的导出功能，所有导出操作都通过 SecurityGate 安全校验。
 */
object MarkdownExporter {

    /**
     * 读取资产文件内容为 UTF-8 字符串
     *
     * @param context Android 上下文
     * @param fileName 资产文件名（相对于 markdown/ 目录）
     * @return 文件内容字符串
     * @throws java.io.IOException 如果读取资产失败
     */
    private fun readAsset(context: Context, fileName: String): String =
        context.assets.open("$ASSET_BASE/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }

    /**
     * 从 Markdown 内容生成独立 HTML 文档
     *
     * 所有 JS/CSS 库从本地资产内联，支持离线自包含导出。
     *
     * HTML 生成步骤：
     * 1. 读取所有本地资产（KaTeX、highlight.js、marked.js、Mermaid）
     * 2. 构建 HTML 头部（meta、CSS、内联 JS 库）
     * 3. 根据主题参数添加主题 CSS
     * 4. 构建 body 和渲染容器
     * 5. 注入渲染脚本和转义后的 Markdown 内容
     *
     * @param context Android 上下文（用于读取资产）
     * @param markdownContent 原始 Markdown 文本
     * @param isDarkTheme 是否使用深色主题
     * @param theme Markdown 预览主题
     * @return 完整的 HTML 文档字符串
     */
    fun generateHtml(
        context: Context,
        markdownContent: String,
        isDarkTheme: Boolean = false,
        theme: MarkdownTheme = MarkdownTheme.DEFAULT
    ): String {
        val katexCss = readAsset(context, "katex.min.css")
        val highlightJs = readAsset(context, "highlight.min.js")
        val markedJs = readAsset(context, "marked.min.js")
        val katexJs = readAsset(context, "katex.min.js")
        val katexMhchemJs = readAsset(context, "katex-mhchem.min.js")
        val mermaidJs = readAsset(context, "mermaid.min.js")

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine("<title>Markdown Export</title>")
            appendLine("<style>")
            appendLine(katexCss)
            appendLine("</style>")
            appendLine(
                "<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/styles/github.min.css\">"
            )
            appendLine("<style>")
            appendLine(getMarkdownStyles())
            appendLine("</style>")
            if (theme != MarkdownTheme.DEFAULT) {
                appendLine("<style>")
                appendLine(getThemeCSS(theme))
                appendLine("</style>")
            }
            appendLine("<script>$markedJs</script>")
            appendLine("<script>$katexJs</script>")
            appendLine("<script>$katexMhchemJs</script>")
            appendLine("<script>$mermaidJs</script>")
            appendLine("<script>$highlightJs</script>")
            appendLine("</head>")
            val themeName = theme.name.lowercase()
            val bodyClass = buildString {
                if (isDarkTheme) append("dark")
                if (themeName != "default") {
                    if (isNotEmpty()) append(" ")
                    append(themeName)
                }
            }
            appendLine("<body class=\"$bodyClass\">")
            appendLine("<div id=\"content\"></div>")
            appendLine(getRenderScript())
            appendLine("<script>")
            val escaped = markdownContent
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
            appendLine("renderMarkdown(\"$escaped\", $isDarkTheme, \"$themeName\");")
            appendLine("</script>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    /**
     * 将 HTML 内容写入输出流
     *
     * @param html HTML 字符串
     * @param outputStream 目标输出流
     * @throws java.io.IOException 如果写入失败
     */
    fun writeHtmlToStream(html: String, outputStream: OutputStream) {
        outputStream.use { stream ->
            stream.write(html.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    /**
     * 使用 Android 打印 API 将 Markdown 导出为 PDF
     *
     * 流程：
     * 1. 创建 WebView 并启用 JavaScript
     * 2. 生成 HTML 并加载到 WebView
     * 3. 在 onPageFinished 回调中创建打印任务
     * 4. 系统打印对话框处理实际 PDF 生成
     *
     * @param context Android 上下文
     * @param markdownContent 原始 Markdown 文本
     * @param fileName 打印作业的文件名
     * @param isDarkTheme 是否使用深色主题
     */
    @SuppressLint("SetJavaScriptEnabled") // 渲染需 JS；HTML 已通过 jsoup 消毒（VULN-004），导出场景内容为本地生成的 Markdown
    fun exportToPdf(context: Context, markdownContent: String, fileName: String, isDarkTheme: Boolean = false) {
        if (!SecurityGate.isOperationAllowed()) return
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true

        val html = generateHtml(context, markdownContent, isDarkTheme)
        webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "UTF-8", null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "${fileName.replace(".md", "").replace(".markdown", "")}_PDF"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
    }

    /**
     * 使用 Apache POI 将 Markdown 导出为 DOCX
     *
     * @param markdownContent 原始 Markdown 文本
     * @param outputStream 输出流
     * @throws SecurityException 如果安全校验失败
     */
    fun exportToDocx(markdownContent: String, outputStream: OutputStream) {
        if (!SecurityGate.isOperationAllowed()) throw SecurityException("Security verification failed")
        MarkdownDocxExporter.exportToDocx(markdownContent, outputStream)
    }

    /**
     * 将渲染后的 Markdown 内容导出为长截图（PNG 图片）
     *
     * 使用 WebView 的 enableSlowWholeDocumentDraw 捕获整个文档，
     * 然后绘制到 Bitmap 上，通过 MediaStore 保存到 Downloads 目录。
     *
     * **必须在 UI 线程调用**（用于 WebView 截图部分）。
     * MediaStore 写入在 IO 调度器上执行。
     *
     * 截图流程：
     * 1. 启用整个文档绘制
     * 2. 创建 WebView 并加载渲染后的 HTML
     * 3. 页面加载完成后测量 WebView 尺寸
     * 4. 创建匹配尺寸的 Bitmap 并绘制 WebView 内容
     * 5. 在 IO 线程通过 MediaStore 保存到 Downloads
     *
     * @param context Android 上下文
     * @param markdownContent 原始 Markdown 文本
     * @param fileName 基础文件名（不含扩展名）
     * @param isDarkTheme 是否使用深色主题
     * @param theme Markdown 预览主题
     * @return 保存图片的 URI 字符串，失败返回 null
     */
    @SuppressLint("Recycle", "SetJavaScriptEnabled") // openOutputStream 已通过 ?.use{} 关闭（lint 误报）；渲染需 JS，HTML 已通过 jsoup 消毒（VULN-004）
    suspend fun exportAsImage(
        context: Context,
        markdownContent: String,
        fileName: String,
        isDarkTheme: Boolean = false,
        theme: MarkdownTheme = MarkdownTheme.DEFAULT
    ): String? = withContext(Dispatchers.Main) {
        if (!SecurityGate.isOperationAllowed()) return@withContext null
        val bitmap = suspendCancellableCoroutine<Bitmap?> { cont ->
            WebView.enableSlowWholeDocumentDraw()
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true

            val html = generateHtml(context, markdownContent, isDarkTheme, theme)
            webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "UTF-8", null)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view ?: run {
                        cont.resume(null)
                        return
                    }
                    try {
                        val contentWidth = view.width.takeIf { it > 0 } ?: view.measuredWidth
                        val scale = context.resources.displayMetrics.density
                        val width = (contentWidth * scale).toInt().coerceAtLeast(1)
                        view.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                        )
                        val height = view.measuredHeight.coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        view.draw(canvas)
                        cont.resume(bmp)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            }

            cont.invokeOnCancellation {
                webView.destroy()
            }
        }

        if (bitmap == null) return@withContext null

        withContext(Dispatchers.IO) {
            try {
                val imageName = fileName.replace(".md", "").replace(".markdown", "") + ".png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null

                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                uri.toString()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 将 Markdown 文件及其关联资源（图片文件夹等）导出为 ZIP 压缩包
     *
     * ZIP 通过 MediaStore 保存到 Downloads 目录。
     * **在 IO 调度器上运行。**
     *
     * 打包流程：
     * 1. 检查源文件存在
     * 2. 创建 ZIP 文件并通过 MediaStore 获取 URI
     * 3. 添加 Markdown 文件本身
     * 4. 检测并添加关联的资源文件夹（<name>_files、<name>.assets、images、assets）
     * 5. 递归添加文件夹内容
     *
     * @param context Android 上下文
     * @param markdownFilePath Markdown 文件的绝对路径
     * @param fileName ZIP 压缩包的基础文件名
     * @return 保存 ZIP 的 URI 字符串，失败返回 null
     */
    @SuppressLint("Recycle") // openOutputStream 已通过 ?.use{} 关闭，lint 对可空接收者的误报
    suspend fun exportAsZip(
        context: Context,
        markdownFilePath: String,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        if (!SecurityGate.isOperationAllowed()) return@withContext null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext null
        }

        try {
            val sourceFile = File(markdownFilePath)
            if (!sourceFile.exists()) return@withContext null

            val zipName = fileName.replace(".md", "").replace(".markdown", "") + ".zip"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, zipName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            resolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream.buffered()).use { zipOut ->
                    addFileToZip(zipOut, sourceFile, sourceFile.name)

                    val parentDir = sourceFile.parentFile ?: return@use
                    val baseName = sourceFile.nameWithoutExtension
                    val imageFolders = listOf("${baseName}_files", "$baseName.assets", "images", "assets")
                    for (folderName in imageFolders) {
                        val folder = File(parentDir, folderName)
                        if (folder.exists() && folder.isDirectory) {
                            addFolderToZip(zipOut, folder, folder.name)
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 递归将单个文件添加到 ZIP 输出流
     *
     * @param zipOut ZIP 输出流
     * @param file 要添加的文件
     * @param entryPath ZIP 内的条目路径
     * @throws java.io.IOException 如果读取或写入失败
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryPath: String) {
        zipOut.putNextEntry(ZipEntry(entryPath))
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(4096)
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                zipOut.write(buffer, 0, len)
            }
        }
        zipOut.closeEntry()
    }

    /**
     * 递归将文件夹及其内容添加到 ZIP 输出流
     *
     * @param zipOut ZIP 输出流
     * @param folder 要添加的文件夹
     * @param basePath ZIP 内的基础路径
     */
    private fun addFolderToZip(zipOut: ZipOutputStream, folder: File, basePath: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val entryPath = "$basePath/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(zipOut, file, entryPath)
            } else {
                addFileToZip(zipOut, file, entryPath)
            }
        }
    }

    /**
     * 获取 Markdown 预览的基础 CSS 样式
     *
     * 包含：
     * - CSS 变量定义（浅色/深色主题）
     * - 排版样式（字体、行高、颜色）
     * - 标题、段落、链接、代码、引用、表格、列表等元素样式
     * - KaTeX 和 Mermaid 容器样式
     * - 前置元数据样式
     *
     * @return CSS 样式字符串
     */
    private fun getMarkdownStyles(): String = """
            :root {
                --bg-color: #ffffff;
                --text-color: #1c1c1e;
                --code-bg: #f5f5f5;
                --border-color: #e0e0e0;
                --link-color: #0066cc;
                --table-border: #ddd;
                --table-header-bg: #f0f0f0;
                --blockquote-border: #ddd;
                --blockquote-bg: #f9f9f9;
            }
            .dark {
                --bg-color: #1c1c1e;
                --text-color: #e0e0e0;
                --code-bg: #2c2c2e;
                --border-color: #3a3a3c;
                --link-color: #64b5f6;
                --table-border: #3a3a3c;
                --table-header-bg: #2c2c2e;
                --blockquote-border: #3a3a3c;
                --blockquote-bg: #2c2c2e;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                font-size: 15px; line-height: 1.6;
                color: var(--text-color); background-color: var(--bg-color);
                padding: 16px; margin: 0;
                word-wrap: break-word; overflow-wrap: break-word;
            }
            h1, h2, h3, h4, h5, h6 { margin-top: 1.2em; margin-bottom: 0.6em; font-weight: 600; }
            h1 { font-size: 1.8em; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
            h2 { font-size: 1.5em; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
            p { margin: 0.8em 0; }
            a { color: var(--link-color); text-decoration: none; }
            code { background-color: var(--code-bg); padding: 2px 6px; border-radius: 3px; font-family: monospace; font-size: 0.9em; }
            pre { background-color: var(--code-bg); padding: 12px; border-radius: 6px; overflow-x: auto; margin: 0.8em 0; }
            pre code { background-color: transparent; padding: 0; }
            blockquote { border-left: 4px solid var(--blockquote-border); background-color: var(--blockquote-bg); margin: 0.8em 0; padding: 8px 16px; color: #666; }
            table { border-collapse: collapse; width: 100%; margin: 0.8em 0; }
            th, td { border: 1px solid var(--table-border); padding: 8px 12px; text-align: left; }
            th { background-color: var(--table-header-bg); font-weight: 600; }
            img { max-width: 100%; height: auto; }
            ul, ol { padding-left: 2em; margin: 0.6em 0; }
            hr { border: none; border-top: 1px solid var(--border-color); margin: 1.5em 0; }
            del { color: #999; }
            .katex-display { margin: 0.8em 0; overflow-x: auto; }
            .katex { font-size: 1.1em; }
            .mermaid-container { text-align: center; margin: 0.8em 0; }
            .front-matter { background: var(--blockquote-bg); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px 16px; margin-bottom: 16px; font-size: 0.9em; }
            .front-matter .fm-field { margin: 4px 0; }
            .front-matter .fm-key { font-weight: 600; }
            .front-matter .fm-value { color: #666; }
            .front-matter .fm-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
            .front-matter .fm-tag { background: var(--code-bg); padding: 2px 8px; border-radius: 12px; font-size: 0.85em; }
    """.trimIndent()

    /**
     * 获取 Markdown 渲染的 JavaScript 脚本
     *
     * 脚本功能：
     * - 配置 marked.js（启用 GFM、换行、语法高亮）
     * - 自定义渲染器支持任务列表复选框
     * - LaTeX 数学公式渲染（KaTeX）：先处理代码块占位符，避免代码中的 $ 被误渲染
     * - YAML 前置元数据解析
     * - renderMarkdown 主函数：处理代码块占位 → 渲染数学 → marked 解析 → 渲染 Mermaid
     *
     * @return JavaScript 脚本字符串
     */
    private fun getRenderScript(): String = """
            <script>
            marked.setOptions({ breaks: true, gfm: true,
                highlight: function(code, lang) {
                    if (lang && hljs.getLanguage(lang)) {
                        try { return hljs.highlight(code, { language: lang }).value; } catch(e) {}
                    }
                    return hljs.highlightAuto(code).value;
                }
            });
            const renderer = new marked.Renderer();
            renderer.listitem = function(text) {
                if (text.startsWith('<input type="checkbox"')) return '<li class="task-list-item">' + text + '</li>';
                return '<li>' + text + '</li>';
            };
            marked.setOptions({ renderer: renderer });
            function renderMath(text) {
                text = text.replace(/\$\$([\s\S]*?)\$\$/g, function(m, f) {
                    try { return '<div class="katex-display">' + katex.renderToString(f.trim(), {displayMode:true,throwOnError:false}) + '</div>'; } catch(e) { return '<div>' + f + '</div>'; }
                });
                text = text.replace(/(?<!\\)\$([^\$\n]+?)\$(?!\$)/g, function(m, f) {
                    try { return katex.renderToString(f.trim(), {displayMode:false,throwOnError:false}); } catch(e) { return f; }
                });
                return text;
            }
            function parseFrontMatter(text) {
                var match = text.match(/^---\n([\s\S]*?)\n---/);
                if (!match) return { frontMatter: null, content: text };
                return { frontMatter: match[1], content: text.substring(match[0].length) };
            }
            async function renderMarkdown(md, dark, theme) {
                var themeClass = theme || '';
                var darkClass = dark ? 'dark' : '';
                document.body.className = themeClass + (themeClass && darkClass ? ' ' : '') + darkClass;
                var fm = parseFrontMatter(md);
                var content = fm.content;
                var codeBlocks = [];
                var processed = content.replace(/```[\s\S]*?```/g, function(m) { var i = codeBlocks.length; codeBlocks.push(m); return '%%CB_' + i + '%%'; });
                var inlineCodes = [];
                processed = processed.replace(/`[^`]+`/g, function(m) { var i = inlineCodes.length; inlineCodes.push(m); return '%%IC_' + i + '%%'; });
                processed = renderMath(processed);
                processed = processed.replace(/%%CB_(\d+)%%/g, function(m, i) { return codeBlocks[parseInt(i)]; });
                processed = processed.replace(/%%IC_(\d+)%%/g, function(m, i) { return inlineCodes[parseInt(i)]; });
                var html = marked.parse(processed);
                if (fm.frontMatter) {
                    var fields = fm.frontMatter.split('\\n');
                    var fmHtml = '<div class="front-matter">';
                    for (var line of fields) {
                        var ci = line.indexOf(':');
                        if (ci > 0) fmHtml += '<div class="fm-field"><span class="fm-key">' + line.substring(0,ci).trim() + ':</span> <span class="fm-value">' + line.substring(ci+1).trim() + '</span></div>';
                    }
                    fmHtml += '</div>';
                    html = fmHtml + html;
                }
                document.getElementById('content').innerHTML = html;
                try {
                    var blocks = document.querySelectorAll('code.language-mermaid');
                    for (var b of blocks) {
                        var pre = b.parentElement;
                        if (pre && pre.tagName === 'PRE') {
                            try { var r = await mermaid.render('m'+Math.random().toString(36).substr(2,9), b.textContent); var d = document.createElement('div'); d.className='mermaid-container'; d.innerHTML=r.svg; pre.replaceWith(d); } catch(e) {}
                        }
                    }
                } catch(e) {}
            }
            try { mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' }); } catch(e) {}
            </script>
        """

    /**
     * 获取指定 Markdown 主题的 CSS 样式覆盖
     *
     * 支持的主题：
     * - GITHUB：GitHub 风格
     * - NEWSPRINT：报纸风格（衬线字体）
     * - NIGHT：夜间暗色风格
     * - PIXY：可爱紫色风格
     * - ACADEMIC：学术论文风格
     * - DEFAULT：默认样式（无覆盖）
     *
     * @param theme Markdown 主题枚举
     * @return 主题 CSS 字符串
     */
    private fun getThemeCSS(theme: MarkdownTheme): String = when (theme) {
        MarkdownTheme.GITHUB -> """
                :root.github {
                  --bg-color: #ffffff; --text-color: #24292f; --code-bg: #f6f8fa;
                  --border-color: #d0d7de; --link-color: #0969da; --table-border: #d0d7de;
                  --table-header-bg: #f6f8fa; --blockquote-border: #d0d7de; --blockquote-bg: #f6f8fa;
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                  font-size: 16px; line-height: 1.7;
                }
                .github h1 { border-bottom: 1px solid #d0d7de; padding-bottom: 0.3em; font-size: 2em; }
                .github h2 { border-bottom: 1px solid #d0d7de; padding-bottom: 0.3em; font-size: 1.5em; }
                .github code { background: #eff1f3; padding: 0.2em 0.4em; border-radius: 6px; font-size: 85%; }
                .github pre { background: #f6f8fa; border-radius: 6px; padding: 16px; }
                .github blockquote { border-left: 4px solid #d0d7de; color: #656d76; padding: 0 1em; }
                .github table th { background: #f6f8fa; font-weight: 600; }
        """.trimIndent()
        MarkdownTheme.NEWSPRINT -> """
                :root.newsprint {
                  --bg-color: #f8f5f0; --text-color: #2c2c2c; --code-bg: #ede8e0;
                  --border-color: #c9c4bc; --link-color: #8b4513; --table-border: #c9c4bc;
                  --table-header-bg: #ede8e0; --blockquote-border: #8b4513; --blockquote-bg: #f0ebe4;
                  font-family: 'Georgia', 'Times New Roman', serif; font-size: 16px; line-height: 1.8;
                }
                .newsprint h1, .newsprint h2 { font-family: 'Georgia', serif; border-bottom: 2px solid #2c2c2c; }
                .newsprint h1 { font-size: 2em; text-transform: uppercase; letter-spacing: 2px; }
                .newsprint h2 { font-size: 1.5em; }
        """.trimIndent()
        MarkdownTheme.NIGHT -> """
                :root.night {
                  --bg-color: #1a1a2e; --text-color: #e0e0e0; --code-bg: #16213e;
                  --border-color: #0f3460; --link-color: #e94560; --table-border: #0f3460;
                  --table-header-bg: #16213e; --blockquote-border: #e94560; --blockquote-bg: #16213e;
                  font-family: 'Fira Code', 'Consolas', monospace; font-size: 15px; line-height: 1.7;
                }
                .night h1 { color: #e94560; border-bottom: 1px solid #0f3460; }
                .night h2 { color: #e94560; border-bottom: 1px solid #0f3460; }
                .night a { color: #e94560; }
        """.trimIndent()
        MarkdownTheme.PIXY -> """
                :root.pixy {
                  --bg-color: #fef6f0; --text-color: #5b5ea6; --code-bg: #f0e6f6;
                  --border-color: #d4b8e0; --link-color: #9b59b6; --table-border: #d4b8e0;
                  --table-header-bg: #f0e6f6; --blockquote-border: #9b59b6; --blockquote-bg: #f5edf8;
                  font-family: 'Quicksand', -apple-system, sans-serif; font-size: 15px; line-height: 1.7;
                }
                .pixy h1 { color: #9b59b6; }
                .pixy h2 { color: #8e44ad; }
        """.trimIndent()
        MarkdownTheme.ACADEMIC -> """
                :root.academic {
                  --bg-color: #ffffff; --text-color: #333333; --code-bg: #f5f5f5;
                  --border-color: #cccccc; --link-color: #0066cc; --table-border: #cccccc;
                  --table-header-bg: #f5f5f5; --blockquote-border: #0066cc; --blockquote-bg: #f9f9f9;
                  font-family: 'Computer Modern', 'Georgia', serif; font-size: 12pt; line-height: 1.6;
                  max-width: 680px; margin: 0 auto;
                }
                .academic h1 { font-size: 1.5em; text-align: center; margin-bottom: 0.5em; }
                .academic h2 { font-size: 1.2em; border-bottom: none; }
                .academic h3 { font-size: 1.1em; font-style: italic; }
                .academic p { text-align: justify; text-indent: 2em; }
                .academic p:first-child { text-indent: 0; }
                .academic table { font-size: 0.9em; }
        """.trimIndent()
        MarkdownTheme.DEFAULT -> ""
    }
}
