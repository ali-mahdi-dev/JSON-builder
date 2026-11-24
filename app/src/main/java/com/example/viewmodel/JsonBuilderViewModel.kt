package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.JsonProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.json.JsonEngine
import com.example.json.ProjectCodec
import com.example.json.SerializeResult
import com.example.model.JsonArrayElement
import com.example.model.JsonDocumentModel
import com.example.model.JsonFieldEntry
import com.example.model.JsonFieldType
import com.example.model.JsonNode
import com.example.model.PathSegment
import com.example.model.ProjectTemplate
import com.example.model.RootMode
import com.example.model.StarterTemplates
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "JsonBuilderViewModel"

enum class WorkspaceTab(val label: String) {
    BUILDER("Builder"),
    JSON("JSON"),
    PROJECTS("Projects")
}

class JsonBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(
        AppDatabase.getInstance(application).jsonProjectDao()
    )

    val savedProjects: StateFlow<List<JsonProjectEntity>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedTab = MutableStateFlow(WorkspaceTab.BUILDER)
    val selectedTab: StateFlow<WorkspaceTab> = _selectedTab.asStateFlow()

    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _currentProjectId = MutableStateFlow(UUID.randomUUID().toString())
    val currentProjectId: StateFlow<String> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow("Untitled JSON")
    val currentProjectName: StateFlow<String> = _currentProjectName.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _document = MutableStateFlow(StarterTemplates.blankObject())
    val document: StateFlow<JsonDocumentModel> = _document.asStateFlow()

    private val _currentPath = MutableStateFlow<List<PathSegment>>(emptyList())
    val currentPath: StateFlow<List<PathSegment>> = _currentPath.asStateFlow()

    private val _schemaSyncEnabled = MutableStateFlow(true)
    val schemaSyncEnabled: StateFlow<Boolean> = _schemaSyncEnabled.asStateFlow()

    private val _indentSpaces = MutableStateFlow(2)
    val indentSpaces: StateFlow<Int> = _indentSpaces.asStateFlow()

    val serializeResult: StateFlow<SerializeResult> = combine(_document, _indentSpaces) { doc, indent ->
        JsonEngine.serialize(doc, indent)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        JsonEngine.serialize(_document.value, 2)
    )

    // Raw JSON editing state (isolated from visual builder until explicitly applied)
    private val _isRawEditMode = MutableStateFlow(false)
    val isRawEditMode: StateFlow<Boolean> = _isRawEditMode.asStateFlow()

    private val _rawEditorText = MutableStateFlow(serializeResult.value.formattedJson)
    val rawEditorText: StateFlow<String> = _rawEditorText.asStateFlow()

    private val _rawValidationError = MutableStateFlow<String?>(null)
    val rawValidationError: StateFlow<String?> = _rawValidationError.asStateFlow()

    private val _hasUnappliedRawChanges = MutableStateFlow(false)
    val hasUnappliedRawChanges: StateFlow<Boolean> = _hasUnappliedRawChanges.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun selectTab(tab: WorkspaceTab) {
        _selectedTab.value = tab
    }

    fun cycleThemePreference() {
        _themePreference.value = when (_themePreference.value) {
            ThemePreference.SYSTEM -> ThemePreference.LIGHT
            ThemePreference.LIGHT -> ThemePreference.DARK
            ThemePreference.DARK -> ThemePreference.SYSTEM
        }
    }

    fun setIndentSpaces(spaces: Int) {
        _indentSpaces.value = if (spaces == 4) 4 else 2
    }

    fun toggleSchemaSync() {
        _schemaSyncEnabled.value = !_schemaSyncEnabled.value
    }

    fun updateProjectName(newName: String) {
        _currentProjectName.value = newName
        _hasUnsavedChanges.value = true
    }

    // --- Root Mode & Multi-Record Management ---

    fun switchRootMode(targetMode: RootMode) {
        val current = _document.value
        if (current.rootMode == targetMode) return
        _currentPath.value = emptyList()
        val updated = when (targetMode) {
            RootMode.SINGLE_OBJECT -> {
                val obj = when (current.rootMode) {
                    RootMode.SINGLE_OBJECT -> current.rootObject
                    RootMode.MULTI_RECORD -> current.records.getOrNull(current.selectedRecordIndex)
                        ?: current.records.firstOrNull()
                        ?: JsonNode.ObjectNode()
                    RootMode.GENERIC_ARRAY -> JsonNode.ObjectNode()
                }
                current.copy(rootMode = RootMode.SINGLE_OBJECT, rootObject = obj)
            }
            RootMode.MULTI_RECORD -> {
                val initialRecords = when (current.rootMode) {
                    RootMode.SINGLE_OBJECT -> listOf(current.rootObject)
                    RootMode.MULTI_RECORD -> current.records
                    RootMode.GENERIC_ARRAY -> {
                        val objects = current.rootArray.items.mapNotNull { it.value as? JsonNode.ObjectNode }
                        objects.ifEmpty { listOf(JsonNode.ObjectNode()) }
                    }
                }
                current.copy(
                    rootMode = RootMode.MULTI_RECORD,
                    records = initialRecords,
                    selectedRecordIndex = 0
                )
            }
            RootMode.GENERIC_ARRAY -> {
                current.copy(rootMode = RootMode.GENERIC_ARRAY)
            }
        }
        mutateDocument(updated)
    }

    fun selectRecord(index: Int) {
        val doc = _document.value
        if (doc.rootMode != RootMode.MULTI_RECORD) return
        val clamped = index.coerceIn(0, (doc.records.size - 1).coerceAtLeast(0))
        _currentPath.value = emptyList()
        _document.value = doc.copy(selectedRecordIndex = clamped)
    }

    fun addRecord() {
        val doc = _document.value
        if (doc.rootMode != RootMode.MULTI_RECORD) return
        val templateSource = doc.records.getOrNull(doc.selectedRecordIndex)
            ?: doc.records.firstOrNull()
            ?: JsonNode.ObjectNode()
        val newRecord = JsonEngine.cloneStructureWithEmptyValues(templateSource) as JsonNode.ObjectNode
        val newList = doc.records + newRecord
        _currentPath.value = emptyList()
        mutateDocument(doc.copy(records = newList, selectedRecordIndex = newList.lastIndex))
    }

    fun duplicateCurrentRecord() {
        val doc = _document.value
        if (doc.rootMode != RootMode.MULTI_RECORD) return
        val source = doc.records.getOrNull(doc.selectedRecordIndex) ?: return
        val copy = deepCloneNodeWithNewIds(source) as JsonNode.ObjectNode
        val insertIndex = (doc.selectedRecordIndex + 1).coerceAtMost(doc.records.size)
        val newList = doc.records.toMutableList().apply { add(insertIndex, copy) }
        _currentPath.value = emptyList()
        mutateDocument(doc.copy(records = newList, selectedRecordIndex = insertIndex))
    }

    fun deleteRecord(index: Int) {
        val doc = _document.value
        if (doc.rootMode != RootMode.MULTI_RECORD) return
        if (doc.records.size <= 1) {
            val cleared = JsonEngine.cloneStructureWithEmptyValues(doc.records.first()) as JsonNode.ObjectNode
            _currentPath.value = emptyList()
            mutateDocument(doc.copy(records = listOf(cleared), selectedRecordIndex = 0))
            emitMessage("Cleared last record values")
            return
        }
        val mutable = doc.records.toMutableList().apply { removeAt(index) }
        val nextIndex = doc.selectedRecordIndex.coerceAtMost(mutable.lastIndex)
        _currentPath.value = emptyList()
        mutateDocument(doc.copy(records = mutable, selectedRecordIndex = nextIndex))
    }

    // --- Nested Navigation ---

    fun drillIntoField(entryId: String, key: String) {
        val nextPath = _currentPath.value + PathSegment.FieldSegment(entryId, key.ifBlank { "object" })
        val root = JsonEngine.activeRootNode(_document.value)
        if (JsonEngine.resolveNodeAt(root, nextPath) != null) {
            _currentPath.value = nextPath
        }
    }

    fun drillIntoArrayItem(elementId: String, index: Int) {
        val nextPath = _currentPath.value + PathSegment.IndexSegment(elementId, index)
        val root = JsonEngine.activeRootNode(_document.value)
        if (JsonEngine.resolveNodeAt(root, nextPath) != null) {
            _currentPath.value = nextPath
        }
    }

    fun navigateToPathDepth(depth: Int) {
        _currentPath.value = _currentPath.value.take(depth.coerceAtLeast(0))
    }

    fun navigateUpOneLevel(): Boolean {
        val path = _currentPath.value
        if (path.isEmpty()) return false
        _currentPath.value = path.dropLast(1)
        return true
    }

    // --- Object Field Mutations ---

    fun addField(key: String, type: JsonFieldType, enforceType: Boolean = true) {
        val doc = _document.value
        val path = _currentPath.value
        val newEntry = JsonFieldEntry(
            key = key.trim(),
            value = createDefaultNode(type, enforceType)
        )

        if (doc.rootMode == RootMode.MULTI_RECORD && path.isEmpty() && _schemaSyncEnabled.value) {
            val updatedRecords = doc.records.mapIndexed { idx, rec ->
                val entryForRec = if (idx == doc.selectedRecordIndex) {
                    newEntry
                } else {
                    JsonFieldEntry(
                        key = newEntry.key,
                        value = JsonEngine.cloneStructureWithEmptyValues(newEntry.value)
                    )
                }
                rec.copy(fields = rec.fields + entryForRec)
            }
            mutateDocument(doc.copy(records = updatedRecords))
        } else {
            updateCurrentContainer { container ->
                val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
                obj.copy(fields = obj.fields + newEntry)
            }
        }
    }

    fun updateFieldKey(entryId: String, newKey: String) {
        val doc = _document.value
        val path = _currentPath.value
        if (doc.rootMode == RootMode.MULTI_RECORD && path.isEmpty() && _schemaSyncEnabled.value) {
            val activeRecord = doc.records.getOrNull(doc.selectedRecordIndex)
            val oldIndex = activeRecord?.fields?.indexOfFirst { it.id == entryId } ?: -1
            val oldKey = activeRecord?.fields?.getOrNull(oldIndex)?.key
            val updatedRecords = doc.records.mapIndexed { recIdx, rec ->
                rec.copy(
                    fields = rec.fields.mapIndexed { fIdx, entry ->
                        val isMatch = if (recIdx == doc.selectedRecordIndex) {
                            entry.id == entryId
                        } else {
                            (oldKey != null && entry.key == oldKey && fIdx == oldIndex) ||
                                (oldKey != null && entry.key == oldKey)
                        }
                        if (isMatch) entry.copy(key = newKey) else entry
                    }
                )
            }
            mutateDocument(doc.copy(records = updatedRecords))
        } else {
            updateCurrentContainer { container ->
                val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
                obj.copy(
                    fields = obj.fields.map { entry ->
                        if (entry.id == entryId) entry.copy(key = newKey) else entry
                    }
                )
            }
        }
    }

    fun updateFieldValue(entryId: String, newValue: String) {
        updateCurrentContainer { container ->
            val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
            obj.copy(
                fields = obj.fields.map { entry ->
                    if (entry.id == entryId && entry.value is JsonNode.Primitive) {
                        entry.copy(value = entry.value.copy(rawValue = newValue))
                    } else {
                        entry
                    }
                }
            )
        }
    }

    fun updateFieldType(entryId: String, newType: JsonFieldType) {
        val doc = _document.value
        val path = _currentPath.value
        if (doc.rootMode == RootMode.MULTI_RECORD && path.isEmpty() && _schemaSyncEnabled.value) {
            val activeRecord = doc.records.getOrNull(doc.selectedRecordIndex)
            val targetKey = activeRecord?.fields?.firstOrNull { it.id == entryId }?.key
            val updatedRecords = doc.records.mapIndexed { recIdx, rec ->
                rec.copy(
                    fields = rec.fields.map { entry ->
                        val isMatch = if (recIdx == doc.selectedRecordIndex) {
                            entry.id == entryId
                        } else {
                            targetKey != null && entry.key == targetKey
                        }
                        if (isMatch) entry.copy(value = convertNodeType(entry.value, newType)) else entry
                    }
                )
            }
            mutateDocument(doc.copy(records = updatedRecords))
        } else {
            updateCurrentContainer { container ->
                val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
                obj.copy(
                    fields = obj.fields.map { entry ->
                        if (entry.id == entryId) {
                            entry.copy(value = convertNodeType(entry.value, newType))
                        } else {
                            entry
                        }
                    }
                )
            }
        }
    }

    fun toggleFieldEnforceType(entryId: String) {
        updateCurrentContainer { container ->
            val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
            obj.copy(
                fields = obj.fields.map { entry ->
                    if (entry.id == entryId && entry.value is JsonNode.Primitive) {
                        entry.copy(value = entry.value.copy(enforceType = !entry.value.enforceType))
                    } else {
                        entry
                    }
                }
            )
        }
    }

    fun moveField(entryId: String, delta: Int) {
        val doc = _document.value
        val path = _currentPath.value
        if (doc.rootMode == RootMode.MULTI_RECORD && path.isEmpty() && _schemaSyncEnabled.value) {
            val active = doc.records.getOrNull(doc.selectedRecordIndex) ?: return
            val fromIndex = active.fields.indexOfFirst { it.id == entryId }
            if (fromIndex < 0) return
            val toIndex = (fromIndex + delta).coerceIn(0, active.fields.lastIndex)
            if (fromIndex == toIndex) return
            val updatedRecords = doc.records.map { rec ->
                if (fromIndex < rec.fields.size && toIndex < rec.fields.size) {
                    val list = rec.fields.toMutableList()
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    rec.copy(fields = list)
                } else {
                    rec
                }
            }
            mutateDocument(doc.copy(records = updatedRecords))
        } else {
            updateCurrentContainer { container ->
                val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
                val idx = obj.fields.indexOfFirst { it.id == entryId }
                if (idx < 0) return@updateCurrentContainer obj
                val target = (idx + delta).coerceIn(0, obj.fields.lastIndex)
                if (idx == target) return@updateCurrentContainer obj
                val mutable = obj.fields.toMutableList()
                val moved = mutable.removeAt(idx)
                mutable.add(target, moved)
                obj.copy(fields = mutable)
            }
        }
    }

    fun removeField(entryId: String) {
        val doc = _document.value
        val path = _currentPath.value
        if (doc.rootMode == RootMode.MULTI_RECORD && path.isEmpty() && _schemaSyncEnabled.value) {
            val active = doc.records.getOrNull(doc.selectedRecordIndex)
            val keyToRemove = active?.fields?.firstOrNull { it.id == entryId }?.key
            val updatedRecords = doc.records.mapIndexed { recIdx, rec ->
                rec.copy(
                    fields = rec.fields.filter { entry ->
                        if (recIdx == doc.selectedRecordIndex) entry.id != entryId
                        else keyToRemove == null || entry.key != keyToRemove
                    }
                )
            }
            mutateDocument(doc.copy(records = updatedRecords))
        } else {
            updateCurrentContainer { container ->
                val obj = container as? JsonNode.ObjectNode ?: return@updateCurrentContainer container
                obj.copy(fields = obj.fields.filterNot { it.id == entryId })
            }
        }
    }

    // --- Array Item Mutations ---

    fun addArrayItem(type: JsonFieldType, enforceType: Boolean = true) {
        val newElement = JsonArrayElement(value = createDefaultNode(type, enforceType))
        updateCurrentContainer { container ->
            val arr = container as? JsonNode.ArrayNode ?: return@updateCurrentContainer container
            arr.copy(items = arr.items + newElement)
        }
    }

    fun updateArrayItemValue(elementId: String, newValue: String) {
        updateCurrentContainer { container ->
            val arr = container as? JsonNode.ArrayNode ?: return@updateCurrentContainer container
            arr.copy(
                items = arr.items.map { elem ->
                    if (elem.id == elementId && elem.value is JsonNode.Primitive) {
                        elem.copy(value = elem.value.copy(rawValue = newValue))
                    } else {
                        elem
                    }
                }
            )
        }
    }

    fun updateArrayItemType(elementId: String, newType: JsonFieldType) {
        updateCurrentContainer { container ->
            val arr = container as? JsonNode.ArrayNode ?: return@updateCurrentContainer container
            arr.copy(
                items = arr.items.map { elem ->
                    if (elem.id == elementId) {
                        elem.copy(value = convertNodeType(elem.value, newType))
                    } else {
                        elem
                    }
                }
            )
        }
    }

    fun toggleArrayItemEnforceType(elementId: String) {
        updateCurrentContainer { container ->
            val arr = container as? JsonNode.ArrayNode ?: return@updateCurrentContainer container
            arr.copy(
                items = arr.items.map { elem ->
                    if (elem.id == elementId && elem.value is JsonNode.Primitive) {
                        elem.copy(value = elem.value.copy(enforceType = !elem.value.enforceType))
                    } else {
                        elem
                    }
                }
            )
        }
    }

    fun moveArrayItem(elementId: String, delta: Int) {
        updateCurrentContainer { container ->
            val arr = container as? JsonNode.ArrayNode ?: return@updateCurrentContainer container
            val idx = arr.items.indexOfFirst { it.id == elementId }
            if (idx < 0) return@updateCurrentContainer arr
            val target = (idx + delta).coerceIn(0, arr.items.lastIndex)
            if (idx == target) return@updateCurrentContainer arr
            val mutable = arr.items.toMutableList()
            val moved = mutable.removeAt(idx)
            mutable.add(target, moved)
            arr.copy(items = mutable)
        }
    }

    fun removeArrayItem(elementId: String) {
        updateCurrentContainer { container ->
            val arr = container as? JsonNode.ArrayNode ?: return@updateCurrentContainer container
            arr.copy(items = arr.items.filterNot { it.id == elementId })
        }
    }

    // --- Raw JSON Safe Sync ---

    fun setRawEditMode(enabled: Boolean) {
        if (enabled) {
            if (!_hasUnappliedRawChanges.value) {
                _rawEditorText.value = serializeResult.value.formattedJson
                _rawValidationError.value = null
            }
            _isRawEditMode.value = true
        } else {
            _isRawEditMode.value = false
        }
    }

    fun updateRawEditorText(newText: String) {
        _rawEditorText.value = newText
        _hasUnappliedRawChanges.value = (newText != serializeResult.value.formattedJson)
        val validation = JsonEngine.validateRawJson(newText)
        _rawValidationError.value = validation.exceptionOrNull()?.localizedMessage
    }

    fun applyRawJsonToVisual(): Boolean {
        val result = JsonEngine.parseRawJson(
            rawJson = _rawEditorText.value,
            existingForMetadata = _document.value
        )
        return result.fold(
            onSuccess = { parsedDoc ->
                _currentPath.value = emptyList()
                _document.value = parsedDoc
                _hasUnsavedChanges.value = true
                _hasUnappliedRawChanges.value = false
                _rawValidationError.value = null
                _rawEditorText.value = JsonEngine.serialize(parsedDoc, _indentSpaces.value).formattedJson
                _isRawEditMode.value = false
                emitMessage("Applied raw JSON to Visual Builder")
                true
            },
            onFailure = { error ->
                _rawValidationError.value = error.localizedMessage ?: "Invalid JSON"
                false
            }
        )
    }

    fun revertRawToVisual() {
        _rawEditorText.value = serializeResult.value.formattedJson
        _rawValidationError.value = null
        _hasUnappliedRawChanges.value = false
        emitMessage("Reverted raw editor to Visual Builder state")
    }

    fun discardRawEditsAndExit() {
        _rawEditorText.value = serializeResult.value.formattedJson
        _rawValidationError.value = null
        _hasUnappliedRawChanges.value = false
        _isRawEditMode.value = false
    }

    // --- Local Projects & File Management ---

    fun saveCurrentProject(customName: String? = null) {
        val targetName = (customName ?: _currentProjectName.value).trim().ifBlank { "Untitled JSON" }
        _currentProjectName.value = targetName
        viewModelScope.launch {
            val saved = repository.saveProject(
                id = _currentProjectId.value,
                name = targetName,
                document = _document.value
            )
            _currentProjectId.value = saved.id
            _hasUnsavedChanges.value = false
            emitMessage("Saved \"$targetName\" locally")
        }
    }

    fun openProject(entity: JsonProjectEntity) {
        runCatching {
            val doc = ProjectCodec.decode(entity.projectStateJson)
            _currentProjectId.value = entity.id
            _currentProjectName.value = entity.name
            _document.value = doc
            _currentPath.value = emptyList()
            _hasUnsavedChanges.value = false
            _isRawEditMode.value = false
            _hasUnappliedRawChanges.value = false
            _rawValidationError.value = null
            _selectedTab.value = WorkspaceTab.BUILDER
            emitMessage("Opened \"${entity.name}\"")
        }.onFailure {
            Log.e(TAG, "Failed to open project", it)
            emitMessage("Could not decode saved project")
        }
    }

    fun createNewProject(name: String, template: ProjectTemplate?) {
        val doc = template?.createDocument?.invoke() ?: StarterTemplates.blankObject()
        val projectName = name.trim().ifBlank { template?.title ?: "Untitled JSON" }
        val newId = UUID.randomUUID().toString()
        _currentProjectId.value = newId
        _currentProjectName.value = projectName
        _document.value = doc
        _currentPath.value = emptyList()
        _isRawEditMode.value = false
        _hasUnappliedRawChanges.value = false
        _rawValidationError.value = null
        _selectedTab.value = WorkspaceTab.BUILDER
        viewModelScope.launch {
            repository.saveProject(newId, projectName, doc)
            _hasUnsavedChanges.value = false
            emitMessage("Created \"$projectName\"")
        }
    }

    fun renameProject(entity: JsonProjectEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val doc = runCatching { ProjectCodec.decode(entity.projectStateJson) }
                .getOrDefault(_document.value)
            repository.saveProject(entity.id, trimmed, doc)
            if (entity.id == _currentProjectId.value) {
                _currentProjectName.value = trimmed
            }
            emitMessage("Renamed project to \"$trimmed\"")
        }
    }

    fun duplicateProject(entity: JsonProjectEntity) {
        viewModelScope.launch {
            val doc = runCatching { ProjectCodec.decode(entity.projectStateJson) }
                .getOrNull() ?: return@launch
            val newId = UUID.randomUUID().toString()
            val copyName = "${entity.name} (Copy)"
            repository.saveProject(newId, copyName, doc)
            emitMessage("Duplicated as \"$copyName\"")
        }
    }

    fun deleteProject(entity: JsonProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(entity.id)
            if (entity.id == _currentProjectId.value) {
                _currentProjectId.value = UUID.randomUUID().toString()
                _hasUnsavedChanges.value = true
            }
            emitMessage("Deleted \"${entity.name}\"")
        }
    }

    fun importJsonString(rawJson: String, fileName: String? = null): Boolean {
        val parsed = JsonEngine.parseRawJson(rawJson)
        return parsed.fold(
            onSuccess = { doc ->
                val cleanName = fileName?.removeSuffix(".json")?.trim()?.takeIf { it.isNotEmpty() }
                if (cleanName != null) {
                    _currentProjectId.value = UUID.randomUUID().toString()
                    _currentProjectName.value = cleanName
                }
                _document.value = doc
                _currentPath.value = emptyList()
                _hasUnsavedChanges.value = true
                _isRawEditMode.value = false
                _hasUnappliedRawChanges.value = false
                _rawValidationError.value = null
                _selectedTab.value = WorkspaceTab.BUILDER
                emitMessage("Imported JSON into Visual Builder")
                true
            },
            onFailure = { error ->
                emitMessage("Import failed: ${error.localizedMessage ?: "Invalid JSON"}")
                false
            }
        )
    }

    fun emitMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    // --- Internal Tree Helpers ---

    private fun updateCurrentContainer(transform: (JsonNode) -> JsonNode) {
        val doc = _document.value
        val path = _currentPath.value
        val updated = when (doc.rootMode) {
            RootMode.SINGLE_OBJECT -> {
                val newRoot = JsonEngine.updateNodeAt(doc.rootObject, path, transform) as JsonNode.ObjectNode
                doc.copy(rootObject = newRoot)
            }
            RootMode.MULTI_RECORD -> {
                val idx = doc.selectedRecordIndex.coerceIn(0, (doc.records.size - 1).coerceAtLeast(0))
                val updatedRecords = doc.records.mapIndexed { i, rec ->
                    if (i == idx) {
                        JsonEngine.updateNodeAt(rec, path, transform) as JsonNode.ObjectNode
                    } else {
                        rec
                    }
                }
                doc.copy(records = updatedRecords)
            }
            RootMode.GENERIC_ARRAY -> {
                val newArr = JsonEngine.updateNodeAt(doc.rootArray, path, transform) as JsonNode.ArrayNode
                doc.copy(rootArray = newArr)
            }
        }
        mutateDocument(updated)
    }

    private fun mutateDocument(updated: JsonDocumentModel) {
        _document.value = updated
        _hasUnsavedChanges.value = true
        if (!_isRawEditMode.value) {
            _rawEditorText.value = JsonEngine.serialize(updated, _indentSpaces.value).formattedJson
            _hasUnappliedRawChanges.value = false
            _rawValidationError.value = null
        }
    }

    private fun createDefaultNode(type: JsonFieldType, enforceType: Boolean): JsonNode {
        return when (type) {
            JsonFieldType.OBJECT -> JsonNode.ObjectNode()
            JsonFieldType.ARRAY -> JsonNode.ArrayNode()
            JsonFieldType.BOOLEAN -> JsonNode.Primitive(type = type, rawValue = "false", enforceType = enforceType)
            JsonFieldType.NULL -> JsonNode.Primitive(type = type, rawValue = "null", enforceType = enforceType)
            else -> JsonNode.Primitive(type = type, rawValue = "", enforceType = enforceType)
        }
    }

    private fun convertNodeType(existing: JsonNode, newType: JsonFieldType): JsonNode {
        return when (newType) {
            JsonFieldType.OBJECT -> existing as? JsonNode.ObjectNode ?: JsonNode.ObjectNode()
            JsonFieldType.ARRAY -> existing as? JsonNode.ArrayNode ?: JsonNode.ArrayNode()
            else -> {
                val prevPrim = existing as? JsonNode.Primitive
                val defaultRaw = when (newType) {
                    JsonFieldType.BOOLEAN -> if (prevPrim?.rawValue?.lowercase() == "true") "true" else "false"
                    JsonFieldType.NULL -> "null"
                    JsonFieldType.NUMBER -> {
                        val prev = prevPrim?.rawValue.orEmpty()
                        if (JsonEngine.isValidJsonNumber(prev)) prev else ""
                    }
                    JsonFieldType.STRING -> {
                        if (prevPrim?.type == JsonFieldType.NULL) "" else prevPrim?.rawValue.orEmpty()
                    }
                    else -> ""
                }
                JsonNode.Primitive(
                    id = existing.id,
                    type = newType,
                    rawValue = defaultRaw,
                    enforceType = prevPrim?.enforceType ?: true
                )
            }
        }
    }

    private fun deepCloneNodeWithNewIds(node: JsonNode): JsonNode {
        return when (node) {
            is JsonNode.Primitive -> node.copy(id = UUID.randomUUID().toString())
            is JsonNode.ObjectNode -> JsonNode.ObjectNode(
                id = UUID.randomUUID().toString(),
                fields = node.fields.map { entry ->
                    JsonFieldEntry(
                        id = UUID.randomUUID().toString(),
                        key = entry.key,
                        value = deepCloneNodeWithNewIds(entry.value)
                    )
                }
            )
            is JsonNode.ArrayNode -> JsonNode.ArrayNode(
                id = UUID.randomUUID().toString(),
                items = node.items.map { elem ->
                    JsonArrayElement(
                        id = UUID.randomUUID().toString(),
                        value = deepCloneNodeWithNewIds(elem.value)
                    )
                }
            )
        }
    }

}
