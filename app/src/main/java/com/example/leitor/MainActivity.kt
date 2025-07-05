package com.example.leitor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leitor.data.AppDatabase
import com.example.leitor.data.book.BookDAO
import com.example.leitor.data.book.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.InputStream
import androidx.core.net.toUri
import com.example.leitor.data.annotation.AnnotationDAO
import nl.siegmann.epublib.domain.Book

class MainActivity : AppCompatActivity() {
    private lateinit var epubPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var bookAdapter: BookAdapter
    private var selectedImageUri: Uri? = null
    private lateinit var bookDao: BookDAO
    private lateinit var annotationDao: AnnotationDAO


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
        loadBooks()
        loadAnnotations()
        loadBookTabSpinners()
    }

    @SuppressLint("SetTextI18n")
    private fun loadAnnotations() {
        val noteListLayout = findViewById<LinearLayout>(R.id.noteList)

        lifecycleScope.launch {
            val annotations = withContext(Dispatchers.IO) {
                annotationDao.getAll()
            }

            noteListLayout.removeAllViews()

            if (annotations != null) {
                for (annotation in annotations) {
                    val view = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.annotation_card, noteListLayout, false)

                    val bookTitleText = view.findViewById<TextView>(R.id.annotationTitle)
                    val contentText = view.findViewById<TextView>(R.id.annotationText)
                    val meta = view.findViewById<TextView>(R.id.annotationMeta)

                    lifecycleScope.launch (Dispatchers.IO){
                        val book = bookDao.getById(annotation.bookId)
                        bookTitleText.text = "${book?.bookTitle}"
                        contentText.text = annotation.content
                        meta.text = "seção ${annotation.chapter + 1}"

                        noteListLayout.addView(view)
                    }

                }
            }
        }
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

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri

                    val imageView = findViewById<ImageView>(R.id.editCoverImage)
                    imageView.setImageURI(uri)
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
        // add book
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

        // edit book details
        val editModal = findViewById<FrameLayout>(R.id.editModalOverlay)

        val editModalContent = findViewById<View>(R.id.editModalContent)

        editModal.setOnClickListener {
            editModal.visibility = View.GONE
        }

        editModalContent.setOnClickListener {
            // Do nothing
        }

        val imageView = findViewById<ImageView>(R.id.editCoverImage)
        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
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
        annotationDao = db.annotationDao()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) {
                bookDao.getAll()
            }.toMutableList()

            val recycler = findViewById<RecyclerView>(R.id.bookRecycler)
            bookAdapter = recycler.adapter as BookAdapter
            bookAdapter.updateBooks(books)
        }
    }

    private fun loadBooks() {
        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) {
                bookDao.getAll()
            }.toMutableList()

            val recycler = findViewById<RecyclerView>(R.id.bookRecycler)
            recycler.layoutManager = GridLayoutManager(/* context = */ this@MainActivity, /* spanCount = */
                2)
            recycler.adapter = BookAdapter(
                books = books,
                onClick = { openBook(it) },
                onLongClickEdit = { book ->
                    openEditModal(book)
                }
            )
        }

    }

    private fun openEditModal(book: BookEntity){
        val modal = findViewById<FrameLayout>(R.id.editModalOverlay)
        val recycler = findViewById<RecyclerView>(R.id.bookRecycler)
        bookAdapter = recycler.adapter as BookAdapter
        modal.visibility = View.VISIBLE

        findViewById<EditText>(R.id.editTitle).setText(book.bookTitle)
        findViewById<EditText>(R.id.editAuthor).setText(book.bookAuthor)


        val coverImage = findViewById<ImageView>(R.id.editCoverImage)
        if (book.imageUri != null && book.imageUri != "default") {
            coverImage.setImageURI(book.imageUri.toUri())
        } else {
            coverImage.setImageResource(R.drawable.book_cover)
        }

        // Optional: setup Save button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val newTitle = findViewById<EditText>(R.id.editTitle).text.toString()
            val newAuthor = findViewById<EditText>(R.id.editAuthor).text.toString()

            val updatedBook = book.copy(bookTitle = newTitle, bookAuthor = newAuthor)
            lifecycleScope.launch (Dispatchers.IO){
                bookDao.update(updatedBook)
            }
            bookAdapter.updateBook(updatedBook)
            modal.visibility = View.GONE
            refresh()
        }

        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                bookDao.delete(book)

                withContext(Dispatchers.Main) {
                    modal.visibility = View.GONE
                    refresh()
                }
            }
        }
    }

    private fun openBook(book: BookEntity) {
        val intent = Intent(this, ReaderActivity::class.java).apply {
            putExtra("bookUri", book.bookUri)
            putExtra("bookId", book.id)
        }
        startActivity(intent)
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
            } ?: "default"

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

            refresh()
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
