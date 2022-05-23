 package com.quizaruim.chat.components.url

import com.quizaruim.chat.components.url.URLConstants.ADVERTISING_ID
import com.quizaruim.chat.components.url.URLConstants.AF_ADSET_ID
import com.quizaruim.chat.components.url.URLConstants.AF_AD_ID
import com.quizaruim.chat.components.url.URLConstants.AF_C_ID
import com.quizaruim.chat.components.url.URLConstants.APPSFLYER_ID
import com.quizaruim.chat.components.url.URLConstants.GEO
import com.quizaruim.chat.components.url.URLConstants.PACKAGE
import com.quizaruim.chat.components.url.URLConstants.SUB1
import com.quizaruim.chat.components.url.URLConstants.SUB2
import com.quizaruim.chat.components.url.URLConstants.SUB3
import com.quizaruim.chat.components.url.URLConstants.SUB4
import com.quizaruim.chat.components.url.URLConstants.SUB5
import com.quizaruim.chat.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParamUtils {
    suspend fun replace_param(sub: HashMap<Int, String>?, advertising_id: String, appsflyer_id: String, referrer: String?, campaign_id: String, adset_id: String, sys_package: String, geo: String, af_ad_id: String) : String = withContext(Dispatchers.IO){
        var url = generateUrl(referrer)
        with(url){
            if(contains(SUB1)) url = url.replace(SUB1, sub!![0].toString())
            if(contains(SUB2)) url = url.replace(SUB2, sub!![1].toString())
            if(contains(SUB3)) url = url.replace(SUB3, sub!![2].toString())
            if(contains(SUB4)) url = url.replace(SUB4, sub!![3].toString())
            if(contains(SUB5)) url = url.replace(SUB5, sub!![4].toString())

            if(contains(ADVERTISING_ID)) url = url.replace(ADVERTISING_ID, advertising_id)
            if(contains(APPSFLYER_ID)) url = url.replace(APPSFLYER_ID, appsflyer_id)
            if(contains(AF_C_ID)) url = url.replace(AF_C_ID, campaign_id)
            if(contains(AF_ADSET_ID)) url = url.replace(AF_ADSET_ID, adset_id)

            if(contains(PACKAGE)) url = url.replace(PACKAGE, sys_package)
            if(contains(GEO)) url = url.replace(GEO, geo)
            if(contains(AF_AD_ID)) url = url.replace(AF_AD_ID, af_ad_id)
        }
        return@withContext url
    }
    private fun generateUrl(ref: String?) : String{
        if(ref != null) return "${Constants.URL}?${ref.replace("!", "&")}"
        else return "${Constants.URL}?sub1=$SUB1&sub2=$SUB2&sub3=$SUB3&sub4=$SUB4&sub5=$SUB5&advertising_id=$ADVERTISING_ID&af_id=$APPSFLYER_ID&af_c_id=$AF_C_ID&af_adset_id=$AF_ADSET_ID&af_ad_id=$AF_AD_ID&package=$PACKAGE&geo=$GEO"
    }
}