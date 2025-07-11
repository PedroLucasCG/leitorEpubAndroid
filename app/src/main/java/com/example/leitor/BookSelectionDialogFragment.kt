package com.example.leitor

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leitor.data.AppDatabase
import com.example.leitor.data.book.BookDAO
import com.example.leitor.data.book.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookSelectionDialogFragment(
    var preselectedIds: List<Int> = emptyList()
) : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookAdapterDialog
    private lateinit var bookDao: BookDAO

    var listener: OnBooksSelectedListener? = null

    interface OnBooksSelectedListener {
        fun onBooksSelected(selected: List<BookEntity>)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.HalfScreenDialog)
        val view = layoutInflater.inflate(R.layout.dialog_book_selection, null)

        recyclerView = view.findViewById(R.id.recyclerViewBooks)
        val buttonConfirm = view.findViewById<Button>(R.id.buttonConfirmBooks)

        bookDao = AppDatabase.getInstance(requireContext()).bookDao()
        adapter = BookAdapterDialog(preselectedIds)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) { bookDao.getAllByTitle() }
            adapter.submitList(books)
        }

        buttonConfirm.setOnClickListener {
            val selected = adapter.getSelectedBooks()
            listener?.onBooksSelected(selected)
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.82).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
