package com.quizaruim.chat.notification

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import androidx.navigation.Navigation.findNavController
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.quizaruim.chat.R


class PushService : FirebaseMessagingService() {

    fun getToken(mPrefs: SharedPreferences, activity: Activity){
        val editor = mPrefs.edit()
        try {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(activity) { instanceIdResult ->
                val newToken: String = instanceIdResult.token
                Log.e("APP_CHECK", newToken)
                val edit = mPrefs.edit()
                edit.putString("token", newToken).apply()
            }
        } catch (e: Exception){
            findNavController(activity, R.id.container).navigate(R.id.webViewFragment)
            e.printStackTrace()
            Log.e("APP_CHECK", "getToken: $e")
        }
    }
}