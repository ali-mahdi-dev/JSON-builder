package com.example.json

import android.util.JsonWriter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.model.JsonArrayElement
import com.example.model.JsonDocumentModel
import com.example.model.JsonFieldEntry
import com.example.model.JsonFieldType
import com.example.model.JsonNode
import com.example.model.PathSegment
import com.example.model.RootMode
import com.example.model.ValidationIssue
import com.example.ui.theme.JsonSyntaxColors
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.StringWriter
import java.math.BigDecimal
import java.util.UUID

data class SerializeResult(
    val formattedJson: String,
    val issues: List<ValidationIssue>,
    val isValidForExport: Boolean
)

enum class InferredJsonKind(val label: String) {
    STRING("String"),
    NUMBER("Number"),
    BOOLEAN("Boolean"),
    NULL("Null"),
    OBJECT("Object"),
    ARRAY("Array")
}

object JsonEngine {

    private val STRICT_JSON_NUMBER_REGEX = Regex("^-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?$")

    fun isValidJsonNumber(input: String): Boolean {
        val trimmed = input.trim()
        if (!STRICT_JSON_NUMBER_REGEX.matches(trimmed)) return false
        return try {
            BigDecimal(trimmed)
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    fun inferFlexibleKind(rawValue: String): InferredJsonKind {
        val trimmed = rawValue.trim()
        return when {
            rawValue.isEmpty() -> InferredJsonKind.STRING
            trimmed == "true" || trimmed == "false" -> InferredJsonKind.BOOLEAN
            trimmed == "null" -> InferredJsonKind.NULL
            isValidJsonNumber(trimmed) -> InferredJsonKind.NUMBER
            trimmed.startsWith("{") && trimmed.endsWith("}") && tryParseJsonContainer(trimmed) is JSONObject ->
                InferredJsonKind.OBJECT
            trimmed.startsWith("[") && trimmed.endsWith("]") && tryParseJsonContainer(trimmed) is JSONArray ->
                InferredJsonKind.ARRAY
            else -> InferredJsonKind.STRING
        }
    }

    private fun tryParseJsonContainer(text: String): Any? {
        return try {
            val tokener = JSONTokener(text)
            val value = tokener.nextValue()
            while (tokener.more()) {
                if (!tokener.next().isWhitespace()) return null
            }
            if (value is JSONObject || value is JSONArray) value else null
        } catch (_: Exception) {
            null
        }
    }

    fun serialize(document: JsonDocumentModel, indentSpaces: Int = 2): SerializeResult {
        val issues = mutableListOf<ValidationIssue>()
        val stringWriter = StringWriter()
        val writer = JsonWriter(stringWriter)
        writer.setIndent(" ".repeat(indentSpaces.coerceIn(0, 8)))

        when (document.rootMode) {
            RootMode.SINGLE_OBJECT -> {
                writeObject(writer, document.rootObject, path = "$", issues = issues)
            }
            RootMode.MULTI_RECORD -> {
                writer.beginArray()
                document.records.forEachIndexed { index, record ->
                    writeObject(writer, record, path = "[$index]", issues = issues)
                }
                writer.endArray()
            }
            RootMode.GENERIC_ARRAY -> {
                writeArray(writer, document.rootArray, path = "$", issues = issues)
            }
        }
        writer.flush()
        writer.close()

        return SerializeResult(
            formattedJson = stringWriter.toString(),
            issues = issues,
            isValidForExport = issues.isEmpty()
        )
    }

    private fun writeObject(
        writer: JsonWriter,
        obj: JsonNode.ObjectNode,
        path: String,
        issues: MutableList<ValidationIssue>
    ) {
        writer.beginObject()
        val seenKeys = mutableSetOf<String>()
        obj.fields.forEachIndexed { idx, entry ->
            val trimmedKey = entry.key
            val fieldPath = if (trimmedKey.isNotEmpty()) "$path.$trimmedKey" else "$path.<field#$idx>"
            if (trimmedKey.isBlank()) {
                issues.add(ValidationIssue(entry.id, fieldPath, "Key cannot be empty"))
            } else if (!seenKeys.add(trimmedKey)) {
                issues.add(ValidationIssue(entry.id, fieldPath, "Duplicate key \"$trimmedKey\""))
            }
            val safeKey = trimmedKey.ifBlank { "field_${idx + 1}" }
            writer.name(safeKey)
            writeNode(writer, entry.value, fieldPath, issues)
        }
        writer.endObject()
    }

    private fun writeArray(
        writer: JsonWriter,
        arr: JsonNode.ArrayNode,
        path: String,
        issues: MutableList<ValidationIssue>
    ) {
        writer.beginArray()
        arr.items.forEachIndexed { index, element ->
            writeNode(writer, element.value, "$path[$index]", issues)
        }
        writer.endArray()
    }

    private fun writeNode(
        writer: JsonWriter,
        node: JsonNode,
        path: String,
        issues: MutableList<ValidationIssue>
    ) {
        when (node) {
            is JsonNode.ObjectNode -> writeObject(writer, node, path, issues)
            is JsonNode.ArrayNode -> writeArray(writer, node, path, issues)
            is JsonNode.Primitive -> {
                if (!node.enforceType) {
                    writeFlexibleValue(writer, node.rawValue)
                } else {
                    when (node.type) {
                        JsonFieldType.STRING -> writer.value(node.rawValue)
                        JsonFieldType.NUMBER -> {
                            val trimmed = node.rawValue.trim()
                            if (isValidJsonNumber(trimmed)) {
                                writeParsedNumber(writer, trimmed)
                            } else {
                                issues.add(
                                    ValidationIssue(
                                        nodeId = node.id,
                                        path = path,
                                        message = "Invalid number value \"${node.rawValue}\""
                                    )
                                )
                                writer.value(0)
                            }
                        }
                        JsonFieldType.BOOLEAN -> {
                            val normalized = node.rawValue.trim().lowercase()
                            when (normalized) {
                                "true" -> writer.value(true)
                                "false" -> writer.value(false)
                                else -> {
                                    issues.add(
                                        ValidationIssue(
                                            nodeId = node.id,
                                            path = path,
                                            message = "Boolean must be true or false"
                                        )
                                    )
                                    writer.value(false)
                                }
                            }
                        }
                        JsonFieldType.NULL -> writer.nullValue()
                        JsonFieldType.OBJECT, JsonFieldType.ARRAY -> writer.nullValue()
                    }
                }
            }
        }
    }

    private fun writeFlexibleValue(writer: JsonWriter, rawValue: String) {
        val trimmed = rawValue.trim()
        when {
            rawValue.isEmpty() -> writer.value("")
            trimmed == "true" -> writer.value(true)
            trimmed == "false" -> writer.value(false)
            trimmed == "null" -> writer.nullValue()
            isValidJsonNumber(trimmed) -> writeParsedNumber(writer, trimmed)
            trimmed.startsWith("{") && trimmed.endsWith("}") -> {
                val parsed = tryParseJsonContainer(trimmed)
                if (parsed is JSONObject) {
                    writeOrgJsonObject(writer, parsed)
                } else {
                    writer.value(rawValue)
                }
            }
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                val parsed = tryParseJsonContainer(trimmed)
                if (parsed is JSONArray) {
                    writeOrgJsonArray(writer, parsed)
                } else {
                    writer.value(rawValue)
                }
            }
            else -> writer.value(rawValue)
        }
    }

    private fun writeOrgJsonObject(writer: JsonWriter, obj: JSONObject) {
        writer.beginObject()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            writer.name(key)
            writeAnyOrgValue(writer, obj.get(key))
        }
        writer.endObject()
    }

    private fun writeOrgJsonArray(writer: JsonWriter, arr: JSONArray) {
        writer.beginArray()
        for (i in 0 until arr.length()) {
            writeAnyOrgValue(writer, arr.get(i))
        }
        writer.endArray()
    }

    private fun writeAnyOrgValue(writer: JsonWriter, value: Any?) {
        when (value) {
            null, JSONObject.NULL -> writer.nullValue()
            is JSONObject -> writeOrgJsonObject(writer, value)
            is JSONArray -> writeOrgJsonArray(writer, value)
            is Boolean -> writer.value(value)
            is Number -> writer.value(value)
            else -> writer.value(value.toString())
        }
    }

    private fun writeParsedNumber(writer: JsonWriter, trimmed: String) {
        if (!trimmed.contains('.') && !trimmed.contains('e', ignoreCase = true)) {
            val asLong = trimmed.toLongOrNull()
            if (asLong != null) {
                writer.value(asLong)
                return
            }
        }
        writer.value(BigDecimal(trimmed))
    }

    fun validateRawJson(rawJson: String): Result<Any> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("JSON cannot be empty"))
        }
        return try {
            val tokener = JSONTokener(trimmed)
            val root = tokener.nextValue()
            while (tokener.more()) {
                val ch = tokener.next()
                if (!ch.isWhitespace()) {
                    return Result.failure(
                        JSONException("Unexpected trailing character '$ch' after root JSON value")
                    )
                }
            }
            if (root !is JSONObject && root !is JSONArray) {
                return Result.failure(
                    JSONException("Root JSON must be an Object { ... } or an Array [ ... ]")
                )
            }
            Result.success(root)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseRawJson(
        rawJson: String,
        existingForMetadata: JsonDocumentModel? = null
    ): Result<JsonDocumentModel> {
        val validated = validateRawJson(rawJson)
        val root = validated.getOrElse { return Result.failure(it) }

        val parsed = when (root) {
            is JSONObject -> {
                val objNode = jsonObjectToNode(root)
                JsonDocumentModel(
                    rootMode = RootMode.SINGLE_OBJECT,
                    rootObject = objNode
                )
            }
            is JSONArray -> {
                val allObjects = (0 until root.length()).all { root.get(it) is JSONObject }
                if (allObjects && root.length() > 0) {
                    val records = (0 until root.length()).map { idx ->
                        jsonObjectToNode(root.getJSONObject(idx))
                    }
                    JsonDocumentModel(
                        rootMode = RootMode.MULTI_RECORD,
                        records = records,
                        selectedRecordIndex = 0
                    )
                } else {
                    val arrayNode = jsonArrayToNode(root)
                    JsonDocumentModel(
                        rootMode = RootMode.GENERIC_ARRAY,
                        rootArray = arrayNode
                    )
                }
            }
            else -> return Result.failure(IllegalArgumentException("Unsupported JSON root"))
        }

        val merged = if (existingForMetadata != null) {
            mergeMetadata(parsed, existingForMetadata)
        } else {
            parsed
        }
        return Result.success(merged)
    }

    private fun jsonObjectToNode(jsonObj: JSONObject): JsonNode.ObjectNode {
        val entries = mutableListOf<JsonFieldEntry>()
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonToNode(jsonObj.get(key))
            entries.add(JsonFieldEntry(key = key, value = value))
        }
        return JsonNode.ObjectNode(fields = entries)
    }

    private fun jsonArrayToNode(jsonArray: JSONArray): JsonNode.ArrayNode {
        val elements = (0 until jsonArray.length()).map { i ->
            JsonArrayElement(value = jsonToNode(jsonArray.get(i)))
        }
        return JsonNode.ArrayNode(items = elements)
    }

    private fun jsonToNode(value: Any?): JsonNode {
        return when (value) {
            null, JSONObject.NULL -> JsonNode.Primitive(
                type = JsonFieldType.NULL,
                rawValue = "null",
                enforceType = true
            )
            is JSONObject -> jsonObjectToNode(value)
            is JSONArray -> jsonArrayToNode(value)
            is Boolean -> JsonNode.Primitive(
                type = JsonFieldType.BOOLEAN,
                rawValue = value.toString(),
                enforceType = true
            )
            is Number -> JsonNode.Primitive(
                type = JsonFieldType.NUMBER,
                rawValue = value.toString(),
                enforceType = true
            )
            else -> JsonNode.Primitive(
                type = JsonFieldType.STRING,
                rawValue = value.toString(),
                enforceType = true
            )
        }
    }

    private fun mergeMetadata(
        parsed: JsonDocumentModel,
        existing: JsonDocumentModel
    ): JsonDocumentModel {
        return when (parsed.rootMode) {
            RootMode.SINGLE_OBJECT -> parsed.copy(
                rootObject = mergeObjectMetadata(parsed.rootObject, existing.rootObject)
            )
            RootMode.MULTI_RECORD -> {
                val mergedRecords = parsed.records.mapIndexed { idx, rec ->
                    val existingRec = existing.records.getOrNull(idx) ?: existing.records.firstOrNull()
                    if (existingRec != null) mergeObjectMetadata(rec, existingRec) else rec
                }
                parsed.copy(
                    records = mergedRecords,
                    selectedRecordIndex = existing.selectedRecordIndex.coerceIn(0, (mergedRecords.size - 1).coerceAtLeast(0))
                )
            }
            RootMode.GENERIC_ARRAY -> parsed
        }
    }

    private fun mergeObjectMetadata(
        target: JsonNode.ObjectNode,
        reference: JsonNode.ObjectNode
    ): JsonNode.ObjectNode {
        val refMap = reference.fields.associateBy { it.key }
        val mergedFields = target.fields.map { entry ->
            val refEntry = refMap[entry.key]
            if (refEntry != null) {
                val newVal = when {
                    entry.value is JsonNode.Primitive && refEntry.value is JsonNode.Primitive -> {
                        entry.value.copy(enforceType = refEntry.value.enforceType)
                    }
                    entry.value is JsonNode.ObjectNode && refEntry.value is JsonNode.ObjectNode -> {
                        mergeObjectMetadata(entry.value, refEntry.value)
                    }
                    else -> entry.value
                }
                entry.copy(value = newVal)
            } else {
                entry
            }
        }
        return target.copy(fields = mergedFields)
    }

    // Tree navigation and immutable mutation helpers
    fun activeRootNode(document: JsonDocumentModel): JsonNode {
        return when (document.rootMode) {
            RootMode.SINGLE_OBJECT -> document.rootObject
            RootMode.MULTI_RECORD -> document.records.getOrElse(document.selectedRecordIndex) {
                document.records.firstOrNull() ?: JsonNode.ObjectNode()
            }
            RootMode.GENERIC_ARRAY -> document.rootArray
        }
    }

    fun resolveNodeAt(root: JsonNode, path: List<PathSegment>): JsonNode? {
        var current: JsonNode? = root
        for (segment in path) {
            current = when (segment) {
                is PathSegment.FieldSegment -> {
                    (current as? JsonNode.ObjectNode)
                        ?.fields
                        ?.firstOrNull { it.id == segment.entryId }
                        ?.value
                }
                is PathSegment.IndexSegment -> {
                    (current as? JsonNode.ArrayNode)
                        ?.items
                        ?.firstOrNull { it.id == segment.elementId }
                        ?.value
                }
            }
            if (current == null) return null
        }
        return current
    }

    fun updateNodeAt(
        root: JsonNode,
        path: List<PathSegment>,
        transform: (JsonNode) -> JsonNode
    ): JsonNode {
        if (path.isEmpty()) return transform(root)
        val head = path.first()
        val tail = path.drop(1)
        return when (head) {
            is PathSegment.FieldSegment -> {
                val obj = root as? JsonNode.ObjectNode ?: return root
                obj.copy(
                    fields = obj.fields.map { entry ->
                        if (entry.id == head.entryId) {
                            entry.copy(value = updateNodeAt(entry.value, tail, transform))
                        } else {
                            entry
                        }
                    }
                )
            }
            is PathSegment.IndexSegment -> {
                val arr = root as? JsonNode.ArrayNode ?: return root
                arr.copy(
                    items = arr.items.map { elem ->
                        if (elem.id == head.elementId) {
                            elem.copy(value = updateNodeAt(elem.value, tail, transform))
                        } else {
                            elem
                        }
                    }
                )
            }
        }
    }

    fun cloneStructureWithEmptyValues(node: JsonNode): JsonNode {
        return when (node) {
            is JsonNode.Primitive -> JsonNode.Primitive(
                id = UUID.randomUUID().toString(),
                type = node.type,
                rawValue = when (node.type) {
                    JsonFieldType.BOOLEAN -> "false"
                    JsonFieldType.NULL -> "null"
                    else -> ""
                },
                enforceType = node.enforceType
            )
            is JsonNode.ObjectNode -> JsonNode.ObjectNode(
                id = UUID.randomUUID().toString(),
                fields = node.fields.map { entry ->
                    JsonFieldEntry(
                        id = UUID.randomUUID().toString(),
                        key = entry.key,
                        value = cloneStructureWithEmptyValues(entry.value)
                    )
                }
            )
            is JsonNode.ArrayNode -> JsonNode.ArrayNode(
                id = UUID.randomUUID().toString(),
                items = emptyList()
            )
        }
    }
}

object ProjectCodec {
    fun encode(doc: JsonDocumentModel): String {
        val root = JSONObject()
        root.put("rootMode", doc.rootMode.name)
        root.put("selectedRecordIndex", doc.selectedRecordIndex)
        root.put("rootObject", encodeNode(doc.rootObject))
        val recs = JSONArray()
        doc.records.forEach { recs.put(encodeNode(it)) }
        root.put("records", recs)
        root.put("rootArray", encodeNode(doc.rootArray))
        return root.toString()
    }

    fun decode(encoded: String): JsonDocumentModel {
        val root = JSONObject(encoded)
        val mode = runCatching { RootMode.valueOf(root.optString("rootMode")) }
            .getOrDefault(RootMode.SINGLE_OBJECT)
        val selectedIndex = root.optInt("selectedRecordIndex", 0)
        val rootObject = (decodeNode(root.getJSONObject("rootObject")) as? JsonNode.ObjectNode)
            ?: JsonNode.ObjectNode()
        val recsJson = root.optJSONArray("records") ?: JSONArray()
        val records = (0 until recsJson.length()).mapNotNull { i ->
            decodeNode(recsJson.getJSONObject(i)) as? JsonNode.ObjectNode
        }.ifEmpty { listOf(JsonNode.ObjectNode()) }
        val rootArray = root.optJSONObject("rootArray")?.let {
            decodeNode(it) as? JsonNode.ArrayNode
        } ?: JsonNode.ArrayNode()
        return JsonDocumentModel(
            rootMode = mode,
            rootObject = rootObject,
            records = records,
            selectedRecordIndex = selectedIndex.coerceIn(0, records.lastIndex),
            rootArray = rootArray
        )
    }

    private fun encodeNode(node: JsonNode): JSONObject {
        val obj = JSONObject()
        obj.put("id", node.id)
        when (node) {
            is JsonNode.Primitive -> {
                obj.put("kind", "primitive")
                obj.put("type", node.type.name)
                obj.put("rawValue", node.rawValue)
                obj.put("enforceType", node.enforceType)
            }
            is JsonNode.ObjectNode -> {
                obj.put("kind", "object")
                val fieldsArr = JSONArray()
                node.fields.forEach { entry ->
                    val entryObj = JSONObject()
                    entryObj.put("id", entry.id)
                    entryObj.put("key", entry.key)
                    entryObj.put("value", encodeNode(entry.value))
                    fieldsArr.put(entryObj)
                }
                obj.put("fields", fieldsArr)
            }
            is JsonNode.ArrayNode -> {
                obj.put("kind", "array")
                val itemsArr = JSONArray()
                node.items.forEach { item ->
                    val itemObj = JSONObject()
                    itemObj.put("id", item.id)
                    itemObj.put("value", encodeNode(item.value))
                    itemsArr.put(itemObj)
                }
                obj.put("items", itemsArr)
            }
        }
        return obj
    }

    private fun decodeNode(obj: JSONObject): JsonNode {
        val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
        return when (obj.optString("kind")) {
            "object" -> {
                val arr = obj.optJSONArray("fields") ?: JSONArray()
                val fields = (0 until arr.length()).map { i ->
                    val f = arr.getJSONObject(i)
                    JsonFieldEntry(
                        id = f.optString("id").ifBlank { UUID.randomUUID().toString() },
                        key = f.optString("key", ""),
                        value = decodeNode(f.getJSONObject("value"))
                    )
                }
                JsonNode.ObjectNode(id = id, fields = fields)
            }
            "array" -> {
                val arr = obj.optJSONArray("items") ?: JSONArray()
                val items = (0 until arr.length()).map { i ->
                    val itObj = arr.getJSONObject(i)
                    JsonArrayElement(
                        id = itObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        value = decodeNode(itObj.getJSONObject("value"))
                    )
                }
                JsonNode.ArrayNode(id = id, items = items)
            }
            else -> {
                val type = runCatching { JsonFieldType.valueOf(obj.optString("type")) }
                    .getOrDefault(JsonFieldType.STRING)
                JsonNode.Primitive(
                    id = id,
                    type = type,
                    rawValue = obj.optString("rawValue", ""),
                    enforceType = obj.optBoolean("enforceType", true)
                )
            }
        }
    }
}

object JsonSyntaxHighlighter {
    fun highlight(json: String, colors: JsonSyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val n = json.length
            while (i < n) {
                val ch = json[i]
                when {
                    ch == '"' -> {
                        val end = findStringEnd(json, i)
                        val token = json.substring(i, end)
                        // Look ahead past whitespace to see if next char is ':' -> key!
                        var j = end
                        while (j < n && json[j].isWhitespace()) j++
                        val isKey = j < n && json[j] == ':'
                        val style = if (isKey) {
                            SpanStyle(color = colors.key, fontWeight = FontWeight.Medium)
                        } else {
                            SpanStyle(color = colors.stringValue)
                        }
                        withStyle(style) { append(token) }
                        i = end
                    }
                    ch == '-' || ch.isDigit() -> {
                        val start = i
                        while (i < n && (json[i].isDigit() || json[i] in ".eE+-")) {
                            i++
                        }
                        withStyle(SpanStyle(color = colors.numberValue)) {
                            append(json.substring(start, i))
                        }
                    }
                    json.startsWith("true", i) && isWordBoundary(json, i + 4) -> {
                        withStyle(SpanStyle(color = colors.booleanValue, fontWeight = FontWeight.SemiBold)) {
                            append("true")
                        }
                        i += 4
                    }
                    json.startsWith("false", i) && isWordBoundary(json, i + 5) -> {
                        withStyle(SpanStyle(color = colors.booleanValue, fontWeight = FontWeight.SemiBold)) {
                            append("false")
                        }
                        i += 5
                    }
                    json.startsWith("null", i) && isWordBoundary(json, i + 4) -> {
                        withStyle(SpanStyle(color = colors.nullValue, fontStyle = FontStyle.Italic)) {
                            append("null")
                        }
                        i += 4
                    }
                    ch in "{}[]:," -> {
                        withStyle(SpanStyle(color = colors.punctuation, fontWeight = FontWeight.SemiBold)) {
                            append(ch)
                        }
                        i++
                    }
                    else -> {
                        append(ch)
                        i++
                    }
                }
            }
        }
    }

    private fun findStringEnd(json: String, startQuote: Int): Int {
        var i = startQuote + 1
        while (i < json.length) {
            val c = json[i]
            if (c == '\\') {
                i += 2
            } else if (c == '"') {
                return i + 1
            } else {
                i++
            }
        }
        return json.length
    }

    private fun isWordBoundary(json: String, idx: Int): Boolean {
        return idx >= json.length || (!json[idx].isLetterOrDigit() && json[idx] != '_')
    }
}
