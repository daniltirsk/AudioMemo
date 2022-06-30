package com.example.audiomemo

import androidx.room.*


@Entity(tableName = "keys")
data class ApiKey(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "key") var key: String
    )

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM keys")
    fun getAll(): List<ApiKey>
    @Query("SELECT * FROM keys LIMIT 1")
    fun getLast(): ApiKey
    @Insert
    fun insert(vararg key: ApiKey)
    @Delete
    fun delete(key: ApiKey)
    @Update
    fun update(vararg key: ApiKey)
}
