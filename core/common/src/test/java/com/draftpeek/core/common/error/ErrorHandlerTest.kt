package com.draftpeek.core.common.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.draftpeek.core.common.R
import io.mockk.every
import io.mockk.mockk
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.UnsupportedCharsetException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ErrorHandler")
class ErrorHandlerTest {

    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        // Default: network is available
        val cm = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val caps = mockk<NetworkCapabilities>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns cm
        every { cm.activeNetwork } returns network
        every { cm.getNetworkCapabilities(network) } returns caps
        every { caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // Mock getString for all resource IDs used
        every { context.getString(any()) } returns "error message"
    }

    @Nested
    @DisplayName("fromException()")
    inner class FromExceptionTest {

        @Test
        @DisplayName("UnknownHostException → Network error")
        fun unknownHostIsNetworkError() {
            val error = ErrorHandler.fromException(UnknownHostException(), context)
            assertTrue(error is AppError.Network)
        }

        @Test
        @DisplayName("SocketTimeoutException → Network error")
        fun socketTimeoutIsNetworkError() {
            val error = ErrorHandler.fromException(SocketTimeoutException(), context)
            assertTrue(error is AppError.Network)
        }

        @Test
        @DisplayName("FileNotFoundException → FileOperation error")
        fun fileNotFoundIsFileError() {
            val error = ErrorHandler.fromException(FileNotFoundException(), context)
            assertTrue(error is AppError.FileOperation)
            assertEquals(FileOp.READ, (error as AppError.FileOperation).operation)
        }

        @Test
        @DisplayName("SecurityException → FileOperation error")
        fun securityExceptionIsFileError() {
            val error = ErrorHandler.fromException(SecurityException(), context)
            assertTrue(error is AppError.FileOperation)
        }

        @Test
        @DisplayName("UnsupportedCharsetException → Parse error")
        fun unsupportedCharsetIsParseError() {
            val error = ErrorHandler.fromException(UnsupportedCharsetException("bad"), context)
            assertTrue(error is AppError.Parse)
            assertEquals("encoding", (error as AppError.Parse).source)
        }

        @Test
        @DisplayName("OutOfMemoryError → Unknown error")
        fun oomIsUnknownError() {
            val error = ErrorHandler.fromException(OutOfMemoryError(), context)
            assertTrue(error is AppError.Unknown)
        }

        @Test
        @DisplayName("generic Exception → Unknown error")
        fun genericExceptionIsUnknown() {
            val error = ErrorHandler.fromException(RuntimeException("test"), context)
            assertTrue(error is AppError.Unknown)
        }

        @Test
        @DisplayName("IOException with no network → Network error")
        fun ioExceptionNoNetworkIsNetworkError() {
            // Override network to be unavailable
            val cm = mockk<ConnectivityManager>()
            every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns cm
            every { cm.activeNetwork } returns null

            val error = ErrorHandler.fromException(IOException(), context)
            assertTrue(error is AppError.Network)
        }
    }

    @Nested
    @DisplayName("getRetryAction()")
    inner class GetRetryActionTest {

        @Test
        @DisplayName("Network errors are retryable")
        fun networkIsRetryable() {
            val error = AppError.Network(R.string.error_network_general)
            assertTrue(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("FileOperation READ is retryable")
        fun fileReadIsRetryable() {
            val error = AppError.FileOperation(FileType.TEXT, FileOp.READ, R.string.error_file_not_found)
            assertTrue(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("FileOperation WRITE is not retryable")
        fun fileWriteNotRetryable() {
            val error = AppError.FileOperation(FileType.TEXT, FileOp.WRITE, R.string.error_file_not_found)
            assertFalse(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("FileOperation DELETE is not retryable")
        fun fileDeleteNotRetryable() {
            val error = AppError.FileOperation(FileType.TEXT, FileOp.DELETE, R.string.error_file_not_found)
            assertFalse(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("FileOperation CREATE is not retryable")
        fun fileCreateNotRetryable() {
            val error = AppError.FileOperation(FileType.TEXT, FileOp.CREATE, R.string.error_file_not_found)
            assertFalse(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("FileOperation EXPORT is not retryable")
        fun fileExportNotRetryable() {
            val error = AppError.FileOperation(FileType.OFFICE, FileOp.EXPORT, R.string.error_file_not_found)
            assertFalse(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("Validation errors are not retryable")
        fun validationNotRetryable() {
            val error = AppError.Validation("field", R.string.error_url_empty)
            assertFalse(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("Parse errors are not retryable")
        fun parseNotRetryable() {
            val error = AppError.Parse("json", R.string.error_parse_json)
            assertFalse(ErrorHandler.getRetryAction(error))
        }

        @Test
        @DisplayName("Unknown errors are retryable")
        fun unknownIsRetryable() {
            val error = AppError.Unknown(R.string.error_unknown)
            assertTrue(ErrorHandler.getRetryAction(error))
        }
    }

    @Nested
    @DisplayName("getUserMessage()")
    inner class GetUserMessageTest {

        @Test
        @DisplayName("returns localized message from context")
        fun returnsLocalizedMessage() {
            every { context.getString(R.string.error_network_general) } returns "Network error occurred"
            val error = AppError.Network(R.string.error_network_general)
            assertEquals("Network error occurred", ErrorHandler.getUserMessage(error, context))
        }

        @Test
        @DisplayName("returns correct message for each error type")
        fun returnsCorrectMessage() {
            every { context.getString(R.string.error_file_not_found) } returns "File not found"
            val error = AppError.FileOperation(FileType.TEXT, FileOp.READ, R.string.error_file_not_found)
            assertEquals("File not found", ErrorHandler.getUserMessage(error, context))
        }
    }

    /**
     * Context-free 布尔重载分支覆盖：面向 ViewModel 层，直接传入网络可用性，
     * 无需 mock Context。锁定异常 -> AppError 分类及其 messageResId/cause 契约。
     */
    @Nested
    @DisplayName("fromException(e, isNetworkAvailable)")
    inner class BooleanOverloadTest {

        @Test
        @DisplayName("ConnectException → Network no-connection")
        fun connectExceptionIsNoConnection() {
            val error = ErrorHandler.fromException(java.net.ConnectException(), isNetworkAvailable = true)
            assertTrue(error is AppError.Network)
            assertEquals(R.string.error_network_no_connection, error.messageResId)
        }

        @Test
        @DisplayName("UnknownHostException carries no-connection resId and cause")
        fun unknownHostResIdAndCause() {
            val cause = UnknownHostException("host")
            val error = ErrorHandler.fromException(cause, isNetworkAvailable = true)
            assertTrue(error is AppError.Network)
            error as AppError.Network
            assertEquals(R.string.error_network_no_connection, error.messageResId)
            assertEquals(cause, error.cause)
        }

        @Test
        @DisplayName("SocketTimeoutException carries timeout resId")
        fun socketTimeoutResId() {
            val error = ErrorHandler.fromException(SocketTimeoutException(), isNetworkAvailable = true)
            assertTrue(error is AppError.Network)
            assertEquals(R.string.error_network_timeout, (error as AppError.Network).messageResId)
        }

        @Test
        @DisplayName("IOException with network available → general Network error")
        fun ioExceptionWithNetworkIsGeneral() {
            val error = ErrorHandler.fromException(IOException(), isNetworkAvailable = true)
            assertTrue(error is AppError.Network)
            assertEquals(R.string.error_network_general, (error as AppError.Network).messageResId)
        }

        @Test
        @DisplayName("IOException without network → no-connection Network error")
        fun ioExceptionWithoutNetworkIsNoConnection() {
            val error = ErrorHandler.fromException(IOException(), isNetworkAvailable = false)
            assertTrue(error is AppError.Network)
            assertEquals(R.string.error_network_no_connection, (error as AppError.Network).messageResId)
        }

        @Test
        @DisplayName("SecurityException → FileOperation permission-denied READ")
        fun securityExceptionIsPermissionDenied() {
            val error = ErrorHandler.fromException(SecurityException(), isNetworkAvailable = true)
            assertTrue(error is AppError.FileOperation)
            error as AppError.FileOperation
            assertEquals(FileOp.READ, error.operation)
            assertEquals(R.string.error_file_permission_denied, error.messageResId)
        }

        @Test
        @DisplayName("JSONException → Parse json")
        fun jsonExceptionIsParseJson() {
            val error = ErrorHandler.fromException(org.json.JSONException("bad"), isNetworkAvailable = true)
            assertTrue(error is AppError.Parse)
            assertEquals("json", (error as AppError.Parse).source)
        }

        @Test
        @DisplayName("OutOfMemoryError → Unknown carries cause")
        fun oomIsUnknownWithCause() {
            val cause = OutOfMemoryError()
            val error = ErrorHandler.fromException(cause, isNetworkAvailable = true)
            assertTrue(error is AppError.Unknown)
            assertEquals(cause, (error as AppError.Unknown).cause)
        }

        @Test
        @DisplayName("generic exception → Unknown with error_unknown resId")
        fun genericIsUnknownResId() {
            val error = ErrorHandler.fromException(RuntimeException("x"), isNetworkAvailable = true)
            assertTrue(error is AppError.Unknown)
            assertEquals(R.string.error_unknown, error.messageResId)
        }
    }
}
