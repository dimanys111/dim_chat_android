package com.example.chat.SearchUtil


import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.example.chat.UserUtil.User
import com.example.chat.Util
import com.example.chat.ui.CircleImageView
import kotlinx.android.synthetic.main.item_list.view.*


class SearchCursorAdapter(
    context: Context?,
    layout: Int,
    c: Cursor?,
    from: Array<String?>?,
    to: IntArray?,
    flags: Int,
    var searchUsers: MutableList<User>
) :
    SimpleCursorAdapter(context, layout, c, from, to, flags) {

    private val mOnClickListener: View.OnClickListener
    init {
        mOnClickListener = View.OnClickListener { v ->
            val user = v.tag as User
            //mListener?.onUsersFragment(user)
        }
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        super.bindView(view, context, cursor)
        val holder_ = view.tag
        if (holder_ == null) {
            val holder = holder_ as ViewHolder
            val user = searchUsers[0]
            holder.mNameView.text = user.username
            Util.set_image_bitmap(
                holder.mImageView,
                user.avatar
            )
            with(holder.ll) {
                tag = user
                setOnClickListener(mOnClickListener)
            }
            view.tag = holder
        }
    }

    inner class ViewHolder(mView: View)  {
        val mImageView: CircleImageView = mView.image
        val mNameView: TextView = mView.name
        val ll: LinearLayout = mView.parentLayout
    }


}