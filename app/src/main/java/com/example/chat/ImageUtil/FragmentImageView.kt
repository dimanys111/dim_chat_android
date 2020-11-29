package com.example.chat.ImageUtil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.chat.R
import com.example.chat.Util
import com.jsibbold.zoomage.ZoomageView


class FragmentImageView(val image: Image) : Fragment() {

    private lateinit var ivImage: ZoomageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.imageview, container, false)
        ivImage = root.findViewById(R.id.ivImageView)
        Util.set_image_bitmap(ivImage, image)
        return root
    }

    companion object {
        fun newInstance(image: Image): FragmentImageView {
            return FragmentImageView(image)
        }
    }
}