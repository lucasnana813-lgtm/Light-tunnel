package com.darktunnel.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.webkit.*

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "DT")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                try { startActivity(Intent(Intent.ACTION_VIEW, r.url)) } catch (e: ActivityNotFoundException) {}
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class Bridge {
        @JavascriptInterface fun connect() = runOnUiThread {
            val i = VpnService.prepare(this@MainActivity)
            if (i != null) startActivityForResult(i, 1) else startVpn()
        }
        @JavascriptInterface fun disconnect() = runOnUiThread {
            startService(Intent(this@MainActivity, DarkVpnService::class.java).setAction("STOP"))
        }
        @JavascriptInterface fun copy(t: String) = runOnUiThread {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("dt", t))
        }
    }

    @Deprecated("ok")
    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        if (r == 1 && c == RESULT_OK) startVpn()
    }

    private fun startVpn() {
        startService(Intent(this, DarkVpnService::class.java).setAction("START"))
    }
}
