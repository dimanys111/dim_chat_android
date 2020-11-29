package com.example.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.chat.MainActivity.Companion.dir_users
import com.example.chat.MessagUtil.MessagesFragment
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.User
import com.example.chat.ui.BaseFragment
import com.example.chat.ui.BlurredLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*


class RandomFragment : BaseFragment() {

    var is_search = false
    var fl_par: FrameLayout?=null
    var cl_pb: ConstraintLayout?=null
    var bl: BlurredLayout?=null
    var cl: ConstraintLayout?=null
    var ll_mask: LinearLayout?=null

    var pol_my = 0
    var pol_their = 0
    var old_my = 0
    var old_their_list = mutableListOf<Int>()
    var want_my_int = 0

    override fun onBackPressed() {
        super.onBackPressed()
        is_search=false
        MyUser.send_webSocket_(
            JSONObject().put(
                "disable_random_search",""
            ).toString()
        )
        ll_mask?.visibility=View.GONE
        (cl?.parent as ViewGroup).removeView(cl)
        fl_par?.addView(cl)
        cl_pb?.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        open_param_to_file()
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_random, container, false) as View
        val cg_pol_my = view.findViewById<ChipGroup>(R.id.cg_pol_my)
        (cg_pol_my.getChildAt(pol_my) as Chip).isChecked=true
        val cg_pol_their = view.findViewById<ChipGroup>(R.id.cg_pol_their)
        (cg_pol_their.getChildAt(pol_their) as Chip).isChecked=true
        val cg_old_my = view.findViewById<ChipGroup>(R.id.cg_old_my)
        (cg_old_my.getChildAt(old_my) as Chip).isChecked=true
        val cg_old_their = view.findViewById<ChipGroup>(R.id.cg_old_their)
        for(a in old_their_list){
            (cg_old_their.getChildAt(a) as Chip).isChecked=true
        }
        val cg_want = view.findViewById<ChipGroup>(R.id.cg_want)
        (cg_want.getChildAt(want_my_int) as Chip).isChecked=true
        val search_but = view.findViewById<Chip>(R.id.search)

        fl_par = view.findViewById(R.id.fl_par)
        cl_pb = view.findViewById(R.id.cl_pb)
        bl = view.findViewById(R.id.bl)
        cl = view.findViewById(R.id.cl)

        ll_mask = view.findViewById(R.id.ll_mask)

        val but_search_close = view.findViewById<Button>(R.id.but_search_close)

        but_search_close.setOnClickListener {
            onBackPressed()
        }

        search_but.setOnClickListener {
            if(MyUser.is_login) {
                for (i in 0 until cg_pol_my.childCount) {
                    val chip = cg_pol_my.getChildAt(i) as Chip
                    if (chip.isChecked) {
                        pol_my = i
                        break
                    }
                }
                for (i in 0 until cg_pol_their.childCount) {
                    val chip = cg_pol_their.getChildAt(i) as Chip
                    if (chip.isChecked) {
                        pol_their = i
                        break
                    }
                }

                for (i in 0 until cg_old_my.childCount) {
                    val chip = cg_old_my.getChildAt(i) as Chip
                    if (chip.isChecked) {
                        old_my = i
                        break
                    }
                }

                val old_their_array = JSONArray()
                old_their_list = mutableListOf()
                for (i in 0 until cg_old_their.childCount) {
                    val chip = cg_old_their.getChildAt(i) as Chip
                    if (chip.isChecked) {
                        old_their_array.put(i)
                        old_their_list.add(i)
                    }
                }

                var want_my = ""
                for (i in 0 until cg_want.childCount) {
                    val chip = cg_want.getChildAt(i) as Chip
                    if (chip.isChecked) {
                        want_my = chip.text.toString()
                        want_my_int = i
                        break
                    }
                }

                save_param_to_file()

                MyUser.send_webSocket_(
                    JSONObject().put(
                        "random_search",
                        JSONObject()
                            .put("my_pol", pol_my)
                            .put("their_pol", pol_their)
                            .put("old_my", old_my)
                            .put("old_their", old_their_array)
                            .put("want_my", want_my)
                    ).toString()
                )

                is_search = true

                ll_mask?.visibility=View.VISIBLE
                (cl?.parent as ViewGroup).removeView(cl)
                bl?.addView(cl)
                cl_pb?.visibility = View.VISIBLE
            } else {
                Toast.makeText(MyApplication.appContext, "Нет инета", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    fun open_param_to_file() {
        val file = File(dir_users, "random_chat_param")
        if (file.exists()) {
            val inputStream = ObjectInputStream(FileInputStream(file))
            pol_my = inputStream.readInt()

            pol_their = inputStream.readInt()
            old_my = inputStream.readInt()
            want_my_int = inputStream.readInt()
            old_their_list = inputStream.readObject() as MutableList<Int>
            inputStream.close()
        }
    }

    fun save_param_to_file() {
        val file = File(dir_users, "random_chat_param")
        val outputStream = ObjectOutputStream(FileOutputStream(file))
        outputStream.writeInt(pol_my)
        outputStream.writeInt(pol_their)
        outputStream.writeInt(old_my)
        outputStream.writeInt(want_my_int)
        outputStream.writeObject(old_their_list)
        outputStream.flush()
        outputStream.close()
    }

    override fun onStart() {
        super.onStart()
        MainActivity.activity?.tv_addition?.text="собеседник"
        MainActivity.activity?.circleImageView?.visibility=View.GONE
        MainActivity.activity?.textview_title?.text = "Случайный"
        MainActivity.activity?.iv_activ_user?.visibility=View.GONE
    }

    override fun onStop() {
        super.onStop()


        cl_pb?.visibility=View.GONE
    }

    companion object {
        var randomFragment:RandomFragment?=null
        var rand_user: User? = null

        fun newInstance(): RandomFragment {
            if(randomFragment==null) {
                randomFragment = RandomFragment()
            }
            return randomFragment!!
        }

        fun find_random_user(it: JSONObject) {
            randomFragment?.is_search = false
            val username = it.getString("username")
            val name = it.getString("name")
            val want = it.getString("want")
            rand_user = User(username, name)
            rand_user?.activ=true
            MainActivity.runOnUiThread(Runnable{
                MainActivity.activity?.tv_addition?.text="Хочет "+want
                val fragmentManager = MainActivity.activity?.supportFragmentManager
                rand_user?.messag_fragment = MessagesFragment.newInstance(rand_user!!)
                rand_user?.messag_fragment!!.random = true
                fragmentManager?.beginTransaction()?.replace(R.id.container_frag, rand_user!!.messag_fragment!!)
                    ?.addToBackStack(rand_user!!.username)
                    ?.commit()
            })
        }

        fun random_chat_close(it: JSONObject) {
            MainActivity.runOnUiThread(Runnable{
                rand_user?.messag_fragment?.show_rand_close()
                rand_user = null
            })
        }
    }
}
