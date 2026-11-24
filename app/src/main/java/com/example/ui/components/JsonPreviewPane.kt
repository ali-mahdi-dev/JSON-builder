package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.json.JsonSyntaxHighlighter
import com.example.json.SerializeResult
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalJsonSyntaxColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonPreviewPane(
    serializeResult: SerializeResult,
    indentSpaces: Int,
    isRawEditMode: Boolean,
    rawEditorText: String,
    rawValidationError: String?,
    hasUnappliedRawChanges: Boolean,
    onSetIndentSpaces: (Int) -> Unit,
    onToggleRawEditMode: (Boolean) -> Unit,
    onUpdateRawText: (String) -> Unit,
    onApplyRawToVisual: () -> Unit,
    onRevertRawToVisual: () -> Unit,
    onCopyJson: (String) -> Unit,
    onShareJson: (String) -> Unit,
    onExportJson: (String) -> Unit,
    onImportJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalJsonSyntaxColors.current
    val activeJsonText = if (isRawEditMode) rawEditorText else serializeResult.formattedJson

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("json_mode_toggle")
                    ) {
                        SegmentedButton(
                            selected = !isRawEditMode,
                            onClick = { onToggleRawEditMode(false) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            icon = {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text("Preview", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SegmentedButton(
                            selected = isRawEditMode,
                            onClick = { onToggleRawEditMode(true) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            icon = {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text("Edit Raw", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    FilterChip(
                        selected = indentSpaces == 4,
                        onClick = { onSetIndentSpaces(if (indentSpaces == 2) 4 else 2) },
                        label = {
                            Text(
                                text = "${indentSpaces}sp",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("indent_toggle_chip")
                    )
                }

                // Action & Validation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isSyntaxOk = if (isRawEditMode) rawValidationError == null else serializeResult.isValidForExport
                    Surface(
                        color = if (isSyntaxOk) syntaxColors.validBadge.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isSyntaxOk) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSyntaxOk) syntaxColors.validBadge else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = when {
                                    isRawEditMode && rawValidationError != null -> "Syntax Error"
                                    !isRawEditMode && !serializeResult.isValidForExport -> "${serializeResult.issues.size} Field Issue(s)"
                                    else -> "Valid JSON"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSyntaxOk) syntaxColors.validBadge else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onCopyJson(activeJsonText) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_json_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy JSON to clipboard")
                        }
                        IconButton(
                            onClick = { onShareJson(activeJsonText) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("share_json_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share JSON")
                        }
                        IconButton(
                            onClick = { onExportJson(activeJsonText) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Export .json file")
                        }
                        IconButton(
                            onClick = onImportJson,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("import_json_button")
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = "Import .json file")
                        }
                    }
                }
            }
        }

        // Raw JSON Sync Controls when in Edit Raw mode
        AnimatedVisibility(visible = isRawEditMode) {
            Surface(
                color = if (rawValidationError != null) MaterialTheme.colorScheme.errorContainer
                else if (hasUnappliedRawChanges) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = rawValidationError
                            ?: if (hasUnappliedRawChanges) "Raw edits are valid. Apply to update Visual Builder (visual data is safely preserved until applied)."
                            else "Editing raw JSON safely. Invalid syntax will never overwrite your visual form.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (rawValidationError != null) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onApplyRawToVisual,
                            enabled = rawValidationError == null && hasUnappliedRawChanges,
                            modifier = Modifier.testTag("apply_raw_button")
                        ) {
                            Text("Apply to Visual")
                        }
                        OutlinedButton(
                            onClick = onRevertRawToVisual,
                            enabled = hasUnappliedRawChanges,
                            modifier = Modifier.testTag("revert_raw_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Revert")
                        }
                    }
                }
            }
        }

        // Code Viewport
        if (!isRawEditMode) {
            val highlighted = remember(serializeResult.formattedJson, syntaxColors) {
                JsonSyntaxHighlighter.highlight(serializeResult.formattedJson, syntaxColors)
            }
            val lineCount = remember(serializeResult.formattedJson) {
                serializeResult.formattedJson.lines().size.coerceAtLeast(1)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(syntaxColors.codeBackground)
                    .padding(12.dp)
            ) {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                ) {
                    // Line Numbers Column
                    Column(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (line in 1..lineCount) {
                            Text(
                                text = line.toString(),
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = syntaxColors.lineNumber
                            )
                        }
                    }

                    // Highlighted JSON Text
                    Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                        Text(
                            text = highlighted,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.testTag("json_preview_text")
                        )
                    }
                }
            }

            // Footer Metadata Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Code,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$lineCount lines • ${serializeResult.formattedJson.length} chars",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "UTF-8 JSON",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = rawEditorText,
                onValueChange = onUpdateRawText,
                isError = rawValidationError != null,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("raw_json_editor_input")
            )
        }
    }
}
