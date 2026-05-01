package com.edgesite.yolov8tflite

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


object WindowInsetsHelper {

    fun applyTopBottomInsets(
        root: View,
        topBar: View,
        bottomBar: View? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            topBar.setPadding(
                topBar.paddingLeft,
                systemBars.top + topBar.paddingBottom,
                topBar.paddingRight,
                topBar.paddingBottom
            )
            bottomBar?.let {
                it.setPadding(
                    it.paddingLeft,
                    it.paddingTop,
                    it.paddingRight,
                    systemBars.bottom + it.paddingTop 
                )
            }

            insets
        }
    }

    fun applyScrollableInsets(
        root: View,
        heroHeader: View,
        bottomSpacer: View? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            heroHeader.setPadding(
                heroHeader.paddingLeft,
                systemBars.top + 16.dpToPx(heroHeader),
                heroHeader.paddingRight,
                heroHeader.paddingBottom
            )

            bottomSpacer?.let { v ->
                v.setPadding(
                    v.paddingLeft,
                    v.paddingTop,
                    v.paddingRight,
                    systemBars.bottom + v.paddingTop
                )
            }

            insets
        }
    }

    private fun Int.dpToPx(view: View): Int {
        return (this * view.context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
