package com.quizaruim.chat.data.repository

import android.content.SharedPreferences
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.quizaruim.chat.data.APIService
import com.quizaruim.chat.utils.Constants
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.*

class Repository {
    suspend fun sendJSONData(tM: TelephonyManager, sPrefs: SharedPreferences, package_name: String, appsflyer_id: String, advertising_id: String, deeplink: String) {
        val editor = sPrefs.edit()
        if(sPrefs.getString("install_id", null) == null) editor.putString("install_id", UUID.randomUUID().toString()).apply() // Проверка на инсталл ид
        CoroutineScope(Dispatchers.IO).launch {
            // /install
            APIService.retrofit.sendInfoInstall(getInstall(tM, sPrefs, package_name))
            // /appsflyerid
            APIService.retrofit.sendAppsflyerID(getAppsflyer(sPrefs, appsflyer_id, advertising_id))
            // /deeplink
            APIService.retrofit.sendDeepLink(getDeeplink(sPrefs, deeplink))
            // /fcm
            APIService.retrofit.sendFirebaseToken(getFirebase(sPrefs))
        }
    }
    private fun getInstall(tM: TelephonyManager, sPrefs: SharedPreferences, package_name: String) : RequestBody {
        val json = JSONObject()
        with(json){
            put("install_id", sPrefs.getString("install_id", null).toString())
            put("type", "1")
            put("carrier_name", tM.simOperatorName)
            put("carrier_id", tM.networkOperator)
            put("carrier_country", tM.networkCountryIso)
            put("carrier_sim_name", tM.simOperatorName)
            put("device_manufacturer", Build.MANUFACTURER)
            put("device_model", Build.MODEL)
            put("device_locale", Locale.getDefault().language)
            put("os_ver", Build.VERSION.RELEASE)
            put("time_offset", TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
            put("time_zone", TimeZone.getDefault().id)
            put("package_name", package_name)
            put("app_ver",  BuildConfig.VERSION_CODE.toString())
            put("app_id", Constants.APP_ID)
        }
        Log.i("APP_CHECK", "getInstall: $json")
        return getRequestJSON(json)
    }
    private fun getAppsflyer(sPrefs: SharedPreferences, appsflyer_id: String, advertising_id: String) : RequestBody {
        val json = JSONObject()
        with(json){
            put("install_id", sPrefs.getString("install_id", null).toString())
            put("appsflyer_id", appsflyer_id)
            put("advertising_id", advertising_id)
        }
        Log.i("APP_CHECK", "getAppsflyer: $json")
        return getRequestJSON(json)
    }
    private fun getDeeplink(sPrefs: SharedPreferences, deeplink: String) : RequestBody {
        val json = JSONObject()
        with(json){
            put("install_id", sPrefs.getString("install_id", null).toString())
            put("deeplink", deeplink)
        }
        Log.i("APP_CHECK", "getDeeplink: $json")
        return getRequestJSON(json)
    }
    private fun getFirebase(sPrefs: SharedPreferences) : RequestBody {
        val json = JSONObject()
        with(json){
            put("install_id", sPrefs.getString("install_id", null).toString())
            put("token", sPrefs.getString("token", null).toString())
        }
        Log.i("APP_CHECK", "getFirebase: $json")
        return getRequestJSON(json)
    }
    //
    private fun getRequestJSON(json: JSONObject) : RequestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
}