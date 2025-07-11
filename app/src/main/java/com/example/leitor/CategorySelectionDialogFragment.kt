package com.example.leitor

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leitor.data.AppDatabase
import com.example.leitor.data.category.CategoryDAO
import com.example.leitor.data.category.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategorySelectionDialogFragment: DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryAdapter
    private lateinit var categoryDao: CategoryDAO

    var listener: OnCategoriesSelectedListener? = null
    private var preselectedIds: List<Int> = emptyList()

    interface OnCategoriesSelectedListener {
        fun onCategoriesSelected(selected: List<CategoryEntity>)
    }

    companion object {
        fun newInstance(preselectedIds: List<Int>): CategorySelectionDialogFragment {
            val fragment = CategorySelectionDialogFragment()
            val bundle = Bundle()
            bundle.putIntegerArrayList("preselectedIds", ArrayList(preselectedIds))
            fragment.arguments = bundle
            return fragment
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.HalfScreenDialog)
        val view = layoutInflater.inflate(R.layout.dialog_category_selection, null)
        preselectedIds = arguments?.getIntegerArrayList("preselectedIds") ?: emptyList()

        recyclerView = view.findViewById(R.id.categoryRecyclerView)
        val buttonAdd = view.findViewById<Button>(R.id.buttonAddCategory)
        val buttonConfirm = view.findViewById<Button>(R.id.buttonConfirm)

        categoryDao = AppDatabase.getInstance(requireContext()).categoryDao()
        adapter = CategoryAdapter(
            onItemClick = { category -> onCategoryClicked(category) },
            preselectedIds = preselectedIds
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadCategories()

        buttonAdd.setOnClickListener {
            showAddCategoryDialog()
        }

        buttonConfirm.setOnClickListener {
            val selected = adapter.getSelectedCategories()
            listener?.onCategoriesSelected(selected)
            dismiss()
        }

        val buttonDelete = view.findViewById<Button>(R.id.buttonDeleteCategory)

        buttonDelete.setOnClickListener {
            val selected = adapter.getSelectedCategories()

            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "No categories selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Delete Categories")
                .setMessage("Are you sure you want to delete ${selected.size} categories?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        selected.forEach { categoryDao.delete(it) }
                        loadCategories()
                        withContext(Dispatchers.Main) {
                            adapter.clearSelection()
                        }

                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        builder.setView(view)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.5).toInt()
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                categoryDao.getAll()
            }
            adapter.submitList(categories)
        }
    }

    private fun onCategoryClicked(category: CategoryEntity) {
        // Show options to edit/delete
        AlertDialog.Builder(requireContext())
            .setTitle("Edit or Delete")
            .setMessage("Edit or delete category '${category.categoryName}'?")
            .setPositiveButton("Edit") { _, _ -> showEditDialog(category) }
            .setNegativeButton("Delete") { _, _ -> deleteCategory(category) }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString()
                lifecycleScope.launch(Dispatchers.IO) {
                    categoryDao.insert(CategoryEntity(categoryName = name))
                    loadCategories()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(category: CategoryEntity) {
        val input = EditText(requireContext())
        input.setText(category.categoryName)
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Category")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val updated = category.copy(categoryName = input.text.toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    categoryDao.update(updated)
                    loadCategories()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: CategoryEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            categoryDao.delete(category)
            loadCategories()
        }
    }
}
