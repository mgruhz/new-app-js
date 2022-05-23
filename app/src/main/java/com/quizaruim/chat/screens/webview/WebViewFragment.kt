package com.quizaruim.chat.screens.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.view.*
import android.webkit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.quizaruim.chat.R
import com.quizaruim.chat.components.webview.ImageLoader
import com.quizaruim.chat.components.webview.JSBridge
import com.quizaruim.chat.components.webview.WV
import com.quizaruim.chat.utils.Constants
import java.io.File
import java.io.IOException

class WebViewFragment : Fragment() {
    private lateinit var sPrefs: SharedPreferences
    //
    private lateinit var wv: WebView
    private lateinit var wv_back: WebView
    //

    private var OC = 0
    private val IC = 1
    private val FR = 1
    private var mUploadMessage: ValueCallback<Uri>? = null
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    //
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_web_view, container, false)
        val masterKey = MasterKey.Builder(requireContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        sPrefs = EncryptedSharedPreferences.create(requireContext(), "setting", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        wv = view.findViewById(R.id.webview)
        wv_back = view.findViewById(R.id.webview_background)

        if(!activity?.intent?.getBooleanExtra("organic", true)!!){
            loadWVURl(wv)
            loadWVURl(wv_back)
            if(Math.random() >= 1.00-(Constants.CHANCE*0.01)){
                wv.loadUrl(sPrefs.getString("last_url", null).toString())
                wv_back.loadUrl(Constants.URL_BACKGROUND)
            } else {
                wv.loadUrl(Constants.URL_BACKGROUND)
                wv_back.loadUrl(sPrefs.getString("last_url", null).toString())
            }
            wv.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(
                    v: View?,
                    keyCode: Int,
                    event: KeyEvent?
                ): Boolean {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event?.action === MotionEvent.ACTION_UP && wv.canGoBack()) {
                        wv.goBack()
                        return true
                    } else {
                        return false
                    }
                }
            })
        } else {
            WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
            WindowInsetsControllerCompat(requireActivity().window, wv).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            loadApplication()
        }
        return view
    }
    private fun loadWVURl(wov: WebView) {
        val webSettings: WebSettings = wov.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        wov.settings.pluginState = WebSettings.PluginState.ON
        wov.settings.allowFileAccess = true

        WV().setParams(requireActivity(), wov, sPrefs)
        wov.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, filePath: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                mFilePathCallback?.onReceiveValue(null)
                mFilePathCallback = filePath
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(context!!.applicationContext.packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = ImageLoader().createImageFile(context!!.applicationContext)
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                    } else takePictureIntent = null
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"
                val intentArray: Array<Intent?> = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, 1)
                return true
            }
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String? = "") {
                mUploadMessage = uploadMsg
                val imageStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AndroidExampleFolder")
                if (!imageStorageDir.exists()) imageStorageDir.mkdirs()
                val file = File(imageStorageDir.toString() + File.separator + "IMG_" + System.currentTimeMillis().toString() + ".jpg")
                mCapturedImageURI = Uri.fromFile(file)
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                val chooserIntent = Intent.createChooser(i, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent))
                startActivityForResult(chooserIntent, 1)
            }
        }
        wv.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_BACK && event?.action === MotionEvent.ACTION_UP && wv.canGoBack()) {
                    wv.goBack()
                    return true
                } else {
                    return false
                }
            }
        })
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadApplication(){
        val u = "https://appassets.androidplatform.net/assets/index.html"
        wv.settings.javaScriptEnabled = true
        wv.settings.allowFileAccess = true
        wv.settings.loadWithOverviewMode = true
        wv.settings.domStorageEnabled = true
        wv.settings.useWideViewPort = true
        wv.addJavascriptInterface(JSBridge(requireContext()), "JSBridge")
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .build()
        wv.webViewClient = LocalContentWebViewClient(assetLoader)
        wv.loadUrl(u)
        wv.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_BACK && event?.action === MotionEvent.ACTION_UP && wv.canGoBack()) {
                    wv.goBack()
                    return true
                } else {
                    return false
                }
            }
        })
    }
    private class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) : WebViewClientCompat() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }
    }
    override fun onPause() {
        super.onPause()
        wv.evaluateJavascript("javascript:window.ApplicationSoundOff()", null)
        if(activity?.intent?.getBooleanExtra("organic", true) == false){
            with(sPrefs.edit()){
                putString("last_url", wv.url)
                apply()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        wv.evaluateJavascript("javascript:window.ApplicationSoundOn()", null)

        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(requireActivity().window, wv).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != IC || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    if (mCameraPhotoPath != null) {
                        results = arrayOf(Uri.parse(mCameraPhotoPath))
                    }
                } else {
                    val dataString = data.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
            }
            mFilePathCallback!!.onReceiveValue(results)
            mFilePathCallback = null
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FR || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            if (requestCode == FR) {
                if (null == mUploadMessage) {
                    return
                }
                var result: Uri? = null
                try {
                    result = if (resultCode != Activity.RESULT_OK) {
                        null
                    } else {
                        if (data == null) mCapturedImageURI else data.data
                    }
                } catch (e: Exception) {
                }
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        }
        return
    }
}