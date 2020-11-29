package com.example.chat.MessagUtil

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.ImageUtil.Image
import com.example.chat.ImageUtil.ImagePagerMessFragment
import com.example.chat.MainActivity
import com.example.chat.MainActivity.Companion.activity
import com.example.chat.MediaPlayer.MediaPlayerHolder
import com.example.chat.MediaPlayer.PlaybackInfoListener
import com.example.chat.MyApplication
import com.example.chat.R
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.User
import com.example.chat.Util
import org.json.JSONObject


class MessagesAdapter(val mf: MessagesFragment, val user: User) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var messages: MutableList<Message>
    private val mOnClickListener: View.OnClickListener
    private val mIVOnClickListener: View.OnClickListener
    private val mOnLongClickListener: View.OnLongClickListener
    val mOnSeekBarChangeListener: OnSeekBarChangeListener
    private var mUserIsSeeking = false
    private var is_playing = false
    var mPlayerAdapter: MediaPlayerHolder? = null
    var list_select_mess: MutableList<Message> = mutableListOf()

    init {
        mPlayerAdapter =
            MediaPlayerHolder(MyApplication.appContext)

        mOnSeekBarChangeListener = object : OnSeekBarChangeListener {
            var userSelectedPosition = 0
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mUserIsSeeking = true
            }

            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    userSelectedPosition = progress
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mUserIsSeeking = false
                mPlayerAdapter!!.seekTo(userSelectedPosition)
            }
        }

        mIVOnClickListener = View.OnClickListener { v ->
            val message = v.tag as Message
            val images: MutableList<Image> = mutableListOf()
            for(m in messages){
                if(m.image!=null){
                    images.add(m.image!!)
                }
            }
            val fragment =
                ImagePagerMessFragment.newInstance(
                    images,
                    images.indexOf(message.image)
                )
            val fragmentManager = activity!!.supportFragmentManager
            fragment.let {
                fragmentManager.beginTransaction().replace(R.id.container_frag, it)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }

        mOnClickListener = View.OnClickListener { v ->
            if(list_select_mess.isEmpty()) {
                v.showContextMenu()
            } else {
                val mes = v.tag as Message
                if(list_select_mess.contains(mes)){
                    list_select_mess.remove(mes)
                    if(list_select_mess.isEmpty()) {
                        mf.requireActivity().invalidateOptionsMenu()
                    }
                } else {
                    list_select_mess.add(mes)
                }
                notifyDataSetChanged()
            }
        }

        mOnLongClickListener = View.OnLongClickListener {v ->
            if(list_select_mess.isEmpty()) {
                mf.requireActivity().invalidateOptionsMenu()

                list_select_mess.add(v.tag as Message)
                notifyDataSetChanged()
            }
            return@OnLongClickListener true
        }
    }

    override fun getItemViewType(position: Int): Int {
        val mess = messages[position]
        if(mess.image==null) {
            if(mess.record!=null){
                return 2
            } else {
                return 0
            }
        } else {
            return 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        lateinit var vh:RecyclerView.ViewHolder
        when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message, parent, false)
                vh = ViewHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_image, parent, false)
                vh = ViewHolderImage(view)
            }
            2 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_record, parent, false)
                vh = ViewHolderRecord(view)
            }
        }

        return vh
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message: Message = messages[position]

        if(list_select_mess.contains(message)){
            holder.itemView.setBackgroundResource(R.drawable.bord_image)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        message.isView=true
        val list_mess: MutableList<Message> = mutableListOf()
        for(a in user.messages_map){
            if(!a.value.isView && !a.value.isMy){
                list_mess.add(a.value)
            }
        }
        if(list_mess.isEmpty()) {
            MainActivity.notif_close(user.username)
        }

        if(!message.isMy && !message.isRead){
            MyUser.send_webSocket_arh(JSONObject()
                    .put(
                        "messag_read", JSONObject()
                            .put("time", message.time.time)
                    ).toString()
            )
        }

        if(holder is ViewHolderBasic) {
            if(message.isMy){
                (holder.itemView as LinearLayout).gravity = Gravity.END
            } else {
                (holder.itemView as LinearLayout).gravity = Gravity.START
            }
            if(message.isMy){
                holder.iv_receiver.visibility = View.VISIBLE
            } else {
                holder.iv_receiver.visibility = View.GONE
            }
            if(message.isSend) {
                if(message.isRead) {
                    holder.iv_receiver.setImageResource(R.drawable.ic_check_mark_2_green)
                } else {
                    if(message.isReceived) {
                        holder.iv_receiver.setImageResource(R.drawable.ic_check_mark_2)
                    } else {
                        holder.iv_receiver.setImageResource(R.drawable.ic_check_mark)
                    }
                }
            } else {
                holder.iv_receiver.setImageResource(R.drawable.ic_access_time_black)
            }


            holder.time.text = Message.sdf.format(message.time)

            with(holder.mView) {
                tag = message
                mf.registerForContextMenu(this)
                setOnClickListener(mOnClickListener)
                setOnLongClickListener(mOnLongClickListener)
            }
        }

        if(holder is ViewHolderRecord) {
            if(message.isMy){
                holder.iv_left.visibility=View.VISIBLE
                holder.iv_right.visibility=View.GONE
            } else {
                holder.iv_left.visibility=View.GONE
                holder.iv_right.visibility=View.VISIBLE
            }

            mPlayerAdapter!!.setPlaybackInfoListener(holder.playbackListener)
            mPlayerAdapter!!.loadMedia(message.record!!.record_src)

            holder.iv_play_pause.setOnClickListener {
                if(!is_playing){
                    mPlayerAdapter!!.setPlaybackInfoListener(holder.playbackListener)
                    mPlayerAdapter!!.loadMedia(message.record!!.record_src)
                    mPlayerAdapter!!.play()
                } else {
                    mPlayerAdapter!!.pause()
                }
                is_playing=!is_playing
            }

        }
        if(holder is ViewHolder) {
            if(message.isMy){
                holder.cl_mess.setBackgroundResource(R.drawable.my_message)
            } else {
                holder.cl_mess.setBackgroundResource(R.drawable.their_message)
            }


            holder.messageBody.text = message.text_mess
        }
        if(holder is ViewHolderImage) {
            holder.imageView.setOnClickListener(mIVOnClickListener)
            holder.imageView.tag = message
            Util.set_image_bitmap(
                holder.imageView,
                message.image!!
            )
            Util.scaleImage(
                holder.imageView,
                250,
                message.image!!.orientation
            )
        }

    }

    override fun getItemCount(): Int {
        return messages.size
    }

    open inner class ViewHolderBasic(val mView: View) : RecyclerView.ViewHolder(mView) {
        val time: TextView = mView.findViewById(R.id.time)
        val iv_receiver: ImageView = mView.findViewById(R.id.iv_receiver)
    }

    inner class ViewHolder(mView: View) : ViewHolderBasic(mView) {
        val cl_mess : ConstraintLayout = mView.findViewById(R.id.cl_mess)
        val messageBody: TextView = mView.findViewById(R.id.message_body)
    }

    inner class ViewHolderImage(mView: View) : MessagesAdapter.ViewHolderBasic(mView) {
        val imageView: ImageView = mView.findViewById(R.id.message_body)
    }

    inner class ViewHolderRecord(mView: View) : MessagesAdapter.ViewHolderBasic(mView) {
        val tv_time_rec: TextView = mView.findViewById(R.id.tv_time_rec)
        val iv_play_pause: ImageView = mView.findViewById(R.id.iv_play_pause)
        val iv_left: ImageView = mView.findViewById(R.id.iv_left)
        val iv_right: ImageView = mView.findViewById(R.id.iv_right)
        val mSeekbarAudio:SeekBar = mView.findViewById(R.id.seekbar_audio)
        val playbackListener = PlaybackListener()

        init {
            mSeekbarAudio.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
        }

        inner class PlaybackListener :
            PlaybackInfoListener {
            override fun onDurationChanged(duration: Int) {
                MainActivity.runOnUiThread(Runnable {
                    mSeekbarAudio.max = duration
                    tv_time_rec.text = (duration.toFloat() / 1000).toInt().toString()
                })
            }

            override fun onPositionChanged(position: Int) {
                if (!mUserIsSeeking) {
                    MainActivity.runOnUiThread(
                        Runnable {
                            mSeekbarAudio.progress = position
                            if (position < 5)
                                tv_time_rec.text =
                                    (mSeekbarAudio.max.toFloat() / 1000).toInt().toString()
                            else
                                tv_time_rec.text = (position.toFloat() / 1000).toInt().toString()
                        })
                }
            }

            override fun onStateChanged(state: Int) {
                if(state==0){
                    is_playing=true
                    iv_play_pause.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp)
                }
                if(state==3){
                    is_playing=false
                    iv_play_pause.setImageResource(R.drawable.ic_play_circle_filled_black_24dp)
                }
            }

            override fun onPlaybackCompleted() {}
        }
    }

}
