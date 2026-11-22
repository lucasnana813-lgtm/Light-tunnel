package com.darktunnel.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.webkit.*
import java.net.InetSocketAddress
import java.net.Socket

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

        @JavascriptInterface fun hasEngine(): Boolean =
            try { Class.forName("libv2ray.Libv2ray"); true } catch (e: Throwable) { false }

        @JavascriptInterface fun diag(): String {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            var vpn = false
            for (n in cm.allNetworks) {
                val c = cm.getNetworkCapabilities(n)
                if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true
            }
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val net = when {
                caps == null -> "aucun"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
                else -> "autre"
            }
            val perm = VpnService.prepare(this@MainActivity) == null
            val model = Build.MODEL.replace("\"", "")
            return "{\"vpn\":$vpn,\"internet\":$internet,\"net\":\"$net\",\"perm\":$perm,\"engine\":${hasEngine()},\"sdk\":${Build.VERSION.SDK_INT},\"model\":\"$model\"}"
        }

        @JavascriptInterface fun tcp(host: String, port: Int, cb: String) {
            Thread {
                val t0 = System.currentTimeMillis()
                var ok = false
                var err = ""
                try {
                    Socket().use { it.connect(InetSocketAddress(host, port), 5000) }
                    ok = true
                } catch (e: Exception) {
                    err = e.javaClass.simpleName
                }
                val ms = System.currentTimeMillis() - t0
                web.post { web.evaluateJavascript("$cb($ok,$ms,'$err')", null) }
            }.start()
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
