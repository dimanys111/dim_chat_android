package com.example.chat.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.exifinterface.media.ExifInterface

class CircleImageView : androidx.appcompat.widget.AppCompatImageView {
    var radius = 0f
    var circlePath: Path? = null
    var paint: Paint? = null
    lateinit var layerPaint_: Paint
    var layerRect: RectF? = null
    var drawableSettled = false
    var orientation = 0

    @Volatile
    var isDirty_ = false

    constructor(context: Context) : super(context) {
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context,attrs) {
    }


    private fun init() {
        scaleType = ScaleType.MATRIX
        circlePath = Path()
        layerPaint_= Paint()
        layerPaint_!!.xfermode = null
        layerRect = RectF()
        paint = Paint()
        paint!!.isAntiAlias = true
        paint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                removeOnLayoutChangeListener(this)
                isDirty_ = true
            }
        })
    }

    private fun syncDrawable(): Boolean {
        val drawable = drawable
        if (drawable != null) {
            layerRect!![paddingLeft.toFloat(), paddingTop.toFloat(), width - paddingRight.toFloat()] =
                height - paddingBottom.toFloat()
            val bounds = drawable.bounds
            val vwidth = width - paddingLeft - paddingRight
            val vheight = height - paddingTop - paddingBottom

            if (scaleType == ScaleType.MATRIX) {
                imageMatrix = getNewMatrix(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom
                ) //support matrix
            }

            val scaleParams = scaleParams
            radius = if (scaleParams != null && !bounds.isEmpty) {
                min(
                    vwidth / 2f,
                    vheight / 2f,
                    bounds.centerX() * scaleParams[0],
                    bounds.centerY() * scaleParams[1]
                )
            } else {
                min(vwidth / 2f, vheight / 2f)
            }
            return true
        }
        return false
    }


    private val scaleParams: FloatArray?
        get() {
            val imageMatrix = imageMatrix
            val martixValues = FloatArray(9)
            imageMatrix.getValues(martixValues)
            return if (martixValues[0] == 0f || martixValues[4] == 0f) null else floatArrayOf(
                martixValues[0], martixValues[4]
            )
        }

    override fun setBackground(background: Drawable) {
        setImageDrawable(background)
    }

    override fun setBackgroundColor(color: Int) {
        background = ColorDrawable(color)
    }

    override fun setBackgroundDrawable(background: Drawable) {
        setBackground(background)
    }

    override fun onDraw(canvas: Canvas) {
        drawableSettled = syncDrawable()
        if (drawableSettled) {
            val layerID = canvas.saveLayer(
                layerRect,
                layerPaint_,
                Canvas.ALL_SAVE_FLAG
            ) //clear background
            circlePath!!.reset()
            circlePath!!.addCircle(
                layerRect!!.centerX(),
                layerRect!!.centerY(),
                radius,
                Path.Direction.CCW
            )
            canvas.clipPath(circlePath!!)
            super.onDraw(canvas) //draw drawable
            circlePath!!.reset()
            circlePath!!.addCircle(
                layerRect!!.centerX(),
                layerRect!!.centerY(),
                radius - 1f,
                Path.Direction.CCW
            ) //Antialiasing
            canvas.drawPath(circlePath!!, paint!!)
            canvas.restoreToCount(layerID)
        } else {
            super.onDraw(canvas)
        }
    }

    private fun getNewMatrix(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Matrix {
        val scale: Float
        var dx = 0f
        var dy = 0f
        val vwidth = width - paddingLeft - paddingRight
        val vheight = height - paddingTop - paddingBottom
        val width = right - left
        val height = bottom - top
        val matrix = Matrix()
        matrix.set(null)
        if(width<height) {
            if (width * vheight > vwidth * height) {
                scale = vheight / width.toFloat()
            } else {
                scale = vwidth / width.toFloat()
            }
        } else {
            if (width * vheight > vwidth * height) {
                scale = vheight / height.toFloat()
            } else {
                scale = vwidth / height.toFloat()
            }
        }
        dx = (vwidth - width * scale) * 0.5f
        dy = (vheight - height * scale) * 0.5f
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            (dx + 0.5f).toInt() + left.toFloat(),
            (dy + 0.5f).toInt() + top.toFloat()
        )
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate(90f, matrix, Pair((vwidth/2).toFloat(), (vheight/2).toFloat()))
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate( 180f, matrix, Pair((vwidth/2).toFloat(), (vheight/2).toFloat()))
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate(270f, matrix, Pair((vwidth/2).toFloat(), (vheight/2).toFloat()))
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip( true, false,matrix)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip( false, true,matrix)
            else -> true
        }
        return matrix
    }

    fun rotate(degrees: Float, matrix: Matrix, pair: Pair<Float, Float>) {
        matrix.postRotate(degrees,pair.first,pair.second)
    }

    fun flip(
        horizontal: Boolean,
        vertical: Boolean,
        matrix: Matrix
    ){
        matrix.preScale(if (horizontal) -1.0f else 1.0f, if (vertical) -1.0f else 1.0f)
    }

    private fun min(vararg values: Float): Float {
        if (values.size <= 0) {
            throw IndexOutOfBoundsException("values is empty")
        }
        var min = values[0]
        for (value in values) {
            min = Math.min(value, min)
        }
        return min
    }

    init {
        init()
    }
}
