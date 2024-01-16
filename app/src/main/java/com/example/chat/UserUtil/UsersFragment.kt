package com.example.chat.UserUtil

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.ImageUtil.Image
import com.example.chat.MainActivity
import com.example.chat.R
import com.example.chat.SearchUtil.SearchFragment
import com.example.chat.Util
import com.example.chat.Util.Companion.orientation

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class UsersFragment : Fragment() {

    var adapter: UsersAdapter? = null
    var searchMenuItem: MenuItem? = null
    var search: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_search_list, container, false)
        adapter = UsersAdapter(
            MyUser.users_map.values.toMutableList()
        )
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = this@UsersFragment.adapter
            }
        }
        return view
    }

    companion object {

        var usersFragment: UsersFragment? = null
        fun newInstance() : UsersFragment {
            if(usersFragment ==null) {
                usersFragment =
                    UsersFragment()
            }
            return usersFragment!!
        }

        fun set_user_avatar(it: JSONObject){
            val username = it.getString("username")
            val name = it.getString("name")
            val activ = it.getBoolean("activ")
            val user = MyUser.users_map[username]
            if(user!=null) {
                user.activ = activ
                user.name = name
                MainActivity.runOnUiThread(Runnable {
                    MainActivity.activity?.activityMainBinding?.appBarMain?.textviewTitle?.text = name
                    var b = false
                    user.messag_fragment?.isVisible?.let {
                        b = it
                    }
                    if (b) {
                        if (activ) {
                            MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility =
                                View.VISIBLE
                        } else {
                            MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility =
                                View.GONE
                        }
                    }
                })
                val sha_avatar_sender = it.getString("sha_avatar_sender")
                if (sha_avatar_sender != "") {
                    val url_avatar_sender = it.getString("url_avatar_sender")
                    if (user.avatar.sha1 != sha_avatar_sender) {
                        if (!MainActivity.files_src_map_all.contains(sha_avatar_sender)) {
                            Thread {
                                val url = URL(url_avatar_sender)
                                val connection: HttpURLConnection =
                                    url.openConnection() as HttpURLConnection
                                connection.doInput = true
                                connection.connect()
                                val inputStream = connection.inputStream
                                val (ba, sha1) = Util.add_file_from_inputStream(
                                    inputStream
                                )
                                user.avatar = Image(
                                    MainActivity.files_src_map_all[sha1]!!,
                                    sha1,
                                    url_avatar_sender,
                                    orientation(sha1)
                                )
                                MainActivity.runOnUiThread(
                                    Runnable {
                                        Util.set_image_bitmap(
                                            MainActivity.activity?.activityMainBinding?.appBarMain?.circleImageView,
                                            user.avatar
                                        )
                                    })
                            }.start()
                        } else {
                            user.avatar = Image(
                                MainActivity.files_src_map_all[sha_avatar_sender]!!,
                                sha_avatar_sender,
                                url_avatar_sender,
                                orientation(sha_avatar_sender)
                            )
                            MainActivity.runOnUiThread(
                                Runnable {
                                    Util.set_image_bitmap(
                                        MainActivity.activity?.activityMainBinding?.appBarMain?.circleImageView,
                                        user.avatar
                                    )
                                })
                        }
                    }
                }
            }
        }

        fun send_user_activ(it: JSONObject) {
            val username = it.getString("username")
            val activ = it.getBoolean("activ")
            val user = MyUser.users_map[username]
            user?.activ=activ
            MainActivity.runOnUiThread(Runnable {
                var b = false
                user?.messag_fragment?.isVisible?.let {
                    b = it
                }
                if (b) {
                    if (activ) {
                        MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility =
                            View.VISIBLE
                    } else {
                        MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility =
                            View.GONE
                    }
                }
                usersFragment?.adapter?.notifyDataSetChanged()
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        searchMenuItem = menu.findItem(R.id.search)
        search = searchMenuItem?.actionView as SearchView
        search?.setOnSearchClickListener {
            val fragment = SearchFragment.newInstance()
            val fragmentManager = requireActivity().supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.container_frag, fragment).commit()
        }
    }

    override fun onStart() {
        super.onStart()
        if(MyUser.is_login){
            MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility=View.VISIBLE
        } else {
            MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility=View.GONE
        }
        MainActivity.activity?.activityMainBinding?.appBarMain?.tvAddition?.text=""
        MainActivity.activity?.activityMainBinding?.appBarMain?.textviewTitle?.text = "Чат"
        MainActivity.activity?.activityMainBinding?.appBarMain?.circleImageView?.visibility=View.GONE
    }

    override fun onStop() {
        searchMenuItem?.collapseActionView()
        super.onStop()

    }
}
