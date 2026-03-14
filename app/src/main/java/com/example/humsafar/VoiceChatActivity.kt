package com.example.humsafar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.humsafar.data.ActiveSiteManager
import com.example.humsafar.ui.VoiceChatScreen
import com.example.humsafar.ui.theme.HumsafarTheme

object VoiceChatExtras {
    const val SITE_ID = "SITE_ID"
    const val SITE_NAME = "SITE_NAME"
    const val NODE_ID = "NODE_ID"
}

class VoiceChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activeSiteId = ActiveSiteManager.activeSiteId
        val intentSiteId = intent.getStringExtra(VoiceChatExtras.SITE_ID)
        val resolvedSiteId = activeSiteId?.toString() ?: intentSiteId ?: ""

        val activeNodeId = ActiveSiteManager.activeNodeId.value
        val intentNodeId = intent.getStringExtra(VoiceChatExtras.NODE_ID)
        val resolvedNodeId = activeNodeId?.toString() ?: intentNodeId ?: ""

        val resolvedSiteName = ActiveSiteManager.activeSiteName
            .ifBlank { intent.getStringExtra(VoiceChatExtras.SITE_NAME) ?: "Heritage Site" }

        Log.d("VoiceChatActivity", """
            ▶ LAUNCHED
              siteId=$resolvedSiteId
              nodeId=$resolvedNodeId
              siteName='$resolvedSiteName'
        """.trimIndent())

        setContent {
            HumsafarTheme {
                VoiceChatScreen(
                    siteName = resolvedSiteName,
                    siteId = resolvedSiteId,
                    nodeId = resolvedNodeId,
                    onBack = { finish() },
                    onNavigateToSettings = { }
                )
            }
        }
    }
}
