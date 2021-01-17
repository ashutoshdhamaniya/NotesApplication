package com.codingfreak.notesapplication.listeners

import com.codingfreak.notesapplication.entity.Note
import java.text.ParsePosition

interface NoteListeners {
    fun onNoteClick(note : Note, position: Int)
}