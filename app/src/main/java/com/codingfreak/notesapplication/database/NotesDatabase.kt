package com.codingfreak.notesapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codingfreak.notesapplication.dao.NoteDao
import com.codingfreak.notesapplication.entity.Note

@Database(entities = [Note::class], version = 1)
abstract class NotesDatabase : RoomDatabase() {

    companion object {
        private lateinit var notesDatabase : NotesDatabase

        fun getDatabase(context: Context): NotesDatabase {
                notesDatabase = Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "notes-db"
                ).build()
            
            return notesDatabase
        }
    }

    abstract val noteDao : NoteDao

}