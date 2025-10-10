package com.teammanduk.adego.feature.create

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.model.Place

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectPlaceRoute(
    onBackClick: () -> Unit = {},
    onPlaceSelected: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    viewModel: SelectPlaceViewModel = hiltViewModel()
) {
    val searchResult by viewModel.currentSearchResult.collectAsStateWithLifecycle()

    // LaunchedEffect를 사용하여 선택된 장소가 있을 때 searchResult 업데이트
    LaunchedEffect(Unit) {
        // ViewModel이 생성될 때 이미 selectedPlace의 변경을 감지하고 있음
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
            onMapClick = { latLng ->
                viewModel.searchByCoordinates(latLng.latitude, latLng.longitude)
                Log.d("SelectPlaceRoute", "onMapClick: $latLng");
            },
            onPlaceSelected = {
                viewModel.confirmSelection()
                onPlaceSelected()
            },
            onSearchClick = onSearchClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SelectPlaceScreen(
    searchResult: Place?,
    onMapClick: (LatLng) -> Unit,
    onPlaceSelected: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // searchResult에서 위치 정보를 가져오거나 기본값 사용
    val selectedPosition = remember(searchResult) {
        if (searchResult != null) {
            LatLng(searchResult.latitude, searchResult.longitude)
        } else {
            LatLng(35.1979, 129.0758) // 기본 위치
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 지도 표시
        MapPlaceholder(
            modifier = Modifier.fillMaxSize(),
            selectedPosition = selectedPosition,
            onMapClick = onMapClick
        )

        // 상단 검색창
        OutlinedTextField(
            value = "",
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onSearchClick() },
            enabled = false,
            placeholder = {
                Text(
                    text = "장소를 검색하세요",
                    style = AdegoTheme.typography.bodyLarge,
                    color = Color(0xFF9E9E9E)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AdegoTheme.colors.main500
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = AdegoTheme.colors.main500,
                disabledContainerColor = Color.White,
                disabledPlaceholderColor = Color(0xFF9E9E9E),
                disabledLeadingIconColor = AdegoTheme.colors.main500
            ),
            shape = RoundedCornerShape(12.dp)
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
    selectedPosition: LatLng,
    onMapClick: (LatLng) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedPosition, 15f)
    }

    LaunchedEffect(selectedPosition) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(selectedPosition, 15f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false
        ),
        onMapClick = onMapClick
    ) {
        Marker(
            state = MarkerState(position = selectedPosition),
            title = "선택한 위치"
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
