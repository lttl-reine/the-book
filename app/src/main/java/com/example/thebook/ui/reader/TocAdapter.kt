package com.example.thebook.ui.reader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.thebook.R

class TocAdapter(
    private val onItemClick: (EnhancedTocItem) -> Unit
) : RecyclerView.Adapter<TocAdapter.TocViewHolder>() {

    private var tocList: List<EnhancedTocItem> = emptyList()
    private var currentChapterIndex: Int = -1

    fun updateTocList(newTocList: List<EnhancedTocItem>, currentIndex: Int = -1) {
        tocList = newTocList
        currentChapterIndex = currentIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TocViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return TocViewHolder(view)
    }

    override fun onBindViewHolder(holder: TocViewHolder, position: Int) {
        holder.bind(tocList[position], currentChapterIndex)
    }

    override fun getItemCount(): Int = tocList.size

    inner class TocViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<android.widget.TextView>(android.R.id.text1)

        fun bind(tocItem: EnhancedTocItem, currentIndex: Int) {
            textView.text = tocItem.title

            // Kiểm tra nếu đây là chương hiện tại
            val isCurrentChapter = tocItem.index == currentIndex

            if (isCurrentChapter) {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.primary_300))
                textView.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                itemView.setBackgroundColor(itemView.context.getColor(android.R.color.white))
                // Phân biệt màu chữ giữa TOC gốc và spine-generated
                val textColor = if (tocItem.isFromOriginalToc) {
                    itemView.context.getColor(android.R.color.black)
                } else {
                    itemView.context.getColor(android.R.color.darker_gray)
                }
                textView.setTextColor(textColor)
            }

            // Style khác nhau cho TOC gốc và spine-generated
            if (tocItem.isFromOriginalToc) {
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
                textView.setPadding(32, 24, 32, 24)
            } else {
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
                textView.setPadding(48, 16, 32, 16) // Indent nhiều hơn
            }

            itemView.setOnClickListener {
                onItemClick(tocItem)
            }
        }
    }
}
