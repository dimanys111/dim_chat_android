package com.example.chat

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.exifinterface.media.ExifInterface
import com.example.chat.ImageUtil.Image
import com.example.chat.ui.CircleImageView
import java.io.*
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import kotlin.math.round

class Util {
    companion object{

        fun hideKeyboard(activity: Activity) {
            val imm: InputMethodManager =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun isStoragePermissionGranted(activity: Activity):Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                    true
                } else {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    false
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                true
            }
        }

        @Throws(IOException::class)
        fun add_file_from_inputStream(inputStream: InputStream): Pair<ByteArray, String> {
            val outputStream = ByteArrayOutputStream()
            var bytesRead = -1
            val buffer = ByteArray(BUFFER_SIZE)
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            val ba = outputStream.toByteArray()
            val sha1 = encodeHex(MessageDigest.getInstance("SHA-1").digest(ba))
            save_ba_to_file(ba,sha1)
            return Pair(ba, sha1)
        }

        fun save_ba_to_file(ba:ByteArray,sha1: String)
        {
            if (!MainActivity.files_src_map_all.contains(sha1)) {
                val src = save_to_file(
                    ba,
                    File(MainActivity.dir_images, sha1)
                )
                MainActivity.files_src_map_all[sha1] = src
                MainActivity.save_files_to_file()
            }
        }

        fun save_to_file(buffer :ByteArray, file: File):String {
            val outputStream: OutputStream = FileOutputStream(file)
            outputStream.write(buffer)
            outputStream.flush()
            outputStream.close()
            return file.absolutePath
        }

        fun set_image_bitmap(imageView: ImageView?, image: Image){
            if (image.bitmap == null) {
                if(image.image_src!="") {
                    image.bitmap = BitmapFactory.decodeFile(image.image_src)
                } else {
                    image.bitmap = BitmapFactory.decodeResource(MainActivity.activity?.resources, R.drawable.user)
                }
            }
            if(imageView is CircleImageView) {
                imageView.orientation=image.orientation
            } else {
                imageView?.let {
                    val vto = it.viewTreeObserver
                    if (vto.isAlive) {
                        vto.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                            override fun onPreDraw(): Boolean {
                                it.viewTreeObserver.removeOnPreDrawListener(this);
                                modifyOrientation(image.orientation, it)
                                return true
                            }
                        })
                    }
                }
            }
            imageView?.setImageBitmap(image.bitmap)
        }

        @Throws(IOException::class)
        fun modifyOrientation(orientation: Int, imageView: ImageView) {
            imageView.scaleType = ImageView.ScaleType.MATRIX
            imageView.imageMatrix = Matrix().apply {
                val dWidth = imageView.drawable.intrinsicWidth
                val dHeight = imageView.drawable.intrinsicHeight

                val vWidth = imageView.measuredWidth
                val vHeight = imageView.measuredHeight

                val drawableLeft = round((vWidth - dWidth) * 0.5f)
                val drawableTop = round((vHeight - dHeight) * 0.5f)

                setTranslate(drawableLeft, drawableTop)
                val (viewCenterX, viewCenterY) = vWidth / 2f to vHeight / 2f

                var s = 1f
                var ws = vWidth / dWidth.toFloat()
                var hs = vHeight / dHeight.toFloat()

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> {
                        rotate(90f, this, Pair(viewCenterX, viewCenterY))
                        ws = vWidth / dHeight.toFloat()
                        hs = vHeight / dWidth.toFloat()
                    }
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotate( 180f, this, Pair(viewCenterX, viewCenterY))
                    ExifInterface.ORIENTATION_ROTATE_270 -> {
                        rotate(270f, this, Pair(viewCenterX, viewCenterY))
                        ws = vWidth / dHeight.toFloat()
                        hs = vHeight / dWidth.toFloat()
                    }
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip( true, false,this)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip( false, true,this)
                    else -> true
                }
                s = if(ws>hs) hs else ws
                postScale(s, s, viewCenterX, viewCenterY)
            }
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


        private val DIGITS_LOWER =
            charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

        fun encodeHex(data: ByteArray): String {
            val toDigits = DIGITS_LOWER
            val l = data.size
            val out = CharArray(l shl 1)
            // two characters form the hex value.
            var i = 0
            var j = 0
            while (i < l) {
                out[j++] = toDigits[0xF0 and data[i].toInt() ushr 4]
                out[j++] = toDigits[0x0F and data[i].toInt()]
                i++
            }
            return String(out)
        }

        fun orientation(sha1: String): Int{
            val ei = ExifInterface(MainActivity.files_src_map_all[sha1]!!)
            return ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }

        val BUFFER_SIZE = 524288
        val RESULT_LOAD_IMG = 5


        @Throws(NoSuchElementException::class)
        fun scale_height_Image(view: ImageView, size: Int, orientation: Int) {
            // Get bitmap from the the ImageView.
            var bitmap = try {
                val drawing: Drawable = view.drawable
                (drawing as BitmapDrawable).bitmap
            } catch (e: NullPointerException) {
                throw NoSuchElementException("No drawable on given view")
            } catch (e: ClassCastException) {
                // Check bitmap is Ion drawable
                BitmapFactory.decodeResource(view.context.resources, R.drawable.user)
            }

            if(bitmap==null){
                bitmap=BitmapFactory.decodeResource(view.context.resources, R.drawable.user)
            }

            val width = bitmap.width
            val height = bitmap.height
            val bounding = dpToPx(size)

            val scale = bounding.toFloat() / height

            var n_width=0
            var n_height=0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270   -> {
                    n_width = (height.toFloat() * scale).toInt()
                    n_height = (width.toFloat() * scale).toInt()
                }
                else -> {
                    n_width = (width.toFloat() * scale).toInt()
                    n_height = (height.toFloat() * scale).toInt()
                }
            }

            // Now change ImageView's dimensions to match the scaled image
            val params = view.layoutParams
            params.width = n_width
            params.height = n_height
            view.layoutParams = params
        }

        @Throws(NoSuchElementException::class)
        fun scaleImage(view: ImageView, size: Int, orientation: Int) {
            // Get bitmap from the the ImageView.
            var bitmap = try {
                val drawing: Drawable = view.drawable
                (drawing as BitmapDrawable).bitmap
            } catch (e: NullPointerException) {
                throw NoSuchElementException("No drawable on given view")
            } catch (e: ClassCastException) {
                // Check bitmap is Ion drawable
                BitmapFactory.decodeResource(view.context.resources, R.drawable.user)
            }

            if(bitmap==null){
                bitmap=BitmapFactory.decodeResource(view.context.resources, R.drawable.user)
            }

            val width = bitmap.width
            val height = bitmap.height
            val bounding = dpToPx(size)

            val xScale = bounding.toFloat() / width
            val yScale = bounding.toFloat() / height
            val scale = if (xScale <= yScale) xScale else yScale

            var n_width=0
            var n_height=0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270   -> {
                    n_width = (height.toFloat() * scale).toInt()
                    n_height = (width.toFloat() * scale).toInt()
                }
                else -> {
                    n_width = (width.toFloat() * scale).toInt()
                    n_height = (height.toFloat() * scale).toInt()
                }
            }

            // Now change ImageView's dimensions to match the scaled image
            val params = view.layoutParams
            params.width = n_width
            params.height = n_height
            view.layoutParams = params
        }


        fun dpToPx(dp: Int): Int {
            return (dp * Resources.getSystem().displayMetrics.density).toInt()
        }

        fun pxToDp(px: Int): Int {
            return (px / Resources.getSystem().displayMetrics.density).toInt()
        }

        fun add_image_from_file(imageStream: InputStream,callback: (sha1_new: String,sha1_old: String) -> Unit): Pair<Bitmap,String> {
            val outputStream = ByteArrayOutputStream()
            var bytesRead = -1
            val buf = ByteArray(BUFFER_SIZE)
            while (imageStream.read(buf).also { bytesRead = it } != -1) {
                outputStream.write(buf, 0, bytesRead)
            }
            var ba = outputStream.toByteArray()
            val sha1_old = encodeHex(MessageDigest.getInstance("SHA-1").digest(ba))

            if (!MainActivity.files_src_map_all.contains(sha1_old)) {
                val src = save_to_file(
                    ba,
                    File(MainActivity.dir_images, sha1_old)
                )
                MainActivity.files_src_map_all[sha1_old] = src
                MainActivity.save_files_to_file()
            }
            val orientation_old = orientation(sha1_old)
            val bmp = BitmapFactory.decodeByteArray(ba,0,ba.size)
            val size = bmp.rowBytes * bmp.height
            val buffer: ByteBuffer = ByteBuffer.allocate(size)
            bmp.copyPixelsToBuffer(buffer)
            ba = buffer.array()
            Thread {
                val sha1_new = compress_webp(ba,bmp.width,bmp.height,MainActivity.dir_images!!.absolutePath,orientation_old)
                if (!MainActivity.files_src_map_all.contains(sha1_new)) {

                    val file = File(MainActivity.dir_images,sha1_new)
                    MainActivity.files_src_map_all[sha1_new] = file.absolutePath
                    File(MainActivity.files_src_map_all[sha1_old]!!).delete()
                    MainActivity.files_src_map_all.remove(sha1_old)
                    MainActivity.save_files_to_file()
                }
                callback(sha1_new,sha1_old)
            }.start()

            return Pair(bmp,sha1_old)
        }

        fun readFiletoByteArray(file: File): ByteArray {
            val bFile = ByteArray(file.length().toInt())
            try {
                val fileInputStream = FileInputStream(file)
                fileInputStream.read(bFile)
                fileInputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bFile
        }

        external fun compress_webp(data: ByteArray, width: Int, height: Int, jstr: String,orient: Int): String

        init {
            System.loadLibrary("native-lib")
        }

        fun isCallPermissionGranted(activity: Activity,requestCode: Int):Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                    true
                } else {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO), requestCode)
                    false
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                true
            }
        }
    }
}