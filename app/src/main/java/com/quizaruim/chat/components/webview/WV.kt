package com.quizaruim.chat.components.webview

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.Navigation
import com.quizaruim.chat.R

class WV {
    fun setParams(activity: Activity, wov: WebView, sPrefs: SharedPreferences){
        val webSettings: WebSettings = wov.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        wov.settings.pluginState = WebSettings.PluginState.ON
        wov.settings.allowFileAccess = true

        wov.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (url == null || url.startsWith("http://") || url.startsWith("https://")) false else try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    view.context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    Log.i("TAG", "shouldOverrideUrlLoading Exception:$e")
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (sPrefs.getString("last_url", null) == null){
                    with(sPrefs.edit()){
                        putString("last_url", wov.url)
                        apply()
                    }
                }
                view.evaluateJavascript("(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();") { html: String ->
                    if (html == "\"\\u003Chtml>\\u003Chead>\\u003C/head>\\u003Cbody>\\u003C/body>\\u003C/html>\"") {
                        Navigation.findNavController(activity, R.id.container).navigate(R.id.webViewFragment)
                    }
                }
            }
        }
    }
}