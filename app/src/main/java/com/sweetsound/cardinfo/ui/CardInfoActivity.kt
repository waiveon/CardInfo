package com.sweetsound.cardinfo.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.sweetsound.cardinfo.R
import com.sweetsound.cardinfo.application.CardInfoApplication.Companion.SHARED_PREF
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.cardinfo.receiver.InitBroadcastReceiver
import com.sweetsound.cardinfo.utils.CardUtils
import com.sweetsound.cardinfo.utils.Utils
import com.sweetsound.logtofile.ui.LogDisplayActivity
import com.sweetsound.permission.PermissionManager
import com.sweetsound.storeplan.db.DbUtil
import kotlinx.android.synthetic.main.activity_card_info.*
import kotlinx.android.synthetic.main.login_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


class CardInfoActivity : AppCompatActivity() {
    private val REQUEST_CODE_PERMISSION = 0

    private val mFirebaseAuth: FirebaseAuth

    private lateinit var mAuthStateListener: FirebaseAuth.AuthStateListener

    init {
        mFirebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_info)

        if (isPermissionGrantred() == false) {
            Toast.makeText(
                baseContext,
                getString(R.string.noti_permission, getString(R.string.app_name)),
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        log_button.setOnClickListener {
            LogDisplayActivity.open(this@CardInfoActivity)
        }

        mAuthStateListener = FirebaseAuth.AuthStateListener() {
            // currentUser가 null 이면 login이 안되어 있는 것
            // autoLoginEmail의 값이 있다면 자동로그인 설정이 되어 있는 것
            it.currentUser ?: procAutoLogin(object : OnCompleteListener<AuthResult> {
                override fun onComplete(task: Task<AuthResult>) {
                    if (task.isSuccessful == true) {
                        Toast.makeText(baseContext, R.string.auto_login_success_n_will_sync, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        woori_card_title_button.setOnClickListener {
            CardUseListActivity.open(this@CardInfoActivity, ConstCardType.CARD_TYPE.WOORI.value)
        }

        hyundai_card_title_button.setOnClickListener {
            CardUseListActivity.open(this@CardInfoActivity, ConstCardType.CARD_TYPE.HYUNDAI.value)
        }

        hana_card_title_button.setOnClickListener {
            CardUseListActivity.open(this@CardInfoActivity, ConstCardType.CARD_TYPE.HANA.value)
        }

        kb_card_title_button.setOnClickListener {
            CardUseListActivity.open(this@CardInfoActivity, ConstCardType.CARD_TYPE.KB.value)
        }

        // 배터리를 사용해야 어플을 종료 했을 때도 알람이 동작 한다.
        if (isFinishing() == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = getPackageName()
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val dialogBuilder = android.app.AlertDialog.Builder(this).setMessage(R.string.need_background_proc)
                    .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    })
                    .setNegativeButton(R.string.cancel, null)
                dialogBuilder.create()
                val dialog = dialogBuilder.create()
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()
            }
        }

        PermissionManager.check(this, REQUEST_CODE_PERMISSION)

        setInitAlarm()

//        CardUtils.parsingNoti(baseContext, "승인 245,000원", "우리BC(8842)승인 이*석님 245,000원 일시불 01/05 13:48 (주) 나무다움", "")
    }

    override fun onStart() {
        super.onStart()

        sumPrice();

        // 사용자가 로그인이 되어 있는지 체크 해서 로그인이 안되어 있다면 자동로그인 여부에 따라 로그인 하도록 한다.
        mFirebaseAuth.addAuthStateListener(mAuthStateListener)
    }

    override fun onStop() {
        super.onStop()

        mFirebaseAuth.removeAuthStateListener(mAuthStateListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                if (SHARED_PREF.firstRunning == true) {
//                if (sharedPreferenceFirstRunning.getBoolean(KEY_FIRST_RUNNING, true) == true) {
                    // 첫 어플 실행 시 SMS에서 카드내역을 가져 온다.
                    CoroutineScope(Dispatchers.Main).launch {
                        val alertDialog = AlertDialog.Builder(this@CardInfoActivity).create()
                        alertDialog.setTitle(R.string.first_running_title)
                        alertDialog.setMessage(getString(R.string.first_running_read_sms))
                        alertDialog.setCancelable(false)
                        alertDialog.setCanceledOnTouchOutside(false)
                        alertDialog.show()

                        CoroutineScope(Dispatchers.IO).async {
                            // 당월 1일로 설정
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                            calendar.set(Calendar.HOUR, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)

                            val where =
                                "${Telephony.Sms.DATE} >= ${calendar.timeInMillis} AND ${Telephony.Sms.ADDRESS} IN ('${ConstCardType.HYUNDAI_CARD_ADDRESS}', '${ConstCardType.KB_CARD_ADDRESS}', '${ConstCardType.WOORI_CARD_ADDRESS}')"

                            val SMS_COLUMNS = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                            val SMS_COLUMN_INDEX_ADDRESS = 0
                            val SMS_COLUMN_INDEX_BODY = 1
                            val SMS_COLUMN_INDEX_DATE = 2

                            val smsCursor = getContentResolver().query(
                                Telephony.Sms.Inbox.CONTENT_URI,
                                SMS_COLUMNS,
                                where,
                                null,
                                null
                            )

                            smsCursor?.let {
                                CoroutineScope(Dispatchers.Main).launch {
                                    alertDialog.setMessage(getString(R.string.first_running_read_sms))
                                }

                                val cardHashMap: HashMap<String, ArrayList<CardUseHistory>> =
                                    HashMap<String, ArrayList<CardUseHistory>>()

                                while (it.moveToNext()) {
                                    val address = it.getString(SMS_COLUMN_INDEX_ADDRESS)
                                    var cardType = ConstCardType.getCardType(address)

                                    if (cardType > ConstCardType.CARD_TYPE.UNKNOWN) {
                                        val cardUseHistory = CardUtils.parsingSms(
                                            baseContext,
                                            cardType,
                                            it.getString(SMS_COLUMN_INDEX_BODY)
                                        )

                                        // 저장 하지 말아야 할 것(통신료 등..)은 가격을 0으로 처리 된다.
                                        if (cardUseHistory.price > 0) {
                                            if (cardHashMap.containsKey(address) == false || cardHashMap.get(address) == null) {
                                                val cardUseHistorys = arrayListOf<CardUseHistory>()
                                                cardUseHistorys.add(cardUseHistory)

                                                cardHashMap.put(address, cardUseHistorys)
                                            } else {
                                                val cardUseHistorys = cardHashMap.get(address)
                                                cardUseHistorys?.add(cardUseHistory)
                                            }
                                        }
                                    }
                                }

                                CoroutineScope(Dispatchers.Main).launch {
                                    alertDialog.setMessage(getString(R.string.first_running_saving))
                                }

                                // 서버에 저장은 나중에 하자. Sync 맞추는 것도 문제고..
//                        val databaseReference = FirebaseDatabase.getInstance().reference
//
//                        cardHashMap.forEach { address, value ->
//                            databaseReference.child(address).setValue(value)
//                        }

                                it.close()

                            }
                        }.await()

                        alertDialog.dismiss()

                        SHARED_PREF.firstRunning = false

                        sumPrice()
                    }
                } else {
                    sumPrice()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.card_info_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.setting -> {
                SettingActivity.open(this)
            }

            R.id.cloud -> {
                // 로그인이 되어 있는지 체크 후 동작 필요
                mFirebaseAuth.currentUser?.let {
                    // 서버와 동기화 시키자
                    syncServer(it.email ?: "")
                }.let {
                    procAutoLogin(object : OnCompleteListener<AuthResult> {
                        override fun onComplete(task: Task<AuthResult>) {
                            if (task.isSuccessful == true) {
                                // 자동 로그인이 되었으면 서버랑 동기화 시키자
                            } else {
                                Toast.makeText(baseContext, R.string.login_fail, Toast.LENGTH_SHORT).show()

                                initAutoLoginInfo()
                                showLonginDialog()
                            }
                        }
                    }).also {
                        if (it == false) {
                            // 자동 로그인이 아니면 로그인 하라는 팝업을 띄운다.
                            showLonginDialog()
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Synchronized
    private fun sumPrice() {
        // 내 DB 읽어서
        val cardUseHistoryMap = DbUtil(baseContext).select()

        setUsedPrice(cardUseHistoryMap.get(ConstCardType.CARD_TYPE.WOORI), used_woori_card_textview)
        setUsedPrice(cardUseHistoryMap.get(ConstCardType.CARD_TYPE.HYUNDAI), used_hyundai_card_textview)
        setUsedPrice(cardUseHistoryMap.get(ConstCardType.CARD_TYPE.KB), used_kb_card_textview)
        setUsedPrice(cardUseHistoryMap.get(ConstCardType.CARD_TYPE.HANA), used_hana_card_textview)
    }

    private fun setUsedPrice(cardUseHistorys: MutableList<CardUseHistory>?, textview: TextView) {
        var cardNumMap = HashMap<String, Long>()

        cardUseHistorys?.forEach { cardUseHistory ->
            var pricePerCardNum = cardNumMap.get(cardUseHistory.cardNum)

            if (pricePerCardNum == null) {
                cardNumMap.put(cardUseHistory.cardNum, cardUseHistory.price)
            } else {
                cardNumMap.put(cardUseHistory.cardNum, (pricePerCardNum + cardUseHistory.price))
            }
        }

        var totalPrice = ""

        cardNumMap.forEach { cardNum, price ->
            if (price > 0) {
                totalPrice += "${Utils.getNumberWithComma(price)}원${cardNum} "
            }
        }

        textview.text = totalPrice
    }

    private fun isPermissionGrantred(): Boolean {
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        return sets != null && sets.contains(packageName)
    }

    private fun checkPasswd(passwd: String) = passwd.startsWith("makin").also {
        if (it == false) {
            Toast.makeText(baseContext, R.string.personal, Toast.LENGTH_SHORT).show()
        }
    }

    private fun login(email: String, passwd: String, onCompleteListener: OnCompleteListener<AuthResult>) {
        mFirebaseAuth.signInWithEmailAndPassword(email, passwd)
            .addOnCompleteListener(onCompleteListener)
    }

    private fun registration(email: String, passwd: String) {
        mFirebaseAuth.createUserWithEmailAndPassword(email, passwd)
            .addOnCompleteListener(object : OnCompleteListener<AuthResult> {
                override fun onComplete(task: Task<AuthResult>) {
                    if (task.isSuccessful == true) { // 등록 성공
                        login(email, passwd, object : OnCompleteListener<AuthResult> {
                            override fun onComplete(task: Task<AuthResult>) {
                                if (task.isSuccessful == true) {
                                    Toast.makeText(baseContext, R.string.login_success_n_will_sync, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        })
                    }
                }
            })
    }

    private fun saveAutoLoginInfo(rootView: View) {
        if (rootView.auto_login_checkbox.isChecked == true) {
            SHARED_PREF.AutoLogin().email = rootView.email_edittext.text.toString()
            SHARED_PREF.AutoLogin().passwd = rootView.passwd_edittext.text.toString()
        } else {
            SHARED_PREF.AutoLogin().clear()
        }
    }

    private fun initAutoLoginInfo() {
        SHARED_PREF.AutoLogin().clear()
    }

    private fun showLonginDialog() {
        val alertDialog = AlertDialog.Builder(this@CardInfoActivity)
        val contentsView = layoutInflater.inflate(R.layout.login_layout, null)

        alertDialog.setTitle(R.string.server_access)
        alertDialog.setView(contentsView)
        alertDialog.setPositiveButton(R.string.login, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                // 로그인
                with(contentsView) {
                    if (checkPasswd(passwd_edittext.text.toString()) == true) {
                        login(
                            email_edittext.text.toString(),
                            passwd_edittext.text.toString(),
                            object : OnCompleteListener<AuthResult> {
                                override fun onComplete(task: Task<AuthResult>) {
                                    if (task.isSuccessful == true) {
                                        Toast.makeText(
                                            baseContext,
                                            R.string.login_success_n_will_sync,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(baseContext, R.string.login_fail, Toast.LENGTH_SHORT).show()

                                        initAutoLoginInfo()
                                    }
                                }
                            })

                        saveAutoLoginInfo(contentsView)
                    }
                }
            }
        })
        alertDialog.setNeutralButton(R.string.register, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                // 등록
                with(contentsView) {
                    if (checkPasswd(passwd_edittext.text.toString()) == true) {
                        registration(
                            email_edittext.text.toString(),
                            passwd_edittext.text.toString()
                        )

                        saveAutoLoginInfo(contentsView)
                    }
                }
            }
        })
        alertDialog.setNegativeButton(android.R.string.cancel, null)
        val dialog = alertDialog.create()
        dialog.setOnShowListener(object : DialogInterface.OnShowListener {
            override fun onShow(dialog: DialogInterface?) {
                contentsView.post {
                    with(contentsView) {
                        email_edittext.requestFocus()

                        val imm =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(email_edittext, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        })
        dialog.show()
    }

    private fun procAutoLogin(onCompleteListener: OnCompleteListener<AuthResult>): Boolean {
        var result = false
        val autoLoginEmail = SHARED_PREF.AutoLogin().email

        autoLoginEmail?.let {
            val autoLoginPasswd = SHARED_PREF.AutoLogin().passwd
            login(autoLoginEmail, autoLoginPasswd!!, onCompleteListener)

            result = true
        }

        return result
    }

    private fun syncServer(email: String) {
        if (TextUtils.isEmpty(email) == false) {
            val databaseRef = FirebaseDatabase.getInstance().getReference()
            databaseRef.child(Utils.stringToHex(email)) // root 설정

            // 서버에서 받오고
            databaseRef.addChildEventListener(object : ChildEventListener {
                override fun onCancelled(databaseError: DatabaseError) {
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                }

                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    // loading Dialog 필요
                    Log.e("TAG", "LJS====================================")
                    Log.e("TAG", "LJS== children : " + dataSnapshot.children)
                    dataSnapshot.children.forEach {
                        Log.e("TAG", "LJS== value : " + it.getValue())
//                        val cardUseHistory = it.getValue(CardUseHistory::class.java) as CardUseHistory
//                        Log.e("TAG", "LJS== cardUseHistory : " + cardUseHistory)
                        it.children.forEach {
                            Log.e("TAG", "LJS== value : " + it.getValue())
                        }
                    }
                    Log.e("TAG", "LJS====================================")

                    // 이메일 밑에 있는 것들이 리스트별로 한번씩 여러번 호출됨.
                    // - email
                    // |- 우리카드 <- dataSnapshot.key
                    // |- 국민카드 <- dataSnapshot.key
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                }

            })

            // 내꺼를 서버로 보낼 떈 서버에서 내려온 데이터 제외(날짜 가격으로 체크) 하고 보내기
            val cardUseHistory = CardUseHistory("(7493)", ConstCardType.CARD_TYPE.KB, 13000, 0, "테스트장소")

            databaseRef.child(cardUseHistory.getKeyPath()).setValue(cardUseHistory.toMap())
        }
    }

    private fun setInitAlarm() {
        // 날짜 체크해서 매월 1일 0시로 알람을 설정
        val initCalendar = Calendar.getInstance()
        val currentMonth = initCalendar.get(Calendar.MONTH)

        if (currentMonth == Calendar.DECEMBER) {
            initCalendar.set(Calendar.MONTH, Calendar.JANUARY)
        } else {
            initCalendar.set(Calendar.MONTH, (currentMonth + 1))
        }

        initCalendar.set(Calendar.DAY_OF_MONTH, 1)
        initCalendar.set(Calendar.HOUR, 0)
        initCalendar.set(Calendar.MINUTE, 0)
        initCalendar.set(Calendar.SECOND, 0)
        initCalendar.set(Calendar.MILLISECOND, 0)


        val initPendingIntent = PendingIntent.getBroadcast(this, 0, Intent(InitBroadcastReceiver.ACTION_INIT), PendingIntent.FLAG_UPDATE_CURRENT)
        val initAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        initAlarmManager.set(AlarmManager.RTC_WAKEUP, initCalendar.timeInMillis, initPendingIntent)
    }
}
