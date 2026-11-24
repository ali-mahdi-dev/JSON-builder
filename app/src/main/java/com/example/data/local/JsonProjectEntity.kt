package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "json_projects")
data class JsonProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rootMode: String,
    val recordCount: Int,
    val fieldCount: Int,
    val projectStateJson: String,
    val previewSnippet: String,
    val updatedAt: Long
)
