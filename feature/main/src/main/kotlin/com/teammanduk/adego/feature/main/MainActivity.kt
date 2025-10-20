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
            val sessionState by viewModel.sessionState.collectAsState()

            AdegoTheme {
                when (val state = sessionState) {
                    SessionState.Loading -> {
                        // 세션 체크 중 로딩 화면
                        com.teammanduk.adego.core.ui.component.LoadingScreen(
                            text = "세션 확인 중..."
                        )
                    }

                    is SessionState.Restored -> {
                        // 세션 복구 성공 → Map 화면부터 시작
                        val navigator = rememberMainNavigator(
                            startDestination = com.teammanduk.adego.core.navigation.Route.Map(
                                roomId = state.sessionInfo.roomId,
                                userId = state.sessionInfo.userId,
                                userName = state.sessionInfo.userName
                            )
                        )

                        // 딥링크 처리
                        LaunchedEffect(intent) {
                            handleDeepLink(intent, navigator)
                        }

                        MainRoute(navigator = navigator)
                    }

                    SessionState.None -> {
                        // 세션 없음 → Home 화면부터 시작
                        val navigator = rememberMainNavigator()

                        // 딥링크 처리
                        LaunchedEffect(intent) {
                            handleDeepLink(intent, navigator)
                        }

                        MainRoute(navigator = navigator)
                    }
                }
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