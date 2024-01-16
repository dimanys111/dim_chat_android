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
import com.example.chat.databinding.ItemListBinding


class UsersAdapter(
    var users: MutableList<User>
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    private lateinit var itemListBinding: ItemListBinding

    private val mOnClickListener: View.OnClickListener = View.OnClickListener { v ->
        val user = v.tag as User
        val fragmentManager = activity!!.supportFragmentManager
        if(user.messag_fragment==null)
            user.messag_fragment= MessagesFragment.newInstance(user)
        fragmentManager.beginTransaction().replace(R.id.container_frag, user.messag_fragment!!)
            .addToBackStack(user.username)
            .commit()
    }
    private val mOnLongClickListener: View.OnLongClickListener = View.OnLongClickListener { v ->
        val user = v.tag as User
        val dialog = UserDialogFragment(user)
        dialog.show(activity!!.supportFragmentManager,"123")
        return@OnLongClickListener true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        itemListBinding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val view = itemListBinding.root
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
        val mUserView: ImageView =itemListBinding.image
        val iv_activ_user: ImageView = itemListBinding.ivActivUser
        val mMessView: TextView = itemListBinding.mess
        val mNameView: TextView = itemListBinding.name
        val ll: LinearLayout = itemListBinding.parentLayout

        override fun toString(): String {
            return super.toString() + " '" + mNameView.text + "'"
        }
    }
}
