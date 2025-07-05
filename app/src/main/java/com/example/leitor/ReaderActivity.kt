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
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup

class ReaderActivity : AppCompatActivity() {

    private lateinit var chapterContent: LinearLayout
    private lateinit var imageResources: List<Resource>
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private var currentChapterIndex = 0
    private lateinit var sections: List<Resource>
    private lateinit var bookTitle: String
    private lateinit var bookUri: Uri

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader_activity)

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0e285e")))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        chapterContent = findViewById(R.id.chapterContent)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)

        val uriString = intent.getStringExtra("bookUri")
        bookUri = Uri.parse(uriString)

        try {
            contentResolver.takePersistableUriPermission(bookUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val inputStream = contentResolver.openInputStream(bookUri)
            val book = EpubReader().readEpub(inputStream)

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

        val rawHtml = String(sections[index].data)
        val document = Jsoup.parse(rawHtml)
        val elements = document.body().select("p, h1, img")

        for (element in elements) {
            if (element.tagName() == "img") {
                val src = element.attr("src")
                val imageView = createImageView(this, src, imageResources)
                chapterContent.addView(imageView)
                continue
            }

            val text = element.text().trim()
            if (text.isNotBlank()) {
                val className = element.classNames().firstOrNull()
                val paragraphView = createStyledTextView(this, text, className, element.tagName())
                chapterContent.addView(paragraphView)
            }
        }

        prevButton.isEnabled = index > 0
        nextButton.isEnabled = index < sections.size - 1
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

    private fun createStyledTextView(context: Context, text: String, className: String?, tagName: String? = null): TextView {
        return TextView(context).apply {
            this.text = text.trim()
            setTextColor(Color.WHITE)
            typeface = ResourcesCompat.getFont(context, R.font.kadwa)

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
        }
    }




    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

