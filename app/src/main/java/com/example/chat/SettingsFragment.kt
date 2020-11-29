package com.example.chat

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.chat.UserUtil.MyUser
import com.google.common.base.CharMatcher
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.json.JSONObject


class SettingsFragment: PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setHasOptionsMenu(true)
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        sharedPref.registerOnSharedPreferenceChangeListener(this)
        val vlue_user_name_pref:EditTextPreference? = findPreference(key_user_name_pref)
        vlue_user_name_pref?.text = MyUser.name
    }

    override fun onStart() {
        super.onStart()
        MainActivity.activity?.iv_activ_user?.visibility= View.GONE
        Util.set_image_bitmap(MainActivity.activity?.circleImageView, MyUser.avatar)
        MainActivity.activity?.tv_addition?.text=
            MyUser.username
        MainActivity.activity?.textview_title?.text = "Настройки"
        MainActivity.activity?.circleImageView?.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_setings, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_exit -> {
                MyUser.clear()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        val key_user_name_pref:String="key_user_name_pref"
        var settingsFragment: SettingsFragment? = null

        fun newInstance(): SettingsFragment {
            if(settingsFragment==null) {
                settingsFragment = SettingsFragment()
            }
            return settingsFragment!!
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        if (p1==key_user_name_pref) {
            p0?.getString(key_user_name_pref, "User")?.let{
                if(!CharMatcher.ascii().matchesAllOf(it) || it==""){
                    val vlue_user_name_pref:EditTextPreference? = findPreference(key_user_name_pref)
                    vlue_user_name_pref?.text = MyUser.name
                } else {
                    MyUser.name = it
                    MyUser.save_pref_user_pass()
                    MainActivity.activity?.tv_user?.text = MyUser.name
                    MyUser.send_webSocket_arh(JSONObject()
                        .put("set_name",
                            JSONObject().put("name", MyUser.name)
                        ).toString()
                    )
                }
            }
        }
    }

}