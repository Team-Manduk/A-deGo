package com.teammanduk.adego.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.feature.main.navigation.rememberMainNavigator
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navigator = rememberMainNavigator()

            // 딥링크 처리
            LaunchedEffect(intent) {
                handleDeepLink(intent, navigator)
            }

            AdegoTheme {
                MainRoute(
                    navigator = navigator
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleDeepLink(intent: Intent, navigator: com.teammanduk.adego.feature.main.navigation.MainNavigator) {
        val data = intent.data ?: return

        val roomId = when {
            // adego://join/{roomId} 형식 처리
            data.scheme == "adego" && data.host == "join" -> {
                data.pathSegments.firstOrNull()
            }
            // http(s)://adego.kr/join/{roomId} 형식 처리
            (data.scheme == "http" || data.scheme == "https") &&
            data.host == "adego.kr" &&
            data.pathSegments.firstOrNull() == "join" -> {
                data.pathSegments.getOrNull(1)
            }
            else -> null
        }

        if (!roomId.isNullOrBlank()) {
            // 새로운 userId 생성
            val userId = UUID.randomUUID().toString()
            navigator.navigateToMap(roomId, userId)
        }
    }
}