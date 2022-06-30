package com.example.audiomemo

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "recordings")
class RecordingModel() : Parcelable {
    @PrimaryKey(autoGenerate = true) var id: Int? = null
    @ColumnInfo(name = "name") var name: String? = null
    @ColumnInfo(name = "text") var text: String? = null
    @ColumnInfo(name = "created_at") var created_at: Long? = null
    @ColumnInfo(name = "updated_at") var updated_at: Long? = null
    @ColumnInfo(name = "path") var path: String? = null
    @ColumnInfo(name = "importance") var importance: Int = 0

    var createdAtFormatted: String? = null
    var updatedAtFormatted: String? = null

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
        text = parcel.readString()
        created_at = parcel.readLong()
        updated_at = parcel.readLong()
        path = parcel.readString()
        importance = parcel.readInt()
    }

    constructor(path: String) : this() {
//        this.name = path.split("/").last()
        this.name = "Long press to edit"
        this.text = "Long press to edit"
        this.created_at = Calendar.getInstance().timeInMillis
        this.updated_at = this.created_at
        this.path = path
        this.importance = 0

        this.createdAtFormatted = SimpleDateFormat("dd,MM,yyyy HH:mm:s").format(this.created_at)
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(text)
        parcel.writeLong(created_at!!)
        parcel.writeLong(updated_at!!)
        parcel.writeString(path)
        parcel.writeInt(importance!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecordingModel> {
        override fun createFromParcel(parcel: Parcel): RecordingModel {
            return RecordingModel(parcel)
        }

        override fun newArray(size: Int): Array<RecordingModel?> {
            return arrayOfNulls(size)
        }
    }
}

@Dao
interface RecordingModelDao {
    @Query("SELECT * FROM recordings ORDER BY created_at DESC")
    fun getAll(): List<RecordingModel>
    @Query("SELECT * FROM recordings ORDER BY created_at DESC LIMIT 1")
    fun getLast(): RecordingModel
    @Insert
    fun insert(vararg recording: RecordingModel)
    @Delete
    fun delete(recording: RecordingModel)
    @Update
    fun update(vararg recording: RecordingModel)
}
