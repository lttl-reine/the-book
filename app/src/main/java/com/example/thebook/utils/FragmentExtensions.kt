package com.example.thebook.utils

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.thebook.R


fun Fragment.setupSystemUI(
    statusBarColorResId: Int = R.color.white,
    isAppearanceLightStatusBars: Boolean = true,
    applyInsetsToRoot: Boolean = true
) {
    val statusBarBackgroundColor = ContextCompat.getColor(requireContext(), statusBarColorResId)
    requireActivity().window.apply {
        statusBarColor = statusBarBackgroundColor
        ViewCompat.getWindowInsetsController(decorView)?.let { controller ->
            controller.isAppearanceLightStatusBars = isAppearanceLightStatusBars
        }
    }

    if (applyInsetsToRoot) {
        ViewCompat.setOnApplyWindowInsetsListener(view ?: return) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBarsInsets.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
    }
}