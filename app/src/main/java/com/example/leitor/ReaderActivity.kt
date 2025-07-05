package com.example.leitor

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.leitor.data.AppDatabase
import com.example.leitor.data.annotation.AnnotationDAO
import com.example.leitor.data.annotation.BookAnnotationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReaderActivity : AppCompatActivity() {

    private lateinit var chapterContent: LinearLayout
    private lateinit var imageResources: List<Resource>
    private lateinit var book: Book
    private lateinit var annotationDao: AnnotationDAO
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private var currentChapterIndex = 0
    private lateinit var sections: List<Resource>
    private lateinit var bookTitle: String
    private lateinit var bookUri: Uri
    private var bookId: Int = -1

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader_activity)

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0e285e")))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        chapterContent = findViewById(R.id.chapterContent)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)

        val db = AppDatabase.getInstance(applicationContext)
        annotationDao = db.annotationDao()

        val uriString = intent.getStringExtra("bookUri")
        bookUri = Uri.parse(uriString)

        bookId = intent.getIntExtra("bookId", -1)

        try {
            contentResolver.takePersistableUriPermission(bookUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val inputStream = contentResolver.openInputStream(bookUri)
            book = EpubReader().readEpub(inputStream)

            sections = book.contents
            bookTitle = book.title ?: "Livro"

            sections = book.contents
            bookTitle = book.title ?: "Livro"

            imageResources = book.resources.all
                .filter { it.mediaType.name.startsWith("image/") }

            displayChapter(currentChapterIndex)

            nextButton.setOnClickListener {
                if (currentChapterIndex < sections.size - 1) {
                    currentChapterIndex++
                    displayChapter(currentChapterIndex)
                }
            }

            prevButton.setOnClickListener {
                if (currentChapterIndex > 0) {
                    currentChapterIndex--
                    displayChapter(currentChapterIndex)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao carregar livro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayChapter(index: Int) {
        chapterContent.removeAllViews()
        supportActionBar?.title = "$bookTitle – Capítulo ${index + 1}"

        val currentSection = sections[index]
        val rawHtml = String(currentSection.data)
        val document = Jsoup.parse(rawHtml)

        val isToc = currentSection.href.contains("toc", ignoreCase = true)

        if (isToc) {
            renderTocInline()
            return
        }

        val elements = document.body().select("p, h1, img")

        lifecycleScope.launch(Dispatchers.IO) {
            val highlights = annotationDao.getHighlightsForChapter(bookId, index)

            withContext(Dispatchers.Main) {
                var paragraphIndex = 0

                for (element in elements) {
                    when (element.tagName()) {
                        "img" -> {
                            val src = element.attr("src")
                            val imageView = createImageView(this@ReaderActivity, src, imageResources)
                            chapterContent.addView(imageView)
                        }
                        else -> {
                            val text = element.text().trim()
                            if (text.isNotBlank()) {
                                val className = element.classNames().firstOrNull()
                                val tagName = element.tagName()
                                val paragraphHighlights = highlights.filter { it.paragraph == paragraphIndex }

                                val textView = createStyledTextView(
                                    context = this@ReaderActivity,
                                    text = text,
                                    className = className,
                                    tagName = tagName,
                                    index = index,
                                    paragraphIndex = paragraphIndex
                                )

                                if (paragraphHighlights.isNotEmpty()) {
                                    val spannable = SpannableString(text)
                                    for (highlight in paragraphHighlights) {
                                        val start = highlight.startSelection.coerceIn(0, text.length)
                                        val end = highlight.endSelection.coerceIn(start, text.length)

                                        val highlightSpan = object : ClickableSpan() {
                                            override fun onClick(widget: View) {
                                                showAnnotationModal(highlight)
                                            }

                                            override fun updateDrawState(ds: TextPaint) {
                                                super.updateDrawState(ds)
                                                ds.bgColor = Color.argb(60, 255, 233, 0)
                                                ds.color = Color.WHITE
                                                ds.isUnderlineText = false
                                            }
                                        }

                                        spannable.setSpan(
                                            highlightSpan,
                                            start,
                                            end,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                    }
                                    textView.text = spannable
                                    textView.movementMethod = LinkMovementMethod.getInstance()
                                }

                                chapterContent.addView(textView)
                                paragraphIndex++
                            }

                        }
                    }
                }

                prevButton.isEnabled = index > 0
                nextButton.isEnabled = index < sections.size - 1
            }
        }
    }


    private fun renderTocInline() {
        val toc = book.tableOfContents.tocReferences

        fun renderItems(items: List<TOCReference>, level: Int = 0) {
            for (ref in items) {
                val title = ref.title ?: "Untitled"
                val resource = ref.resource

                val textView = TextView(this).apply {
                    text = "→ ".repeat(level) + title
                    setTextColor(Color.CYAN)
                    textSize = 20f
                    setPadding(16 + level * 16, 8, 16, 8)
                    setOnClickListener {
                        val index = sections.indexOfFirst { it.href == resource.href }
                        if (index >= 0) {
                            currentChapterIndex = index
                            displayChapter(index)
                        }
                    }
                }

                chapterContent.addView(textView)

                if (ref.children.isNotEmpty()) {
                    renderItems(ref.children, level + 1)
                }
            }
        }

        renderItems(toc)
    }

    private fun createImageView(context: Context, src: String, resources: List<Resource>): ImageView {
        val imageView = ImageView(context)

        val cleanSrc = src.replace("../", "").lowercase()

        val imageResource = resources.find {
            it.href.replace("%20", " ").lowercase().endsWith(cleanSrc)
        }

        if (imageResource != null) {
            try {
                val bytes = imageResource.data
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageView.setImageBitmap(bitmap)
                imageView.adjustViewBounds = true
                imageView.setPadding(0, 24, 0, 24)
                imageView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            println("❌ Image not found: $src")
        }

        return imageView
    }

    private fun createStyledTextView(
        context: Context,
        text: String,
        className: String?,
        tagName: String? = null,
        index: Int,
        paragraphIndex: Int): TextView {

        val annotationModal = findViewById<FrameLayout>(R.id.annotationModal)
        val modalText = annotationModal.findViewById<TextView>(R.id.modalText)
        val modalDate = annotationModal.findViewById<TextView>(R.id.modalDate)
        val modalClose = annotationModal.findViewById<TextView>(R.id.modalClose)
        val buttonSalvar = annotationModal.findViewById<Button>(R.id.buttonSalvar)
        val buttonCancelar = annotationModal.findViewById<Button>(R.id.buttonCancelar)
        val buttonExcluir = annotationModal.findViewById<Button>(R.id.buttonExcluir)

        return TextView(context).apply {
            this.text = text.trim()
            setTextColor(Color.WHITE)
            typeface = ResourcesCompat.getFont(context, R.font.kadwa)
            this.setTextIsSelectable(true)

            val isHeader = tagName == "h1" || className in listOf("calibre2", "calibre5")
            val isSubHeader = className == "calibre7"

            textSize = when {
                isHeader -> 36f
                isSubHeader -> 20f
                else -> 24f
            }

            setTypeface(typeface, when {
                isHeader || isSubHeader -> Typeface.BOLD
                else -> Typeface.NORMAL
            })

            gravity = when {
                isHeader || isSubHeader -> Gravity.CENTER
                else -> Gravity.START
            }

            setLineSpacing(0f, 1.4f)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, 0, 24)
            setLayoutParams(layoutParams)

            if (className == "calibre4" || className == "calibre6") {
                setPadding(60, 0, 0, 30)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }

            customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
                override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                    menu?.add(0, R.id.menu_annotate, 0, "Anotar")
                    return true
                }

                override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                    return false
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    if (item.itemId == R.id.menu_annotate) {
                        val start = selectionStart
                        val end = selectionEnd
                        val selectedText = text.substring(start, end)

                        modalText.setText(selectedText) // Ensure modalText is EditText
                        modalDate.text = LocalDate.now().format(
                            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
                        )
                        annotationModal.visibility = View.VISIBLE

                        val closeModal = {
                            annotationModal.visibility = View.GONE
                        }

                        modalClose.setOnClickListener { closeModal() }
                        buttonCancelar.setOnClickListener { closeModal() }

                        buttonSalvar.setOnClickListener {
                            val updatedContent = modalText.text.toString()

                            lifecycleScope.launch(Dispatchers.IO) {
                                val annotation = BookAnnotationEntity(
                                    content = updatedContent,
                                    chapter = index,
                                    paragraph = paragraphIndex,
                                    startSelection = start,
                                    endSelection = end,
                                    bookId = bookId,
                                    createdAt = LocalDateTime.now()
                                )
                                annotationDao.insert(annotation)

                                val allParagraphHighlights = annotationDao
                                    .getHighlightsForChapter(bookId, index)
                                    .filter { it.paragraph == paragraphIndex }

                                withContext(Dispatchers.Main) {
                                    val spannable = SpannableString(text)

                                    for (highlight in allParagraphHighlights) {
                                        val hlStart = highlight.startSelection.coerceIn(0, text.length)
                                        val hlEnd = highlight.endSelection.coerceIn(hlStart, text.length)

                                        val highlightSpan = object : ClickableSpan() {
                                            override fun onClick(widget: View) {
                                                showAnnotationModal(highlight)
                                            }

                                            override fun updateDrawState(ds: TextPaint) {
                                                super.updateDrawState(ds)
                                                ds.bgColor = Color.argb(60, 255, 233, 0)
                                                ds.color = Color.WHITE
                                                ds.isUnderlineText = false
                                            }
                                        }

                                        spannable.setSpan(
                                            highlightSpan,
                                            hlStart,
                                            hlEnd,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                    }

                                    this@apply.text = spannable
                                    this@apply.movementMethod = LinkMovementMethod.getInstance()
                                    annotationModal.visibility = View.GONE
                                }
                            }
                        }

                        // Optional delete button logic
                        buttonExcluir.setOnClickListener {
                            Toast.makeText(context, "Delete is disabled for new annotations", Toast.LENGTH_SHORT).show()
                        }

                        mode.finish()
                        return true
                    }
                    return false
                }



                override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
            }

        }
    }

    private fun showAnnotationModal(highlight: BookAnnotationEntity) {
        runOnUiThread {
            val modal = findViewById<FrameLayout>(R.id.annotationModal)
            val date = findViewById<TextView>(R.id.modalDate)
            val editText = findViewById<EditText>(R.id.modalText)

            date.text = highlight.createdAt.toString() // Format if needed
            editText.setText(highlight.content)

            modal.visibility = View.VISIBLE

            findViewById<TextView>(R.id.modalClose).setOnClickListener {
                modal.visibility = View.GONE
            }

            findViewById<Button>(R.id.buttonCancelar).setOnClickListener {
                modal.visibility = View.GONE
            }

            findViewById<Button>(R.id.buttonExcluir).setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    annotationDao.delete(highlight)
                    withContext(Dispatchers.Main) {
                        modal.visibility = View.GONE
                        displayChapter(highlight.chapter) // Refresh display
                    }
                }
            }

            findViewById<Button>(R.id.buttonSalvar).setOnClickListener {
                val updatedText = editText.text.toString()
                val updatedHighlight = highlight.copy(content = updatedText)
                lifecycleScope.launch(Dispatchers.IO) {
                    annotationDao.update(updatedHighlight)
                    withContext(Dispatchers.Main) {
                        modal.visibility = View.GONE
                        displayChapter(highlight.chapter)
                    }
                }
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

