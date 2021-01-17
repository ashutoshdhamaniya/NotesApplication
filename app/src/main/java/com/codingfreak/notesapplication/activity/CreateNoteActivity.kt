package com.codingfreak.notesapplication.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.Image
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codingfreak.notesapplication.R
import com.codingfreak.notesapplication.database.NotesDatabase
import com.codingfreak.notesapplication.entity.Note
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.InputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class CreateNoteActivity : AppCompatActivity() {

    private lateinit var backButton: RelativeLayout
    private lateinit var noteTitle: EditText
    private lateinit var noteSubtitle: EditText
    private lateinit var noteText: EditText
    private lateinit var noteDateTime: TextView
    private lateinit var saveNote: ImageView
    private lateinit var subtitleIndicatorColor: View
    private lateinit var imageNote: ImageView
    private lateinit var textWebUrl: TextView
    private lateinit var webLayout: LinearLayout

    private lateinit var selectedColor: String
    private lateinit var selectedImagePath: String
    private val REQUEST_CODE_STORAGE_PERMISSION = 1
    private val REQUEST_CODE_SELECT_IAMGE = 2

    private var dialogAddUrl: AlertDialog? = null
    private var dialogDeleteNode: AlertDialog? = null

    private var alreadyAvailableNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        // Initialize Fields
        backButton = findViewById(R.id.backButton)
        noteTitle = findViewById(R.id.noteTitle)
        noteSubtitle = findViewById(R.id.noteSubtitle)
        noteText = findViewById(R.id.noteText)
        noteDateTime = findViewById(R.id.textDateTime)
        saveNote = findViewById(R.id.saveNoteButton)
        subtitleIndicatorColor = findViewById(R.id.subtitleIndicator)
        imageNote = findViewById(R.id.imageNote)
        textWebUrl = findViewById(R.id.textWebUrl)
        webLayout = findViewById(R.id.textWebLayout)

        backButton.setOnClickListener {
            onBackPressed()
        }

        noteDateTime.text =
            SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(
                Date()
            )

        saveNote.setOnClickListener {
            saveNote()
        }

        selectedImagePath = ""

        if (intent.getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = intent.getSerializableExtra("note") as Note
            setViewOrUpdateNote()
        }

        findViewById<ImageView>(R.id.imageRemoveWebLink).setOnClickListener {
            textWebUrl.setText(null)
            webLayout.visibility = View.GONE
        }

        findViewById<ImageView>(R.id.imageRemoveImage).setOnClickListener {
            imageNote.setImageBitmap(null)
            imageNote.visibility = View.GONE
            findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.INVISIBLE
            selectedImagePath = ""
        }

        if(intent.getBooleanExtra("isFromQuickActions" , false)) {
            val type : String? = intent.getStringExtra("quickActionType")

            if(type != null) {
                if(type == "image") {
                    selectedImagePath = intent.getStringExtra("imagePath").toString()
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath))
                    imageNote.visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.VISIBLE
                } else if(type == "URL") {
                    textWebUrl.text = intent.getStringExtra("URL")
                    webLayout.visibility = View.VISIBLE
                }
            }
        }

        selectedColor = "#48000000"
        setSubtitleIndicator()
        initNoteColor()
    }

    private fun setViewOrUpdateNote() {
        noteTitle.setText(alreadyAvailableNote?.noteTitle)
        noteSubtitle.setText(alreadyAvailableNote?.noteSubtitle)
        noteDateTime.text = alreadyAvailableNote?.dateTime
        noteText.setText(alreadyAvailableNote?.noteText)

        if (alreadyAvailableNote?.imagePath != null && alreadyAvailableNote?.imagePath.toString()
                .trim()
                .isNotEmpty()
        ) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote?.imagePath))
            imageNote.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.VISIBLE
            selectedImagePath = alreadyAvailableNote?.imagePath.toString()
        }

        if (alreadyAvailableNote?.webLink != null && alreadyAvailableNote?.webLink.toString()
                .isNotEmpty()
        ) {
            textWebUrl.setText(alreadyAvailableNote?.webLink)
            webLayout.visibility = View.VISIBLE
        }
    }

    private fun saveNote() {
        if (noteTitle.text.trim().toString().isEmpty()) {
            Toast.makeText(this, "Note Title Please", Toast.LENGTH_SHORT).show()
            return
        } else if (noteSubtitle.text.trim().toString().isEmpty() || noteText.text.trim().toString()
                .isEmpty()
        ) {
            Toast.makeText(this, "Note Can't Be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val title: String = noteTitle.text.toString()
        val subtitle: String = noteSubtitle.text.toString()
        val text: String = noteText.text.toString()
        val dateTime: String = noteDateTime.text.toString()

        var webLink: String = ""
        if (webLayout.visibility == View.VISIBLE) {
            webLink = textWebUrl.text.toString()
        }

        val note: Note =
            Note(dateTime, title, subtitle, text, selectedColor, selectedImagePath, webLink)

        if (alreadyAvailableNote != null) {
            note.id = alreadyAvailableNote?.id!!
        }

        class SaveNoteTask() : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg p0: Void?): Void? {
                NotesDatabase.getDatabase(this@CreateNoteActivity).noteDao.insertNote(note = note)
                return null
            }

            override fun onPostExecute(result: Void?) {
                super.onPostExecute(result)
                val intent: Intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }

        }

        SaveNoteTask().execute()
    }

    private fun initNoteColor() {
        val linearLayout: LinearLayout = findViewById(R.id.noteColorLinearLayout)
        val bottomSheetBehavior: BottomSheetBehavior<LinearLayout> =
            BottomSheetBehavior.from(linearLayout)
        linearLayout.findViewById<TextView>(R.id.noteColorText).setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        val imageView1: ImageView = linearLayout.findViewById(R.id.imageView1)
        val imageView2: ImageView = linearLayout.findViewById(R.id.imageView2)
        val imageView3: ImageView = linearLayout.findViewById(R.id.imageView3)
        val imageView4: ImageView = linearLayout.findViewById(R.id.imageView4)
        val imageView5: ImageView = linearLayout.findViewById(R.id.imageView5)
        val imageView6: ImageView = linearLayout.findViewById(R.id.imageView6)

        linearLayout.findViewById<View>(R.id.viewColor1).setOnClickListener {
            selectedColor = "#48000000"
            imageView1.setImageResource(R.drawable.ic_baseline_done_24)
            imageView2.setImageResource(0)
            imageView3.setImageResource(0)
            imageView4.setImageResource(0)
            imageView5.setImageResource(0)
            imageView6.setImageResource(0)

            setSubtitleIndicator()
        }

        linearLayout.findViewById<View>(R.id.viewColor2).setOnClickListener {
            selectedColor = "#FFEA00"
            imageView1.setImageResource(0)
            imageView2.setImageResource(R.drawable.ic_baseline_done_24)
            imageView3.setImageResource(0)
            imageView4.setImageResource(0)
            imageView5.setImageResource(0)
            imageView6.setImageResource(0)

            setSubtitleIndicator()
        }

        linearLayout.findViewById<View>(R.id.viewColor3).setOnClickListener {
            selectedColor = "#FF0031"
            imageView1.setImageResource(0)
            imageView2.setImageResource(0)
            imageView3.setImageResource(R.drawable.ic_baseline_done_24)
            imageView4.setImageResource(0)
            imageView5.setImageResource(0)
            imageView6.setImageResource(0)

            setSubtitleIndicator()
        }

        linearLayout.findViewById<View>(R.id.viewColor4).setOnClickListener {
            selectedColor = "#5000FF"
            imageView1.setImageResource(0)
            imageView2.setImageResource(0)
            imageView3.setImageResource(0)
            imageView4.setImageResource(R.drawable.ic_baseline_done_24)
            imageView5.setImageResource(0)
            imageView6.setImageResource(0)

            setSubtitleIndicator()
        }

        linearLayout.findViewById<View>(R.id.viewColor5).setOnClickListener {
            selectedColor = "#93000000"
            imageView1.setImageResource(0)
            imageView2.setImageResource(0)
            imageView3.setImageResource(0)
            imageView4.setImageResource(0)
            imageView5.setImageResource(R.drawable.ic_baseline_done_24)
            imageView6.setImageResource(0)

            setSubtitleIndicator()
        }

        linearLayout.findViewById<View>(R.id.viewColor6).setOnClickListener {
            selectedColor = "#D5841A"
            imageView1.setImageResource(0)
            imageView2.setImageResource(0)
            imageView3.setImageResource(0)
            imageView4.setImageResource(0)
            imageView5.setImageResource(0)
            imageView6.setImageResource(R.drawable.ic_baseline_done_24)

            setSubtitleIndicator()
        }

        if (alreadyAvailableNote != null && alreadyAvailableNote?.noteColor != null && alreadyAvailableNote?.noteColor.toString()
                .isNotEmpty()
        ) {
            when (alreadyAvailableNote?.noteColor) {
                "#FFEA00" -> {
                    linearLayout.findViewById<View>(R.id.viewColor2).performClick()
                }
                "#FF0031" -> {
                    linearLayout.findViewById<View>(R.id.viewColor3).performClick()
                }
                "#5000FF" -> {
                    linearLayout.findViewById<View>(R.id.viewColor4).performClick()
                }
                "#93000000" -> {
                    linearLayout.findViewById<View>(R.id.viewColor5).performClick()
                }
                "#D5841A" -> {
                    linearLayout.findViewById<View>(R.id.viewColor6).performClick()
                }
            }
        }

        linearLayout.findViewById<LinearLayout>(R.id.layoutAddImage).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

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

        linearLayout.findViewById<LinearLayout>(R.id.layoutNoteWebLink).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            showAddUrlDialog()
        }

        if (alreadyAvailableNote != null) {
            linearLayout.findViewById<LinearLayout>(R.id.layoutDeleteNote).visibility = View.VISIBLE
            linearLayout.findViewById<LinearLayout>(R.id.layoutDeleteNote).setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                showDeleteNoteDialog()
            }
        }

    }

    private fun selectImage() {
        val intent: Intent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IAMGE)
        }
    }

    private fun setSubtitleIndicator() {
        val gradientDrawable: GradientDrawable =
            subtitleIndicatorColor.background as GradientDrawable
        gradientDrawable.setColor(Color.parseColor(selectedColor))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage()
            } else {
                Toast.makeText(this, "Permission Denied !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_IAMGE && resultCode == RESULT_OK) {
            if (data != null) {
                val selectedImageUri: Uri? = data.data

                if (selectedImageUri != null) {
                    try {
                        val inputStream: InputStream? =
                            contentResolver.openInputStream(selectedImageUri)
                        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
                        imageNote.setImageBitmap(bitmap)
                        imageNote.visibility = View.VISIBLE
                        findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.VISIBLE

                        selectedImagePath = getPathFromUri(selectedImageUri)

                    } catch (e: Exception) {
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
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
                    textWebUrl.text = inputUrl.text.toString()
                    webLayout.visibility = View.VISIBLE
                    dialogAddUrl!!.dismiss()
                }
            }

            view.findViewById<TextView>(R.id.textCancelUrl).setOnClickListener {
                dialogAddUrl!!.dismiss()
            }
        }

        dialogAddUrl!!.show()
    }

    private fun showDeleteNoteDialog() {
        if (dialogDeleteNode == null) {
            var builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val view: View = LayoutInflater.from(this)
                .inflate(R.layout.layout_delete_note, findViewById(R.id.layoutDeleteNoteContainer))

            builder.setView(view)

            dialogDeleteNode = builder.create()

            if(dialogDeleteNode!!.window != null) {
                dialogDeleteNode!!.window?.setBackgroundDrawable(ColorDrawable(0))
            }

            view.findViewById<TextView>(R.id.textDeleteNote).setOnClickListener {
                class DeleteNote : AsyncTask<Void , Void , Void>() {
                    override fun doInBackground(vararg p0: Void?): Void? {
                        alreadyAvailableNote?.let { it1 ->
                            NotesDatabase.getDatabase(this@CreateNoteActivity).noteDao.deleteNote(
                                it1
                            )
                        }

                        return null
                    }

                    override fun onPostExecute(result: Void?) {
                        super.onPostExecute(result)
                        val intent : Intent = Intent();
                        intent.putExtra("isNoteDeleted" , true)
                        setResult(RESULT_OK , intent)
                        finish()
                    }
                }

                DeleteNote().execute()
            }

            view.findViewById<TextView>(R.id.textCancelNote).setOnClickListener {
                dialogDeleteNode!!.dismiss()
            }
        }

        dialogDeleteNode!!.show()
    }
}