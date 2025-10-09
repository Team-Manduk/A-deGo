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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.model.Place

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectPlaceRoute(
    onBackClick: () -> Unit = {},
    onPlaceSelected: () -> Unit = {},
    viewModel: SelectPlaceViewModel = hiltViewModel()
) {
    val searchResult by viewModel.currentSearchResult.collectAsStateWithLifecycle()

    // 초기 테스트 데이터 로드 (지도 중심 위경도)
    LaunchedEffect(Unit) {
        viewModel.searchByCoordinates(35.1979, 129.0758)
    }

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
        SelectPlaceScreen(
            searchResult = searchResult,
            onMapClick = { lat, lng ->
                // TODO: 실제 지도 클릭 시 호출
                viewModel.searchByCoordinates(lat, lng)
            },
            onPlaceSelected = {
                viewModel.confirmSelection()
                onPlaceSelected()
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SelectPlaceScreen(
    searchResult: Place?,
    onMapClick: (Double, Double) -> Unit,
    onPlaceSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 지도 Placeholder
        MapPlaceholder(
            modifier = Modifier.fillMaxSize(),
            onMapClick = onMapClick
        )

        // 하단 장소 정보 카드 + 버튼
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            searchResult?.let { place ->
                PlaceInfoCard(
                    placeName = place.name,
                    placeAddress = place.address
                )
            }

            Button(
                onClick = onPlaceSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = searchResult != null
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

@Composable
private fun MapPlaceholder(
    modifier: Modifier = Modifier,
    onMapClick: (Double, Double) -> Unit
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
        // TODO: 실제 Google Maps Compose 연동 시 onMapClick 사용
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
