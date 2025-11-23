package com.huybd.tool_pdf.ui.imagetopdf

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SquareGridLayoutManager(
    context: Context,
    spanCount: Int
) : GridLayoutManager(context, spanCount) {

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        val width = RecyclerView.LayoutManager.getChildMeasureSpec(
            widthSpec,
            paddingLeft + paddingRight,
            RecyclerView.LayoutParams.MATCH_PARENT,
            false
        )
        val height = RecyclerView.LayoutManager.getChildMeasureSpec(
            heightSpec,
            paddingTop + paddingBottom,
            RecyclerView.LayoutParams.MATCH_PARENT,
            false
        )
        setMeasuredDimension(width, height)
    }
}