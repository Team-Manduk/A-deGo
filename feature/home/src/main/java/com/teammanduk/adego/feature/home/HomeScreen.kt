package com.teammanduk.adego.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@Composable
internal fun HomeRoute(
    onNavigateToCreate: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onJoinWithCode: (String) -> Unit = {},
) {
    HomeScreen(
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToSettings = onNavigateToSettings,
        onJoinWithCode = onJoinWithCode,
    )
}

@Composable
private fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onJoinWithCode: (String) -> Unit,
) {
    var inviteCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdegoTheme.colors.background)
    ) {
        // 설정 아이콘
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "설정",
                tint = AdegoTheme.colors.main900
            )
        }

        // 메인 컨텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 타이틀
            Text(
                text = "지금 만나기로 했나요?",
                style = AdegoTheme.typography.headlineLarge,
                color = AdegoTheme.colors.main500
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 서브타이틀
            Text(
                text = "바로 시작해 보세요",
                style = AdegoTheme.typography.bodyLarge,
                color = AdegoTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 새 모임 만들기 버튼
            Button(
                onClick = onNavigateToCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+ 새 모임 만들기",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onMain500
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 초대 코드로 시작하기
            Text(
                text = "초대 코드로 시작하기",
                style = AdegoTheme.typography.titleLarge,
                color = AdegoTheme.colors.main500,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 코드 입력 및 입장 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AdegoTheme.colors.main500,
                        unfocusedBorderColor = AdegoTheme.colors.main500,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = { onJoinWithCode(inviteCode) },
                    modifier = Modifier
                        .width(80.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdegoTheme.colors.main500
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "입장",
                        style = AdegoTheme.typography.titleSmall,
                        color = AdegoTheme.colors.onMain500
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 안내 문구
            Text(
                text = "모임이 종료될 때까지 백그라운드에서도\n위치정보를 공유합니다.",
                style = AdegoTheme.typography.bodySmall,
                color = AdegoTheme.colors.line500
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    AdegoTheme {
        HomeScreen(
            onNavigateToCreate = {},
            onNavigateToSettings = {},
            onJoinWithCode = {},
        )
    }
}
