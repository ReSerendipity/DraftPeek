/**
 * 文件读取操作基准测试。
 *
 * 本模块提供文件读写性能的基准测试，用于衡量以下操作的性能表现：
 * - 小文件读取（小于 1KB）
 * - 中等文件读取（1KB - 100KB）
 * - 大文件读取（100KB - 1MB）
 * - 超大文件读取（大于 1MB，流式模式）
 * - 二进制文件检测
 * - 文件编码检测
 * - 文件写入性能
 * - 内部文件路径判断
 *
 * 参考：core/common 模块中的 PerformanceBenchmark。
 */
package com.draftpeek.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
// measureRepeated 是 BenchmarkRule 的顶层扩展函数（编译产物 BenchmarkRuleKt），
// 必须显式 import；只 import BenchmarkRule 不会带入它。
import androidx.benchmark.junit4.measureRepeated
import com.draftpeek.core.data.repository.EditorFileReadOutcome
import com.draftpeek.core.data.repository.EditorFileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 文件读取操作基准测试类。
 *
 * 使用 Android Benchmark 框架和 Hilt 依赖注入，对 EditorFileRepository 的各项文件操作进行性能基准测试。
 * 测试前会创建各种大小和类型的测试文件，用于模拟真实使用场景。
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FileReadBenchmark {

    /** 基准测试规则，用于性能测量 */
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /** Hilt 依赖注入规则 */
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    /** 文件仓库实例，通过 Hilt 注入 */
    @Inject
    lateinit var repository: EditorFileRepository

    /** 应用上下文 */
    private lateinit var context: android.content.Context

    /** 测试文件目录 */
    private lateinit var testDir: File

    /**
     * 测试前初始化方法。
     *
     * 执行 Hilt 依赖注入，获取应用上下文，创建测试目录并生成测试文件。
     */
    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "benchmark_files")
        testDir.mkdirs()
        createTestFiles()
    }

    /**
     * 创建各种类型的测试文件。
     *
     * 生成以下测试文件：
     * - small.txt：100字节的小文件
     * - medium.txt：10KB的中等文件
     * - large.txt：100KB的大文件
     * - very_large.txt：1MB的超大文件
     * - binary.dat：二进制文件
     * - bom.txt：带UTF-8 BOM标记的文件
     */
    private fun createTestFiles() {
        File(testDir, "small.txt").writeText("A".repeat(100))

        File(testDir, "medium.txt").writeText("B".repeat(10 * 1024))

        File(testDir, "large.txt").writeText("C".repeat(100 * 1024))

        File(testDir, "very_large.txt").writeText("D".repeat(1024 * 1024))

        val binaryFile = File(testDir, "binary.dat")
        binaryFile.writeBytes(ByteArray(1024) { (it % 256).toByte() })

        val bomFile = File(testDir, "bom.txt")
        bomFile.writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "UTF-8 BOM content".toByteArray())
    }

    /**
     * 基准测试：小文件读取性能。
     *
     * 测试读取 100 字节小文件的性能表现。
     */
    @Test
    fun benchmarkSmallFileRead() {
        val file = File(testDir, "small.txt")
        val uri = android.net.Uri.fromFile(file)

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.readFile(uri)
            }
        }
    }

    /**
     * 基准测试：中等文件读取性能。
     *
     * 测试读取 10KB 中等文件的性能表现。
     */
    @Test
    fun benchmarkMediumFileRead() {
        val file = File(testDir, "medium.txt")
        val uri = android.net.Uri.fromFile(file)

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.readFile(uri)
            }
        }
    }

    /**
     * 基准测试：大文件读取性能。
     *
     * 测试读取 100KB 大文件的性能表现。
     */
    @Test
    fun benchmarkLargeFileRead() {
        val file = File(testDir, "large.txt")
        val uri = android.net.Uri.fromFile(file)

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.readFile(uri)
            }
        }
    }

    /**
     * 基准测试：超大文件读取性能。
     *
     * 测试读取 1MB 超大文件的性能表现。
     */
    @Test
    fun benchmarkVeryLargeFileRead() {
        val file = File(testDir, "very_large.txt")
        val uri = android.net.Uri.fromFile(file)

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.readFile(uri)
            }
        }
    }

    /**
     * 基准测试：二进制文件检测性能。
     *
     * 测试读取文件并判断是否为二进制文件的性能表现。
     */
    @Test
    fun benchmarkBinaryFileDetection() {
        val file = File(testDir, "binary.dat")
        val uri = android.net.Uri.fromFile(file)

        benchmarkRule.measureRepeated {
            runBlocking {
                // readFile 返回 sealed EditorFileReadOutcome，字段在 Success.result 下
                (repository.readFile(uri) as? EditorFileReadOutcome.Success)?.result?.isBinaryFile ?: false
            }
        }
    }

    /**
     * 基准测试：文件编码检测性能。
     *
     * 测试读取带 BOM 的文件并检测编码的性能表现。
     */
    @Test
    fun benchmarkEncodingDetection() {
        val file = File(testDir, "bom.txt")
        val uri = android.net.Uri.fromFile(file)

        benchmarkRule.measureRepeated {
            runBlocking {
                (repository.readFile(uri) as? EditorFileReadOutcome.Success)?.result?.detectedEncoding ?: ""
            }
        }
    }

    /**
     * 基准测试：文件写入性能。
     *
     * 测试写入 10KB 内容到文件的性能表现。
     */
    @Test
    fun benchmarkFileWrite() {
        val file = File(testDir, "write_test.txt")
        val uri = android.net.Uri.fromFile(file)
        val content = "E".repeat(10 * 1024)

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.writeFile(uri, content)
            }
        }
    }

    /**
     * 基准测试：内部文件路径判断性能。
     *
     * 测试判断 URI 是否为应用内部文件路径的性能表现，包括内部路径和外部 Content URI 两种情况。
     */
    @Test
    fun benchmarkInternalFileCheck() {
        val internalUri = "file:///data/data/com.draftpeek/files/test.txt"
        val externalUri = "content://com.android.providers.downloads.documents/document/123"

        benchmarkRule.measureRepeated {
            repository.isInternalFile(internalUri)
            repository.isInternalFile(externalUri)
        }
    }
}
