package com.example.chat.SearchUtil

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.ImageUtil.Image
import com.example.chat.MainActivity
import com.example.chat.R
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.User
import com.example.chat.UserUtil.UsersFragment
import com.example.chat.Util
import com.example.chat.Util.Companion.orientation
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SearchFragment : Fragment() {
    var searchMenuItem: MenuItem? = null
    var search: SearchView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_search_list, container, false)

        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                Companion.adapter =
                    SearchAdapter(
                        mutableListOf()
                    )
                adapter =
                    Companion.adapter
            }
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)

        searchMenuItem = menu.findItem(R.id.search)
        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                val fragment =
                    UsersFragment.newInstance()
                val fragmentManager = activity!!.supportFragmentManager
                fragmentManager.beginTransaction().replace(R.id.container_frag, fragment).commit()
                return true
            }
        })

        search = searchMenuItem?.actionView as SearchView


        search?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText!="") {
                    val jo = JSONObject().put("get_find_users", newText)
                    MyUser.send_webSocket_(jo.toString())
                } else {
                    adapter?.searchUsers= mutableListOf()
                    adapter?.notifyDataSetChanged()
                }
                return true
            }

        })

        searchMenuItem?.expandActionView()
    }


    companion object {
        var adapter: SearchAdapter? = null

        fun newInstance() = SearchFragment()

        fun set_find_users(it: JSONObject) {
            val name = it.getJSONArray("name")
            val username = it.getJSONArray("username")
            val url_avatars = it.getJSONArray("url_avatars")
            val sha1_avatars = it.getJSONArray("sha1_avatars")
            val activs = it.getJSONArray("activs")
            val list_search_user = mutableListOf<User>()

            for (i in 0 until username.length()) {
                val user = User(
                    username.getString(i),
                    name.getString(i),
                    activs.getBoolean(i)
                )
                list_search_user.add(user)
                val url_avatar = url_avatars.getString(i)
                val sha1_avatar = sha1_avatars.getString(i)
                if (url_avatar != "") {
                    Thread {
                        if (!MainActivity.files_src_map_all.contains(sha1_avatar)) {
                            val url = URL(url_avatar)
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
                                url_avatar,
                                orientation(sha1)
                            )

                        } else {
                            user.avatar = Image(
                                MainActivity.files_src_map_all[sha1_avatar]!!,
                                sha1_avatar,
                                url_avatar,
                                orientation(sha1_avatar)
                            )
                        }
                        MainActivity.runOnUiThread(
                            Runnable {
                                adapter?.notifyDataSetChanged()
                            })
                    }.start()
                }
            }
            MainActivity.runOnUiThread(Runnable {
                adapter?.searchUsers =
                    list_search_user
                adapter?.notifyDataSetChanged()
            })
        }
    }


}
