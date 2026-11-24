package com.example.model

import java.util.UUID

enum class JsonFieldType(val label: String, val shortLabel: String) {
    STRING("String", "ABC"),
    NUMBER("Number", "123"),
    BOOLEAN("Boolean", "T/F"),
    NULL("Null", "NULL"),
    OBJECT("Object", "{ }"),
    ARRAY("Array", "[ ]")
}

enum class RootMode(val label: String) {
    SINGLE_OBJECT("Single Object"),
    MULTI_RECORD("Multiple Records"),
    GENERIC_ARRAY("Array")
}

sealed interface JsonNode {
    val id: String

    data class Primitive(
        override val id: String = UUID.randomUUID().toString(),
        val type: JsonFieldType = JsonFieldType.STRING,
        val rawValue: String = "",
        val enforceType: Boolean = true
    ) : JsonNode

    data class ObjectNode(
        override val id: String = UUID.randomUUID().toString(),
        val fields: List<JsonFieldEntry> = emptyList()
    ) : JsonNode

    data class ArrayNode(
        override val id: String = UUID.randomUUID().toString(),
        val items: List<JsonArrayElement> = emptyList()
    ) : JsonNode
}

data class JsonFieldEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: JsonNode
)

data class JsonArrayElement(
    val id: String = UUID.randomUUID().toString(),
    val value: JsonNode
)

sealed interface PathSegment {
    data class FieldSegment(val entryId: String, val key: String) : PathSegment
    data class IndexSegment(val elementId: String, val index: Int) : PathSegment
}

data class ValidationIssue(
    val nodeId: String,
    val path: String,
    val message: String
)

data class JsonDocumentModel(
    val rootMode: RootMode = RootMode.SINGLE_OBJECT,
    val rootObject: JsonNode.ObjectNode = JsonNode.ObjectNode(),
    val records: List<JsonNode.ObjectNode> = listOf(JsonNode.ObjectNode()),
    val selectedRecordIndex: Int = 0,
    val rootArray: JsonNode.ArrayNode = JsonNode.ArrayNode()
)

data class ProjectTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val modeLabel: String,
    val createDocument: () -> JsonDocumentModel
)

object StarterTemplates {
    fun blankObject(): JsonDocumentModel = JsonDocumentModel(
        rootMode = RootMode.SINGLE_OBJECT,
        rootObject = JsonNode.ObjectNode(
            fields = listOf(
                JsonFieldEntry(
                    key = "name",
                    value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "", enforceType = true)
                )
            )
        )
    )

    fun userDatabaseMultiRecord(): JsonDocumentModel = JsonDocumentModel(
        rootMode = RootMode.MULTI_RECORD,
        records = listOf(
            JsonNode.ObjectNode(
                fields = listOf(
                    JsonFieldEntry(key = "name", value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "Alice", enforceType = true)),
                    JsonFieldEntry(key = "age", value = JsonNode.Primitive(type = JsonFieldType.NUMBER, rawValue = "22", enforceType = true)),
                    JsonFieldEntry(key = "email", value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "alice@example.com", enforceType = true)),
                    JsonFieldEntry(key = "isActive", value = JsonNode.Primitive(type = JsonFieldType.BOOLEAN, rawValue = "true", enforceType = true))
                )
            ),
            JsonNode.ObjectNode(
                fields = listOf(
                    JsonFieldEntry(key = "name", value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "Jordan", enforceType = true)),
                    JsonFieldEntry(key = "age", value = JsonNode.Primitive(type = JsonFieldType.NUMBER, rawValue = "25", enforceType = true)),
                    JsonFieldEntry(key = "email", value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "jordan@example.com", enforceType = true)),
                    JsonFieldEntry(key = "isActive", value = JsonNode.Primitive(type = JsonFieldType.BOOLEAN, rawValue = "false", enforceType = true))
                )
            )
        ),
        selectedRecordIndex = 0
    )

    fun nestedProfile(): JsonDocumentModel = JsonDocumentModel(
        rootMode = RootMode.SINGLE_OBJECT,
        rootObject = JsonNode.ObjectNode(
            fields = listOf(
                JsonFieldEntry(
                    key = "user",
                    value = JsonNode.ObjectNode(
                        fields = listOf(
                            JsonFieldEntry(key = "name", value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "Alice", enforceType = true)),
                            JsonFieldEntry(
                                key = "contact",
                                value = JsonNode.ObjectNode(
                                    fields = listOf(
                                        JsonFieldEntry(key = "email", value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "alice@example.com", enforceType = true))
                                    )
                                )
                            )
                        )
                    )
                ),
                JsonFieldEntry(
                    key = "roles",
                    value = JsonNode.ArrayNode(
                        items = listOf(
                            JsonArrayElement(value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "admin", enforceType = true)),
                            JsonArrayElement(value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "editor", enforceType = true))
                        )
                    )
                )
            )
        )
    )

    val all: List<ProjectTemplate> = listOf(
        ProjectTemplate(
            id = "user_db",
            title = "User Database",
            subtitle = "Multi-record array with name, age, email, and isActive",
            modeLabel = "Multi-Record [ ]",
            createDocument = ::userDatabaseMultiRecord
        ),
        ProjectTemplate(
            id = "nested_profile",
            title = "Nested Profile",
            subtitle = "Nested user & contact objects plus a roles array",
            modeLabel = "Nested { }",
            createDocument = ::nestedProfile
        ),
        ProjectTemplate(
            id = "blank_object",
            title = "Blank Object",
            subtitle = "Start clean with an empty structured JSON object",
            modeLabel = "Object { }",
            createDocument = ::blankObject
        )
    )
}
