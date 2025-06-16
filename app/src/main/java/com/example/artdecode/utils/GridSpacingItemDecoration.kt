package com.example.artdecode.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view) // item position
        if (position == RecyclerView.NO_POSITION) { // Added null check
            return
        }

        val layoutManager = parent.layoutManager
        if (layoutManager !is GridLayoutManager) {
            // This decoration is specific to GridLayoutManager
            super.getItemOffsets(outRect, view, parent, state)
            return
        }

        val spanSize = layoutManager.spanSizeLookup.getSpanSize(position)
        val column = (layoutManager.spanSizeLookup.getSpanIndex(position, spanCount)) % spanCount // item column

        // If it's a header (spanning full width), give it full width
        if (spanSize == spanCount) {
            outRect.left = 0
            outRect.right = 0
            outRect.top = if (position == 0) 0 else spacing // Top spacing for subsequent headers
            outRect.bottom = 0
            return
        }

        // For regular grid items
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) { // top edge
                outRect.top = spacing
            }
            outRect.bottom = spacing // item bottom
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing // item top
            }
        }
    }
}