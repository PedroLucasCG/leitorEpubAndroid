package com.example.leitor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.leitor.data.category.CategoryEntity

class CategoryAdapter(
    private val onItemClick: (CategoryEntity) -> Unit,
    private val preselectedIds: List<Int>
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    private val selectedCategories = mutableSetOf<CategoryEntity>()

    init {
        preselectedIds.forEach { id ->
            selectedCategories.addAll(currentList.filter { it.id == id })
        }
    }
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.categoryCheckBox)
        val layout: LinearLayout = itemView.findViewById(R.id.categoryItemLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)

        holder.checkBox.text = category.categoryName
        holder.checkBox.isChecked = selectedCategories.contains(category)

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedCategories.add(category)
            else selectedCategories.remove(category)
        }

        holder.layout.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<CategoryEntity>,
        currentList: MutableList<CategoryEntity>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        selectedCategories.clear()
        selectedCategories.addAll(currentList.filter { it.id in preselectedIds })
    }

    fun getSelectedCategories(): List<CategoryEntity> = selectedCategories.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedCategories.clear()
        notifyDataSetChanged()
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CategoryEntity>() {
            override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
