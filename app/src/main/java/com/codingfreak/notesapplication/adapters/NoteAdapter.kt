package com.codingfreak.notesapplication.adapters

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codingfreak.notesapplication.R
import com.codingfreak.notesapplication.entity.Note
import com.codingfreak.notesapplication.listeners.NoteListeners
import com.makeramen.roundedimageview.RoundedImageView
import java.util.*
import kotlin.collections.ArrayList

class NoteAdapter(private var notes: List<Note>, private val noteListeners: NoteListeners) :
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var timer: Timer? = null
    private val searchNotes: List<Note> = notes

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.containerNoteTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.containerNoteSubtitle)
        private val dateTime: TextView = itemView.findViewById(R.id.containerDateTime)
        val layoutNote: LinearLayout = itemView.findViewById(R.id.containerLayoutNote)
        private val imageNote: RoundedImageView = itemView.findViewById(R.id.imageNote)

        fun setNote(note: Note) {
            title.text = note.noteTitle

            if (note.noteSubtitle.toString().trim().isEmpty()) {
                subtitle.visibility = View.GONE
            } else {
                subtitle.text = note.noteSubtitle
            }

            dateTime.text = note.dateTime
            val gradientDrawable: GradientDrawable = layoutNote.background as GradientDrawable
            if (note.noteColor != null) {
                gradientDrawable.setColor(Color.parseColor(note.noteColor))
            } else {
                gradientDrawable.setColor(Color.parseColor("#48000000"))
            }

            if (note.imagePath != null) {
                imageNote.setImageBitmap(BitmapFactory.decodeFile(note.imagePath))
                imageNote.visibility = View.VISIBLE
            } else {
                imageNote.visibility = View.GONE
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.note_item_container, parent, false)

        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.setNote(notes[position])
        holder.layoutNote.setOnClickListener {
            noteListeners.onNoteClick(notes[position], position)
        }
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    public fun searchNotes(keyword: String) {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                if (keyword.trim().isEmpty()) {
                    notes = searchNotes
                } else {
                    var tempList: ArrayList<Note> = ArrayList()
                    for (note: Note in searchNotes) {
                        if (note.noteTitle.toLowerCase().contains(keyword.toLowerCase())
                            || note.noteSubtitle.toLowerCase().contains(keyword.toLowerCase())
                            || note.noteText.toLowerCase().contains(keyword.toLowerCase())
                        ) {
                            tempList.add(note)
                        }
                    }

                    notes = tempList
                }

                Handler(Looper.getMainLooper()).post {
                    notifyDataSetChanged()
                }

            }
        }, 500)
    }

    public fun cancelTimer() {
        if(timer != null) {
            timer!!.cancel()
        }
    }

}