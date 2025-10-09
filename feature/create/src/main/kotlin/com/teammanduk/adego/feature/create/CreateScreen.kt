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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CreateRoute(
    onNavigateBack: () -> Unit = {},
    onNavigateToSelectPlace: () -> Unit = {},
    onCreateMeeting: (String, Int, Int) -> Unit = { _, _, _ -> },
    selectedPlaceFromNav: String? = null,
    viewModel: CreateViewModel = hiltViewModel()
) {
    val selectedPlace by viewModel.selectedPlace.collectAsStateWithLifecycle()

    CreateScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToSelectPlace = onNavigateToSelectPlace,
        onCreateMeeting = onCreateMeeting,
        selectedPlaceFromNav = selectedPlaceFromNav,
        selectedPlaceFromViewModel = selectedPlace
    )
}

@Composable
private fun CreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelectPlace: () -> Unit,
    onCreateMeeting: (String, Int, Int) -> Unit,
    selectedPlaceFromNav: String? = null,
    selectedPlaceFromViewModel: com.teammanduk.adego.core.model.Place? = null
) {
    // ViewModel에서 전달받은 장소 정보 사용
    val selectedPlace = selectedPlaceFromViewModel?.name ?: ""
    var selectedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedMinute by rememberSaveable { mutableStateOf<Int?>(null) }
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
            IconButton(onClick = onNavigateBack) {
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
            val displayDate = selectedDate?.let { dateFormat.format(Date(it)) } ?: "날짜 선택"
            val displayTime = if (selectedHour != null && selectedMinute != null) {
                String.format("%02d시 %02d분", selectedHour, selectedMinute)
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

            // 장소 선택
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "장소 선택",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.main500
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = selectedPlace,
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigateToSelectPlace()
                        },
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 시간 선택
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                            .clickable { showDatePicker = true },
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
                            .clickable { showTimePicker = true },
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

            Spacer(modifier = Modifier.weight(1f))

            // 모임 생성하기 버튼
            Button(
                onClick = {
                    val hour = selectedHour ?: 0
                    val minute = selectedMinute ?: 0
                    onCreateMeeting(selectedPlace, hour, minute)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedPlace.isNotEmpty() && selectedDate != null && selectedHour != null && selectedMinute != null
            ) {
                Text(
                    text = "모임 생성하기",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onMain500
                )
            }
        }
    }

    // DatePicker
    AdegoDatePicker(
        show = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { selectedDate = it },
        initialDate = selectedDate
    )

    // TimePicker
    AdegoTimePicker(
        show = showTimePicker,
        onDismiss = { showTimePicker = false },
        onConfirm = { hour, minute ->
            selectedHour = hour
            selectedMinute = minute
        },
        initialHour = selectedHour ?: 0,
        initialMinute = selectedMinute ?: 0
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateScreenPreview() {
    AdegoTheme {
        CreateScreen(
            onNavigateBack = {},
            onNavigateToSelectPlace = {},
            onCreateMeeting = { _, _, _ -> },
        )
    }
}
