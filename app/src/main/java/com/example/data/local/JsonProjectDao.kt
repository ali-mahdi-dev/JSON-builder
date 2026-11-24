package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JsonProjectDao {
    @Query("SELECT * FROM json_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<JsonProjectEntity>>

    @Query("SELECT * FROM json_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): JsonProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: JsonProjectEntity)

    @Query("DELETE FROM json_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
