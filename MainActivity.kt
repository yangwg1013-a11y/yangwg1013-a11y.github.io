package com.qcp.androidshell

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private fun getStatusBarHeight(): Int {
        var res = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val density = resources.displayMetrics.density
        if (resourceId > 0) res = (resources.getDimensionPixelSize(resourceId) / density).toInt()
        return res
    }

    private fun backHome() {
        val setIntent = Intent(Intent.ACTION_MAIN)
        setIntent.addCategory(Intent.CATEGORY_HOME)
        setIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(setIntent)
    }

    private fun startFrontService() {
        val intent = Intent(this, ForegroundService::class.java)
        runBlocking {
            try {
                startForegroundService(intent)
                println("启动成功")
            } catch (e: Exception) {
                println("出现错误")
                print(e)
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startFrontService()

          if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            setupWebView() // 如果已经拥有权限，直接设置WebView
        }      

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupWebView() // 用户授予了权限，设置WebView
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show() // 用户拒绝权限，给出提示或处理逻辑
            }
        }
    }

   private fun setupWebView() {
        // 设置WebView的代码，例如加载一个URL等
	 WebView.setWebContentsDebuggingEnabled(true)

        webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
        }
        webView.scrollBarSize = 0
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.mixedContentMode = 0
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.allowFileAccess = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowContentAccess = true


        class JsObject {
            @get:JavascriptInterface
            val statusBarHeight = "${getStatusBarHeight()}px"

        }

        webView.addJavascriptInterface(JsObject(), "shell")
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.postInvalidate()
    }

    private fun loadRealContent(
        url: String,
        requestHeaders: Map<String, String>?
    ): Pair<String, ByteArray> {
        // 使用 HttpURLConnection 进行网络请求获取实际请求内容
        val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection

        // 将 request 的头信息传递到实际请求中
        requestHeaders?.let { headers ->
            for ((key, value) in headers) {
                connection.setRequestProperty(key, value)
            }
        }

        try {
            val inputStream: InputStream = connection.inputStream

            return Pair(connection.contentType, inputStream.readBytes())
        } finally {
            connection.disconnect()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_BACK) {
            if (webView.canGoBack()) webView.goBack()
            else backHome()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
