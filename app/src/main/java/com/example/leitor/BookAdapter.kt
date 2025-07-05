package com.example.leitor

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.leitor.data.book.BookEntity

class BookAdapter(
    private val books: List<BookEntity>,
    private val onClick: (BookEntity) -> Unit) :
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image = view.findViewById<ImageView>(R.id.bookImage)
        val title = view.findViewById<TextView>(R.id.bookTitle)
        val author = view.findViewById<TextView>(R.id.bookAuthor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_book,
                parent,
                false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.title.text = book.bookTitle
        holder.author.text = book.bookAuthor
        holder.itemView.setOnClickListener { onClick(book) }

        if (book.imageUri != null) {
            holder.image.setImageURI(Uri.parse(book.imageUri))
        } else {
            holder.image.setImageResource(R.drawable.book_cover)
        }
    }

    override fun getItemCount(): Int = books.size
}
