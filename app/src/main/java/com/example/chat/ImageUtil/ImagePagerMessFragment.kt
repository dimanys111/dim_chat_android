package com.example.chat.ImageUtil

import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.chat.MainActivity
import com.example.chat.R
import com.example.chat.Util
import kotlinx.android.synthetic.main.app_bar_main.*


class ImagesMessAdapter(
    val images: MutableList<Image>,
    val imagePagerMessFragment: ImagePagerMessFragment
) : RecyclerView.Adapter<ImagesMessAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val position = v.tag as Int
            imagePagerMessFragment.viewPage?.currentItem = position
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.imageview, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image: Image = images[position]
        Util.set_image_bitmap(holder.mImage, image)
        Util.scale_height_Image(
            holder.mImage,
            100,
            image.orientation
        )
        with(holder.mImage) {
            tag = position
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mImage: ImageView = mView.findViewById(R.id.ivImageView)
    }
}

class FragmentPagerMessAdapter(
    val images: MutableList<Image>,
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return images.size
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any
    ) {
        super.destroyItem(container, position, `object`)
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun getItem(position: Int): Fragment {
        return FragmentImageView.newInstance(images[position])
    }
}

class ImagePagerMessFragment(var images: MutableList<Image>, var pos: Int) : Fragment(),
    OnPageChangeListener {
    private var position = 0
    var viewPage: ViewPager? = null
    private var adapter_ViewPager: FragmentPagerMessAdapter? = null
    private var layoutManager: LinearLayoutManager? = null

    private var adapter_RecyclerView: ImagesMessAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val root: View = inflater.inflate(R.layout.activity_imageview_page, container, false)
        viewPage = root.findViewById(R.id.viewPager)
        adapter_ViewPager = FragmentPagerMessAdapter(
            images,
            requireActivity().supportFragmentManager
        )
        viewPage?.adapter = adapter_ViewPager
        viewPage?.addOnPageChangeListener(this)
        layoutManager=LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter_RecyclerView =
            ImagesMessAdapter(images, this)
        val list: RecyclerView = root.findViewById(R.id.list_images)
        with(list) {
            layoutManager = this@ImagePagerMessFragment.layoutManager
            adapter = this@ImagePagerMessFragment.adapter_RecyclerView
        }
        viewPage?.currentItem = pos
        return root
    }

    override fun onStart() {
        super.onStart()
        MainActivity.activity?.iv_activ_user?.visibility=View.GONE
        MainActivity.activity?.tv_addition?.text=""
        MainActivity.activity?.textview_title?.text = "Images"
        MainActivity.activity?.circleImageView?.visibility = View.GONE

        val h = Handler()
        h.postDelayed({
            adapter_ViewPager?.notifyDataSetChanged()
        },50)

    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        this.position = position
        layoutManager?.scrollToPosition(position)
    }

    companion object{
        fun newInstance(images: MutableList<Image>, pos: Int): ImagePagerMessFragment {
            return ImagePagerMessFragment(images, pos)
        }
    }
}