package com.example.chat.ui


import androidx.fragment.app.Fragment

interface OnBackPressed {
    fun onBackPressed()
}

open class BaseFragment : Fragment(), OnBackPressed {

    override fun onBackPressed() {}
}