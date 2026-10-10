package com.draftpeek.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.crdt.CrdtDocument
import com.draftpeek.core.crdt.CrdtSnapshotCodec
import com.draftpeek.core.crdt.HybridLogicalClock
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepository
import com.draftpeek.core.sync.SyncSnapshot
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [RoomSnapshotSource] 单测：键前缀、载荷往返、**删除墓碑**（防复活回归）、
 * 同值跳过、远端墓碑落地、缓存损坏降级。
 */
@RunWith(RobolectricTestRunner::class)
class RoomSnapshotSourceTest {

    private lateinit var context: Context
    private val repo = FakeSnippetRepository()
    private val codec = CrdtSnapshotCodec()
    private val remoteClock = HybridLogicalClock("remote-node")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        docFile().delete()
    }

    private fun docFile(): File = File(context.filesDir, "sync/snippet-doc.json")

    private fun newSource(nodeId: String = "local-node") = RoomSnapshotSource(
        snippets = repo,
        nodeIdSource = SyncNodeIdSource { nodeId },
        context = context
    )

    private fun snippet(syncId: String, title: String = "t-$syncId", content: String = "val x = 1") = Snippet(
        id = 0L,
        syncId = syncId,
        title = title,
        content = content,
        language = "kotlin",
        category = "默认",
        createdAt = 1_730_000_000_000,
        updatedAt = 1_730_000_000_000
    )

    /** 以远端节点身份构造快照（模拟协调器交来的 merged）。 */
    private fun snapshotOf(vararg visible: Snippet, tombstoneKeys: List<String> = emptyList()): SyncSnapshot {
        var document = CrdtDocument()
        visible.forEach {
            document = document.put("snippet:${it.syncId}", SnippetPayload.encode(it), remoteClock.next())
        }
        tombstoneKeys.forEach { document = document.remove("snippet:$it", remoteClock.next()) }
        return codec.encode(document)
    }

    @Test
    fun `read 用 snippet 前缀且载荷可往返`() = runTest {
        val a = snippet("sa").copy(id = 1L)
        repo.addSnippet(a)
        val source = newSource()

        val document = codec.decode(source.read())
        assertEquals(setOf("snippet:sa"), document.visibleValues().keys)
        val decoded = SnippetPayload.decode(document.visibleValues().getValue("snippet:sa"), "sa")
        assertEquals(a.title, decoded.title)
        assertEquals(a.content, decoded.content)
    }

    @Test
    fun `本机删除后 read 产生墓碑 —— 不得被远端复活（回归）`() = runTest {
        val source = newSource()
        // 第一轮：远端有 X → 写入本地并落盘缓存文档
        source.write(snapshotOf(snippet("sx")))
        assertEquals(1, repo.getAllSnippets().first().size)

        // 用户在本机删除 X（绕过 source 直改仓库，模拟 UI 删除路径）
        repo.deleteSnippet(repo.getAllSnippets().first().first())

        // 关键断言：read 的文档里 X 必须是墓碑而非「不存在」
        val document = codec.decode(source.read())
        assertTrue("删除未产生墓碑 → 下轮合并会被远端复活", document.tombstones().contains("snippet:sx"))
        assertNull(document.visibleValues()["snippet:sx"])
    }

    @Test
    fun `write 插入新片段 同值跳过 落地远端墓碑`() = runTest {
        val source = newSource()
        val existing = snippet("s1", content = "val a = 1").copy(id = 10L)
        repo.addSnippet(existing)

        source.write(
            snapshotOf(
                snippet("s1", content = "val a = 1"), // 与本地同值 → 不更新
                snippet("s2", content = "val b = 2"), // 本地没有 → 插入
                tombstoneKeys = listOf("s3") // 本地也没有 → 无操作
            )
        )
        assertEquals(0, repo.updateCount)
        assertEquals(2, repo.addCount) // setup 1 次 + write 插入 s2 1 次
        assertEquals(0, repo.deleteCount)
        assertEquals(setOf("s1", "s2"), repo.getAllSnippets().first().map { it.syncId }.toSet())

        // 对已存在的 s1 打墓碑 → 必须删除
        source.write(snapshotOf(tombstoneKeys = listOf("s1")))
        assertEquals(1, repo.deleteCount)
    }

    @Test
    fun `缓存文档损坏时降级为空 不炸同步`() = runTest {
        val source = newSource()
        source.write(snapshotOf(snippet("sx")))
        docFile().writeText("corrupt{{{")

        val document = codec.decode(source.read()) // 不抛异常
        assertTrue(document.tombstones().isEmpty()) // 降级：失去墓碑记忆，但同步继续
    }

    @Test
    fun `空仓库无缓存 read 返回空文档`() = runTest {
        val document = codec.decode(newSource().read())
        assertTrue(document.visibleValues().isEmpty() && document.tombstones().isEmpty())
    }

    @Test
    fun `非 snippet 前缀的键不被本类越界处理`() = runTest {
        val source = newSource()
        // 缓存里混入未知前缀（假设未来版本写入）→ read 不得给它打墓碑、不得解析其载荷
        val polluted = CrdtDocument()
            .put("stat:words", "123", remoteClock.next())
            .let { it.put("snippet:keep", SnippetPayload.encode(snippet("keep")), remoteClock.next()) }
        docFile().parentFile?.mkdirs()
        docFile().writeText(codec.persist(polluted))
        repo.addSnippet(snippet("keep").copy(id = 1L))

        val document = codec.decode(source.read())
        assertEquals("123", document.visibleValues()["stat:words"]) // 原样保留
        assertTrue(document.tombstones().none { it.startsWith("stat:") }) // 未越界
    }

    /** 内存版仓库：只真实现 source 用到的四个方法，其余按接口语义最小过滤。 */
    private class FakeSnippetRepository : SnippetRepository {
        private val rows = MutableStateFlow<List<Snippet>>(emptyList())
        private var nextId = 1L
        var addCount = 0
            private set
        var updateCount = 0
            private set
        var deleteCount = 0
            private set

        override fun getAllSnippets() = rows

        override fun getSnippetsByCategory(category: String) =
            rows.map { list -> list.filter { it.category == category } }

        override fun getSnippetsByLanguage(language: String) =
            rows.map { list -> list.filter { it.language == language } }

        override fun searchSnippets(query: String) =
            rows.map { list -> list.filter { it.title.contains(query) || it.content.contains(query) } }

        override fun searchSnippetsByLanguage(query: String, language: String) = rows.map { l ->
            l.filter {
                (it.title.contains(query) || it.content.contains(query)) &&
                    it.language == language
            }
        }

        override fun searchSnippetsByCategory(query: String, category: String) = rows.map { l ->
            l.filter {
                (it.title.contains(query) || it.content.contains(query)) &&
                    it.category == category
            }
        }

        override fun searchSnippetsSubstring(query: String) = searchSnippets(query)

        override fun getSnippetById(id: Long) = rows.map { list -> list.firstOrNull { it.id == id } }

        override fun getAllCategories() = rows.map { list -> list.map { it.category }.distinct() }

        override suspend fun addSnippet(snippet: Snippet): Long {
            addCount++
            val row = snippet.copy(
                id = nextId++,
                syncId = snippet.syncId.ifEmpty { UUID.randomUUID().toString() } // 与真实实现的兜底纪律一致
            )
            rows.value = rows.value + row
            return row.id
        }

        override suspend fun updateSnippet(snippet: Snippet) {
            updateCount++
            rows.value = rows.value.map { if (it.id == snippet.id) snippet else it }
        }

        override suspend fun deleteSnippet(snippet: Snippet) {
            deleteCount++
            rows.value = rows.value.filter { it.id != snippet.id }
        }
    }
}
