package com.example.leitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.leitor.data.book.BookEntity

class BookAdapterDialog(private val preselectedIds: List<Int>) :
    ListAdapter<BookEntity, BookAdapterDialog.BookViewHolder>(DIFF_CALLBACK) {

    private val selectedBooks = mutableSetOf<BookEntity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_checkbox, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book, preselectedIds.contains(book.id))
    }

    fun getSelectedBooks(): List<BookEntity> = selectedBooks.toList()

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkBox = view.findViewById<CheckBox>(R.id.checkBoxBook)

        fun bind(book: BookEntity, isChecked: Boolean) {
            checkBox.text = book.bookTitle
            checkBox.isChecked = isChecked

            if (isChecked) selectedBooks.add(book)

            checkBox.setOnCheckedChangeListener { _, checked ->
                if (checked) selectedBooks.add(book)
                else selectedBooks.remove(book)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookEntity>() {
            override fun areItemsTheSame(old: BookEntity, new: BookEntity) = old.id == new.id
            override fun areContentsTheSame(old: BookEntity, new: BookEntity) = old == new
        }
    }
}
