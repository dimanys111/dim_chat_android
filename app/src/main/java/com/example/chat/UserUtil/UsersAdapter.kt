package com.example.chat.UserUtil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.MainActivity.Companion.activity
import com.example.chat.MessagUtil.MessagesFragment
import com.example.chat.R
import com.example.chat.UserDialogFragment
import com.example.chat.Util
import kotlinx.android.synthetic.main.item_list.view.*

class UsersAdapter(
    var users: MutableList<User>
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    private val mOnLongClickListener: View.OnLongClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val user = v.tag as User
            val fragmentManager = activity!!.supportFragmentManager
            if(user.messag_fragment==null)
                user.messag_fragment= MessagesFragment.newInstance(user)
            fragmentManager.beginTransaction().replace(R.id.container_frag, user.messag_fragment!!)
                .addToBackStack(user.username)
                .commit()
        }
        mOnLongClickListener = View.OnLongClickListener {v ->
            val user = v.tag as User
            val dialog = UserDialogFragment(user)
            dialog.show(activity!!.supportFragmentManager,"123")
            return@OnLongClickListener true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.mNameView.text = user.name
        Util.set_image_bitmap(
            holder.mUserView,
            user.avatar
        )

        if(user.activ){
            holder.iv_activ_user.visibility=View.VISIBLE
        } else{
            holder.iv_activ_user.visibility=View.GONE
        }

        if(!user.messages_map.isEmpty()) {
            holder.mMessView.text = user.messages_map[user.messages_map.lastKey()]?.text_mess
        } else{
            holder.mMessView.text = "Пусто"
        }
        with(holder.ll) {
            tag = user
            setOnClickListener(mOnClickListener)
            setOnLongClickListener(mOnLongClickListener)
        }
    }

    override fun getItemCount(): Int = users.size

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mUserView: ImageView =mView.image
        val iv_activ_user: ImageView = mView.iv_activ_user
        val mMessView: TextView = mView.mess
        val mNameView: TextView = mView.name
        val ll: LinearLayout = mView.parentLayout

        override fun toString(): String {
            return super.toString() + " '" + mNameView.text + "'"
        }
    }
}
