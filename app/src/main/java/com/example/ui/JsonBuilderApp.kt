package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.JsonPreviewPane
import com.example.ui.components.ProjectsScreen
import com.example.ui.components.VisualBuilderPane
import com.example.ui.theme.JsonBuilderTheme
import com.example.ui.theme.ThemePreference
import com.example.viewmodel.JsonBuilderViewModel
import com.example.viewmodel.WorkspaceTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonBuilderApp(viewModel: JsonBuilderViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val projectId by viewModel.currentProjectId.collectAsStateWithLifecycle()
    val projectName by viewModel.currentProjectName.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val document by viewModel.document.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val schemaSyncEnabled by viewModel.schemaSyncEnabled.collectAsStateWithLifecycle()
    val indentSpaces by viewModel.indentSpaces.collectAsStateWithLifecycle()
    val serializeResult by viewModel.serializeResult.collectAsStateWithLifecycle()

    val isRawEditMode by viewModel.isRawEditMode.collectAsStateWithLifecycle()
    val rawEditorText by viewModel.rawEditorText.collectAsStateWithLifecycle()
    val rawValidationError by viewModel.rawValidationError.collectAsStateWithLifecycle()
    val hasUnappliedRawChanges by viewModel.hasUnappliedRawChanges.collectAsStateWithLifecycle()

    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()

    var pendingExportText by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingTabNavigation by remember { mutableStateOf<WorkspaceTab?>(null) }
    var pendingRawExitPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val contentToWrite = pendingExportText
        pendingExportText = null
        if (uri != null && contentToWrite != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(contentToWrite.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                viewModel.emitMessage("Exported .json file successfully")
            }.onFailure { err ->
                viewModel.emitMessage("Export failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val rawText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: ""
                val fileName = resolveDisplayName(context, uri)
                viewModel.importJsonString(rawText, fileName)
            }.onFailure { err ->
                viewModel.emitMessage("Could not read file: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("JSON", text))
        viewModel.emitMessage("Copied JSON to clipboard")
    }

    val shareJson: (String) -> Unit = { text ->
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$projectName.json")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share JSON"))
    }

    val triggerExport: (String) -> Unit = { text ->
        pendingExportText = text
        val safeName = projectName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "data" }
            .let { if (it.endsWith(".json", ignoreCase = true)) it else "$it.json" }
        exportLauncher.launch(safeName)
    }

    val triggerImport: () -> Unit = {
        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
    }

    val attemptTabSwitch: (WorkspaceTab) -> Unit = { target ->
        if (selectedTab == WorkspaceTab.JSON &&
            target != WorkspaceTab.JSON &&
            isRawEditMode &&
            hasUnappliedRawChanges
        ) {
            pendingTabNavigation = target
            pendingRawExitPrompt = true
        } else {
            viewModel.selectTab(target)
        }
    }

    val attemptToggleRawMode: (Boolean) -> Unit = { enableRaw ->
        if (!enableRaw && isRawEditMode && hasUnappliedRawChanges) {
            pendingTabNavigation = null
            pendingRawExitPrompt = true
        } else {
            viewModel.setRawEditMode(enableRaw)
        }
    }

    JsonBuilderTheme(themePreference = themePreference) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isMediumOrWide = maxWidth >= 600.dp
            val isExpandedSplit = maxWidth >= 840.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        title = {
                            Column {
                                Text(
                                    text = projectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (hasUnsavedChanges) "JSON Builder • Unsaved" else "JSON Builder • Offline Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_save_button")
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = "Save project locally")
                            }
                            IconButton(
                                onClick = { viewModel.cycleThemePreference() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_theme_button")
                            ) {
                                Icon(
                                    imageVector = when (themePreference) {
                                        ThemePreference.SYSTEM -> Icons.Rounded.BrightnessAuto
                                        ThemePreference.LIGHT -> Icons.Rounded.Brightness7
                                        ThemePreference.DARK -> Icons.Rounded.Brightness4
                                    },
                                    contentDescription = "Cycle light/dark theme"
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isMediumOrWide) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            WorkspaceTab.entries.forEach { tab ->
                                if (isExpandedSplit && tab == WorkspaceTab.JSON) return@forEach
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { attemptTabSwitch(tab) },
                                    icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isMediumOrWide) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            Spacer(Modifier.size(8.dp))
                            WorkspaceTab.entries.forEach { tab ->
                                if (isExpandedSplit && tab == WorkspaceTab.JSON) return@forEach
                                NavigationRailItem(
                                    selected = selectedTab == tab,
                                    onClick = { attemptTabSwitch(tab) },
                                    icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                )
                            }
                        }
                        VerticalDivider()
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isExpandedSplit && (selectedTab == WorkspaceTab.BUILDER || selectedTab == WorkspaceTab.JSON)) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                VisualBuilderPane(
                                    document = document,
                                    currentPath = currentPath,
                                    schemaSyncEnabled = schemaSyncEnabled,
                                    serializeResult = serializeResult,
                                    onSwitchRootMode = viewModel::switchRootMode,
                                    onSelectRecord = viewModel::selectRecord,
                                    onAddRecord = viewModel::addRecord,
                                    onDuplicateRecord = viewModel::duplicateCurrentRecord,
                                    onDeleteRecord = viewModel::deleteRecord,
                                    onToggleSchemaSync = viewModel::toggleSchemaSync,
                                    onDrillIntoField = viewModel::drillIntoField,
                                    onDrillIntoArrayItem = viewModel::drillIntoArrayItem,
                                    onNavigateToDepth = viewModel::navigateToPathDepth,
                                    onNavigateUp = viewModel::navigateUpOneLevel,
                                    onAddField = viewModel::addField,
                                    onUpdateFieldKey = viewModel::updateFieldKey,
                                    onUpdateFieldValue = viewModel::updateFieldValue,
                                    onUpdateFieldType = viewModel::updateFieldType,
                                    onToggleFieldEnforceType = viewModel::toggleFieldEnforceType,
                                    onMoveField = viewModel::moveField,
                                    onRemoveField = viewModel::removeField,
                                    onAddArrayItem = viewModel::addArrayItem,
                                    onUpdateArrayItemValue = viewModel::updateArrayItemValue,
                                    onUpdateArrayItemType = viewModel::updateArrayItemType,
                                    onToggleArrayItemEnforceType = viewModel::toggleArrayItemEnforceType,
                                    onMoveArrayItem = viewModel::moveArrayItem,
                                    onRemoveArrayItem = viewModel::removeArrayItem,
                                    onOpenTemplates = { viewModel.selectTab(WorkspaceTab.PROJECTS) },
                                    modifier = Modifier.weight(1.15f).fillMaxHeight()
                                )
                                VerticalDivider()
                                JsonPreviewPane(
                                    serializeResult = serializeResult,
                                    indentSpaces = indentSpaces,
                                    isRawEditMode = isRawEditMode,
                                    rawEditorText = rawEditorText,
                                    rawValidationError = rawValidationError,
                                    hasUnappliedRawChanges = hasUnappliedRawChanges,
                                    onSetIndentSpaces = viewModel::setIndentSpaces,
                                    onToggleRawEditMode = attemptToggleRawMode,
                                    onUpdateRawText = viewModel::updateRawEditorText,
                                    onApplyRawToVisual = viewModel::applyRawJsonToVisual,
                                    onRevertRawToVisual = viewModel::revertRawToVisual,
                                    onCopyJson = copyToClipboard,
                                    onShareJson = shareJson,
                                    onExportJson = triggerExport,
                                    onImportJson = triggerImport,
                                    modifier = Modifier.weight(0.85f).fillMaxHeight()
                                )
                            }
                        } else {
                            when (selectedTab) {
                                WorkspaceTab.BUILDER -> VisualBuilderPane(
                                    document = document,
                                    currentPath = currentPath,
                                    schemaSyncEnabled = schemaSyncEnabled,
                                    serializeResult = serializeResult,
                                    onSwitchRootMode = viewModel::switchRootMode,
                                    onSelectRecord = viewModel::selectRecord,
                                    onAddRecord = viewModel::addRecord,
                                    onDuplicateRecord = viewModel::duplicateCurrentRecord,
                                    onDeleteRecord = viewModel::deleteRecord,
                                    onToggleSchemaSync = viewModel::toggleSchemaSync,
                                    onDrillIntoField = viewModel::drillIntoField,
                                    onDrillIntoArrayItem = viewModel::drillIntoArrayItem,
                                    onNavigateToDepth = viewModel::navigateToPathDepth,
                                    onNavigateUp = viewModel::navigateUpOneLevel,
                                    onAddField = viewModel::addField,
                                    onUpdateFieldKey = viewModel::updateFieldKey,
                                    onUpdateFieldValue = viewModel::updateFieldValue,
                                    onUpdateFieldType = viewModel::updateFieldType,
                                    onToggleFieldEnforceType = viewModel::toggleFieldEnforceType,
                                    onMoveField = viewModel::moveField,
                                    onRemoveField = viewModel::removeField,
                                    onAddArrayItem = viewModel::addArrayItem,
                                    onUpdateArrayItemValue = viewModel::updateArrayItemValue,
                                    onUpdateArrayItemType = viewModel::updateArrayItemType,
                                    onToggleArrayItemEnforceType = viewModel::toggleArrayItemEnforceType,
                                    onMoveArrayItem = viewModel::moveArrayItem,
                                    onRemoveArrayItem = viewModel::removeArrayItem,
                                    onOpenTemplates = { viewModel.selectTab(WorkspaceTab.PROJECTS) }
                                )

                                WorkspaceTab.JSON -> JsonPreviewPane(
                                    serializeResult = serializeResult,
                                    indentSpaces = indentSpaces,
                                    isRawEditMode = isRawEditMode,
                                    rawEditorText = rawEditorText,
                                    rawValidationError = rawValidationError,
                                    hasUnappliedRawChanges = hasUnappliedRawChanges,
                                    onSetIndentSpaces = viewModel::setIndentSpaces,
                                    onToggleRawEditMode = attemptToggleRawMode,
                                    onUpdateRawText = viewModel::updateRawEditorText,
                                    onApplyRawToVisual = viewModel::applyRawJsonToVisual,
                                    onRevertRawToVisual = viewModel::revertRawToVisual,
                                    onCopyJson = copyToClipboard,
                                    onShareJson = shareJson,
                                    onExportJson = triggerExport,
                                    onImportJson = triggerImport
                                )

                                WorkspaceTab.PROJECTS -> ProjectsScreen(
                                    projects = savedProjects,
                                    activeProjectId = projectId,
                                    activeProjectName = projectName,
                                    hasUnsavedChanges = hasUnsavedChanges,
                                    onSaveCurrent = { showSaveDialog = true },
                                    onOpenProject = viewModel::openProject,
                                    onCreateProject = viewModel::createNewProject,
                                    onRenameProject = viewModel::renameProject,
                                    onDuplicateProject = viewModel::duplicateProject,
                                    onDeleteProject = viewModel::deleteProject,
                                    onImportJsonFile = triggerImport
                                )
                            }
                        }
                    }
                }
            }

            if (showSaveDialog) {
                SaveProjectDialog(
                    initialName = projectName,
                    onDismiss = { showSaveDialog = false },
                    onSave = { name ->
                        viewModel.saveCurrentProject(name)
                        showSaveDialog = false
                    }
                )
            }

            if (pendingRawExitPrompt) {
                RawEditsGuardDialog(
                    canApply = rawValidationError == null,
                    validationError = rawValidationError,
                    onKeepEditing = {
                        pendingRawExitPrompt = false
                        pendingTabNavigation = null
                    },
                    onApplyAndContinue = {
                        if (viewModel.applyRawJsonToVisual()) {
                            pendingRawExitPrompt = false
                            pendingTabNavigation?.let { viewModel.selectTab(it) }
                            pendingTabNavigation = null
                        }
                    },
                    onDiscardAndContinue = {
                        viewModel.discardRawEditsAndExit()
                        pendingRawExitPrompt = false
                        pendingTabNavigation?.let { viewModel.selectTab(it) }
                        pendingTabNavigation = null
                    }
                )
            }
        }
    }
}

private fun tabIcon(tab: WorkspaceTab) = when (tab) {
    WorkspaceTab.BUILDER -> Icons.Rounded.Tune
    WorkspaceTab.JSON -> Icons.Rounded.Code
    WorkspaceTab.PROJECTS -> Icons.Rounded.FolderOpen
}

@Composable
private fun SaveProjectDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Project Locally") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Saved projects stay on your device and work completely offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_project_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                modifier = Modifier.testTag("confirm_save_project_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RawEditsGuardDialog(
    canApply: Boolean,
    validationError: String?,
    onKeepEditing: () -> Unit,
    onApplyAndContinue: () -> Unit,
    onDiscardAndContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("Unapplied Raw JSON Edits") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (canApply) {
                        "You modified raw JSON. Would you like to apply those changes to the Visual Builder or discard them?"
                    } else {
                        "Your raw JSON has syntax errors ($validationError). Your Visual Builder data is untouched."
                    }
                )
            }
        },
        confirmButton = {
            if (canApply) {
                Button(onClick = onApplyAndContinue) {
                    Text("Apply to Visual")
                }
            } else {
                Button(onClick = onKeepEditing) {
                    Text("Keep Editing Raw")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canApply) {
                    TextButton(onClick = onKeepEditing) { Text("Keep Editing") }
                }
                TextButton(onClick = onDiscardAndContinue) {
                    Text("Discard Raw Edits", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
