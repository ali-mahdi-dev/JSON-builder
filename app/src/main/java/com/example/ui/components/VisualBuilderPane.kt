package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataArray
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.json.JsonEngine
import com.example.json.SerializeResult
import com.example.model.JsonArrayElement
import com.example.model.JsonDocumentModel
import com.example.model.JsonFieldEntry
import com.example.model.JsonFieldType
import com.example.model.JsonNode
import com.example.model.PathSegment
import com.example.model.RootMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalJsonSyntaxColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualBuilderPane(
    document: JsonDocumentModel,
    currentPath: List<PathSegment>,
    schemaSyncEnabled: Boolean,
    serializeResult: SerializeResult,
    onSwitchRootMode: (RootMode) -> Unit,
    onSelectRecord: (Int) -> Unit,
    onAddRecord: () -> Unit,
    onDuplicateRecord: () -> Unit,
    onDeleteRecord: (Int) -> Unit,
    onToggleSchemaSync: () -> Unit,
    onDrillIntoField: (String, String) -> Unit,
    onDrillIntoArrayItem: (String, Int) -> Unit,
    onNavigateToDepth: (Int) -> Unit,
    onNavigateUp: () -> Boolean,
    onAddField: (String, JsonFieldType, Boolean) -> Unit,
    onUpdateFieldKey: (String, String) -> Unit,
    onUpdateFieldValue: (String, String) -> Unit,
    onUpdateFieldType: (String, JsonFieldType) -> Unit,
    onToggleFieldEnforceType: (String) -> Unit,
    onMoveField: (String, Int) -> Unit,
    onRemoveField: (String) -> Unit,
    onAddArrayItem: (JsonFieldType, Boolean) -> Unit,
    onUpdateArrayItemValue: (String, String) -> Unit,
    onUpdateArrayItemType: (String, JsonFieldType) -> Unit,
    onToggleArrayItemEnforceType: (String) -> Unit,
    onMoveArrayItem: (String, Int) -> Unit,
    onRemoveArrayItem: (String) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentPath.isNotEmpty()) {
        BackHandler {
            onNavigateUp()
        }
    }

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var showAddArrayItemDialog by remember { mutableStateOf(false) }

    val activeRoot = remember(document) { JsonEngine.activeRootNode(document) }
    val activeNode = remember(activeRoot, currentPath) {
        JsonEngine.resolveNodeAt(activeRoot, currentPath) ?: activeRoot
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top workspace mode & records controls
        if (currentPath.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("root_mode_selector")
                        ) {
                            SegmentedButton(
                                selected = document.rootMode == RootMode.SINGLE_OBJECT,
                                onClick = { onSwitchRootMode(RootMode.SINGLE_OBJECT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    Icon(
                                        Icons.Rounded.DataObject,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            ) {
                                Text("Object { }", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            SegmentedButton(
                                selected = document.rootMode == RootMode.MULTI_RECORD,
                                onClick = { onSwitchRootMode(RootMode.MULTI_RECORD) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    Icon(
                                        Icons.Rounded.ViewList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            ) {
                                Text("Records [ ]", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    AnimatedVisibility(visible = document.rootMode == RootMode.MULTI_RECORD) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(document.records) { index, _ ->
                                    val selected = index == document.selectedRecordIndex
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectRecord(index) },
                                        label = {
                                            Text(
                                                "Record ${index + 1}",
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.testTag("record_chip_$index")
                                    )
                                }
                                item {
                                    FilledTonalButton(
                                        onClick = onAddRecord,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("add_record_button")
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Add Record",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Record")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = schemaSyncEnabled,
                                    onClick = onToggleSchemaSync,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            if (schemaSyncEnabled) "Sync Schema: ON" else "Sync Schema: OFF",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag("schema_sync_chip")
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = onDuplicateRecord,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("duplicate_record_button")
                                    ) {
                                        Icon(
                                            Icons.Rounded.ContentCopy,
                                            contentDescription = "Duplicate current record"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteRecord(document.selectedRecordIndex) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("delete_record_button")
                                    ) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete current record",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Nested Breadcrumb Bar
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onNavigateUp() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("nested_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to parent"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rootTitle = when (document.rootMode) {
                            RootMode.MULTI_RECORD -> "Record ${document.selectedRecordIndex + 1}"
                            RootMode.GENERIC_ARRAY -> "Root [ ]"
                            RootMode.SINGLE_OBJECT -> "Root { }"
                        }
                        BreadcrumbChip(
                            label = rootTitle,
                            isActive = false,
                            onClick = { onNavigateToDepth(0) }
                        )
                        currentPath.forEachIndexed { idx, segment ->
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            val segLabel = when (segment) {
                                is PathSegment.FieldSegment -> segment.key
                                is PathSegment.IndexSegment -> "[${segment.index}]"
                            }
                            BreadcrumbChip(
                                label = segLabel,
                                isActive = idx == currentPath.lastIndex,
                                onClick = { onNavigateToDepth(idx + 1) }
                            )
                        }
                    }
                }
            }
        }

        // Validation warning banner if enforced field(s) have errors
        AnimatedVisibility(visible = serializeResult.issues.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    val firstMsg = serializeResult.issues.firstOrNull()?.let { "${it.path}: ${it.message}" } ?: ""
                    Text(
                        text = "${serializeResult.issues.size} validation issue(s) — $firstMsg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Main Node Content (Object fields OR Array elements)
        when (activeNode) {
            is JsonNode.ObjectNode -> {
                val keyCounts = remember(activeNode.fields) {
                    activeNode.fields.groupingBy { it.key }.eachCount()
                }
                if (activeNode.fields.isEmpty()) {
                    EmptyStructureCard(
                        isObject = true,
                        onAddClick = { showAddFieldDialog = true },
                        onOpenTemplates = onOpenTemplates,
                        showTemplateHint = currentPath.isEmpty()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = activeNode.fields,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            val isDuplicate = entry.key.isNotBlank() && (keyCounts[entry.key] ?: 0) > 1
                            ObjectFieldCard(
                                entry = entry,
                                index = index,
                                totalCount = activeNode.fields.size,
                                isDuplicateKey = isDuplicate,
                                onKeyChange = { onUpdateFieldKey(entry.id, it) },
                                onValueChange = { onUpdateFieldValue(entry.id, it) },
                                onTypeChange = { onUpdateFieldType(entry.id, it) },
                                onToggleEnforceType = { onToggleFieldEnforceType(entry.id) },
                                onMoveUp = { onMoveField(entry.id, -1) },
                                onMoveDown = { onMoveField(entry.id, 1) },
                                onRemove = { onRemoveField(entry.id) },
                                onOpenNested = { onDrillIntoField(entry.id, entry.key) }
                            )
                        }

                        item {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { showAddFieldDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_field_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Field")
                            }
                        }
                    }
                }
            }

            is JsonNode.ArrayNode -> {
                if (activeNode.items.isEmpty()) {
                    EmptyStructureCard(
                        isObject = false,
                        onAddClick = { showAddArrayItemDialog = true },
                        onOpenTemplates = onOpenTemplates,
                        showTemplateHint = false
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = activeNode.items,
                            key = { _, item -> item.id }
                        ) { index, element ->
                            ArrayElementCard(
                                element = element,
                                index = index,
                                totalCount = activeNode.items.size,
                                onValueChange = { onUpdateArrayItemValue(element.id, it) },
                                onTypeChange = { onUpdateArrayItemType(element.id, it) },
                                onToggleEnforceType = { onToggleArrayItemEnforceType(element.id) },
                                onMoveUp = { onMoveArrayItem(element.id, -1) },
                                onMoveDown = { onMoveArrayItem(element.id, 1) },
                                onRemove = { onRemoveArrayItem(element.id) },
                                onOpenNested = { onDrillIntoArrayItem(element.id, index) }
                            )
                        }

                        item {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { showAddArrayItemDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_array_item_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Array Item")
                            }
                        }
                    }
                }
            }

            is JsonNode.Primitive -> Unit
        }
    }

    if (showAddFieldDialog) {
        AddFieldDialog(
            onDismiss = { showAddFieldDialog = false },
            onConfirm = { key, type, enforce ->
                onAddField(key, type, enforce)
                showAddFieldDialog = false
            }
        )
    }

    if (showAddArrayItemDialog) {
        AddArrayItemDialog(
            onDismiss = { showAddArrayItemDialog = false },
            onConfirm = { type, enforce ->
                onAddArrayItem(type, enforce)
                showAddArrayItemDialog = false
            }
        )
    }
}

@Composable
private fun BreadcrumbChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (!isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFamily,
            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ObjectFieldCard(
    entry: JsonFieldEntry,
    index: Int,
    totalCount: Int,
    isDuplicateKey: Boolean,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onTypeChange: (JsonFieldType) -> Unit,
    onToggleEnforceType: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenNested: () -> Unit
) {
    val node = entry.value
    val fieldType = when (node) {
        is JsonNode.Primitive -> node.type
        is JsonNode.ObjectNode -> JsonFieldType.OBJECT
        is JsonNode.ArrayNode -> JsonFieldType.ARRAY
    }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isDuplicateKey || entry.key.isBlank()) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Key Name + Reorder + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = entry.key,
                    onValueChange = onKeyChange,
                    label = { Text("JSON Key") },
                    placeholder = { Text("e.g. email") },
                    singleLine = true,
                    isError = entry.key.isBlank() || isDuplicateKey,
                    supportingText = when {
                        entry.key.isBlank() -> { { Text("Key cannot be empty") } }
                        isDuplicateKey -> { { Text("Duplicate key in this object") } }
                        else -> null
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("field_key_input_$index")
                )

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = index > 0,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                contentDescription = "Move field up",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = index < totalCount - 1,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowDownward,
                                contentDescription = "Move field down",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("remove_field_button_$index")
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove field",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Type & Enforcement Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { typeMenuExpanded = true },
                        label = {
                            Text(
                                text = "${fieldType.shortLabel}  ${fieldType.label}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("field_type_chip_$index")
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        JsonFieldType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            option.shortLabel,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(option.label)
                                    }
                                },
                                onClick = {
                                    onTypeChange(option)
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (node is JsonNode.Primitive) {
                    FilterChip(
                        selected = node.enforceType,
                        onClick = onToggleEnforceType,
                        leadingIcon = {
                            Icon(
                                imageVector = if (node.enforceType) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (node.enforceType) "Strict Type" else "Freeform",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("field_enforce_chip_$index")
                    )
                }
            }

            // Value Editor
            NodeValueEditor(
                node = node,
                testTagPrefix = "field_value_$index",
                onValueChange = onValueChange,
                onOpenNested = onOpenNested
            )
        }
    }
}

@Composable
private fun ArrayElementCard(
    element: JsonArrayElement,
    index: Int,
    totalCount: Int,
    onValueChange: (String) -> Unit,
    onTypeChange: (JsonFieldType) -> Unit,
    onToggleEnforceType: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenNested: () -> Unit
) {
    val node = element.value
    val itemType = when (node) {
        is JsonNode.Primitive -> node.type
        is JsonNode.ObjectNode -> JsonFieldType.OBJECT
        is JsonNode.ArrayNode -> JsonFieldType.ARRAY
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("array_item_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "[$index]",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { menuExpanded = true },
                            label = {
                                Text(itemType.label, style = MaterialTheme.typography.labelMedium)
                            }
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            JsonFieldType.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.shortLabel}  ${option.label}") },
                                    onClick = {
                                        onTypeChange(option)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (node is JsonNode.Primitive) {
                        FilterChip(
                            selected = node.enforceType,
                            onClick = onToggleEnforceType,
                            leadingIcon = {
                                Icon(
                                    if (node.enforceType) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (node.enforceType) "Strict" else "Auto",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move item up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = index < totalCount - 1, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move item down", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            NodeValueEditor(
                node = node,
                testTagPrefix = "array_value_$index",
                onValueChange = onValueChange,
                onOpenNested = onOpenNested
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeValueEditor(
    node: JsonNode,
    testTagPrefix: String,
    onValueChange: (String) -> Unit,
    onOpenNested: () -> Unit
) {
    val syntaxColors = LocalJsonSyntaxColors.current
    when (node) {
        is JsonNode.Primitive -> {
            if (!node.enforceType) {
                val inferred = remember(node.rawValue) { JsonEngine.inferFlexibleKind(node.rawValue) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = node.rawValue,
                        onValueChange = onValueChange,
                        label = { Text("Flexible Value (type enforcement off)") },
                        placeholder = { Text("Type text, 42, true, null, or {\"a\":1}") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(testTagPrefix)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Auto-serialized as valid JSON ${inferred.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (node.type) {
                    JsonFieldType.STRING -> {
                        OutlinedTextField(
                            value = node.rawValue,
                            onValueChange = onValueChange,
                            label = { Text("String Value") },
                            placeholder = { Text("Enter text...") },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = syntaxColors.stringValue,
                                fontFamily = JetBrainsMonoFamily
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(testTagPrefix)
                        )
                    }
                    JsonFieldType.NUMBER -> {
                        val isValid = JsonEngine.isValidJsonNumber(node.rawValue)
                        OutlinedTextField(
                            value = node.rawValue,
                            onValueChange = onValueChange,
                            label = { Text("Number Value") },
                            placeholder = { Text("e.g. 25 or 3.14") },
                            singleLine = true,
                            isError = !isValid,
                            supportingText = if (!isValid) {
                                { Text("Enter a valid JSON number (e.g., 25 or -3.14)") }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = syntaxColors.numberValue,
                                fontFamily = JetBrainsMonoFamily
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(testTagPrefix)
                        )
                    }
                    JsonFieldType.BOOLEAN -> {
                        val isTrue = node.rawValue.trim().equals("true", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isTrue) "true" else "false",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = syntaxColors.booleanValue
                                )
                                Text(
                                    text = "Boolean literal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = isTrue,
                                    onClick = { onValueChange("true") },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                                ) { Text("true") }
                                SegmentedButton(
                                    selected = !isTrue,
                                    onClick = { onValueChange("false") },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                                ) { Text("false") }
                            }
                        }
                    }
                    JsonFieldType.NULL -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "null",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = syntaxColors.nullValue
                                )
                                Text(
                                    text = "JSON null value",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        is JsonNode.ObjectNode -> {
            val previewKeys = node.fields.take(3).joinToString(", ") { it.key.ifBlank { "unnamed" } }
            val summary = if (node.fields.isEmpty()) "{ empty object }"
            else "{ $previewKeys${if (node.fields.size > 3) ", …" else ""} } • ${node.fields.size} field(s)"

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenNested)
                    .testTag("${testTagPrefix}_open_nested")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DataObject,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Nested Object",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = summary,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Edit nested object",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        is JsonNode.ArrayNode -> {
            val summary = if (node.items.isEmpty()) "[ empty array ]"
            else "[ ${node.items.size} item(s) ]"

            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenNested)
                    .testTag("${testTagPrefix}_open_nested")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DataArray,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                text = "Nested Array",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = summary,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Edit nested array",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStructureCard(
    isObject: Boolean,
    onAddClick: () -> Unit,
    onOpenTemplates: () -> Unit,
    showTemplateHint: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isObject) Icons.Rounded.DataObject else Icons.Rounded.DataArray,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = if (isObject) "No fields yet" else "Array is empty",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isObject) {
                    "Add fields to build structured JSON visually without typing syntax."
                } else {
                    "Add strings, numbers, booleans, or nested objects to this array."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_add_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isObject) "Add First Field" else "Add First Item")
            }
            if (showTemplateHint) {
                OutlinedButton(onClick = onOpenTemplates) {
                    Text("Browse Templates & Saved Projects")
                }
            }
        }
    }
}

@Composable
private fun AddFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, JsonFieldType, Boolean) -> Unit
) {
    var keyText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(JsonFieldType.STRING) }
    var enforceType by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add JSON Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Field Key") },
                    placeholder = { Text("e.g. name, age, isActive, roles") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_field_key_input")
                )

                Text("Data Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JsonFieldType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                if (selectedType != JsonFieldType.OBJECT && selectedType != JsonFieldType.ARRAY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce Type", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (enforceType) "Validates and formats strictly as ${selectedType.label}"
                                else "Freeform input, auto-serialized to valid JSON",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enforceType,
                            onCheckedChange = { enforceType = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(keyText.ifBlank { "field" }, selectedType, enforceType) },
                modifier = Modifier.testTag("dialog_confirm_add_field")
            ) {
                Text("Add Field")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddArrayItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (JsonFieldType, Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf(JsonFieldType.STRING) }
    var enforceType by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Array Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Item Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JsonFieldType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                if (selectedType != JsonFieldType.OBJECT && selectedType != JsonFieldType.ARRAY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce Type", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (enforceType) "Strict ${selectedType.label} validation"
                                else "Freeform input, always valid JSON",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enforceType,
                            onCheckedChange = { enforceType = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedType, enforceType) }) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
