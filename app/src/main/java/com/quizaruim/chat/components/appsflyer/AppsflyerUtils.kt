package com.quizaruim.chat.components.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsflyerUtils {
    suspend fun getAdvertisingId(context: Context) : String = withContext(Dispatchers.IO) {
        val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
        return@withContext adInfo.id.toString()
    }
    suspend fun getAppsflyerId(context: Context) : String = withContext(Dispatchers.IO){
        return@withContext AppsFlyerLib.getInstance().getAppsFlyerUID(context).toString()
    }
}