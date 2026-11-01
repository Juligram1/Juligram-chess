package com.juligram.chess

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class Bridge {
        @JavascriptInterface
        fun startHosting() {
            startForegroundService(Intent(this@MainActivity, HostService::class.java))
        }

        @JavascriptInterface
        fun stopHosting() {
            stopService(Intent(this@MainActivity, HostService::class.java))
        }

        @JavascriptInterface
        fun localAddress(): String {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifi.connectionInfo.ipAddress
            return "%d.%d.%d.%d".format(ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
