package com.example.leitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.leitor.data.AppDatabase
import com.example.leitor.data.book.BookDAO
import com.example.leitor.data.book.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var epubPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var bookDao: BookDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        dbInit()
        setupTabs()
        setupModalButtons()
        setupAnnotationModal()
        //loadEpubFromAssets("example.epub")
        loadBookTabSpinners()
    }

    // ========== UI CONFIGURATION ==========

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        epubPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    openEpubFromUri(it)
                }
            }
        }
    }

    private fun setupTabs() {
        val tabLivros = findViewById<TextView>(R.id.tabLivros)
        val tabNotas = findViewById<TextView>(R.id.tabNotas)
        val notasTab = findViewById<View>(R.id.notasTab)
        val notasBook = findViewById<View>(R.id.notasBook)

        tabLivros.setOnClickListener {
            notasBook.visibility = View.VISIBLE
            notasTab.visibility = View.GONE
            loadBookTabSpinners()
        }

        tabNotas.setOnClickListener {
            notasTab.visibility = View.VISIBLE
            notasBook.visibility = View.GONE
            loadNotasTabSpinners()
        }
    }

    private fun setupModalButtons() {
        val fabAdd = findViewById<View>(R.id.fabAdd)
        val modalOverlay = findViewById<View>(R.id.modalOverlay)
        val modalView = findViewById<View>(R.id.modalView)

        fabAdd.setOnClickListener {
            modalOverlay.visibility = View.VISIBLE
        }

        modalOverlay.setOnClickListener {
            modalOverlay.visibility = View.GONE
        }

        modalView.setOnClickListener { /* Do nothing to prevent closing */ }

        findViewById<View>(R.id.buttonBrowse).setOnClickListener {
            openFilePicker()
        }
    }

    private fun setupAnnotationModal() {
        val annotationModal = findViewById<View>(R.id.annotationModal)
        val modalCard = findViewById<View>(R.id.modalCard)
        val closeBtn = findViewById<View>(R.id.modalClose)

        annotationModal.setOnClickListener {
            annotationModal.visibility = View.GONE
        }

        modalCard.setOnClickListener { /* Do nothing */ }

        closeBtn.setOnClickListener {
            annotationModal.visibility = View.GONE
        }
    }

    // ====== Database Initializer =====

    private fun dbInit() {
        val db = AppDatabase.getInstance(applicationContext)
        bookDao = db.bookDao()
    }

    // ========== EPUB LOADER ==========

    private fun loadEpubFromAssets(fileName: String) {
        val inputStream: InputStream = assets.open(fileName)
        val book = EpubReader().readEpub(inputStream)

        println("Book title: ${book.title}")
        book.contents.forEachIndexed { index, section ->
            val html = String(section.data)
            println("Section ${index + 1}:\n$html")
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/epub+zip" // EPUB MIME type
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/epub+zip", "application/octet-stream"))
        }
        epubPickerLauncher.launch(intent)
    }

    private fun openEpubFromUri(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val inputStream = contentResolver.openInputStream(uri)
            val book = EpubReader().readEpub(inputStream)

            val bookTitle = book.title
            val bookAuthor = book.metadata.authors.joinToString(", ") { it.firstname + " " + it.lastname }
            val coverImage = book.coverImage
            val bookUri = uri.toString()

            val imageUri: String? = coverImage?.let {
                val filename = "cover_${System.currentTimeMillis()}.jpg"
                val file = File(cacheDir, filename)
                file.outputStream().use { os -> os.write(it.data) }
                file.toURI().toString()
            }

            val bookEntity = BookEntity(
                bookUri = bookUri,
                bookTitle = bookTitle,
                bookAuthor = bookAuthor,
                imageUri = imageUri
            )

            // Insert into Room (must be called inside coroutine)
            lifecycleScope.launch (Dispatchers.IO){
                bookDao.insert(bookEntity)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== SPINNER LOADING ==========

    private fun loadBookTabSpinners() {
        setupSpinner(R.id.sortAZ, R.array.sort_options)
        setupSpinner(R.id.sortRecente, R.array.date_options)
        setupSpinner(R.id.sortCategoria, R.array.category_options)
    }

    private fun loadNotasTabSpinners() {
        setupSpinner(R.id.notasAZ, R.array.sort_options)
        setupSpinner(R.id.notasRecente, R.array.date_options)
    }

    private fun setupSpinner(spinnerId: Int, arrayRes: Int) {
        val spinner = findViewById<Spinner>(spinnerId)
        spinner?.adapter = ArrayAdapter.createFromResource(
            this,
            arrayRes,
            R.layout.spinner_item_white
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_white)
        }
    }
}
