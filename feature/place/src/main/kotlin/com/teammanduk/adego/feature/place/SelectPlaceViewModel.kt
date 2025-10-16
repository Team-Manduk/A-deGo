package com.teammanduk.adego.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.teammanduk.adego.core.domain.usecase.GetCurrentLocationUseCase
import com.teammanduk.adego.core.domain.usecase.GetSelectedPlaceUseCase
import com.teammanduk.adego.core.domain.usecase.SearchPlaceByCoordinatesUseCase
import com.teammanduk.adego.core.domain.usecase.SetSelectedPlaceUseCase
import com.teammanduk.adego.core.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SelectPlaceViewModel @Inject constructor(
    private val searchPlaceByCoordinatesUseCase: SearchPlaceByCoordinatesUseCase,
    private val getSelectedPlaceUseCase: GetSelectedPlaceUseCase,
    private val setSelectedPlaceUseCase: SetSelectedPlaceUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel() {

    // Repository의 검색 결과를 구독
    // SearchPlaceScreen에서 장소를 선택하면 여기에 반영됨
    val currentSearchResult: StateFlow<Place?> = getSelectedPlaceUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 초기 위치 (현재 위치 또는 기본값)
    private val _initialPosition = MutableStateFlow<LatLng?>(null)
    val initialPosition: StateFlow<LatLng?> = _initialPosition

    // 로딩 상태 관리
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 현재 카메라 위치 추적
    private val _currentCameraPosition = MutableStateFlow<LatLng?>(null)

    // 사용자가 드래그 중인지 여부 추적
    private val _isUserDragging = MutableStateFlow(false)
    val isUserDragging: StateFlow<Boolean> = _isUserDragging

    // 검색으로 선택된 장소인지 여부 (역지오코딩 결과 무시용)
    private val _isFromSearch = MutableStateFlow(false)

    // 역지오코딩 요청 ID (최신 요청만 처리하기 위함)
    private var geocodingRequestId = 0L

    // 카메라 위치 변경을 위한 SharedFlow
    private val cameraPositionFlow = MutableSharedFlow<LatLng>(replay = 0)

    init {
        // 초기 위치 가져오기 (현재 위치)
        viewModelScope.launch {
            val currentLocation = getCurrentLocationUseCase()
            if (currentLocation != null) {
                _initialPosition.value = LatLng(currentLocation.latitude, currentLocation.longitude)
            } else {
                // 현재 위치를 가져올 수 없으면 기본 위치 사용 (부산 만덕동)
                _initialPosition.value = LatLng(35.1979, 129.0758)
            }
        }

        // Debouncing: 카메라가 멈춘 후 500ms 대기 후 지오코딩 실행
        viewModelScope.launch {
            cameraPositionFlow
                .debounce(500L) // 500ms 대기
                .collect { requestedLatLng ->
                    // 요청 ID를 캡처 (API 호출 전에)
                    val currentRequestId = geocodingRequestId

                    // API 응답을 받을 때 현재 카메라 위치와 비교
                    val place = searchPlaceByCoordinatesUseCase(
                        requestedLatLng.latitude,
                        requestedLatLng.longitude
                    )

                    // 응답이 왔을 때:
                    // 1. 요청 ID가 여전히 유효한지 확인 (새로운 액션이 시작되지 않았는지)
                    // 2. 현재 카메라 위치와 요청했던 위치가 같은지 확인
                    // 3. 사용자가 드래그 중이 아닌지 확인
                    // 4. 검색으로 선택된 장소가 아닌지 확인
                    val currentPosition = _currentCameraPosition.value
                    val shouldApplyResult = currentRequestId == geocodingRequestId &&
                        currentPosition != null &&
                        isSameLocation(requestedLatLng, currentPosition) &&
                        !_isUserDragging.value &&
                        !_isFromSearch.value

                    if (shouldApplyResult) {
                        // 위치가 같고 드래그 중이 아니며 검색 결과가 아니면 결과 적용
                        place?.let { setSelectedPlaceUseCase(it) }
                        // 결과를 적용했으므로 로딩 종료
                        _isLoading.value = false
                    }
                    // 조건 불만족으로 결과를 무시하면 로딩 상태 유지
                    // (다음 드래그가 끝나고 새로운 응답이 올 때까지)
                }
        }

        // 검색으로 선택된 장소가 변경되면 로딩 상태 해제 및 플래그 설정
        viewModelScope.launch {
            currentSearchResult.collect { place ->
                if (place != null) {
                    // 검색으로 선택된 장소는 이미 확정된 정보이므로 로딩 해제
                    _isLoading.value = false
                    // 검색으로부터 선택됨 플래그 설정 (역지오코딩 결과 무시용)
                    _isFromSearch.value = true
                    // 모든 진행 중인 역지오코딩 요청 무효화
                    geocodingRequestId++
                }
            }
        }
    }

    // 두 위치가 실질적으로 같은지 확인 (소수점 6자리까지 비교, 약 0.1m 오차)
    private fun isSameLocation(pos1: LatLng, pos2: LatLng): Boolean {
        val latDiff = kotlin.math.abs(pos1.latitude - pos2.latitude)
        val lngDiff = kotlin.math.abs(pos1.longitude - pos2.longitude)
        return latDiff < 0.000001 && lngDiff < 0.000001
    }

    // 카메라 이동 시작 (로딩 상태 활성화, 드래그 상태 추적)
    fun onCameraMove() {
        _isUserDragging.value = true
        _isLoading.value = true
        // 사용자가 직접 드래그하기 시작하면 검색 플래그 해제
        _isFromSearch.value = false
        // 새로운 드래그 세션 시작: 이전 역지오코딩 요청 무효화
        geocodingRequestId++
    }

    // 카메라 위치가 변경될 때 호출 (debouncing 적용)
    fun onCameraPositionChanged(latLng: LatLng) {
        _currentCameraPosition.value = latLng
        viewModelScope.launch {
            cameraPositionFlow.emit(latLng)
        }
    }

    // 카메라 이동 완료 (드래그 종료)
    fun onCameraIdle() {
        _isUserDragging.value = false
    }

    // 위경도로 장소 검색 (검색 화면에서 장소 선택 시) - 즉시 실행
    fun searchByCoordinates(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val place = searchPlaceByCoordinatesUseCase(latitude, longitude)
            place?.let { setSelectedPlaceUseCase(it) }
        }
    }

    // 검색 결과를 최종 선택으로 확정 (CreateScreen으로 전달)
    fun confirmSelection() {
        viewModelScope.launch {
            currentSearchResult.value?.let { place ->
                setSelectedPlaceUseCase(place)
            }
        }
    }
}
