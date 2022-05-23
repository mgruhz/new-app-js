package com.quizaruim.chat.activities.splash

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.quizaruim.chat.R
import com.quizaruim.chat.activities.webview.WebViewActivity
import com.quizaruim.chat.components.appsflyer.AppsflyerUtils
import com.quizaruim.chat.components.settings.ScreenUtils
import com.quizaruim.chat.components.url.ParamUtils
import com.quizaruim.chat.data.repository.Repository
import com.quizaruim.chat.notification.PushService
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    private val model: SplashViewModel by lazy { ViewModelProvider(this)[SplashViewModel::class.java] }
    //
    private lateinit var sPrefs: SharedPreferences
    private lateinit var job: Job
    private lateinit var observer: Observer<Boolean>
    private lateinit var tm: TelephonyManager

    //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        tm = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        sPrefs = EncryptedSharedPreferences.create(this, "setting", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        ScreenUtils().setFull(window)
        PushService().getToken(sPrefs, this)
        if(isInternetConnection(applicationContext)){
            if(sPrefs.getString("last_url", null) != null){
                CoroutineScope(Dispatchers.IO).launch {
                    Repository().sendJSONData(
                        tm,
                        sPrefs,
                        applicationContext.packageName,
                        AppsflyerUtils().getAppsflyerId(applicationContext),
                        AppsflyerUtils().getAdvertisingId(applicationContext),
                        "Second Join"
                    )
                }
                startActivity(Intent(this, WebViewActivity::class.java).putExtra("organic", false))
                finish()
            } else {
                model.initViewModel()
                observer = observer()
                model.finished.observe(this, observer)
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                Repository().sendJSONData(
                    tm,
                    sPrefs,
                    applicationContext.packageName,
                    AppsflyerUtils().getAppsflyerId(applicationContext),
                    AppsflyerUtils().getAdvertisingId(applicationContext),
                    "organic"
                )
            }
            startActivity(Intent(this, WebViewActivity::class.java).putExtra("organic", true))
            finish()
        }

    }
    private fun observer() = Observer<Boolean> {
        if(it){
            job = CoroutineScope(Dispatchers.IO).launch {
                val url = createURL(
                    if(model.campaign.value != null) model.campaign.value else null,
                    if(model.referrer.value != null) model.referrer.value else null,
                    model.campaign_id.value.toString(),
                    model.adset_id.value.toString(),
                    model.af_adid.value.toString()
                )
                with(sPrefs.edit()){
                    putString("last_url", url)
                    apply()
                }
                withContext(coroutineContext){
                    startActivity(Intent(applicationContext, WebViewActivity::class.java)
                        .putExtra("url", url)
                        .putExtra("organic", false))
                    finish()
                    Log.i("APP_CHECK", "check: ${model.campaign.value} + ${model.campaign_id.value} + ${model.adset_id.value} + $url")
                }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                Repository().sendJSONData(
                    tm,
                    sPrefs,
                    applicationContext.packageName,
                    AppsflyerUtils().getAppsflyerId(applicationContext),
                    AppsflyerUtils().getAdvertisingId(applicationContext),
                    "organic"
                )
            }
            startActivity(Intent(applicationContext, WebViewActivity::class.java))
            finish()
        }
    }

    private suspend fun createURL(sub: HashMap<Int, String>?, referrer: String?, campaign_id: String, adset_id: String, af_ad_id: String) : String = withContext(Dispatchers.IO){
        val tM = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return@withContext ParamUtils().replace_param(
            sub,
            AppsflyerUtils().getAdvertisingId(applicationContext),
            AppsflyerUtils().getAppsflyerId(applicationContext),
            referrer,
            campaign_id,
            adset_id,
            applicationContext.packageName,
            tM.networkCountryIso,
            af_ad_id
        )
    }
    private fun isInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true
        }
        return false
    }
    override fun onStop() {
        super.onStop()
        if(model.finished.hasActiveObservers()) model.finished.removeObserver(observer)
    }
}