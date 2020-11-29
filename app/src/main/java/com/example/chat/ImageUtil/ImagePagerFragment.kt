package com.example.chat.ImageUtil

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.chat.MainActivity
import com.example.chat.R
import com.example.chat.UserUtil.MyUser
import com.example.chat.Util
import com.example.chat.Util.Companion.RESULT_LOAD_IMG
import com.example.chat.Util.Companion.add_image_from_file
import com.example.chat.Util.Companion.orientation
import com.example.chat.Util.Companion.readFiletoByteArray
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import kotlin.collections.set


class ImagesAdapter(
    val imagePagerFragment: ImagePagerFragment
) : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    var images: MutableList<Image> = mutableListOf()

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val position = v.tag as Int
            imagePagerFragment.viewPage?.currentItem = position
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
        images = MyUser.images_map.values.toMutableList()
        return images.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mImage: ImageView = mView.findViewById(R.id.ivImageView)
    }
}

class FragmentPagerAdapter(
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var images: MutableList<Image> = mutableListOf()

    override fun getCount(): Int {
        images = MyUser.images_map.values.toMutableList()
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

class ImagePagerFragment : Fragment(),
    OnPageChangeListener {
    private var position = 0
    var viewPage: ViewPager? = null
    private var adapter_ViewPager: FragmentPagerAdapter? = null
    private var layoutManager: LinearLayoutManager? = null

    private var adapter_RecyclerView: ImagesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imagePagerFragment =this
        setHasOptionsMenu(true)
        val root: View = inflater.inflate(R.layout.activity_imageview_page, container, false)
        viewPage = root.findViewById(R.id.viewPager)
        adapter_ViewPager = FragmentPagerAdapter(
            requireActivity().supportFragmentManager
        )
        viewPage?.adapter = adapter_ViewPager
        viewPage?.addOnPageChangeListener(this)
        layoutManager=LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter_RecyclerView = ImagesAdapter(this)
        val list: RecyclerView = root.findViewById(R.id.list_images)
        with(list) {
            layoutManager = this@ImagePagerFragment.layoutManager
            adapter = this@ImagePagerFragment.adapter_RecyclerView
        }
        val pos = MyUser.images_map.values.toMutableList().indexOf(
            MyUser.avatar
        )
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_images, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_image -> {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG)
                true
            }
            R.id.dell_image -> {
                if(MyUser.images_map.isNotEmpty()) {
                    val sha1 = adapter_ViewPager?.images!![position].sha1
                    MyUser.images_map.remove(sha1)
                    adapter_RecyclerView?.notifyDataSetChanged()
                    adapter_ViewPager?.notifyDataSetChanged()

                    if (MyUser.images_map.isNotEmpty()) {
                        position = 0
                        viewPage?.currentItem = position
                        MyUser.avatar = MyUser.images_map.values.toMutableList()[0]
                        Util.set_image_bitmap(
                            MainActivity.activity?.iv_icon_user,
                            MyUser.avatar
                        )
                    } else {
                        MainActivity.activity?.iv_icon_user?.scaleType = ImageView.ScaleType.FIT_CENTER
                        MainActivity.activity?.iv_icon_user?.setImageBitmap(
                            BitmapFactory.decodeResource(
                                MainActivity.activity?.resources,
                                R.drawable.user
                            )
                        )
                    }

                    MyUser.send_webSocket_arh(JSONObject()
                        .put("del_image", sha1)
                        .toString()
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Thread {
                try {
                    val selectedPicture: Uri = data?.data!!
                    val imageStream = requireActivity().contentResolver.openInputStream(selectedPicture)!!

                    val (bmp,sha1_old) = add_image_from_file(imageStream) { sha1_new, sha1_old ->
                        MyUser.images_map.remove(sha1_old)
                        add_images_map_user(sha1_new)
                        val ba = readFiletoByteArray(File(MainActivity.files_src_map_all[sha1_new]!!))
                        MyUser.send_webSocket_arh(ba)
                        MyUser.send_webSocket_arh(JSONObject()
                                .put("set_file", JSONObject()
                                        .put("sha", sha1_new)
                                        .put("mime", "image")
                                        //.put("ba", Base64.encodeToString(ba,0))
                                ).toString()
                        )
                        MyUser.send_webSocket_arh(JSONObject()
                                .put(
                                    "set_avatar", JSONObject()
                                        .put("sha", sha1_new)
                                ).toString()
                        )
                    }
                    MyUser.images_map[sha1_old] =
                        Image(
                            MainActivity.files_src_map_all[sha1_old]!!,
                            sha1_old,
                            MainActivity.files_src_map_all[sha1_old]!!,
                            orientation(sha1_old)
                        )
                    MyUser.images_map[sha1_old]?.bitmap=bmp

                    MyUser.avatar = MyUser.images_map[sha1_old]!!
                    MainActivity.runOnUiThread(
                        Runnable {
                            Util.set_image_bitmap(
                                MainActivity.activity?.iv_icon_user,
                                MyUser.avatar
                            )
                            adapter_ViewPager?.notifyDataSetChanged()
                            adapter_RecyclerView?.notifyDataSetChanged()
                            viewPage?.currentItem = adapter_ViewPager!!.images.size - 1
                        })
                } catch (e: FileNotFoundException) {
                    MainActivity.runOnUiThread(
                        Runnable {
                            e.printStackTrace()
                            Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_LONG).show()
                        })
                }
            }.start()
        } else {
            Toast.makeText(requireContext(), "You haven't picked Image", Toast.LENGTH_LONG).show()
        }
    }

    private fun add_images_map_user(sha1_new: String) {
        MyUser.images_map[sha1_new] = Image(
            MainActivity.files_src_map_all[sha1_new]!!,
            sha1_new,
            MainActivity.files_src_map_all[sha1_new]!!,
            orientation(sha1_new)
        )

        MyUser.avatar = MyUser.images_map[sha1_new]!!
        MainActivity.runOnUiThread(Runnable {
            Util.set_image_bitmap(
                MainActivity.activity?.iv_icon_user,
                MyUser.avatar
            )
            adapter_ViewPager?.notifyDataSetChanged()
            adapter_RecyclerView?.notifyDataSetChanged()
            viewPage?.currentItem = adapter_ViewPager!!.images.size - 1
        })
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        this.position = position
        MyUser.avatar = MyUser.images_map.values.toMutableList()[position]
        MainActivity.activity?.let {
            Util.set_image_bitmap(
                it.iv_icon_user,
                MyUser.avatar
            )
        }
        MyUser.send_webSocket_arh(JSONObject()
                .put(
                    "set_avatar", JSONObject()
                        .put(
                            "sha",
                            MyUser.avatar.sha1
                        )
                ).toString()
        )
        layoutManager?.scrollToPosition(position)
    }

    companion object{
        var imagePagerFragment: ImagePagerFragment? = null

        fun newInstance(): ImagePagerFragment {
            return ImagePagerFragment()
        }
    }
}