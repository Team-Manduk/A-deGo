package com.teammanduk.adego.feature.place.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun SelectPlaceMap(
    selectedPosition: LatLng,
    isUserDragging: Boolean,
    onCameraMove: () -> Unit,
    onCameraIdle: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedPosition, 17f)
    }

    // 이전 selectedPosition을 기억하여 실제로 변경되었을 때만 카메라 이동
    var previousPosition by remember { mutableStateOf<LatLng?>(null) }

    // 자동 애니메이션 진행 중인지 추적 (사용자 드래그와 구분하기 위함)
    var isAutoAnimating by remember { mutableStateOf(false) }

    // searchResult가 변경될 때만 카메라 이동 (검색 결과가 있을 때)
    LaunchedEffect(selectedPosition) {
        // 위치가 실제로 변경되었을 때 카메라 이동 (previousPosition이 null이거나 다를 때)
        if (previousPosition == null || selectedPosition != previousPosition) {
            isAutoAnimating = true
            // 첫 번째 위치 설정일 때는 줌을 17로 설정, 이후에는 현재 줌 유지
            val targetZoom = if (previousPosition == null) 17f else cameraPositionState.position.zoom
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(
                        selectedPosition, // 타겟 위치
                        targetZoom, // 첫 진입시 17, 이후 현재 줌 유지
                        cameraPositionState.position.tilt, // 현재 기울기 유지
                        cameraPositionState.position.bearing // 현재 회전 유지
                    )
                )
            )
            previousPosition = selectedPosition
            isAutoAnimating = false
        }
    }

    // 카메라 이동 감지 (사용자 드래그만 감지, 자동 애니메이션 제외)
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && !isAutoAnimating) {
            onCameraMove()
        }
    }

    // 카메라 이동 완료 감지 및 역지오코딩
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .collect { isMoving ->
                if (!isMoving && !isAutoAnimating) {
                    // 지도 이동이 완료되고, 자동 애니메이션이 아닐 때만 역지오코딩
                    // (사용자가 직접 드래그한 경우에만 역지오코딩 실행)
                    val centerLatLng = cameraPositionState.position.target
                    onCameraIdle(centerLatLng)
                }
            }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = true
        ),
        contentPadding = PaddingValues(
            top = 80.dp,
            bottom = 200.dp
        )
    ) {
        // 마커 제거 - 화면 중앙에 고정된 아이콘 사용
    }
}
