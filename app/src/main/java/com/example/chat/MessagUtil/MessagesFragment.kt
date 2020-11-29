package com.example.chat.MessagUtil


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.*
import com.example.chat.CallUtil.CallFragment
import com.example.chat.ImageUtil.Image
import com.example.chat.ImageUtil.Record
import com.example.chat.MainActivity.Companion.current_user
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.User
import com.example.chat.UserUtil.UsersFragment
import com.example.chat.Util.Companion.RESULT_LOAD_IMG
import com.example.chat.Util.Companion.add_image_from_file
import com.example.chat.Util.Companion.encodeHex
import com.example.chat.Util.Companion.isCallPermissionGranted
import com.example.chat.Util.Companion.orientation
import com.example.chat.Util.Companion.readFiletoByteArray
import com.example.chat.Util.Companion.save_ba_to_file
import com.example.chat.ui.BaseFragment
import com.example.chat.ui.MyConstraintLayout
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*

class MessagesFragment(val user: User) : BaseFragment() {

    var fileName_rec: String = ""
    private var recorder: MediaRecorder? = null

    var rootView: View?= null
    var random: Boolean = false
    lateinit var adapter: MessagesAdapter
    private var layoutManager: LinearLayoutManager? = null

    var is_record = false
    var x = 0
    var y = 0

    override fun onBackPressed() {
        super.onBackPressed()
        MyUser.send_webSocket_(
            JSONObject().put(
                "random_chat_close", JSONObject()
                    .put("username", user.username)
            ).toString()
        )
    }

    override fun onStart() {
        cur_MessagesFragment=this
        super.onStart()
        if(user.activ){
            MainActivity.activity?.iv_activ_user?.visibility=View.VISIBLE
        } else {
            MainActivity.activity?.iv_activ_user?.visibility=View.GONE
        }
        if(!random) {
            MainActivity.activity?.tv_addition?.text=""
            MainActivity.activity?.textview_title?.text = user.name
            MainActivity.activity?.circleImageView?.visibility = View.VISIBLE
            Util.set_image_bitmap(
                MainActivity.activity?.circleImageView,
                user.avatar
            )
            MyUser.send_webSocket_(
                JSONObject().put(
                    "get_user_avatar", JSONObject()
                        .put("username", user.username)
                ).toString()
            )
        }
    }

    var isOpened = false

    fun setListenerToRootView(rootView: View) {
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height;
            if (heightDiff > 200) {
                if (isOpened == false) {
                    layoutManager?.scrollToPosition(adapter?.messages!!.size-1)
                }
                isOpened = true;
            } else if (isOpened == true) {
                layoutManager?.scrollToPosition(adapter?.messages!!.size-1)
                isOpened = false
            }
        }
    }

    fun show_rand_close(){
        val cl_random_close = rootView?.findViewById<ConstraintLayout>(R.id.cl_random_close)
        cl_random_close?.visibility=View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        current_user=user
        rootView = inflater.inflate(R.layout.fragment_messages_list, container, false)
        val button_close = rootView?.findViewById<Button>(R.id.button_close)
        button_close?.setOnClickListener {
            RandomFragment.rand_user = null
            MainActivity.activity?.onBackPressed()
        }

        val button_view = rootView?.findViewById<Button>(R.id.button_view)
        button_view?.setOnClickListener {
            val cl_random_close = rootView?.findViewById<ConstraintLayout>(R.id.cl_random_close)
            cl_random_close?.visibility=View.GONE
            val ll_send_mess = rootView?.findViewById<LinearLayout>(R.id.ll_send_mess)
            ll_send_mess?.visibility=View.GONE
        }

        val iv_send = rootView?.findViewById<ImageView>(R.id.iv_send)
        val editText = rootView?.findViewById<EditText>(R.id.editText)

        val ll_send = rootView?.findViewById<LinearLayout>(R.id.ll_send)
        val ll_send_record = rootView?.findViewById<LinearLayout>(R.id.ll_send_record)
        val cl_iv_record = rootView?.findViewById<MyConstraintLayout>(R.id.cl_iv_record)
        val iv_record = rootView?.findViewById<ImageView>(R.id.iv_record)

        editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p0?.length==0){
                    iv_record?.visibility=View.VISIBLE
                    ll_send_record?.visibility=View.VISIBLE
                    ll_send?.visibility=View.GONE
                } else {
                    iv_record?.visibility=View.GONE
                    ll_send_record?.visibility=View.GONE
                    ll_send?.visibility=View.VISIBLE
                }
            }
        })

        iv_record?.setOnTouchListener { view, motionEvent ->
            if(isCallPermissionGranted(
                    requireActivity(),
                    25
                )
            ) {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        iv_record.setBackgroundResource(R.drawable.ic_record_red)

                        val newLayoutParams = iv_record.layoutParams as ConstraintLayout.LayoutParams
                        newLayoutParams.width=
                            Util.dpToPx(80)
                        newLayoutParams.height=
                            Util.dpToPx(80)
                        iv_record.layoutParams = newLayoutParams

                        val time = Date()
                        val file = File(MainActivity.dir_record, time.time.toString())
                        fileName_rec = file.absolutePath
                        onRecord(true)
                    }
                }
            }
            true
        }

        cl_iv_record?.setOnTouchListener { view, motionEvent ->
            if(is_record) {
                x = motionEvent.x.toInt()
                y = motionEvent.y.toInt()
                when (motionEvent?.action) {
                    MotionEvent.ACTION_MOVE -> {
                        val newLayoutParams =
                            iv_record?.layoutParams as ConstraintLayout.LayoutParams
                        newLayoutParams.bottomMargin =
                            cl_iv_record.height - y - iv_record.height / 2
                        newLayoutParams.rightMargin = cl_iv_record.width - x - iv_record.width / 2
                        iv_record.layoutParams = newLayoutParams
                    }
                    MotionEvent.ACTION_UP -> {
                        val newLayoutParams =
                            iv_record?.layoutParams as ConstraintLayout.LayoutParams
                        newLayoutParams.bottomMargin = 0
                        newLayoutParams.rightMargin =
                            Util.dpToPx(4)
                        newLayoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                        newLayoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                        iv_record.layoutParams = newLayoutParams
                        iv_record.setBackgroundResource(R.drawable.ic_mic_black_24dp)
                        up_record()
                    }
                }
            }
            false
        }

        setListenerToRootView(rootView!!)
        iv_send?.setOnClickListener {
            val message = editText?.text.toString()
            if (message.length > 0) {
                val time = Date()
                editText?.text?.clear()
                user.messages_map[time.time] =
                    Message(
                        message,
                        time,
                        true,
                        user.username
                    )
                adapter.messages = user.messages_map.values.toMutableList()
                adapter.notifyDataSetChanged()
                layoutManager?.scrollToPosition(adapter.messages.size-1)
                MyUser.send_webSocket_arh(JSONObject()
                        .put(
                            "send_mess", JSONObject()
                                .put("user_receiver", user.username)
                                .put("mess", message)
                                .put("time_sender", time.time)
                                .put("random", random)
                        ).toString()
                )
            }
        }

        val iv_send_image=rootView?.findViewById<ImageView>(R.id.iv_send_image)
        iv_send_image?.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG)
        }

        adapter = MessagesAdapter(this,user)
        adapter.messages = user.messages_map.values.toMutableList()
        layoutManager=LinearLayoutManager(context)

        val list: RecyclerView = rootView?.findViewById(R.id.list)!!
        with(list) {
            layoutManager =  this@MessagesFragment.layoutManager
            adapter = this@MessagesFragment.adapter
        }
        layoutManager?.scrollToPosition(adapter.messages.size-1)
        return rootView
    }

    var mess_cont:Message?=null

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        mess_cont = v.tag as Message
        val inflater: MenuInflater = requireActivity().menuInflater
        inflater.inflate(R.menu.menu_mess, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.dell_messeg_item -> {
                val dialog = MessDialogFragment(mutableListOf(mess_cont!!), user, adapter!!)
                dialog.show(requireActivity().supportFragmentManager,"123")
                return true
            }
        }
        return super.onContextItemSelected(item)
    }
    private fun up_record() {
        onRecord(false)
        val time = Date()
        val file = File(fileName_rec)
        val ba = readFiletoByteArray(File(fileName_rec))
        val sha1 =
            encodeHex(MessageDigest.getInstance("SHA-1").digest(ba))
        val file_new = File(MainActivity.dir_record, sha1)
        file.renameTo(file_new)

        fileName_rec = file_new.absolutePath
        MainActivity.files_src_map_all[sha1] = fileName_rec
        MainActivity.save_files_to_file()

        MyUser.send_webSocket_arh(ba)
        MyUser.send_webSocket_arh(JSONObject()
                .put("send_mess_file", JSONObject()
                        .put("user_receiver", user.username)
                        .put("mess", "Record")
                        .put("time_sender", time.time)
                        .put("sha", sha1)
                        .put("mime", "sound")
                        .put("random", random)
                        //.put("ba", ba)
                ).toString()
        )

        user.messages_map[time.time] = Message(
            "Record",
            time,
            true, user.username
        )
        user.messages_map[time.time]?.record =
            Record(fileName_rec, sha1, "")

        adapter.messages = user.messages_map.values.toMutableList()
        adapter.notifyDataSetChanged()
        layoutManager?.scrollToPosition(adapter.messages.size - 1)
    }

    private fun onRecord(start: Boolean) = if (start) {
        is_record = true
        startRecording()
    } else {
        is_record = false
        stopRecording()
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName_rec)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("LOG_TAG", "prepare() failed")
            }
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                val h = Handler()
                val run = object : Runnable {
                    override fun run() {
                        stopRecording()
                    }
                }
                h.postDelayed(run, 100)
                Log.e("LOG_TAG", "stop() failed")
            }
        }
        recorder = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Thread {
                try {
                    val selectedPicture: Uri = data?.data!!
                    val imageStream = activity?.contentResolver?.openInputStream(selectedPicture)!!
                    val time = Date()
                    val (bmp,sha1) = add_image_from_file(imageStream) { sha1_new, sha1_old ->
                        val mes = user.messages_map[time.time]!!
                        mes.image= Image(
                            MainActivity.files_src_map_all[sha1_new]!!,
                            sha1_new,
                            MainActivity.files_src_map_all[sha1_new]!!,
                            orientation(sha1_new)
                        )
                        val ba = readFiletoByteArray(File(MainActivity.files_src_map_all[sha1_new]!!))
                        MyUser.send_webSocket_arh(ba)
                        MyUser.send_webSocket_arh(JSONObject()
                                .put("send_mess_file", JSONObject()
                                        .put("user_receiver", user.username)
                                        .put("mess", mes.text_mess)
                                        .put("time_sender", time.time)
                                        .put("sha", sha1_new)
                                        .put("mime", "image")
                                        .put("random", random)
                                        //.put("ba", ba)
                                ).toString()
                        )
                    }

                    user.messages_map[time.time] =
                        Message(
                            "Image", time,true, user.username,
                            Image(
                                MainActivity.files_src_map_all[sha1]!!,
                                sha1,
                                MainActivity.files_src_map_all[sha1]!!,
                                orientation(sha1)
                            )
                        )
                    user.messages_map[time.time]?.image?.bitmap = bmp
                    adapter?.messages = user.messages_map.values.toMutableList()
                    MainActivity.runOnUiThread(
                        Runnable {
                            adapter?.notifyDataSetChanged()
                            layoutManager?.scrollToPosition(adapter?.messages!!.size - 1)
                        })
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(activity, "Something went wrong", Toast.LENGTH_LONG).show()
                }
            }.start()
        } else {
            Toast.makeText(activity, "You haven't picked Image", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu (menu: Menu,
                                      inflater: MenuInflater
    ) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(adapter.list_select_mess.isEmpty()){
            inflater.inflate(R.menu.menu_mess_frag, menu)
        } else {
            inflater.inflate(R.menu.menu_mess, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.call_item -> {
                if(MyUser.is_login) {
                    if(user.activ) {
                        if (isCallPermissionGranted(
                                requireActivity(),
                                1
                            )
                        ) {
                            val fragmentManager = requireActivity().supportFragmentManager
                            val random = user == RandomFragment.rand_user
                            val fragment =
                                CallFragment.newInstance(
                                    user.username,
                                    random
                                )
                            fragmentManager.beginTransaction()
                                .replace(R.id.container_frag, fragment, "CallFragment")
                                .addToBackStack("CallFragment")
                                .commit()
                        } else {
                            MainActivity.username_call = user.username
                            MainActivity.offer_call = null
                        }
                    } else {
                        Toast.makeText(requireContext(), "Пользователь не в сети", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isCallPermissionGranted(
                        requireActivity(),
                        25
                    )
                    Toast.makeText(requireContext(), "Нет инета", Toast.LENGTH_SHORT).show()
                }
                false
            }
            R.id.clear_messegs -> {
                user.messages_map.clear()
                adapter.messages.clear()
                adapter.notifyDataSetChanged()
                false
            }

            R.id.dell_messeg_item -> {
                val dialog = MessDialogFragment(adapter.list_select_mess.toMutableList(), user, adapter)
                dialog.show(requireActivity().supportFragmentManager,"123")
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        var binary_file: ByteArray? = null

        var cur_MessagesFragment: MessagesFragment?=null

        fun newInstance(user: User) =
            MessagesFragment(user)

        fun del_mess(it: JSONObject) {
            val time = it.getLong("time")
            val username = it.getString("username")
            val random = it.getBoolean("random")

            val user : User
            user = if(random){
                RandomFragment.rand_user!!
            } else {
                MyUser.users_map[username]!!
            }

            val mess = user.messages_map[time]
            user.messages_map.remove(time)
            cur_MessagesFragment?.adapter?.list_select_mess?.remove(mess)
            user.messag_fragment?.adapter?.messages=user.messages_map.values.toMutableList()
            MainActivity.runOnUiThread(Runnable {
                user.messag_fragment?.adapter?.notifyDataSetChanged()
                if(cur_MessagesFragment!=null && cur_MessagesFragment!!.adapter.list_select_mess.isEmpty()){
                    cur_MessagesFragment!!.adapter.mf.requireActivity().invalidateOptionsMenu()
                }
            })

            MyUser.send_webSocket_arh(JSONObject()
                    .put(
                        "del_mess_true", JSONObject()
                            .put("time", time)
                    ).toString()
            )
        }

        fun is_send_mess(it: JSONObject) {
            val time = it.getLong("time")
            val user_receiver = it.getString("user_receiver")
            val random = it.getBoolean("random")

            val user : User
            user = if(random){
                RandomFragment.rand_user!!
            } else {
                MyUser.users_map[user_receiver]!!
            }
            user.messages_map[time]?.isSend = true
            MainActivity.runOnUiThread(Runnable {
                user.messag_fragment?.adapter?.notifyDataSetChanged()
            })
        }

        fun is_messag_read(it: JSONObject) {
            val time = it.getLong("time")
            val user_receiver = it.getString("user_receiver")
            val random = it.getBoolean("random")

            val user : User
            user = if(random){
                RandomFragment.rand_user!!
            } else {
                MyUser.users_map[user_receiver]!!
            }
            user.messages_map[time]?.isRead=true
            MainActivity.runOnUiThread(Runnable {
                user.messag_fragment?.adapter?.notifyDataSetChanged()
            })

            MyUser.send_webSocket_arh(JSONObject()
                    .put(
                        "is_messag_read", JSONObject()
                            .put("time", time)
                    ).toString()
            )
        }

        fun is_messag_read_receiver(it: JSONObject) {
            val time = it.getLong("time")
            val sender = it.getString("user_sender")
            val random = it.getBoolean("random")

            val user : User
            user = if(random){
                RandomFragment.rand_user!!
            } else {
                MyUser.users_map[sender]!!
            }
            user.messages_map[time]?.isRead=true
            MainActivity.runOnUiThread(Runnable {
                user.messag_fragment?.adapter?.notifyDataSetChanged()
            })
        }

        fun is_messag_received(it: JSONObject) {
            val time = it.getLong("time")
            val user_receiver = it.getString("user_receiver")
            val random = it.getBoolean("random")

            val user : User
            user = if(random){
                RandomFragment.rand_user!!
            } else {
                MyUser.users_map[user_receiver]!!
            }
            user.messages_map[time]?.isReceived=true
            MainActivity.runOnUiThread(Runnable {
                user.messag_fragment?.adapter?.notifyDataSetChanged()
            })

            MyUser.send_webSocket_arh(JSONObject()
                    .put(
                        "is_messag_received", JSONObject()
                            .put("time", time)
                    ).toString()
            )
        }

        fun receive_messag(it: JSONObject) {
            val time = it.getLong("time")
            val date = Date(time)
            val text_mess = it.getString("text")
            val user_sender = it.getString("user_sender")
            val user_sender_name = it.getString("user_sender_name")
            val random = it.getBoolean("random")

            val user : User
            if(random){
                user = RandomFragment.rand_user!!
            } else {
                if (!MyUser.users_map.contains(user_sender)) {
                    MyUser.users_map[user_sender] =
                        User(
                            user_sender,
                            user_sender_name
                        )
                    UsersFragment.usersFragment?.adapter?.users = MyUser.users_map.values.toMutableList()
                }
                user = MyUser.users_map[user_sender]!!
            }

            if (!user.messages_map.contains(time)) {
                user.messages_map[time] = Message(
                    text_mess,
                    date,
                    false,
                    user_sender
                )
                user.messag_fragment?.adapter?.messages = user.messages_map.values.toMutableList()
                MainActivity.runOnUiThread(Runnable {
                    UsersFragment.usersFragment?.adapter?.notifyDataSetChanged()
                    user.messag_fragment?.adapter?.notifyDataSetChanged()
                    user.messag_fragment?.layoutManager?.scrollToPosition(user.messag_fragment?.adapter?.messages!!.size - 1)
                })
            }

            val url_str = it.getString("url")
            if (url_str != "") {
                val sha1 = it.getString("sha")
                val mime = it.getString("mime")

                if (!MainActivity.files_src_map_all.contains(sha1)) {
                    Thread {
                        if(binary_file !=null){
                            save_ba_to_file(binary_file!!,sha1)
                            binary_file = null
                        } else {
                            val url = URL(url_str)
                            val connection: HttpURLConnection =
                                url.openConnection() as HttpURLConnection
                            connection.doInput = true
                            connection.connect()
                            val inputStream = connection.inputStream
                            val (ba, sha1) = Util.add_file_from_inputStream(
                                inputStream
                            )
                        }
                        if(mime=="image") {
                            user.messages_map[time]?.image =
                                Image(
                                    MainActivity.files_src_map_all[sha1]!!,
                                    sha1,
                                    url_str,
                                    orientation(sha1)
                                )
                        }
                        if(mime=="sound") {
                            user.messages_map[time]?.record =
                                Record(
                                    MainActivity.files_src_map_all[sha1]!!,
                                    sha1,
                                    url_str
                                )
                        }
                        MainActivity.runOnUiThread(
                            Runnable {
                                user.messag_fragment?.adapter?.notifyDataSetChanged()
                            })
                    }.start()
                } else {
                    binary_file = null
                    if(mime=="image") {
                        user.messages_map[time]?.image =
                            Image(
                                MainActivity.files_src_map_all[sha1]!!,
                                sha1,
                                url_str,
                                orientation(sha1)
                            )
                    }
                    if(mime=="sound") {
                        user.messages_map[time]?.record =
                            Record(
                                MainActivity.files_src_map_all[sha1]!!,
                                sha1,
                                url_str
                            )
                    }
                    MainActivity.runOnUiThread(
                        Runnable {
                            user.messag_fragment?.adapter?.notifyDataSetChanged()
                        })
                }
            }

            val list_mess: MutableList<Message> = mutableListOf()
            for(a in user.messages_map){
                if(!a.value.isView && !a.value.isMy){
                    list_mess.add(a.value)
                }
            }
            if(random){
                MainActivity.notif_show(
                    list_mess,
                    "user"
                )
            } else {
                MainActivity.notif_show(
                    list_mess,
                    user.username
                )
            }

            MyUser.send_webSocket_arh(JSONObject()
                .put(
                    "messag_received", JSONObject().put("time", time)
                ).toString()
            )
        }
    }
}