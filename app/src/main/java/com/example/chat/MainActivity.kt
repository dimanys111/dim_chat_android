package com.example.chat

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.TaskStackBuilder
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.chat.CallUtil.CallFragment
import com.example.chat.CallUtil.CallFragment.Companion.dialog
import com.example.chat.ImageUtil.ImagePagerFragment
import com.example.chat.MessagUtil.Message
import com.example.chat.MessagUtil.MessagesFragment
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.User
import com.example.chat.UserUtil.UsersFragment
import com.example.chat.Util.Companion.hideKeyboard
import com.example.chat.ui.login.LoginActivity
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {
    var is_visible = false

    override fun onDestroy() {
        super.onDestroy()
        activity=null
        alarm_timer()
    }

    private val TIME_INTERVAL = 2000 // # milliseconds

    private var mBackPressed: Long = 0

    override fun onBackPressed() {
        if(MessagesFragment.cur_MessagesFragment!=null && MessagesFragment.cur_MessagesFragment!!.adapter.list_select_mess.isNotEmpty()){
            MessagesFragment.cur_MessagesFragment!!.adapter.list_select_mess.clear()
            MessagesFragment.cur_MessagesFragment?.adapter?.notifyDataSetChanged()
            invalidateOptionsMenu()
        } else {
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                drawer_layout.closeDrawer(GravityCompat.START)
            } else {
                var b_tim = false
                if (current_user != null && current_user == RandomFragment.rand_user && current_user!!.messag_fragment != null && current_user!!.messag_fragment!!.isVisible) {
                    Toast.makeText(baseContext, "Вы хотите завершить чат?", Toast.LENGTH_SHORT)
                        .show()
                    b_tim = true
                }
                if (CallFragment.callFragment != null && CallFragment.callFragment!!.isVisible) {
                    b_tim = true
                }
                if (RandomFragment.randomFragment != null && RandomFragment.randomFragment!!.isVisible && RandomFragment.randomFragment!!.is_search) {
                    b_tim = true
                }
                if (b_tim) {
                    if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
                        if (RandomFragment.randomFragment != null && RandomFragment.randomFragment!!.isVisible && RandomFragment.randomFragment!!.is_search) {
                            RandomFragment.randomFragment!!.onBackPressed()
                        } else {
                            if (CallFragment.callFragment != null && CallFragment.callFragment!!.isVisible) {
                                CallFragment.callFragment!!.onBackPressed()
                            }
                            if (current_user != null && current_user == RandomFragment.rand_user && current_user!!.messag_fragment != null && current_user!!.messag_fragment!!.isVisible) {
                                current_user!!.messag_fragment!!.onBackPressed()
                            }
                            super.onBackPressed()
                        }
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Нажмите ещё раз для выхода",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    super.onBackPressed()
                }
                mBackPressed = System.currentTimeMillis();
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==1) {
            val fragmentManager = supportFragmentManager
            val fragment = CallFragment.newInstance(username_call, random_call, offer_call)
            fragmentManager.beginTransaction().replace(R.id.container_frag, fragment, "CallFragment")
                .addToBackStack("CallFragment")
                .commit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity=this
        init()
        alarm_timer()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        nav_view.setNavigationItemSelectedListener(this)

        val toggle = object: ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ){
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                hideKeyboard(this@MainActivity)
            }
        }

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        start_UsersFragment()
        title = ""

        val headerView = nav_view.getHeaderView(0)
        val tv_in = headerView.findViewById(R.id.tv_in) as TextView
        MainActivity.tv_in=tv_in
        if(MyUser.password=="") {
            tv_in.visibility=View.VISIBLE
        } else {
            tv_in.visibility=View.GONE
        }
        tv_in.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val tv_user = headerView.findViewById(R.id.tv_user) as TextView
        MainActivity.tv_user = tv_user

        val iv_icon_user = headerView.findViewById(R.id.iv_icon_user) as ImageView
        iv_icon_user.setOnClickListener {
            if(ImagePagerFragment.imagePagerFragment==null || (ImagePagerFragment.imagePagerFragment!=null && !ImagePagerFragment.imagePagerFragment!!.isVisible)) {
                val fragment = ImagePagerFragment.newInstance()
                // Вставляем фрагмент, заменяя текущий фрагмент
                val fragmentManager = supportFragmentManager
                fragment.let {
                    fragmentManager.beginTransaction().replace(R.id.container_frag, it)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                }
            }
            val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        if(current_username == "") {
            tv_user.text = "User"
        }
        else {
            tv_user.text = MyUser.name
            if(MyUser.avatar.image_src!="") {
                Util.set_image_bitmap(iv_icon_user,
                    MyUser.avatar)
            }
        }
    }

    fun unlockScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    fun lockScreen(){
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    override fun onStart() {
        is_visible=true
        MyUser.start()
        val id = intent.getStringExtra("id")
        if(id=="call"){
            unlockScreen()
            val username = intent.getStringExtra("username")
            val offer = intent.getStringExtra("offer")
            val random = intent.getBooleanExtra("random",false)
            dialog = CallDialogFragment(username, offer,random)
            dialog?.show(supportFragmentManager,"123")
            intent = Intent()
        }
        val username_mess = intent.getStringExtra("username_mess")
        if(username_mess!=null){
            val fragmentManager = supportFragmentManager
            val user = MyUser.users_map[username_mess]
            if(user!=null) {
                if (user.messag_fragment == null)
                    user.messag_fragment = MessagesFragment.newInstance(user)
                fragmentManager.beginTransaction()
                    .replace(R.id.container_frag, user.messag_fragment!!)
                    .addToBackStack(user.username)
                    .commit()
            }
        }
        super.onStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        is_visible=false
        save_list_websok_send_mess_to_file()
        MyUser.save()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        hideKeyboard(this)
        // Создадим новый фрагмент
        var fragment: Fragment? = null
        // Handle navigation view item clicks here.
        try {
            when (item.itemId) {
                R.id.nav_users-> {
                    fragment= UsersFragment.newInstance()
                }
                R.id.nav_random-> {
                    fragment=RandomFragment.newInstance()
                }
                R.id.nav_setings->{
                    fragment=SettingsFragment.newInstance()
                }
            }
        } catch (e:Exception) {
            e.printStackTrace();
        }

        // Вставляем фрагмент, заменяя текущий фрагмент
        if(!fragment!!.isVisible) {
            val fragmentManager = supportFragmentManager
            fragment.let {
                fragmentManager.beginTransaction().replace(R.id.container_frag, it)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }
        // Выделяем выбранный пункт меню в шторке
        item.isChecked = true

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun start_UsersFragment(){
        val fragment = UsersFragment.newInstance()
        // Вставляем фрагмент, заменяя текущий фрагмент
        val fragmentManager = supportFragmentManager
        fragment.let { fragmentManager.beginTransaction().replace(R.id.container_frag, it)
            .commitAllowingStateLoss() }
    }

    companion object{
        var username_call = ""
        var offer_call:String? = null
        var random_call = false

        fun init() {
            if(handler==null) {
                handler = Handler()
                dir_users = File(MyApplication.appContext.getDir("data", Context.MODE_PRIVATE), "users")
                if(!dir_users!!.exists()){
                    dir_users?.mkdirs()
                }

                dir_images = File(dir_users, "images")
                if(!dir_images!!.exists()){
                    dir_images?.mkdirs()
                }
                dir_record = File(dir_users, "record")
                if(!dir_record!!.exists()){
                    dir_record?.mkdirs()
                }

                open_list_websok_send_mess_to_file()
                open_files_to_file()
                open_pref_current_user()
                MyUser.init()
            }
        }

        fun runOnUiThread(runnable: Runnable) {
            handler?.post(runnable)
        }

        fun save_pref_current_user(current_username_:String){
            current_username=current_username_
            val edit: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(MyApplication.appContext).edit()
            edit.putString(KEY_PREF_USER_CUR, current_username)
            edit.apply()
        }

        fun open_pref_current_user() {
            val prefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(MyApplication.appContext)
            current_username = prefs.getString(KEY_PREF_USER_CUR, "")!!
        }

        fun open_files_to_file() {
            val file = File(dir_users, "files_map_all")
            if (file.exists()) {
                val inputStream = ObjectInputStream(FileInputStream(file))
                files_src_map_all = inputStream.readObject() as MutableMap<String,String>
                inputStream.close()
            } else {
                files_src_map_all = mutableMapOf()
            }
        }

        fun save_files_to_file() {
            val file = File(dir_users, "files_map_all")
            val outputStream = ObjectOutputStream(FileOutputStream(file))
            outputStream.writeObject(files_src_map_all)
            outputStream.flush()
            outputStream.close()
        }

        fun alarm_timer() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 2)
            val time = calendar.timeInMillis

            val time_delta = AlarmManager.INTERVAL_HOUR / 30

            val alarmManager =
                MyApplication.appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                time,
                time_delta,
                getPendAlarm()
            )
        }

        private fun getPendAlarm(): PendingIntent? {
            val intent = Intent(MyApplication.appContext, AlarmTimerReceiver::class.java)
            return PendingIntent.getBroadcast(
                MyApplication.appContext,
                192,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun creatNotificationManagerCompat()
        {
            if(notificationManagerCompat==null) {
                update_notificationManager()
            }
        }

        fun update_notificationManager() {
            notificationManagerCompat = NotificationManagerCompat.from(MyApplication.appContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                create_channel_soind()
                create_channel()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun create_channel_soind() {
            val name = "channel_sound"
            val Description = "This is my channel sound"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID_SOUND, name, importance)
            val audioAttributes = AudioAttributes.Builder().build()
            val notifSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mChannel.setSound(notifSound, audioAttributes)
            mChannel.description = Description
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            notificationManagerCompat?.createNotificationChannel(mChannel)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun create_channel() {
            val name = "channel"
            val Description = "This is my channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.setSound(null, null)
            mChannel.description = Description
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            notificationManagerCompat?.createNotificationChannel(mChannel)
        }

        fun notif_show(list_mess: MutableList<Message>, username: String)
        {
            creatNotificationManagerCompat()
            notificationManagerCompat?.notify(username,NOTIFICATION_ID, notification_messag(list_mess,username))
        }

        fun notif_close(username: String)
        {
            creatNotificationManagerCompat()
            notificationManagerCompat?.cancel(username,NOTIFICATION_ID)
        }

        fun notification_messag(list_mess: MutableList<Message>, username: String): Notification {
            val resultIntent = Intent(MyApplication.appContext, MainActivity::class.java)
            resultIntent.putExtra("username_mess",username)
            val stackBuilder = TaskStackBuilder.create(MyApplication.appContext)
            stackBuilder.addNextIntentWithParentStack(resultIntent)
            val resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            val person = Person.Builder().setName(username).build()
            val person_my = Person.Builder().setName("My").build()

            val mess_styl = NotificationCompat.MessagingStyle(person_my)
            mess_styl.conversationTitle = "Arh"
            for(a in list_mess){
                mess_styl.addMessage(a.text_mess,a.time.time,person)
            }
            var CHANNEL_ID_= CHANNEL_ID
            if(bool_notif_sound){
                CHANNEL_ID_ = CHANNEL_ID_SOUND
            }
            val builder = NotificationCompat.Builder(MyApplication.appContext, CHANNEL_ID_)
                .setSmallIcon(R.drawable.ic_send_black_24dp)
                .setContentTitle("1")
                .setContentText("2")
                .setContentIntent(resultPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setStyle(mess_styl)
            if(bool_notif_sound){
                val notifSound : Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                builder.setDefaults(Notification.DEFAULT_SOUND).setSound(notifSound)
            } else {
                builder.setDefaults(Notification.DEFAULT_VIBRATE)
            }

            return builder.build()
        }

        fun wake_lock_create() {
            if (wakeLock == null) {
                val powerManager =
                    MyApplication.appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "my:Tag"
                )
                wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
            }
        }

        fun wake_lock_destroy() {
            wakeLock?.let {
                it.release()
                wakeLock = null
            }
        }

        fun save_list_websok_send_mess_to_file() {
            val file = File(dir_users, "list_websok_send_mess")
            val outputStream = ObjectOutputStream(FileOutputStream(file))
            outputStream.writeObject(list_websok_send_mess)
            outputStream.flush()
            outputStream.close()
        }

        fun open_list_websok_send_mess_to_file() {
            val file = File(dir_users, "list_websok_send_mess")
            if (file.exists()) {
                val inputStream = ObjectInputStream(FileInputStream(file))
                try {
                    list_websok_send_mess = inputStream.readObject() as  MutableList<Any>
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                inputStream.close()
            } else {
                list_websok_send_mess = mutableListOf()
            }
        }

        var list_websok_send_mess : MutableList<Any> = mutableListOf()
        var bool_notif_sound:Boolean = false
        var notificationManagerCompat:NotificationManagerCompat? = null
        private val NOTIFICATION_ID = 234
        private val CHANNEL_ID = "my_channel"
        private val CHANNEL_ID_SOUND = "my_channel_sound"

        private val KEY_PREF_USER_CUR = "KEY_PREF_USER_CUR"

        var tv_user: TextView? = null
        var tv_in: TextView? = null

        var current_username: String = ""

        var dir_users: File? = null
        var dir_images: File? = null
        var dir_record: File? = null

        var activity: MainActivity? = null

        var handler: Handler? = null

        var files_src_map_all: MutableMap<String,String> = mutableMapOf()

        var ring:Ringtone? = null

        var wakeLock:PowerManager.WakeLock?=null

        var current_user: User?=null
    }

}