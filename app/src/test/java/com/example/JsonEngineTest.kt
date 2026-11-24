package com.example

import com.example.json.JsonEngine
import com.example.json.JsonSyntaxHighlighter
import com.example.model.JsonFieldEntry
import com.example.model.JsonFieldType
import com.example.model.JsonNode
import com.example.model.PathSegment
import com.example.model.RootMode
import com.example.model.StarterTemplates
import com.example.ui.theme.LightJsonSyntaxColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonEngineTest {

    @Test
    fun isValidJsonNumber_validatesStrictJsonNumbers() {
        assertTrue(JsonEngine.isValidJsonNumber("25"))
        assertTrue(JsonEngine.isValidJsonNumber("-3.14"))
        assertTrue(JsonEngine.isValidJsonNumber("0"))
        assertTrue(JsonEngine.isValidJsonNumber("1.5e10"))
        assertFalse(JsonEngine.isValidJsonNumber("01"))
        assertFalse(JsonEngine.isValidJsonNumber("abc"))
        assertFalse(JsonEngine.isValidJsonNumber(""))
        assertFalse(JsonEngine.isValidJsonNumber("25px"))
    }

    @Test
    fun nestedTreeHelpers_resolveUpdateAndClone() {
        val doc = StarterTemplates.nestedProfile()
        val userEntry = doc.rootObject.fields.first { it.key == "user" }
        val userObj = userEntry.value as JsonNode.ObjectNode
        val contactEntry = userObj.fields.first { it.key == "contact" }

        val path = listOf(
            PathSegment.FieldSegment(userEntry.id, "user"),
            PathSegment.FieldSegment(contactEntry.id, "contact")
        )
        val resolved = JsonEngine.resolveNodeAt(doc.rootObject, path) as? JsonNode.ObjectNode
        assertNotNull(resolved)
        assertEquals("alice@example.com", (resolved!!.fields.first().value as JsonNode.Primitive).rawValue)

        val updatedRoot = JsonEngine.updateNodeAt(doc.rootObject, path) { node ->
            val obj = node as JsonNode.ObjectNode
            obj.copy(
                fields = obj.fields + JsonFieldEntry(
                    key = "phone",
                    value = JsonNode.Primitive(type = JsonFieldType.STRING, rawValue = "12345", enforceType = true)
                )
            )
        } as JsonNode.ObjectNode

        val updatedContact = JsonEngine.resolveNodeAt(updatedRoot, path) as JsonNode.ObjectNode
        assertEquals(2, updatedContact.fields.size)

        val cloned = JsonEngine.cloneStructureWithEmptyValues(doc.rootObject) as JsonNode.ObjectNode
        val clonedUser = cloned.fields.first { it.key == "user" }.value as JsonNode.ObjectNode
        val clonedName = clonedUser.fields.first { it.key == "name" }.value as JsonNode.Primitive
        assertEquals("", clonedName.rawValue)
    }

    @Test
    fun multiRecordStarter_hasTwoConsistentRecords() {
        val multi = StarterTemplates.userDatabaseMultiRecord()
        assertEquals(RootMode.MULTI_RECORD, multi.rootMode)
        assertEquals(2, multi.records.size)
        assertEquals(4, multi.records[0].fields.size)
        assertEquals(4, multi.records[1].fields.size)
    }

    @Test
    fun syntaxHighlighter_stylesKeysStringsNumbersBooleansAndNull() {
        val sample = """{"name": "Ali", "age": 22, "active": true, "extra": null}"""
        val annotated = JsonSyntaxHighlighter.highlight(sample, LightJsonSyntaxColors)
        assertEquals(sample, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
