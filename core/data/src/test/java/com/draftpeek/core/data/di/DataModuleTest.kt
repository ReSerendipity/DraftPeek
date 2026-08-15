package com.draftpeek.core.data.di

import com.draftpeek.core.data.repository.BookmarkRepository
import com.draftpeek.core.data.repository.BookmarkRepositoryImpl
import com.draftpeek.core.data.repository.EditorFileRepository
import com.draftpeek.core.data.repository.EditorFileRepositoryImpl
import com.draftpeek.core.data.repository.RecentFilesRepository
import com.draftpeek.core.data.repository.RecentFilesRepositoryImpl
import com.draftpeek.core.data.repository.SecurityEventRepository
import com.draftpeek.core.data.repository.SecurityEventRepositoryImpl
import com.draftpeek.core.data.repository.SnippetRepository
import com.draftpeek.core.data.repository.SnippetRepositoryImpl
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.repository.UserActivityRepositoryImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Hilt DI 模块集成测试。
 *
 * 验证 DataModule 中所有 @Binds 绑定的 Repository 接口与实现类匹配。
 * 这确保 Hilt 依赖图在编译时能正确解析所有绑定。
 */
@DisplayName("DataModule Hilt DI Bindings")
class DataModuleTest {

    @Nested
    @DisplayName("Repository bindings")
    inner class RepositoryBindingTests {

        @Test
        @DisplayName("BookmarkRepository 绑定到 BookmarkRepositoryImpl")
        fun bookmarkRepository_bindingExists() {
            val implClass = BookmarkRepositoryImpl::class.java
            val interfaceClass = BookmarkRepository::class.java
            Assertions.assertTrue(interfaceClass.isAssignableFrom(implClass),
                "BookmarkRepositoryImpl 必须实现 BookmarkRepository 接口")
        }

        @Test
        @DisplayName("RecentFilesRepository 绑定到 RecentFilesRepositoryImpl")
        fun recentFilesRepository_bindingExists() {
            val implClass = RecentFilesRepositoryImpl::class.java
            val interfaceClass = RecentFilesRepository::class.java
            Assertions.assertTrue(interfaceClass.isAssignableFrom(implClass),
                "RecentFilesRepositoryImpl 必须实现 RecentFilesRepository 接口")
        }

        @Test
        @DisplayName("SnippetRepository 绑定到 SnippetRepositoryImpl")
        fun snippetRepository_bindingExists() {
            val implClass = SnippetRepositoryImpl::class.java
            val interfaceClass = SnippetRepository::class.java
            Assertions.assertTrue(interfaceClass.isAssignableFrom(implClass),
                "SnippetRepositoryImpl 必须实现 SnippetRepository 接口")
        }

        @Test
        @DisplayName("UserActivityRepository 绑定到 UserActivityRepositoryImpl")
        fun userActivityRepository_bindingExists() {
            val implClass = UserActivityRepositoryImpl::class.java
            val interfaceClass = UserActivityRepository::class.java
            Assertions.assertTrue(interfaceClass.isAssignableFrom(implClass),
                "UserActivityRepositoryImpl 必须实现 UserActivityRepository 接口")
        }

        @Test
        @DisplayName("EditorFileRepository 绑定到 EditorFileRepositoryImpl")
        fun editorFileRepository_bindingExists() {
            val implClass = EditorFileRepositoryImpl::class.java
            val interfaceClass = EditorFileRepository::class.java
            Assertions.assertTrue(interfaceClass.isAssignableFrom(implClass),
                "EditorFileRepositoryImpl 必须实现 EditorFileRepository 接口")
        }

        @Test
        @DisplayName("SecurityEventRepository 绑定到 SecurityEventRepositoryImpl")
        fun securityEventRepository_bindingExists() {
            val implClass = SecurityEventRepositoryImpl::class.java
            val interfaceClass = SecurityEventRepository::class.java
            Assertions.assertTrue(interfaceClass.isAssignableFrom(implClass),
                "SecurityEventRepositoryImpl 必须实现 SecurityEventRepository 接口")
        }
    }

    @Nested
    @DisplayName("Inject constructor verification")
    inner class InjectConstructorTests {

        @Test
        @DisplayName("所有 Repository 实现类有 @Inject 构造器")
        fun allRepositoryImplsHaveInjectConstructor() {
            val implClasses = listOf(
                BookmarkRepositoryImpl::class.java,
                RecentFilesRepositoryImpl::class.java,
                SnippetRepositoryImpl::class.java,
                UserActivityRepositoryImpl::class.java,
                SecurityEventRepositoryImpl::class.java,
            )

            implClasses.forEach { clazz ->
                val hasInjectConstructor = clazz.declaredConstructors.any { constructor ->
                    constructor.annotations.any { annotation ->
                        annotation.annotationClass.simpleName == "Inject"
                    }
                }
                Assertions.assertTrue(hasInjectConstructor,
                    "${clazz.simpleName} 必须有 @Inject 构造器")
            }
        }
    }
}
