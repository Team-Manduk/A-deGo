package com.teammanduk.adego.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

/**
 * 경로 설정 다이얼로그
 */
@Composable
internal fun RouteSetupDialog(
    destination: com.teammanduk.adego.core.model.Place?,
    startPlace: com.teammanduk.adego.core.model.Place?,
    isSearchingRoute: Boolean,
    onDismiss: () -> Unit,
    onSelectStartPlace: () -> Unit,
    onSearchRoute: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    text = "경로 설정",
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

            // 출발지 선택
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "출발지",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (startPlace != null) AdegoTheme.colors.main500 else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = if (startPlace != null) AdegoTheme.colors.main500.copy(alpha = 0.05f) else Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectStartPlace() }
                        .padding(16.dp)
                ) {
                    if (startPlace != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "출발지",
                                tint = AdegoTheme.colors.main500,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = startPlace.name,
                                    style = AdegoTheme.typography.bodyLarge,
                                    color = AdegoTheme.colors.onBackground
                                )
                                Text(
                                    text = startPlace.address,
                                    style = AdegoTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "출발지를 선택하세요",
                            style = AdegoTheme.typography.bodyLarge,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 목적지 표시 (자동 설정됨)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "목적지",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = AdegoTheme.colors.main500,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = AdegoTheme.colors.main500.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "목적지",
                            tint = AdegoTheme.colors.main500,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = destination?.name ?: "목적지 없음",
                                style = AdegoTheme.typography.bodyLarge,
                                color = AdegoTheme.colors.onBackground
                            )
                            destination?.address?.let { address ->
                                Text(
                                    text = address,
                                    style = AdegoTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 경로 검색 버튼
            Button(
                onClick = onSearchRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = startPlace != null && !isSearchingRoute
            ) {
                if (isSearchingRoute) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "경로 검색 중...",
                        style = AdegoTheme.typography.titleLarge,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "경로 검색",
                        style = AdegoTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 초대 다이얼로그
 */
@Composable
internal fun InviteDialog(
    inviteCode: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    text = "모임 초대",
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 코드 복사 버튼
            Button(
                onClick = {
                    // TODO: 클립보드에 코드 복사
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "코드 복사",
                    style = AdegoTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SNS 공유 버튼
            OutlinedButton(
                onClick = {
                    // TODO: SNS 공유 기능
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = AdegoTheme.colors.main500
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SNS 공유",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.main500
                )
            }
        }
    }
}

/**
 * 경로 목록 다이얼로그
 */
@Composable
internal fun RouteListDialog(
    routes: List<com.teammanduk.adego.feature.map.model.RouteUiModel>,
    selectedRouteIndex: Int?,
    onRouteSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "경로 선택",
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

            Spacer(modifier = Modifier.height(16.dp))

            // 경로 목록
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                routes.forEachIndexed { index, route ->
                    RouteCard(
                        route = route,
                        isSelected = selectedRouteIndex == index,
                        onClick = { onRouteSelect(index) }
                    )
                }
            }
        }
    }
}

/**
 * 경로 카드 컴포넌트
 */
@Composable
private fun RouteCard(
    route: com.teammanduk.adego.feature.map.model.RouteUiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pathTypeText = when (route.pathType) {
        1 -> "지하철"
        2 -> "버스"
        3 -> "지하철+버스"
        else -> "도보"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AdegoTheme.colors.main500 else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isSelected) AdegoTheme.colors.main500.copy(alpha = 0.05f) else Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // 경로 타입 배지
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = AdegoTheme.colors.main500,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = pathTypeText,
                    style = AdegoTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            if (route.transferCount > 0) {
                Text(
                    text = "환승 ${route.transferCount}회",
                    style = AdegoTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 경로 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시간 및 거리
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${route.totalTime}분",
                        style = AdegoTheme.typography.titleLarge,
                        color = AdegoTheme.colors.onBackground
                    )
                    Text(
                        text = "• ${(route.totalDistance / 1000.0).format(1)}km",
                        style = AdegoTheme.typography.bodyLarge,
                        color = Color(0xFF757575)
                    )
                }
            }

            // 요금
            Text(
                text = "${route.totalFare}원",
                style = AdegoTheme.typography.titleLarge,
                color = AdegoTheme.colors.main500
            )
        }
    }
}

// 소수점 포맷 헬퍼 함수
private fun Double.format(digits: Int) = "%.${digits}f".format(this)
