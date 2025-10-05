package com.teammanduk.adego.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@Composable
internal fun SelectPlaceRoute(
    onBackClick: () -> Unit = {},
    onPlaceSelected: () -> Unit = {}
) {
    SelectPlaceScreen(
        onBackClick = onBackClick,
        onPlaceSelected = onPlaceSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPlaceScreen(
    onBackClick: () -> Unit,
    onPlaceSelected: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "모임 장소 선택",
                        style = AdegoTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 지도 Placeholder
            MapPlaceholder(
                modifier = Modifier.fillMaxSize()
            )

            // 하단 장소 정보 카드 + 버튼
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlaceInfoCard(
                    placeName = "부산 북구 만덕대로 291",
                    placeAddress = "부산 북구 만덕동 607-1"
                )

                Button(
                    onClick = onPlaceSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdegoTheme.colors.main500
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "모임 장소 선택하기",
                        style = AdegoTheme.typography.titleLarge,
                        color = AdegoTheme.colors.onMain500
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "지도 영역",
            tint = Color(0xFF757575),
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun PlaceInfoCard(
    placeName: String,
    placeAddress: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = placeName,
            style = AdegoTheme.typography.titleLarge
        )
        Text(
            text = placeAddress,
            style = AdegoTheme.typography.bodySmall,
            color = Color(0xFF757575)
        )
    }
}
