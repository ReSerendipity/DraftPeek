package com.draftpeek.feature.browser.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.viewmodel.FileBrowserViewModel
import kotlinx.coroutines.launch

/**
 * 文件浏览器动作对话框组（新建文件夹 / 重命名 / 移动 / 删除 / 加密导出 / 解密打开）。
 * 从 FileBrowserScreen 拆出（2026-10-11 任务A 第三步）。
 */
@Composable
internal fun NewFolderDialog(
    onDismiss: () -> Unit,
    viewModel: FileBrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        var folderName by remember { mutableStateOf("") }
        BrandDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(stringResource(R.string.browser_dialog_new_folder_title)) },
            content = {
                BrandOutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.browser_hint_enter_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                BrandFilledButton(
                    onClick = {
                        if (folderName.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.browser_error_folder_name_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BrandFilledButton
                        }
                        val existing = viewModel.getInternalFolders().any { it.name == folderName }
                        if (existing) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.browser_error_folder_exists),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BrandFilledButton
                        }
                        val created = viewModel.createFolder(folderName)
                        if (created) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.browser_folder_create_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.browser_folder_create_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.browser_action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
        )
}

@Composable
internal fun RenameFileDialog(
    fileToRename: FileItem,
    onDismiss: () -> Unit,
    viewModel: FileBrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        val renameTarget = fileToRename
        var newName by remember(renameTarget) { mutableStateOf(renameTarget?.name ?: "") }
        BrandDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(stringResource(R.string.browser_dialog_rename_title)) },
            content = {
                BrandOutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.browser_hint_enter_new_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.browser_action_rename),
                    onClick = {
                        val item = fileToRename
                        val trimmed = newName.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.browser_error_name_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BrandFilledButton
                        }
                        scope.launch {
                            val ok = viewModel.renameFile(item, trimmed)
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (ok) R.string.browser_rename_success else R.string.browser_rename_failed
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            if (ok) {
                                onDismiss()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
        )
}

@Composable
internal fun MoveFileDialog(
    fileToMove: FileItem?,
    isMultiSelectMode: Boolean,
    selectedItems: Set<String>,
    onDismiss: () -> Unit,
    viewModel: FileBrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        val folders = viewModel.getInternalFolders()
        val isMultiSelect = isMultiSelectMode && selectedItems.isNotEmpty()
        BrandDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(stringResource(R.string.browser_dialog_move_to_title)) },
            content = {
                Column {
                    // Move to internal storage root
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browser_move_to_internal)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = PrototypeTokens.accent
                            )
                        },
                        onClick = {
                            val userFilesDir = com.draftpeek.core.common.util.AppFileManager.getUserFilesDir(context)
                            val rootUri = com.draftpeek.core.common.util.AppFileManager.fileToInternalUri(userFilesDir)
                            if (isMultiSelect) {
                                val count = viewModel.moveSelectedFiles(rootUri)
                                Toast.makeText(
                                    context,
                                    if (count > 0) {
                                        context.getString(R.string.browser_move_success, count)
                                    } else {
                                        context.getString(R.string.browser_move_failed)
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val item = fileToMove
                                if (item != null) {
                                    val moved = viewModel.moveFile(item.uri.toString(), rootUri)
                                    Toast.makeText(
                                        context,
                                        if (moved) {
                                            context.getString(R.string.browser_move_success, 1)
                                        } else {
                                            context.getString(R.string.browser_move_failed)
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            onDismiss()
                        }
                    )
                    if (folders.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = PrototypeTokens.accent
                                    )
                                },
                                onClick = {
                                    val folderUri = com.draftpeek.core.common.util.AppFileManager.fileToInternalUri(
                                        folder
                                    )
                                    if (isMultiSelect) {
                                        val count = viewModel.moveSelectedFiles(folderUri)
                                        Toast.makeText(
                                            context,
                                            if (count > 0) {
                                                context.getString(R.string.browser_move_success, count)
                                            } else {
                                                context.getString(R.string.browser_move_failed)
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val item = fileToMove
                                        if (item != null) {
                                            val moved = viewModel.moveFile(item.uri.toString(), folderUri)
                                            Toast.makeText(
                                                context,
                                                if (moved) {
                                                    context.getString(R.string.browser_move_success, 1)
                                                } else {
                                                    context.getString(R.string.browser_move_failed)
                                                },
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
        )
}

@Composable
internal fun DeleteFileConfirmDialog(
    fileToDelete: FileItem,
    onDismiss: () -> Unit,
    viewModel: FileBrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        val fileItem = fileToDelete
        BrandDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(stringResource(R.string.browser_dialog_delete_file_title)) },
            content = {
                Text(
                    text = stringResource(R.string.browser_dialog_delete_file_message, fileItem.name),
                    style = DraftPeekTypography.bodyMedium
                )
            },
            confirmButton = {
                BrandFilledButton(
                    onClick = {
                        scope.launch {
                            val deleted = viewModel.deleteInternalFile(fileItem)
                            val msg = if (deleted) {
                                context.getString(R.string.browser_delete_success)
                            } else {
                                context.getString(R.string.browser_delete_failed)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.browser_action_delete_file))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
        )
}

@Composable
internal fun EncryptExportDialog(
    fileToEncrypt: FileItem,
    onDismiss: () -> Unit,
    viewModel: FileBrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        val encryptFileItem = fileToEncrypt
        var password by rememberSaveable { mutableStateOf("") }
        var confirmPassword by rememberSaveable { mutableStateOf("") }
        var passwordError by remember { mutableStateOf(false) }
        BrandDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = {
                Text(
                    stringResource(R.string.browser_dialog_encrypt_export_title) +
                        " · " + encryptFileItem.name
                )
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandOutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text(stringResource(R.string.browser_dialog_encrypt_export_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BrandOutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = false
                        },
                        label = { Text(stringResource(R.string.browser_dialog_encrypt_export_confirm)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError,
                        supportingText = if (passwordError) {
                            {
                                Text(
                                    if (password.length < 4) {
                                        stringResource(R.string.browser_encrypt_password_too_short)
                                    } else {
                                        stringResource(R.string.browser_encrypt_password_mismatch)
                                    },
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    onClick = {
                        if (password.length < 4) {
                            passwordError = true
                            return@BrandFilledButton
                        }
                        if (password != confirmPassword) {
                            passwordError = true
                            return@BrandFilledButton
                        }
                        scope.launch {
                            val ok = viewModel.encryptExportFile(encryptFileItem, password)
                            val msg = if (ok) {
                                context.getString(R.string.browser_encrypt_export_success)
                            } else {
                                context.getString(R.string.browser_encrypt_export_failed)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.browser_action_encrypt_export))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
        )
}

@Composable
internal fun DecryptOpenDialog(
    fileToDecrypt: FileItem,
    onDismiss: () -> Unit,
    viewModel: FileBrowserViewModel,
    onFileClick: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        val decryptFileItem = fileToDecrypt
        var password by rememberSaveable { mutableStateOf("") }
        var passwordError by remember { mutableStateOf(false) }
        BrandDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = {
                Text(
                    stringResource(R.string.browser_dialog_decrypt_title) +
                        " · " + decryptFileItem.name
                )
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandOutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text(stringResource(R.string.browser_dialog_decrypt_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError,
                        supportingText = if (passwordError) {
                            {
                                Text(
                                    stringResource(R.string.browser_decrypt_failed),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    onClick = {
                        scope.launch {
                            val decryptedItem = viewModel.decryptAndOpenFile(decryptFileItem, password)
                            if (decryptedItem != null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.browser_decrypt_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                                onFileClick(decryptedItem)
                            } else {
                                passwordError = true
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.browser_decrypt_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.browser_dialog_decrypt_title))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
        )
}
