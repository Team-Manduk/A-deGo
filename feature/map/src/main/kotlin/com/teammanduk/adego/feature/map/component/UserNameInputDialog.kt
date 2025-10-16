package com.teammanduk.adego.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@Composable
internal fun UserNameInputDialog(
    userName: String,
    onUserNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    error: String?,
    onGenerateRandomName: () -> Unit = {}
) {
    // 최초 다이얼로그 표시 시 랜덤 닉네임 생성
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (userName.isBlank()) {
            onGenerateRandomName()
        }
    }

    Dialog(onDismissRequest = { }) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "사용자 이름 입력",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 이름 입력 필드
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "이름을 입력해주세요",
                            style = AdegoTheme.typography.bodyLarge,
                            color = AdegoTheme.colors.line500
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = onGenerateRandomName) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "랜덤 닉네임 생성",
                                tint = AdegoTheme.colors.main500
                            )
                        }
                    }
                )

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = Color.Red,
                        style = AdegoTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 확인 버튼
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = userName.isNotBlank()
            ) {
                Text(
                    text = "확인",
                    style = AdegoTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }
}