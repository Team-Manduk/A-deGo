package com.teammanduk.adego.feature.place

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.ui.permission.PermissionRequester
import com.teammanduk.adego.core.ui.permission.PermissionType
import com.teammanduk.adego.feature.place.component.PlaceInfoCard
import com.teammanduk.adego.feature.place.component.SelectPlaceMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPlaceRoute(
    title: String = "장소 선택",
    buttonText: String = "장소 선택하기",
    showTopBar: Boolean = true,
    onBackClick: () -> Unit = {},
    onPlaceSelected: (Place) -> Unit = {},
    onSearchClick: () -> Unit = {},
    viewModel: SelectPlaceViewModel = hiltViewModel()
) {
    val searchResult by viewModel.currentSearchResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isUserDragging by viewModel.isUserDragging.collectAsStateWithLifecycle()

    PermissionRequester(
        permissionTypes = listOf(PermissionType.Location)
    ) {
        if (showTopBar) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
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
                    isLoading = isLoading,
                    isUserDragging = isUserDragging,
                    buttonText = buttonText,
                    onCameraMove = {
                        viewModel.onCameraMove()
                    },
                    onCameraIdle = { latLng ->
                        viewModel.onCameraIdle()
                        viewModel.onCameraPositionChanged(latLng)
                    },
                    onPlaceSelected = {
                        viewModel.confirmSelection()
                        searchResult?.let { onPlaceSelected(it) }
                    },
                    onSearchClick = onSearchClick,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        } else {
            // TopBar 없이 사용 (다이얼로그 등에서)
            SelectPlaceScreen(
                searchResult = searchResult,
                isLoading = isLoading,
                isUserDragging = isUserDragging,
                buttonText = buttonText,
                onCameraMove = {
                    viewModel.onCameraMove()
                },
                onCameraIdle = { latLng ->
                    viewModel.onCameraIdle()
                    viewModel.onCameraPositionChanged(latLng)
                },
                onPlaceSelected = {
                    viewModel.confirmSelection()
                    searchResult?.let { onPlaceSelected(it) }
                },
                onSearchClick = onSearchClick
            )
        }
    }
}

@Composable
private fun SelectPlaceScreen(
    searchResult: Place?,
    isLoading: Boolean,
    isUserDragging: Boolean,
    buttonText: String = "장소 선택하기",
    onCameraMove: () -> Unit,
    onCameraIdle: (LatLng) -> Unit,
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
        SelectPlaceMap(
            modifier = Modifier.fillMaxSize(),
            selectedPosition = selectedPosition,
            isUserDragging = isUserDragging,
            onCameraMove = onCameraMove,
            onCameraIdle = onCameraIdle
        )

        // 화면 중앙에 고정된 마커
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "선택 위치",
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .offset(y = (-24).dp), // 마커의 하단이 중심점이 되도록 오프셋
            tint = AdegoTheme.colors.main500
        )

        // 로딩 인디케이터
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 60.dp),
                color = AdegoTheme.colors.main500
            )
        }

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
            // 검색 결과가 있고 로딩 중이 아닐 때 정보 카드 표시
            AnimatedVisibility(
                visible = searchResult != null && !isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                searchResult?.let {
                    PlaceInfoCard(
                        place = it
                    )
                }
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
                enabled = !isLoading && searchResult != null
            ) {
                Text(
                    text = buttonText,
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onMain500
                )
            }
        }
    }
}
