package com.teammanduk.adego.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.feature.main.navigation.rememberMainNavigator
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navigator = rememberMainNavigator()
            val sessionState by viewModel.sessionState.collectAsState()

            // 세션 복구 처리
            LaunchedEffect(sessionState) {
                when (val state = sessionState) {
                    is SessionState.Restored -> {
                        // 세션이 있으면 Map 화면으로 자동 이동
                        navigator.navigateToMap(state.sessionInfo.roomId, state.sessionInfo.userId)
                    }
                    SessionState.None, SessionState.Loading -> {
                        // 세션이 없거나 로딩 중이면 아무것도 안 함 (Home 화면에 머무름)
                    }
                }
            }

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
            // http(s)://a-dego.web.app/join/{roomId} 또는 adego.kr/join/{roomId} 형식 처리
            (data.scheme == "http" || data.scheme == "https") &&
            (data.host == "a-dego.web.app" || data.host == "adego.kr") &&
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