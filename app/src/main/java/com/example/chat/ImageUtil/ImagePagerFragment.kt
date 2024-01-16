package com.example.chat.ImageUtil

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import com.example.chat.UserUtil.MyUser.Companion.add_images_map
import com.example.chat.UserUtil.MyUser.Companion.images_list
import com.example.chat.UserUtil.MyUser.Companion.remove_images_map
import com.example.chat.Util
import com.example.chat.Util.Companion.RESULT_LOAD_IMG
import com.example.chat.Util.Companion.add_image_from_file
import com.example.chat.Util.Companion.orientation
import com.example.chat.Util.Companion.readFiletoByteArray
import com.example.chat.ui.CircleImageView
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException


class ImagesAdapter(
    val imagePagerFragment: ImagePagerFragment
) : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

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
        val image: Image = images_list[position]
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
        return images_list.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mImage: ImageView = mView.findViewById(R.id.ivImageView)
    }
}

class FragmentPagerAdapter(
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return images_list.size
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
        return FragmentImageView.newInstance(images_list[position])
    }
}

class ImagePagerFragment : Fragment(),
    OnPageChangeListener {
    private var position = 0
    var viewPage: ViewPager? = null
    private var layoutManager: LinearLayoutManager? = null

    var adapter_ViewPager: FragmentPagerAdapter? = null
    var adapter_RecyclerView: ImagesAdapter? = null

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
        val pos = images_list.indexOf(
            MyUser.avatar
        )
        viewPage?.currentItem = pos
        return root
    }

    override fun onStart() {
        super.onStart()
        MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility=View.GONE
        MainActivity.activity?.activityMainBinding?.appBarMain?.tvAddition?.text=""
        MainActivity.activity?.activityMainBinding?.appBarMain?.textviewTitle?.text = "Images"
        MainActivity.activity?.activityMainBinding?.appBarMain?.circleImageView?.visibility = View.GONE

//        val h = Handler()
//        h.postDelayed({
//            adapter_ViewPager?.notifyDataSetChanged()
//        },50)

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
                if(images_list.isNotEmpty()) {
                    val sha1 = images_list[position].sha1
                    remove_images_map(sha1)
                    if (images_list.isNotEmpty()) {
                        position = 0
                        viewPage?.currentItem = position
                        MyUser.avatar = images_list[0]
                        Util.set_image_bitmap(
                            MainActivity.activity?.activityMainBinding?.navView?.getHeaderView(0)?.findViewById(R.id.iv_icon_user),
                            MyUser.avatar
                        )
                    } else {
                        MainActivity.activity?.activityMainBinding?.navView?.getHeaderView(0)?.findViewById<CircleImageView>(R.id.iv_icon_user)?.scaleType = ImageView.ScaleType.FIT_CENTER
                        MainActivity.activity?.activityMainBinding?.navView?.getHeaderView(0)?.findViewById<CircleImageView>(R.id.iv_icon_user)?.setImageBitmap(
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
                        remove_images_map(sha1_old)
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
                    val im = Image(
                        MainActivity.files_src_map_all[sha1_old]!!,
                        sha1_old,
                        MainActivity.files_src_map_all[sha1_old]!!,
                        orientation(sha1_old),
                        bmp
                    )
                    add_images_map(sha1_old,im)

                    MyUser.avatar = im
                    MainActivity.runOnUiThread(
                        Runnable {
                            Util.set_image_bitmap(
                                MainActivity.activity?.activityMainBinding?.navView?.getHeaderView(0)?.findViewById<CircleImageView>(R.id.iv_icon_user),
                                MyUser.avatar
                            )
                            viewPage?.currentItem = images_list.size - 1
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
        val im = Image(
            MainActivity.files_src_map_all[sha1_new]!!,
            sha1_new,
            MainActivity.files_src_map_all[sha1_new]!!,
            orientation(sha1_new)
        )
        add_images_map(sha1_new, im)

        MyUser.avatar = im
        MainActivity.runOnUiThread(Runnable {
            Util.set_image_bitmap(
                MainActivity.activity?.activityMainBinding?.navView?.getHeaderView(0)?.findViewById<CircleImageView>(R.id.iv_icon_user),
                MyUser.avatar
            )
            viewPage?.currentItem = images_list.size - 1
        })
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        this.position = position
        MyUser.avatar = images_list[position]
        MainActivity.activity?.let {
            Util.set_image_bitmap(
                it.activityMainBinding.navView.getHeaderView(0)?.findViewById<CircleImageView>(R.id.iv_icon_user),
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