@file:OptIn(ExperimentalMaterial3Api::class)

package com.socialsaver.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.socialsaver.data.Account
import com.socialsaver.data.Accounts
import kotlinx.coroutines.delay

/** Shows the site's own login page. We only watch for the session cookie, never the password. */
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val account = runCatching { Account.valueOf(intent.getStringExtra(EXTRA).orEmpty()) }.getOrDefault(Account.INSTAGRAM)
        setContent {
            SocialSaverTheme {
                LoginScreen(
                    account = account,
                    onClose = { finish() },
                    onDone = { setResult(RESULT_OK); finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA = "account"
        fun intent(ctx: Context, account: Account): Intent =
            Intent(ctx, LoginActivity::class.java).putExtra(EXTRA, account.name)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginScreen(account: Account, onClose: () -> Unit, onDone: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val label = account.platform.label

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (Accounts.isLoggedIn(account)) {
                CookieManager.getInstance().flush()
                onDone()
                break
            }
        }
    }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Log in to $label", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your password goes straight to $label.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Some sites refuse logins from embedded browsers; present as regular Chrome.
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                            }
                        }
                        loadUrl(account.loginUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}
