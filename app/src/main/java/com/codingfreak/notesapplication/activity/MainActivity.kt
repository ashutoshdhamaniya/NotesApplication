package com.codingfreak.notesapplication.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.codingfreak.notesapplication.R
import com.codingfreak.notesapplication.adapters.NoteAdapter
import com.codingfreak.notesapplication.database.NotesDatabase
import com.codingfreak.notesapplication.entity.Note
import com.codingfreak.notesapplication.listeners.NoteListeners
import java.io.Serializable
import java.lang.Exception

class MainActivity : AppCompatActivity() , NoteListeners {

    private val REQUEST_CODE_ADD_NOTE : Int = 1
    private val REQUEST_CODE_UPDATE_NOTE : Int = 2
    private val REQUEST_CODE_SHOW_NOTE : Int = 3
    private val REQUEST_CODE_SELECT_IMAGE : Int = 4
    private val REQUEST_CODE_STORAGE_PERMISSION : Int = 5

    private lateinit var addNoteMainButton : ImageView
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var noteList : ArrayList<Note>
    private lateinit var noteAdapter: NoteAdapter

    private lateinit var searchNote : EditText

    private var noteClickedPosition : Int = -1

    private var dialogAddUrl : AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addNoteMainButton = findViewById(R.id.addNoteMainButtton)

        addNoteMainButton.setOnClickListener {
            startActivityForResult(
                Intent(this , CreateNoteActivity::class.java) ,
                REQUEST_CODE_ADD_NOTE
            )
        }

        notesRecyclerView = findViewById(R.id.allNotesRecyclerView)
        notesRecyclerView.layoutManager = StaggeredGridLayoutManager(2 , StaggeredGridLayoutManager.VERTICAL)

        noteList = ArrayList<Note>()
        noteAdapter = NoteAdapter(noteList , this)
        notesRecyclerView.adapter = noteAdapter

        getAllNotes(REQUEST_CODE_SHOW_NOTE , false)

        searchNote = findViewById(R.id.searchNotesEditText)
        searchNote.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                noteAdapter.cancelTimer()
            }

            override fun afterTextChanged(p0: Editable?) {
                if(noteList.size != 0) {
                    noteAdapter.searchNotes(p0.toString())
                }
            }

        })

        findViewById<ImageView>(R.id.quickAddNoteImageView).setOnClickListener {
            startActivityForResult(
                Intent(this , CreateNoteActivity::class.java) ,
                REQUEST_CODE_ADD_NOTE
            )
        }

        findViewById<ImageView>(R.id.quickAddPictureImageView).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {
                selectImage()
            }
        }

        findViewById<ImageView>(R.id.quickAddWebLinkImageView).setOnClickListener {
            showAddUrlDialog()
        }
    }

    private fun selectImage() {
        val intent: Intent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty()) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage()
            } else {
                Toast.makeText(this , "Permission Denied" , Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPathFromUri(contentUri: Uri): String {
        var filePath: String

        val cursor: Cursor? = contentResolver.query(contentUri, null, null, null, null)

        if (cursor == null) {
            filePath = contentUri.path.toString()
        } else {
            cursor.moveToFirst()
            val index: Int = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }

        return filePath
    }

    private fun getAllNotes(requestCode: Int , isNoteDeleted : Boolean) {
        class GetNotesTask : AsyncTask<Void, Void, List<Note>>() {
            override fun doInBackground(vararg p0: Void?): List<Note> {
                return NotesDatabase.getDatabase(this@MainActivity).noteDao.getAllNotes()
            }

            override fun onPostExecute(notes: List<Note>) {
                super.onPostExecute(notes)

                if(requestCode == REQUEST_CODE_SHOW_NOTE){
                    noteList.addAll(notes)
                    noteAdapter.notifyDataSetChanged()
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0 , notes[0])
                    noteAdapter.notifyItemInserted(0)
                    notesRecyclerView.smoothScrollToPosition(0)
                } else if(requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.removeAt(noteClickedPosition)

                    if(isNoteDeleted) {
                        noteAdapter.notifyItemRemoved(noteClickedPosition)
                    } else {
                        noteList.add(noteClickedPosition , notes[noteClickedPosition])
                        noteAdapter.notifyItemChanged(noteClickedPosition)
                    }
                }

//                if(noteList.isEmpty()) {
//                    noteList.addAll(notes)
//                    noteAdapter.notifyDataSetChanged()
//                } else {
//                    noteList.add(0 , notes[0])
//                    noteAdapter.notifyDataSetChanged()
//                }
//                notesRecyclerView.smoothScrollToPosition(0)
            }

        }

        GetNotesTask().execute()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getAllNotes(REQUEST_CODE_ADD_NOTE , false)
        } else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if(data != null){
                getAllNotes(REQUEST_CODE_UPDATE_NOTE , data.getBooleanExtra("isNoteDeleted" , false))
            }
        } else if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if(data != null) {
                val selectedImageUri : Uri? = data.data

                if(selectedImageUri != null) {
                    try {
                        val selectedImagePath : String = getPathFromUri(selectedImageUri)
                        val intent : Intent = Intent(applicationContext , CreateNoteActivity::class.java)
                        intent.putExtra("isFromQuickActions" , true)
                        intent.putExtra("quickActionType" , "image")
                        intent.putExtra("imagePath" , selectedImagePath)

                        startActivityForResult(intent , REQUEST_CODE_ADD_NOTE)

                    } catch (e : Exception) {
                        Toast.makeText(this , "Some Error Occurred" , Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onNoteClick(note : Note, position : Int) {
        noteClickedPosition = position
        val intent : Intent = Intent(this , CreateNoteActivity::class.java)
        intent.putExtra("isViewOrUpdate" , true)
        intent.putExtra("note" , note)
        startActivityForResult(intent , REQUEST_CODE_UPDATE_NOTE)
    }

    private fun showAddUrlDialog() {
        if (dialogAddUrl == null) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val view: View = LayoutInflater.from(this).inflate(
                R.layout.layout_add_url,
                findViewById<ViewGroup>(R.id.layoutAddUrlContainer)
            )
            builder.setView(view)

            dialogAddUrl = builder.create()

            if (dialogAddUrl!!.window != null) {
                dialogAddUrl!!.window?.setBackgroundDrawable(ColorDrawable(0))
            }

            val inputUrl: EditText = view.findViewById(R.id.inputUrl)
            inputUrl.requestFocus()

            view.findViewById<TextView>(R.id.textAddUrl).setOnClickListener {
                if (inputUrl.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Enter Url", Toast.LENGTH_SHORT).show()
                } else if (Patterns.WEB_URL.matcher(inputUrl.text.toString()).matches()) {
                    Toast.makeText(this, "Enter a Valid Url", Toast.LENGTH_SHORT).show()
                } else {
                    dialogAddUrl!!.dismiss()
                    val intent : Intent = Intent(applicationContext , CreateNoteActivity::class.java)
                    intent.putExtra("isFromQuickActions" , true)
                    intent.putExtra("quickActionType" , "URL")
                    intent.putExtra("URL" , inputUrl.text.toString())
                }
            }

            view.findViewById<TextView>(R.id.textCancelUrl).setOnClickListener {
                dialogAddUrl!!.dismiss()
            }
        }

        dialogAddUrl!!.show()
    }

}