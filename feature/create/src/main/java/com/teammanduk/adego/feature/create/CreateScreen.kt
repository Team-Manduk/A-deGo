package com.teammanduk.adego.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@Composable
internal fun CreateRoute(
    onNavigateBack: () -> Unit = {},
    onNavigateToSelectPlace: () -> Unit = {},
    onCreateMeeting: (String, Int, Int) -> Unit = { _, _, _ -> },
) {
    CreateScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToSelectPlace = onNavigateToSelectPlace,
        onCreateMeeting = onCreateMeeting,
    )
}

@Composable
private fun CreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelectPlace: () -> Unit,
    onCreateMeeting: (String, Int, Int) -> Unit,
) {
    var selectedPlace by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("") }
    var selectedMinute by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdegoTheme.colors.background)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = AdegoTheme.colors.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "모임 생성",
                style = AdegoTheme.typography.titleLarge,
                color = AdegoTheme.colors.onBackground
            )
        }

        // 메인 컨텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = buildString {
                    append(selectedHour.ifEmpty { "00" })
                    append("시 ")
                    append(selectedMinute.ifEmpty { "00" })
                    append("분 모임 시작!")
                },
                style = AdegoTheme.typography.headlineLarge,
                color = AdegoTheme.colors.main500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 장소 선택
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "장소 선택",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.main500
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = selectedPlace,
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSelectPlace() },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = AdegoTheme.colors.main500,
                        disabledContainerColor = Color.Transparent,
                        disabledTextColor = AdegoTheme.colors.onBackground,
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 시간 선택
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "시간 선택",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.main500
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = selectedHour,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 0..23)) {
                                selectedHour = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "시",
                                color = AdegoTheme.colors.line500
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdegoTheme.colors.main500,
                            unfocusedBorderColor = AdegoTheme.colors.main500,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = AdegoTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center
                        )
                    )

                    OutlinedTextField(
                        value = selectedMinute,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 0..59)) {
                                selectedMinute = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "분",
                                color = AdegoTheme.colors.line500
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdegoTheme.colors.main500,
                            unfocusedBorderColor = AdegoTheme.colors.main500,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = AdegoTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 모임 생성하기 버튼
            Button(
                onClick = {
                    val hour = selectedHour.toIntOrNull() ?: 0
                    val minute = selectedMinute.toIntOrNull() ?: 0
                    onCreateMeeting(selectedPlace, hour, minute)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedPlace.isNotEmpty() && selectedHour.isNotEmpty() && selectedMinute.isNotEmpty()
            ) {
                Text(
                    text = "모임 생성하기",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onMain500
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateScreenPreview() {
    AdegoTheme {
        CreateScreen(
            onNavigateBack = {},
            onNavigateToSelectPlace = {},
            onCreateMeeting = { _, _, _ -> },
        )
    }
}
