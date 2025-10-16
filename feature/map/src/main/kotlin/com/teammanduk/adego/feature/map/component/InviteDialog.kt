package com.teammanduk.adego.feature.map.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@Composable
fun InviteDialog(
    inviteCode: String,
    roomName: String,
    destinationName: String,
    meetingTime: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = AdegoTheme.colors.onBackground,
                )
            }

            // 초대 코드 표시
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "초대 코드",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 2.dp,
                                color = AdegoTheme.colors.main500,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = inviteCode,
                            style = AdegoTheme.typography.headlineLarge,
                            color = AdegoTheme.colors.main500,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            val clipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText("invite_code", inviteCode)
                            clipboardManager.setPrimaryClip(clipData)
                            Toast.makeText(context, "초대 코드가 복사되었습니다", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AdegoTheme.colors.main500
                        ),
                        modifier = Modifier.fillMaxHeight(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "복사",
                            style = AdegoTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.height(24.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 문자 공유 버튼
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            val deepLink = "https://a-dego.web.app/join/$inviteCode"
                            val message = """
                                |🗺️ A-dego 모임 초대
                                |
                                |모임: $roomName
                                |목적지: $destinationName
                                |시간: $meetingTime
                                |
                                |참가하기: $deepLink
                            """.trimMargin()

                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:")
                                putExtra("sms_body", message)
                            }

                            try {
                                context.startActivity(smsIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "문자 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "문자",
                            tint = AdegoTheme.colors.main500
                        )
                    }
                    Text(
                        text = "문자",
                        style = AdegoTheme.typography.bodySmall,
                        color = AdegoTheme.colors.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 카카오톡 공유 버튼
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            val deepLink = "https://a-dego.web.app/join/$inviteCode"
                            val message = """
                                |🗺️ A-dego 모임 초대
                                |
                                |모임: $roomName
                                |목적지: $destinationName
                                |시간: $meetingTime
                                |
                                |참가하기: $deepLink
                            """.trimMargin()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }

                            val chooserIntent = Intent.createChooser(shareIntent, "초대 링크 공유")

                            try {
                                context.startActivity(chooserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "공유할 수 있는 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "카카오톡",
                            tint = AdegoTheme.colors.main500
                        )
                    }
                    Text(
                        text = "카카오톡",
                        style = AdegoTheme.typography.bodySmall,
                        color = AdegoTheme.colors.onBackground
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InviteDialogPreview() {
    AdegoTheme {
        InviteDialog(
            inviteCode = "ABC123",
            roomName = "주말 브런치 모임",
            destinationName = "강남역 카페",
            meetingTime = "12시 30분",
            onDismiss = {}
        )
    }
}
