package com.example.leitor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.annotation.RequiresApi
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
import androidx.core.net.toUri
import com.example.leitor.data.annotation.AnnotationDAO
import com.example.leitor.data.annotation.BookAnnotationEntity
import com.example.leitor.data.category.CategoryDAO
import com.example.leitor.data.category.CategoryEntity
import com.example.leitor.data.crossRef.BookCategoryCrossRef
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity(),  CategorySelectionDialogFragment.OnCategoriesSelectedListener {
    private lateinit var epubPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var bookAdapter: BookAdapter
    private var selectedImageUri: Uri? = null
    private lateinit var bookDao: BookDAO
    private lateinit var annotationDao: AnnotationDAO
    private lateinit var categoryDao: CategoryDAO

    private var titleSortState = SortState.NONE
    private var dateSortState = SortState.NONE
    private var categoriesList: List<Int> = emptyList()
    private var selectedCategoriesForEdit: List<Int> = emptyList()
    private var annotationSortDateState = SortState.NONE
    private var selectedAnnotationBookIds: List<Int> = emptyList()


    override fun onCategoriesSelected(selected: List<CategoryEntity>) {
        categoriesList = selected.map { it.id }
        Log.d("MainActivity", "Selected categories: $categoriesList")
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
    }

    // ========== UI CONFIGURATION ==========

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {
        val tabLivros = findViewById<TextView>(R.id.tabLivros)
        val tabNotas = findViewById<TextView>(R.id.tabNotas)
        val notasTab = findViewById<View>(R.id.notasTab)
        val notasBook = findViewById<View>(R.id.notasBook)

        val bookToggleTitle = findViewById<Button>(R.id.bookToggleTitle)
        val bookToggleDate = findViewById<Button>(R.id.bookToggleDate)

        bookToggleTitle.setOnClickListener {
            titleSortState = when (titleSortState) {
                SortState.NONE -> SortState.ASCENDING
                SortState.ASCENDING -> SortState.DESCENDING
                SortState.DESCENDING -> SortState.NONE
            }

            dateSortState = SortState.NONE
            updateToggleUI(bookToggleTitle, titleSortState, "Title")
            updateToggleUI(bookToggleDate, dateSortState, "Date")

            loadBooks()
        }

        bookToggleDate.setOnClickListener {
            dateSortState = when (dateSortState) {
                SortState.NONE -> SortState.ASCENDING
                SortState.ASCENDING -> SortState.DESCENDING
                SortState.DESCENDING -> SortState.NONE
            }

            titleSortState = SortState.NONE
            updateToggleUI(bookToggleDate, dateSortState, "Date")
            updateToggleUI(bookToggleTitle, titleSortState, "Title")

            loadBooks()
        }


        tabLivros.setOnClickListener {
            notasBook.visibility = View.VISIBLE
            notasTab.visibility = View.GONE
        }

        tabNotas.setOnClickListener {
            notasTab.visibility = View.VISIBLE
            notasBook.visibility = View.GONE
            loadAnnotations()
        }

        val buttonAnnotationDate = findViewById<Button>(R.id.annotationsSortDate)
        val buttonAnnotationBooks = findViewById<Button>(R.id.annotationsSortByBooks)

        buttonAnnotationDate.setOnClickListener {
            annotationSortDateState = when (annotationSortDateState) {
                SortState.NONE -> SortState.ASCENDING
                SortState.ASCENDING -> SortState.DESCENDING
                SortState.DESCENDING -> SortState.NONE
            }

            updateToggleUI(buttonAnnotationDate, annotationSortDateState, "Date")
            loadAnnotations()
        }

        buttonAnnotationBooks.setOnClickListener {
            val dialog = BookSelectionDialogFragment(selectedAnnotationBookIds)

            dialog.listener = object : BookSelectionDialogFragment.OnBooksSelectedListener {
                override fun onBooksSelected(selected: List<BookEntity>) {
                    selectedAnnotationBookIds = selected.map { it.id }

                    buttonAnnotationBooks.text = if (selectedAnnotationBookIds.isNotEmpty())
                        "Books ✓"
                    else "Books"

                    loadAnnotations()
                }
            }

            dialog.show(supportFragmentManager, "bookDialog")
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

        val buttonChooseCategories = findViewById<Button>(R.id.bookChooseCategories)

        buttonChooseCategories.setOnClickListener {
            val dialog = CategorySelectionDialogFragment.newInstance(categoriesList)
            dialog.listener = object : CategorySelectionDialogFragment.OnCategoriesSelectedListener {
                override fun onCategoriesSelected(selected: List<CategoryEntity>) {
                    categoriesList = selected.map { it.id }
                    buttonChooseCategories.text = if (categoriesList.isNotEmpty())
                            "Categories ✓"
                    else "Categories"
                    loadBooks()
                }
            }

            dialog.show(supportFragmentManager, "categoryDialog")
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

    private fun getAllBooksWithFilters(): List<BookEntity> {
        if (categoriesList.isEmpty()) {
            return when {
                titleSortState == SortState.ASCENDING -> bookDao.getAllByTitle()
                titleSortState == SortState.DESCENDING -> bookDao.getAllByTitle().reversed()

                dateSortState == SortState.ASCENDING -> bookDao.getAllByCreatedAt()
                dateSortState == SortState.DESCENDING -> bookDao.getAllByCreatedAt().reversed()

                else -> bookDao.getAllByTitle()
            }
        } else {
            return when {
                titleSortState == SortState.ASCENDING -> bookDao.getBooksByCategoriesByTitle(categoriesList)
                titleSortState == SortState.DESCENDING -> bookDao.getBooksByCategoriesByTitle(categoriesList).reversed()

                dateSortState == SortState.ASCENDING -> bookDao.getBooksByCategoriesByCreatedAt(categoriesList)
                dateSortState == SortState.DESCENDING -> bookDao.getBooksByCategoriesByCreatedAt(categoriesList).reversed()

                else -> bookDao.getBooksByCategories(categoriesList)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateToggleUI(toggle: Button, state: SortState, label: String) {
        when (state) {
            SortState.NONE -> {
                toggle.text = "$label ⬤"
            }
            SortState.ASCENDING -> {
                toggle.text = "$label ▲"
            }
            SortState.DESCENDING -> {
                toggle.text = "$label ▼"
            }
        }
    }

    // ====== Database Initializer =====
    private fun dbInit() {
        val db = AppDatabase.getInstance(applicationContext)
        bookDao = db.bookDao()
        annotationDao = db.annotationDao()
        categoryDao = db.categoryDao()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) {
                getAllBooksWithFilters()
            }.toMutableList()

            val recycler = findViewById<RecyclerView>(R.id.bookRecycler)
            bookAdapter = recycler.adapter as BookAdapter
            bookAdapter.updateBooks(books)
        }
    }

    private fun loadBooks() {
        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) {
                getAllBooksWithFilters()
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun loadAnnotations() {
        val noteListLayout = findViewById<LinearLayout>(R.id.noteList)

        lifecycleScope.launch{
            val annotations = withContext(Dispatchers.IO) {
                val all = annotationDao.getAll()
                val filtered = if (selectedAnnotationBookIds.isNotEmpty()) {
                    all?.filter { selectedAnnotationBookIds.contains(it.bookId) }
                } else all

                when (annotationSortDateState) {
                    SortState.ASCENDING -> filtered?.sortedBy { it.createdAt }
                    SortState.DESCENDING -> filtered?.sortedByDescending { it.createdAt }
                    else -> filtered
                }
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

                        lifecycleScope.launch (Dispatchers.Main){
                            bookTitleText.text = "${book?.bookTitle}"
                            contentText.text = annotation.content
                            meta.text = "seção ${annotation.chapter + 1}"

                            view.setOnClickListener {
                                openAnnotationModal(annotation)
                            }
                            noteListLayout.addView(view)
                        }
                    }

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openAnnotationModal(annotation: BookAnnotationEntity) {
        val modalText = findViewById<EditText?>(R.id.modalText)
        val modalDate = findViewById<TextView?>(R.id.modalDate)
        val annotationModal = findViewById<View?>(R.id.annotationModal)
        val buttonSalvar = findViewById<Button?>(R.id.buttonSalvar)
        val buttonCancelar = findViewById<Button?>(R.id.buttonCancelar)
        val buttonExcluir = findViewById<Button?>(R.id.buttonExcluir)

        if (modalText == null || modalDate == null || annotationModal == null ||
            buttonSalvar == null || buttonCancelar == null || buttonExcluir == null) {
            Log.e("AnnotationModal", "Some views not found")
            return
        }

        modalText.setText(annotation.content)
        modalDate.text = annotation.createdAt.toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")))
        annotationModal.visibility = View.VISIBLE

        buttonSalvar.setOnClickListener {
            val updated = annotation.copy(content = modalText.text.toString())
            lifecycleScope.launch(Dispatchers.IO) {
                annotationDao.update(updated)
                withContext(Dispatchers.Main) {
                    annotationModal.visibility = View.GONE
                    loadAnnotations()
                }
            }
        }

        buttonExcluir.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                annotationDao.delete(annotation)
                withContext(Dispatchers.Main) {
                    annotationModal.visibility = View.GONE
                    loadAnnotations()
                }
            }
        }

        buttonCancelar.setOnClickListener {
            annotationModal.visibility = View.GONE
        }
    }


    @SuppressLint("SetTextI18n")
    private fun openEditModal(book: BookEntity){
        val modal = findViewById<FrameLayout>(R.id.editModalOverlay)
        val recycler = findViewById<RecyclerView>(R.id.bookRecycler)
        selectedCategoriesForEdit = emptyList()
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

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val newTitle = findViewById<EditText>(R.id.editTitle).text.toString()
            val newAuthor = findViewById<EditText>(R.id.editAuthor).text.toString()

            val updatedBook = if (selectedImageUri != null) {
                book.copy(
                    bookTitle = newTitle,
                    bookAuthor = newAuthor,
                    imageUri = copyUriToInternalStorage(this, selectedImageUri as Uri)
                )
            } else {
                book.copy(
                    bookTitle = newTitle,
                    bookAuthor = newAuthor,
                )
            }

            lifecycleScope.launch (Dispatchers.IO){
                bookDao.update(updatedBook)
                bookDao.clearCategories(updatedBook.id)
                selectedCategoriesForEdit.forEach {
                    bookDao.insertCrossRef(BookCategoryCrossRef(updatedBook.id, it))
                }
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
                    loadBooks()
                    refresh()
                }
            }
        }

        val buttonChooseEdit = findViewById<TextView>(R.id.bookChooseCategoriesEdit)

        lifecycleScope.launch(Dispatchers.IO) {
            val bookWithCategories = bookDao.getBookWithCategories(book.id)
            val categoryNames = bookWithCategories.categories.map { it.categoryName }
            val categoryIds = bookWithCategories.categories.map { it.id }

            selectedCategoriesForEdit = categoryIds

            withContext(Dispatchers.Main) {
                if (categoryNames.isEmpty()) {
                    buttonChooseEdit.text = "Choose Categories for this book"
                } else {
                    buttonChooseEdit.text = categoryNames.joinToString(", ")
                }
            }
        }

        buttonChooseEdit.setOnClickListener {
            lifecycleScope.launch (Dispatchers.IO){
                val prev = categoryDao.getBookCategory(book.id)
                val dialog = CategorySelectionDialogFragment
                    .newInstance(prev.map { it.id })

                dialog.listener = object : CategorySelectionDialogFragment.OnCategoriesSelectedListener {
                    @SuppressLint("SetTextI18n")
                    override fun onCategoriesSelected(selected: List<CategoryEntity>) {
                        selectedCategoriesForEdit = selected.map { it.id }
                        if (selected.isEmpty()) {
                            buttonChooseEdit.text = "Choose Categories for this book"
                        } else {
                            buttonChooseEdit.text = selected.joinToString(", ") { it.categoryName }
                        }
                    }
                }

                dialog.show(supportFragmentManager, "categoryDialog")
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

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/epub+zip" // EPUB MIME type
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/epub+zip", "application/octet-stream"))
        }
        epubPickerLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openEpubFromUri(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val inputStream = contentResolver.openInputStream(uri)
            val book = EpubReader().readEpub(inputStream)

            val bookTitle = book.title
            val bookAuthor = book.metadata.authors.joinToString(", ") { it.firstname + " " + it.lastname }
            val coverImage = book.coverImage
            val bookUri = uri.toString()

            val imageUri: String = coverImage?.let {
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
                refresh()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val file = File(context.filesDir, "selected_image.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return file.absolutePath
    }

    // ========== SPINNER LOADING ==========

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
