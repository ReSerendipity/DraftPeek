package com.draftpeek.feature.browser.sample

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "SampleFileManager"

private val Context.browserDataStore by preferencesDataStore(name = "browser_prefs")

/**
 * Represents a built-in sample code file shipped in the APK assets.
 *
 * @param name The display file name, e.g. "basics.py".
 * @param assetPath The path inside assets, e.g. "samples/basics.py".
 * @param language A human-readable language label, e.g. "Python".
 * @param uri The android_asset URI that the editor can open.
 */
data class SampleFile(val name: String, val assetPath: String, val language: String, val uri: String)

/**
 * Manager for built-in sample files located in `assets/samples/`.
 */
object SampleFileManager {

    private const val SAMPLES_DIR = "samples"
    private val SAMPLE_ORDER_KEY = stringPreferencesKey("sample_file_order")

    /** Maps common source file extensions to display language names. */
    private val LANGUAGE_MAP = mapOf(
        "c" to "C",
        "cpp" to "C++",
        "cs" to "C#",
        "java" to "Java",
        "py" to "Python",
        "js" to "JavaScript",
        "ts" to "TypeScript",
        "kt" to "Kotlin",
        "html" to "HTML",
        "css" to "CSS",
        "md" to "Markdown",
        "sql" to "SQL",
        "go" to "Go",
        "rs" to "Rust",
        "dart" to "Dart",
        "ps1" to "PowerShell",
        "sh" to "Bash",
        "bat" to "BAT",
        "swift" to "Swift"
    )

    /**
     * Language popularity ranking (based on TIOBE, Stack Overflow Survey 2024).
     * Lower number = more popular. Used for sorting sample files.
     */
    private val LANGUAGE_POPULARITY = mapOf(
        "Python" to 1,
        "JavaScript" to 2,
        "Java" to 3,
        "C#" to 4,
        "C" to 5,
        "C++" to 6,
        "TypeScript" to 7,
        "Kotlin" to 8,
        "Go" to 9,
        "Rust" to 10,
        "Swift" to 11,
        "HTML" to 12,
        "CSS" to 13,
        "Markdown" to 14,
        "SQL" to 15,
        "Dart" to 16,
        "PowerShell" to 17,
        "Bash" to 18,
        "BAT" to 19
    )

    /**
     * Lists all sample files bundled in `assets/samples/`.
     *
     * @param customOrder Optional list of file names defining custom order.
     *        If provided, files are ordered according to this list. Files not
     *        in the list are appended at the end in default order.
     * @return A sorted list of [SampleFile], or an empty list if the
     *         directory cannot be read.
     */
    fun listSamples(context: Context, customOrder: List<String>? = null): List<SampleFile> {
        val assetManager = context.assets
        val files = try {
            assetManager.list(SAMPLES_DIR) ?: emptyArray()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list sample files", e)
            emptyArray()
        }

        val defaultSorted = files
            .filter { it.isNotBlank() }
            .map { name ->
                val extension = name.substringAfterLast('.', "")
                val language = LANGUAGE_MAP[extension.lowercase()] ?: extension.uppercase()
                val assetPath = "$SAMPLES_DIR/$name"
                SampleFile(
                    name = name,
                    assetPath = assetPath,
                    language = language,
                    uri = "file:///android_asset/$assetPath"
                )
            }
            .sortedWith(
                compareBy<SampleFile> { sample ->
                    LANGUAGE_POPULARITY[sample.language] ?: Int.MAX_VALUE
                }.thenBy { it.name }
            )

        return if (customOrder != null) {
            val orderIndex = customOrder.withIndex().associate { it.value to it.index }
            defaultSorted.sortedWith(
                compareBy<SampleFile> { orderIndex[it.name] ?: Int.MAX_VALUE }
                    .thenBy { LANGUAGE_POPULARITY[it.language] ?: Int.MAX_VALUE }
                    .thenBy { it.name }
            )
        } else {
            defaultSorted
        }
    }

    /**
     * Loads the saved custom sample order as a Flow.
     */
    fun observeSampleOrder(context: Context): Flow<List<String>> = context.browserDataStore.data.map { prefs ->
        prefs[SAMPLE_ORDER_KEY]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    /**
     * Loads the saved custom sample order (suspending).
     */
    suspend fun loadSampleOrder(context: Context): List<String> =
        context.browserDataStore.data.first()[SAMPLE_ORDER_KEY]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    /**
     * Saves the custom sample order.
     */
    suspend fun saveSampleOrder(context: Context, orderedNames: List<String>) {
        context.browserDataStore.edit { prefs ->
            prefs[SAMPLE_ORDER_KEY] = orderedNames.joinToString(",")
        }
    }
}
