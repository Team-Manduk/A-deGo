package com.teammanduk.adego.core.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    confirmText: String = "다시 요청",
    dismissText: String = "취소",
    onDismiss: () -> Unit,
    onRequestAgain: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
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
                    text = title,
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onBackground
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = AdegoTheme.colors.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Message
            Text(
                text = message,
                style = AdegoTheme.typography.bodyLarge,
                color = AdegoTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AdegoTheme.colors.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = dismissText,
                        style = AdegoTheme.typography.titleLarge
                    )
                }

                Button(
                    onClick = onRequestAgain,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdegoTheme.colors.main500
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = confirmText,
                        style = AdegoTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GoToSettingsDialog(
    title: String,
    message: String,
    confirmText: String = "설정 열기",
    dismissText: String = "취소",
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
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
                    text = title,
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onBackground
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = AdegoTheme.colors.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Message
            Text(
                text = message,
                style = AdegoTheme.typography.bodyLarge,
                color = AdegoTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AdegoTheme.colors.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = dismissText,
                        style = AdegoTheme.typography.titleLarge
                    )
                }

                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdegoTheme.colors.main500
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = confirmText,
                        style = AdegoTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}
