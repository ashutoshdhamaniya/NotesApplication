package com.codingfreak.notesapplication.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")
class Note (
    @ColumnInfo(name = "dateTime")
    val dateTime : String,

    @ColumnInfo(name = "noteTitle")
    val noteTitle : String,

    @ColumnInfo(name = "noteSubtitle")
    val noteSubtitle : String,

    @ColumnInfo(name = "noteText")
    val noteText : String,

    @ColumnInfo(name = "noteColor")
    val noteColor : String?,

    @ColumnInfo(name = "imagePath")
    val imagePath : String?,

    @ColumnInfo(name = "webLink")
    val webLink : String?



) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id : Int = 0
}