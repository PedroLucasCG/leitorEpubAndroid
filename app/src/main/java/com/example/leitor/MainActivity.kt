package com.example.leitor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.InputStream
import nl.siegmann.epublib.epub.EpubReader

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val inputStream: InputStream = assets.open("example.epub")
        val book = EpubReader().readEpub(inputStream)
        val title = book.title
        val sections = book.contents

        for ((i, section) in sections.withIndex()) {
            val html = String(section.data)
            println("Section ${i + 1}:\n$html")
        }

        val fabAdd = findViewById<View>(R.id.fabAdd)
        val modalOverlay = findViewById<View>(R.id.modalOverlay)
        val modalView = findViewById<View>(R.id.modalView)

        fabAdd.setOnClickListener {
            modalOverlay.visibility = View.VISIBLE
        }

        modalOverlay.setOnClickListener {
            modalOverlay.visibility = View.GONE
        }

        modalView.setOnClickListener {  }

        val book1 = findViewById<View>(R.id.book1)
        if (book1 != null) {
            val editModalOverlay = findViewById<View>(R.id.editModalOverlay)
            val editModalContent = findViewById<View>(R.id.editModalContent)

            book1.setOnLongClickListener {
                editModalOverlay.visibility = View.VISIBLE
                true
            }

            book1.setOnClickListener {
                val intent = Intent(this, BookDetailsActivity::class.java)
                startActivity(intent)
            }

            editModalOverlay.setOnClickListener {
                editModalOverlay.visibility = View.GONE
            }

            editModalContent.setOnClickListener {  }
        }

        val nota1 = findViewById<View>(R.id.nota1)
        val annotationModal = findViewById<View>(R.id.annotationModal)
        val modalCard = findViewById<View>(R.id.modalCard)
        val closeBtn = findViewById<View>(R.id.modalClose)

        nota1.setOnLongClickListener {
            annotationModal.visibility = View.VISIBLE
            true
        }

        annotationModal.setOnClickListener {
            annotationModal.visibility = View.GONE
        }

        modalCard.setOnClickListener {  }

        closeBtn.setOnClickListener {
            annotationModal.visibility = View.GONE
        }

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

        loadBookTabSpinners()


    }

    private fun loadBookTabSpinners() {
        val spinnerAZ = findViewById<Spinner>(R.id.sortAZ)
        val spinnerRecente = findViewById<Spinner>(R.id.sortRecente)
        val spinnerCategoria = findViewById<Spinner>(R.id.sortCategoria)

        spinnerAZ?.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.sort_options,
            R.layout.spinner_item_white
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_white)
        }

        spinnerRecente?.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.date_options,
            R.layout.spinner_item_white
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_white)
        }

        spinnerCategoria?.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.category_options,
            R.layout.spinner_item_white
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_white)
        }
    }

    private fun loadNotasTabSpinners() {
        val spinnerNotasAZ = findViewById<Spinner>(R.id.notasAZ)
        val spinnerNotasRecente = findViewById<Spinner>(R.id.notasRecente)

        spinnerNotasAZ?.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.sort_options,
            R.layout.spinner_item_white
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_white)
        }

        spinnerNotasRecente?.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.date_options,
            R.layout.spinner_item_white
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_white)
        }

    }
}
