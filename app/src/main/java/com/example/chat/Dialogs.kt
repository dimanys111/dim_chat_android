package com.example.chat

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.chat.CallUtil.CallFragment
import com.example.chat.MessagUtil.Message
import com.example.chat.MessagUtil.MessagesAdapter
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.User
import com.example.chat.UserUtil.UsersFragment
import com.example.chat.Util.Companion.isCallPermissionGranted
import org.json.JSONObject


class CallDialogFragment(val username: String, val offer: String, var random: Boolean) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        if(random){
            builder.setMessage("Звонок от " + "user")
        } else {
            builder.setMessage("Звонок от " + username)
        }

        builder.setPositiveButton("Ответить"
        ) { dialog, id ->
            if(isCallPermissionGranted(
                    requireActivity(),
                    1
                )
            ){
                val fragmentManager = requireActivity().supportFragmentManager
                val fragment= CallFragment.newInstance(username,random,offer)
                fragmentManager.beginTransaction().replace(R.id.container_frag, fragment,"CallFragment")
                    .addToBackStack("CallFragment")
                    .commit()
            } else {
                MainActivity.username_call=username
                MainActivity.offer_call=offer
                MainActivity.random_call=random
            }
            MainActivity.ring?.stop()
            MainActivity.wake_lock_destroy()
        }
        builder.setNegativeButton("Отвергнуть"
        ) { dialog, id ->
            MyUser.send_webSocket_(JSONObject().
            put("call_close", JSONObject().
            put("username", username)).toString())
            MainActivity.ring?.stop()
            MainActivity.activity?.lockScreen()
            MainActivity.wake_lock_destroy()
        }
        val dialog = builder.create()
        dialog.setOnKeyListener { arg0, keyCode, event -> // TODO Auto-generated method stub
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                MyUser.send_webSocket_(JSONObject()
                    .put("call_close", JSONObject()
                        .put("username", username)).toString())
                MainActivity.ring?.stop()
                MainActivity.activity?.lockScreen()
                MainActivity.wake_lock_destroy()
                dialog.dismiss()
            }
            true
        }
        return dialog
    }
}

class MessDialogFragment(val messs: MutableList<Message>, val user: User, val adapter: MessagesAdapter) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Удалить сообщение")
        builder.setPositiveButton("Удалить") { dialog, id ->
            for(mess in messs) {
                val time = mess.time.time
                user.messages_map.remove(time)
                adapter.list_select_mess.remove(mess)
            }
            adapter.messages=user.messages_map.values.toMutableList()
            if(adapter.list_select_mess.isEmpty()){
                adapter.mf.requireActivity().invalidateOptionsMenu()
            }
            adapter.notifyDataSetChanged()
        }
        var isMy=false
        for(mess in messs) {
            if(mess.isMy){
                isMy=true
                break
            }
        }
        if(isMy) {
            builder.setNegativeButton("Удалить и у "+user.name) { dialog, id ->
                for(mess in messs) {
                    val time = mess.time.time
                    user.messages_map.remove(time)
                    adapter.list_select_mess.remove(mess)
                    if(mess.isMy) {
                        MyUser.send_webSocket_arh(JSONObject()
                                .put(
                                    "del_mess_all", JSONObject()
                                        .put("time", time)
                                        .put("username", user.username)
                                        .put("random", adapter.mf.random)
                                ).toString()
                        )
                    }
                }
                adapter.messages=user.messages_map.values.toMutableList()
                if(adapter.list_select_mess.isEmpty()){
                    adapter.mf.requireActivity().invalidateOptionsMenu()
                }
                adapter.notifyDataSetChanged()
            }
        }
        builder.setNeutralButton("Закрать") { dialog, id -> }
        return builder.create()
    }
}

class UserDialogFragment(val user: User) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Удалить user")

        builder.setPositiveButton("Удалить"
        ) { dialog, id ->
            MyUser.users_map.remove(user.username)
            UsersFragment.usersFragment?.adapter?.users = MyUser.users_map.values.toMutableList()
            UsersFragment.usersFragment?.adapter?.notifyDataSetChanged()
        }

        builder.setNegativeButton("Закрать"
        ) { dialog, id ->
            // User cancelled the dialog
        }

        // Create the AlertDialog object and return it
        return builder.create()
    }
}