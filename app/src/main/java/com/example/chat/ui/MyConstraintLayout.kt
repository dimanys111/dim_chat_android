package com.example.chat.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

class MyConstraintLayout : ConstraintLayout {

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context,attrs){
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.actionMasked) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_DOWN -> {
                false
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP ->{
                true
            }
            else -> {
                false
            }
        }
    }
}