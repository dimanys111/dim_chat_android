package com.example.chat.UserUtil

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.chat.*
import com.example.chat.ImageUtil.Image
import com.example.chat.ImageUtil.ImagePagerFragment.Companion.imagePagerFragment
import com.example.chat.MainActivity.Companion.list_websok_send_mess
import com.example.chat.Util.Companion.orientation
import com.example.chat.ui.login.LoginActivity
import com.example.chat.ui.login.RegistrActivity
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.android.synthetic.main.nav_header_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class MyUser {

    companion object{

        private const val SERVER = "wss://defimov.ddns.net/ws"
        private const val TIMEOUT = 10000

        var dir_current_user: File? = null

        private val KEY_PREF_USER = "KEY_PREF_USER_"
        private val KEY_PREF_PASS = "KEY_PREF_PASS_"
        private val KEY_PREF_NAME = "KEY_PREF_NAME_"

        var username = ""
        var password = ""
        var name: String = "User"

        var images_map_: MutableMap<String, Image> = mutableMapOf()
        var images_list: MutableList<Image> = images_map_.values.toMutableList()

        fun set_images_map(im: MutableMap<String, Image>) {
            images_map_=im
            images_list = images_map_.values.toMutableList()
            MainActivity.runOnUiThread(Runnable {
                imagePagerFragment?.adapter_ViewPager?.notifyDataSetChanged()
                imagePagerFragment?.adapter_RecyclerView?.notifyDataSetChanged()
            })
        }

        fun add_images_map(string: String, image: Image) {
            images_map_[string]=image
            MainActivity.runOnUiThread(Runnable {
                images_list = images_map_.values.toMutableList()
                imagePagerFragment?.adapter_ViewPager?.notifyDataSetChanged()
                imagePagerFragment?.adapter_RecyclerView?.notifyDataSetChanged()
            })
        }
        fun remove_images_map(string: String) {
            images_map_.remove(string)
            MainActivity.runOnUiThread(Runnable {
                images_list = images_map_.values.toMutableList()
                imagePagerFragment?.adapter_ViewPager?.notifyDataSetChanged()
                imagePagerFragment?.adapter_RecyclerView?.notifyDataSetChanged()
            })
        }

        var avatar = Image("", "", "", 0)
        var users_map: MutableMap<String, User> = mutableMapOf()

        var ws: WebSocket? = null
        var is_login = false

        fun open_users_from_file() {
            val file = File(dir_current_user, "user_map")
            if (file.exists()) {
                val inputStream = ObjectInputStream(FileInputStream(file))
                users_map = inputStream.readObject() as MutableMap<String, User>
                inputStream.close()
            } else {
                users_map = mutableMapOf()
            }
        }

        fun save()
        {
            save_users_to_file()
            save_images_to_file()
            save_pref_user_pass()
        }

        fun save_users_to_file() {
            val file = File(dir_current_user, "user_map")
            val outputStream = ObjectOutputStream(FileOutputStream(file))
            outputStream.writeObject(users_map)
            outputStream.flush()
            outputStream.close()
        }

        fun open_images_to_file() {
            val file = File(dir_current_user, "images_map")
            if (file.exists()) {
                val inputStream = ObjectInputStream(FileInputStream(file))
                set_images_map(inputStream.readObject() as MutableMap<String, Image>)
                avatar = inputStream.readObject() as Image
                inputStream.close()
            } else {
                set_images_map(mutableMapOf())
                avatar = Image("", "", "", 0)
            }
        }

        fun save_images_to_file() {
            val file = File(dir_current_user, "images_map")
            val outputStream = ObjectOutputStream(FileOutputStream(file))
            outputStream.writeObject(images_map_)
            outputStream.writeObject(avatar)
            outputStream.flush()
            outputStream.close()
        }

        fun save_pref_user_pass(){
            val edit: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(
                MyApplication.appContext
            ).edit()
            edit.putString(
                KEY_PREF_USER + MainActivity.current_username,
                username
            )
            edit.putString(
                KEY_PREF_PASS + MainActivity.current_username,
                password
            )
            edit.putString(
                KEY_PREF_NAME + MainActivity.current_username,
                name
            )
            edit.apply()
        }

        fun open_pref_user_pass() {
            val prefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(MyApplication.appContext)
            username = prefs.getString(
                KEY_PREF_USER + MainActivity.current_username, "")!!
            password = prefs.getString(
                KEY_PREF_PASS + MainActivity.current_username, "")!!
            name = prefs.getString(
                KEY_PREF_NAME + MainActivity.current_username, "")!!

//            val edit: SharedPreferences.Editor = prefs.edit()
//            edit.putString(
//                SettingsFragment.key_user_name_pref,
//                name
//            )
//            edit.apply()
        }

        fun clear() {
            if(password!="") {
                save()
            }
            val del_user = username
            username = ""
            MainActivity.save_pref_current_user(username)
            init()
            MainActivity.activity?.start_UsersFragment()
            MainActivity.tv_in?.visibility=View.VISIBLE
            MainActivity.tv_user?.text = name
            MainActivity.activity?.iv_icon_user?.setImageResource( R.drawable.user )
            UsersFragment.usersFragment?.adapter?.users = users_map.values.toMutableList()
            UsersFragment.usersFragment?.adapter?.notifyDataSetChanged()
            send_webSocket_arh(JSONObject()
                .put("del_user", del_user)
                .toString()
            )
            send_webSocket_arh(JSONObject()
                .put("login", JSONObject()
                    .put("username", username)
                    .put("password",password)
                ).toString()
            )
        }

        private fun clear_images_users() {
            avatar = Image("", "", "", 0)
            set_images_map(mutableMapOf())
            save_images_to_file()
            users_map = mutableMapOf()
            save_users_to_file()
        }

        private fun clear_username_name_password() {
            username = ""
            name = "User"
            password = ""
            save_pref_user_pass()
        }

        private fun clear_curent_user() {
            clear_username_name_password()
            clear_images_users()
        }

        fun init() {
            set_dir()
            open_init()
        }

        private fun open_init() {
            open_pref_user_pass()
            open_users_from_file()
            open_images_to_file()
        }

        private fun set_dir() {
            dir_current_user = File(
                MainActivity.dir_users,
                MainActivity.current_username
            )
            if (!dir_current_user!!.exists()) {
                dir_current_user!!.mkdirs()
            }
        }

        fun login(it: JSONObject) {
            val username = it.getString("username")
            val name = it.getString("name")
            if(username != Companion.username) {
                MainActivity.runOnUiThread(Runnable {
                    LoginActivity.loading?.visibility = View.GONE
                })
            } else {
                MainActivity.runOnUiThread(Runnable {
                    LoginActivity.activity?.finish()
                })
            }
            if (MainActivity.current_username != username) {
                if(password =="") {
                    MainActivity.runOnUiThread(Runnable {
                            MainActivity.tv_in?.visibility = View.VISIBLE
                        })
                } else {
                    MainActivity.runOnUiThread(Runnable {
                            MainActivity.tv_in?.visibility = View.GONE
                        })
                }
                MainActivity.save_pref_current_user(username)

                set_dir()
                open_users_from_file()
                open_images_to_file()

                UsersFragment.usersFragment?.adapter?.users = users_map.values.toMutableList()
                MainActivity.runOnUiThread(Runnable {
                    MainActivity.activity?.tv_user?.text = name
                    UsersFragment.usersFragment?.adapter?.notifyDataSetChanged()
                })
            }
            Companion.username = username
            Companion.name = name
            save_pref_user_pass()

            val images_url = it.getJSONArray("images_url")
            val images_sha = it.getJSONArray("images_sha")

            Thread {
                for (i in 0 until images_url.length()) {
                    val sha1 = images_sha[i].toString()
                    val url_str = images_url[i].toString()
                    if (!MainActivity.files_src_map_all.contains(sha1)) {
                        try {
                            val url = URL(url_str)
                            val connection: HttpURLConnection =
                                url.openConnection() as HttpURLConnection
                            connection.doInput = true
                            connection.connect()
                            val inputStream = connection.inputStream
                            val (ba, sha1) = Util.add_file_from_inputStream(
                                inputStream
                            )
                            add_images_map(sha1, Image(
                                MainActivity.files_src_map_all[sha1]!!,
                                sha1,
                                url_str,
                                orientation(sha1)
                            ))
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                        }
                    } else {
                        if (!images_map_.contains(sha1)) {
                            add_images_map(sha1, Image(
                                MainActivity.files_src_map_all[sha1]!!,
                                sha1,
                                url_str,
                                orientation(sha1)
                            ))
                        }
                    }
                }

                if (it.get("avatar_url").toString() != "") {
                    val sha1 = it.get("avatar_sha").toString()
                    if(avatar.sha1!=sha1) {
                        avatar.url = it.get("avatar_url").toString()
                        avatar.sha1 = it.get("avatar_sha").toString()
                        set_avatar_users(
                            avatar.sha1
                        )
                    }
                } else {
                    MainActivity.runOnUiThread(
                        Runnable {
                            MainActivity.activity?.iv_icon_user?.setImageBitmap(
                                BitmapFactory.decodeResource(
                                    MainActivity.activity?.resources,
                                    R.drawable.user
                                )
                            )
                        })
                }

            }.start()
            val ja = JSONArray()
            for(user in users_map){
                ja.put(user.value.username)
            }
            send_webSocket_(
                JSONObject()
                    .put(
                        "send_activ", JSONObject()
                            .put("usernames", ja)
                    ).toString()
            )
        }

        fun registr(it: JSONObject) {
            val user = it.get("user").toString()
            if (user != "") {
                MainActivity.runOnUiThread(Runnable {
                    RegistrActivity.activity?.finish()
                })
            } else {
                MainActivity.runOnUiThread(Runnable {
                    RegistrActivity.loading?.visibility = View.GONE
                    Toast.makeText(
                        MyApplication.appContext,
                        "Такой пользователь есть!",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }
        }

        private fun set_avatar_users(sha1: String) {
            if (!MainActivity.files_src_map_all.contains(sha1)) {
                try {
                    val url = URL(avatar.url)
                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.inputStream
                    val (ba, sha1) = Util.add_file_from_inputStream(
                        inputStream
                    )
                    val im = Image(
                        MainActivity.files_src_map_all[sha1]!!,
                        sha1,
                        avatar.url,
                        orientation(sha1)
                    )
                    add_images_map(sha1, im)
                    avatar = im
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            } else {
                if(!images_map_.contains(sha1)){
                    add_images_map(sha1, Image(
                        MainActivity.files_src_map_all[sha1]!!,
                        sha1,
                        avatar.url,
                        orientation(sha1)
                    ))
                } else {
                    images_map_[sha1]?.bitmap=BitmapFactory.decodeFile(
                        images_map_[sha1]?.image_src)
                }
                avatar = images_map_[sha1]!!
            }
            MainActivity.runOnUiThread(Runnable {
                Util.set_image_bitmap(
                    MainActivity.activity?.iv_icon_user,
                    avatar
                )
            })
        }

        fun start() {
            if(ws ==null) {
                ws =
                    connect()
            }
        }

        private fun connect(): WebSocket? {
            val factory = WebSocketFactory().setConnectionTimeout(TIMEOUT)
            try {
                val ws = factory.createSocket(SERVER)
                ws.addListener(WebSocketListener())
                ws.connectAsynchronously()
                return ws
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun send_all(){
            val ml = list_websok_send_mess.toMutableList()
            for(a in ml) {
                if(is_login) {
                    if (a is ByteArray) {
                        ws?.sendBinary(a)
                    } else {
                        if (a is String) {
                            ws?.sendText(a)
                        }
                    }
                    list_websok_send_mess.remove(a)
                }
            }
        }

        fun send_webSocket_(any: Any){
            start()
            list_websok_send_mess.add(any)
            if(is_login) {
                send_all()
            } else {
                list_websok_send_mess.remove(any)
            }
        }

        fun send_webSocket_arh(any: Any){
            start()
            if(!list_websok_send_mess.contains(any)) {
                list_websok_send_mess.add(any)
            }
            if(is_login) {
                send_all()
            }
        }


    }
}
