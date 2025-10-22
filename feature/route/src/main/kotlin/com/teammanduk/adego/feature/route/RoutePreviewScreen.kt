package com.teammanduk.adego.feature.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.teammanduk.adego.core.model.Route

enum class RouteScreenMode {
    PREVIEW,  // 경로 미리보기 (버튼 표시)
    GUIDANCE  // 경로 안내 (버튼 숨김, 내 위치 추적 버튼 표시)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutePreviewScreen(
    route: Route,
    isLoadingDetails: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    mode: RouteScreenMode = RouteScreenMode.PREVIEW,
    onMyLocationClick: (() -> Unit)? = null,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState? = null
) {
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val scope = rememberCoroutineScope()

    val allPoints = route.calculateAllPoints()
    val internalCameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(allPoints.center(), 14f)
    }
    val actualCameraPositionState = cameraPositionState ?: internalCameraPositionState

    val onSubPathClick: (Int) -> Unit = { index ->
        val subPath = route.subPaths[index]
        val targetLat = subPath.startLatitude
        val targetLng = subPath.startLongitude

        if (targetLat != null && targetLng != null) {
            actualCameraPositionState.position = CameraPosition.fromLatLngZoom(
                com.google.android.gms.maps.model.LatLng(targetLat, targetLng),
                16f
            )
            scope.launch {
                bottomSheetState.partialExpand()
            }
        }
    }

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetDragHandle = { DragHandle() },
        sheetContent = {
            RouteDetailContent(
                route = route,
                isLoadingDetails = isLoadingDetails,
                onConfirm = onConfirm,
                onCancel = onCancel,
                onSubPathClick = onSubPathClick,
                showButtons = mode == RouteScreenMode.PREVIEW
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteMapView(
                route = route,
                allPoints = allPoints,
                cameraPositionState = actualCameraPositionState,
                showMyLocationButton = mode == RouteScreenMode.GUIDANCE,
                onMyLocationClick = onMyLocationClick
            )
        }
    }
}
