package com.example.data.repository

import com.example.data.local.JsonProjectDao
import com.example.data.local.JsonProjectEntity
import com.example.json.JsonEngine
import com.example.json.ProjectCodec
import com.example.model.JsonDocumentModel
import com.example.model.RootMode
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProjectRepository(private val dao: JsonProjectDao) {
    val projects: Flow<List<JsonProjectEntity>> = dao.observeAllProjects()

    suspend fun getById(id: String): JsonProjectEntity? = dao.getProjectById(id)

    suspend fun saveProject(
        id: String,
        name: String,
        document: JsonDocumentModel
    ): JsonProjectEntity {
        val serialized = JsonEngine.serialize(document, indentSpaces = 2).formattedJson
        val snippet = serialized.take(180).replace('\n', ' ').trim()
        val recordCount = when (document.rootMode) {
            RootMode.SINGLE_OBJECT -> 1
            RootMode.MULTI_RECORD -> document.records.size
            RootMode.GENERIC_ARRAY -> document.rootArray.items.size
        }
        val fieldCount = when (document.rootMode) {
            RootMode.SINGLE_OBJECT -> document.rootObject.fields.size
            RootMode.MULTI_RECORD -> document.records.firstOrNull()?.fields?.size ?: 0
            RootMode.GENERIC_ARRAY -> document.rootArray.items.size
        }
        val entity = JsonProjectEntity(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name = name.ifBlank { "Untitled JSON" },
            rootMode = document.rootMode.name,
            recordCount = recordCount,
            fieldCount = fieldCount,
            projectStateJson = ProjectCodec.encode(document),
            previewSnippet = snippet,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertProject(entity)
        return entity
    }

    suspend fun deleteProject(id: String) = dao.deleteProjectById(id)
}
