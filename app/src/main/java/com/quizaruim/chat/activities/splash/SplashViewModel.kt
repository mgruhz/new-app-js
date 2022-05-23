package com.quizaruim.chat.activities.splash

import android.app.Application
import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.facebook.FacebookSdk
import com.facebook.applinks.AppLinkData
import com.onesignal.OneSignal
import com.quizaruim.chat.components.appsflyer.AppsflyerUtils
import com.quizaruim.chat.components.url.DeeplinkChecker
import com.quizaruim.chat.data.repository.Repository
import com.quizaruim.chat.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private var af_campaign : String? = null
    private var fb_campaign : String? = null
    //
    private var af_adset_id : String? = null
    private var af_campaign_id : String? = null
    private var af_ad_id : String? = null
    //
    private var ref : String? = null
    //
    val campaign_id: MutableLiveData<String> = MutableLiveData()
    val adset_id: MutableLiveData<String> = MutableLiveData()
    val af_adid: MutableLiveData<String> = MutableLiveData()
    //
    val finished: MutableLiveData<Boolean> = MutableLiveData()
    val campaign: MutableLiveData<HashMap<Int, String>> = MutableLiveData()
    val referrer: MutableLiveData<String> = MutableLiveData()

    fun initViewModel(){
        viewModelScope.launch {
            val tm = getApplication<Application>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val masterKey = MasterKey.Builder(getApplication()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val sPrefs = EncryptedSharedPreferences.create(getApplication(), "setting", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
            initialOneSignalNotification()
            getCampaign()
            initialFacebookDeepLink()
            initialReferrer()
            when{
                af_campaign != null -> {
                    saveData(af_campaign!!)
                    Repository().sendJSONData(
                        tm,
                        sPrefs,
                        getApplication<Application>().packageName,
                        AppsflyerUtils().getAppsflyerId(getApplication()),
                        AppsflyerUtils().getAdvertisingId(getApplication()),
                        af_campaign.toString()
                    )
                    finished.value = true
                }
                fb_campaign != null -> {
                    saveData(fb_campaign!!)
                    Repository().sendJSONData(
                        tm,
                        sPrefs,
                        getApplication<Application>().packageName,
                        AppsflyerUtils().getAppsflyerId(getApplication()),
                        AppsflyerUtils().getAdvertisingId(getApplication()),
                        fb_campaign.toString()
                    )
                    finished.value = true
                }
                ref?.contains("sub") == true -> {
                    referrer.value = ref
                    Repository().sendJSONData(
                        tm,
                        sPrefs,
                        getApplication<Application>().packageName,
                        AppsflyerUtils().getAppsflyerId(getApplication()),
                        AppsflyerUtils().getAdvertisingId(getApplication()),
                        ref.toString()
                    )
                    finished.value = true
                }
                else -> finished.value = false
            }
        }
    }
    private suspend fun saveData(_campaign: String){
        campaign.value = DeeplinkChecker().getSubs(_campaign)
        campaign_id.value = af_campaign_id
        adset_id.value = af_adset_id
        af_adid.value = af_ad_id
    }
    private fun initialOneSignalNotification() {
        OneSignal.initWithContext(getApplication())
        OneSignal.setAppId(Constants.ONE_SIGNAL_ID)
    }
    private suspend fun initialReferrer() = suspendCancellableCoroutine<Unit> {
        val referrerClient = InstallReferrerClient.newBuilder(getApplication()).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK ->  {
                        it.resume(Unit)
                        val response = referrerClient.installReferrer
                        ref = response.installReferrer
                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        it.resume(Unit)
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        it.resume(Unit)
                    }
                }
            }
            override fun onInstallReferrerServiceDisconnected() {}
        })
    }
    private suspend fun initialFacebookDeepLink() = suspendCancellableCoroutine<Unit>{
        FacebookSdk.setApplicationId(Constants.FB_APP_ID)
        FacebookSdk.setClientToken(Constants.FB_CLIENT_TOKEN)
        FacebookSdk.sdkInitialize(getApplication())
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
        AppLinkData.fetchDeferredAppLinkData(getApplication(), AppLinkData.CompletionHandler { appLink ->
            if(appLink?.targetUri != null) fb_campaign = (appLink.targetUri).toString().substringAfter("//")
            it.resume(Unit)
        })
    }
    private suspend fun getCampaign() = suspendCancellableCoroutine<Unit> {
        val conversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>) {
                it.resume(Unit)
                if(conversionData["campaign"] != null)
                    af_campaign = conversionData["campaign"].toString()
                if(conversionData["af_adset_id"] != null)
                    af_adset_id = conversionData["af_adset_id"].toString()
                if(conversionData["af_c_id"] != null)
                    af_campaign_id = conversionData["af_c_id"].toString()
                if(conversionData["af_ad_id"] != null)
                    af_ad_id = conversionData["af_ad_id"].toString()
            }
            override fun onAppOpenAttribution(attributionData: MutableMap<String, String>?) {
                it.resume(Unit)
                if(attributionData?.get("host") != null)
                    fb_campaign = attributionData["host"]

            }
            override fun onAttributionFailure(errorMessage: String?) {
                it.resume(Unit)
            }
            override fun onConversionDataFail(errorMessage: String?) {
                it.resume(Unit)
            }
        }
        AppsFlyerLib.getInstance().init(Constants.APPSFLYER, conversionListener, getApplication())
        AppsFlyerLib.getInstance().start(getApplication())
    }
}