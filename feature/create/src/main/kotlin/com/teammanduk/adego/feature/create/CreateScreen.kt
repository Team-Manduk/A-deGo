package com.teammanduk.adego.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teammanduk.adego.core.designsystem.component.AdegoDatePicker
import com.teammanduk.adego.core.designsystem.component.AdegoTimePicker
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.ui.extension.clickableOnce
import com.teammanduk.adego.feature.create.model.CreateIntent
import com.teammanduk.adego.feature.create.model.CreateSideEffect
import com.teammanduk.adego.feature.create.model.CreateStep
import com.teammanduk.adego.feature.create.model.CreateUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CreateRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSelectPlace: () -> Unit,
    onNavigateToMap: (String, String) -> Unit,
    viewModel: CreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // SideEffect 처리
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                CreateSideEffect.NavigateToSelectPlace -> onNavigateToSelectPlace()
                CreateSideEffect.NavigateBack -> onNavigateBack()
                is CreateSideEffect.NavigateToMap -> onNavigateToMap(
                    sideEffect.roomId,
                    sideEffect.userId
                )
            }
        }
    }

    // 에러 표시
    uiState.error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(CreateIntent.ClearError) },
            title = { Text("방 생성 실패") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onIntent(CreateIntent.ClearError) }
                ) {
                    Text("확인")
                }
            }
        )
    }

    CreateScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent
    )
}

@Composable
private fun CreateScreen(
    uiState: CreateUiState,
    onIntent: (CreateIntent) -> Unit
) {
    val currentCalendar = java.util.Calendar.getInstance()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(AdegoTheme.colors.background)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onIntent(CreateIntent.NavigateBack) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = AdegoTheme.colors.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "모임 생성",
                style = AdegoTheme.typography.titleLarge,
                color = AdegoTheme.colors.onBackground
            )
        }

        // 메인 컨텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            val dateFormat = SimpleDateFormat("MM월 dd일", Locale.KOREAN)
            val displayDate = uiState.selectedDate?.let { dateFormat.format(Date(it)) } ?: "날짜 선택"
            val displayTime = if (uiState.selectedHour != null && uiState.selectedMinute != null) {
                String.format("%02d시 %02d분", uiState.selectedHour, uiState.selectedMinute)
            } else {
                "00시 00분"
            }

            Text(
                text = "$displayTime 모임 시작!",
                style = AdegoTheme.typography.headlineLarge,
                color = AdegoTheme.colors.main500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 장소 선택 (항상 표시)
            PlaceSelectionSection(
                selectedPlace = uiState.selectedPlace,
                meetingPlaceName = uiState.meetingPlaceName,
                onMeetingPlaceNameChange = { onIntent(CreateIntent.UpdateMeetingPlaceName(it)) },
                onIntent = onIntent
            )

            // 시간 선택 (장소 선택 완료 후 표시)
            if (uiState.currentStep >= CreateStep.TIME_SELECTION) {
                Spacer(modifier = Modifier.height(32.dp))
                TimeSelectionSection(
                    selectedDate = uiState.selectedDate,
                    selectedHour = uiState.selectedHour,
                    selectedMinute = uiState.selectedMinute,
                    displayDate = displayDate,
                    displayTime = displayTime,
                    onDateClick = { showDatePicker = true },
                    onTimeClick = { showTimePicker = true }
                )
            }

            // 방 이름 입력 (시간 선택 완료 후 표시)
            if (uiState.currentStep >= CreateStep.ROOM_NAME_INPUT) {
                Spacer(modifier = Modifier.height(32.dp))
                RoomNameInputSection(
                    roomName = uiState.roomName,
                    onRoomNameChange = { onIntent(CreateIntent.UpdateRoomName(it)) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 모임 생성하기 버튼 (모든 단계 완료 후 표시)
            if (uiState.currentStep == CreateStep.READY_TO_CREATE) {
                Button(
                    onClick = {
                        onIntent(
                            CreateIntent.CreateRoom(
                                userId = "user_${System.currentTimeMillis()}",
                                userName = ""
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdegoTheme.colors.main500
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isCreating
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AdegoTheme.colors.onMain500
                        )
                    } else {
                        Text(
                            text = "모임 생성하기",
                            style = AdegoTheme.typography.titleLarge,
                            color = AdegoTheme.colors.onMain500
                        )
                    }
                }
            }
        }
    }

    // DatePicker
    AdegoDatePicker(
        show = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { dateMillis ->
            dateMillis?.let {
                onIntent(CreateIntent.SelectDate(it))
                showDatePicker = false
            }
        },
        initialDate = uiState.selectedDate ?: currentCalendar.timeInMillis
    )

    // TimePicker
    AdegoTimePicker(
        show = showTimePicker,
        onDismiss = { showTimePicker = false },
        onConfirm = { hour, minute ->
            onIntent(CreateIntent.SelectTime(hour, minute))
            showTimePicker = false
        },
        initialHour = uiState.selectedHour ?: currentCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = uiState.selectedMinute ?: currentCalendar.get(java.util.Calendar.MINUTE)
    )
}

// 장소 선택 섹션
@Composable
private fun PlaceSelectionSection(
    selectedPlace: Place?,
    meetingPlaceName: String,
    onMeetingPlaceNameChange: (String) -> Unit,
    onIntent: (CreateIntent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "장소 선택",
            style = AdegoTheme.typography.titleLarge,
            color = AdegoTheme.colors.main500
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 주소 표시 필드
        OutlinedTextField(
            value = selectedPlace?.address ?: "",
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .clickableOnce { onIntent(CreateIntent.NavigateToSelectPlace) },
            enabled = false,
            placeholder = {
                Text(
                    text = "모임 장소를 선택해 주세요",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.line500
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AdegoTheme.colors.main500,
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = AdegoTheme.colors.main500,
                disabledContainerColor = Color.Transparent,
                disabledTextColor = AdegoTheme.colors.onBackground,
                disabledPlaceholderColor = AdegoTheme.colors.line500,
                disabledLeadingIconColor = AdegoTheme.colors.main500
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // 장소 선택 후 모임 장소 이름 입력 필드 표시
        if (selectedPlace != null) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = meetingPlaceName,
                onValueChange = { if (it.length <= 30) onMeetingPlaceNameChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "모임 장소 이름을 입력해 주세요",
                        style = AdegoTheme.typography.bodyLarge,
                        color = AdegoTheme.colors.line500
                    )
                },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "${meetingPlaceName.length}/30",
                        style = AdegoTheme.typography.bodySmall,
                        color = AdegoTheme.colors.line500,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdegoTheme.colors.main500,
                    unfocusedBorderColor = AdegoTheme.colors.main500,
                    focusedTextColor = AdegoTheme.colors.onBackground,
                    unfocusedTextColor = AdegoTheme.colors.onBackground,
                    cursorColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// 시간 선택 섹션
@Composable
private fun TimeSelectionSection(
    selectedDate: Long?,
    selectedHour: Int?,
    selectedMinute: Int?,
    displayDate: String,
    displayTime: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "시간 선택",
            style = AdegoTheme.typography.titleLarge,
            color = AdegoTheme.colors.main500
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 날짜 선택
            OutlinedTextField(
                value = if (selectedDate != null) displayDate else "",
                onValueChange = { },
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDateClick),
                enabled = false,
                placeholder = {
                    Text(
                        text = "날짜 선택",
                        style = AdegoTheme.typography.bodyLarge,
                        color = AdegoTheme.colors.line500
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = AdegoTheme.colors.main500,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = AdegoTheme.colors.main500,
                    disabledContainerColor = Color.Transparent,
                    disabledTextColor = AdegoTheme.colors.onBackground,
                    disabledPlaceholderColor = AdegoTheme.colors.line500,
                    disabledLeadingIconColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // 시간 선택
            OutlinedTextField(
                value = if (selectedHour != null && selectedMinute != null) displayTime else "",
                onValueChange = { },
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTimeClick),
                enabled = false,
                placeholder = {
                    Text(
                        text = "시간 선택",
                        style = AdegoTheme.typography.bodyLarge,
                        color = AdegoTheme.colors.line500
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = AdegoTheme.colors.main500,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = AdegoTheme.colors.main500,
                    disabledContainerColor = Color.Transparent,
                    disabledTextColor = AdegoTheme.colors.onBackground,
                    disabledPlaceholderColor = AdegoTheme.colors.line500,
                    disabledLeadingIconColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// 방 이름 입력 섹션
@Composable
private fun RoomNameInputSection(
    roomName: String,
    onRoomNameChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "방 이름",
            style = AdegoTheme.typography.titleLarge,
            color = AdegoTheme.colors.main500
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = roomName,
            onValueChange = {
                if (it.length <= 20) {
                    onRoomNameChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "모임 이름을 입력해 주세요",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.line500
                )
            },
            singleLine = true,
            supportingText = {
                Text(
                    text = "${roomName.length}/20",
                    style = AdegoTheme.typography.bodySmall,
                    color = AdegoTheme.colors.line500,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AdegoTheme.colors.main500,
                unfocusedBorderColor = AdegoTheme.colors.main500,
                focusedTextColor = AdegoTheme.colors.onBackground,
                unfocusedTextColor = AdegoTheme.colors.onBackground,
                cursorColor = AdegoTheme.colors.main500
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateScreenPreview() {
    AdegoTheme {
        CreateScreen(
            uiState = CreateUiState(),
            onIntent = {}
        )
    }
}
