package com.teammanduk.adego.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class TimePickerType {
    HOUR,
    MINUTE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdegoTimePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 0,
    initialMinute: Int = 0
) {
    if (!show) return

    // 5분 단위로 시간을 관리하기 때문에 시간에는 12를 곱하고 분은 5로 나눔
    // 하루를 228 단위로 관리하고 1은 5분으로 함
    var timeValue by remember { mutableIntStateOf((initialHour * 12) + (initialMinute / 5)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            // Time Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 오전/오후 Picker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableButton(
                        text = "오전",
                        isSelected = !timeValue.isAfternoon,
                        onClick = {
                            if (timeValue.isAfternoon) {
                                timeValue -= 144  // 오후 -> 오전
                            }
                        }
                    )

                    SelectableButton(
                        text = "오후",
                        isSelected = timeValue.isAfternoon,
                        onClick = {
                            if (!timeValue.isAfternoon) {
                                timeValue += 144  // 오전 -> 오후
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Hour Picker (0-11, 12시간 형식)
                ScrollableTimePickerColumn(
                    timeValue = timeValue,
                    type = TimePickerType.HOUR,
                    onValueChange = { diff ->
                        timeValue += diff * 12  // 1시간 = 12칸(60분)
                    }
                )

                Text(
                    text = ":",
                    style = AdegoTheme.typography.headlineLarge,
                    color = AdegoTheme.colors.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Minute Picker (0-11, 5분 단위)
                ScrollableTimePickerColumn(
                    timeValue = timeValue,
                    type = TimePickerType.MINUTE,
                    onValueChange = { diff ->
                        timeValue += diff  // 1칸 = 5분
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm Button
            Button(
                onClick = {
                    onConfirm(timeValue.hour24, timeValue.minute)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "시간 선택",
                    style = AdegoTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollableTimePickerColumn(
    timeValue: Int,
    type: TimePickerType,
    onValueChange: (diff: Int) -> Unit
) {
    // type에 따라 현재 인덱스, 범위, 표시 정보 결정
    val currentIndex = when (type) {
        TimePickerType.HOUR -> timeValue.hour12
        TimePickerType.MINUTE -> timeValue.minuteIndex
    }

    val displayRange = when (type) {
        TimePickerType.HOUR -> 1..12
        TimePickerType.MINUTE -> 0..11
    }

    val displayMultiplier = when (type) {
        TimePickerType.HOUR -> 1
        TimePickerType.MINUTE -> 5
    }

    val itemCount = 12
    val repeatCount = 1000
    val totalItems = itemCount * repeatCount
    val middleStart = totalItems / 2 - (totalItems / 2) % itemCount

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleStart + currentIndex
    )
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // 프로그래밍 방식 업데이트 플래그
    var isUpdatingProgrammatically by remember { mutableStateOf(false) }

    // 이전 값 추적
    var previousValue by remember { mutableIntStateOf(currentIndex) }

    // 애니메이션 Job 추적
    var animationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // currentIndex가 외부에서 변경되면 스크롤 위치 자동 업데이트
    // 단, 사용자가 스크롤 중일 때는 업데이트하지 않음
    LaunchedEffect(currentIndex, listState.isScrollInProgress) {
        // 스크롤 중이면 업데이트 안 함
        if (listState.isScrollInProgress) return@LaunchedEffect

        val currentScrollIndex = ((listState.firstVisibleItemIndex % itemCount) + itemCount) % itemCount
        if (currentScrollIndex != currentIndex) {
            // 이전 애니메이션 취소
            animationJob?.cancel()

            // 새 애니메이션 시작
            animationJob = coroutineScope.launch {
                try {
                    isUpdatingProgrammatically = true
                    listState.animateScrollToItem(middleStart + currentIndex)  // 애니메이션으로 이동
                    previousValue = currentIndex
                } finally {
                    // 취소되어도 플래그는 반드시 리셋
                    isUpdatingProgrammatically = false
                }
            }
        }
    }

    // 사용자 스크롤 감지
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> ((index % itemCount) + itemCount) % itemCount }
            .distinctUntilChanged()
            .collect { newValue ->
                if (!isUpdatingProgrammatically) {  // 사용자 스크롤만 처리
                    // 차이 계산
                    var diff = newValue - previousValue
                    // 순환 처리: 최단 거리
                    if (diff > itemCount / 2) diff -= itemCount
                    if (diff < -itemCount / 2) diff += itemCount

                    previousValue = newValue

                    // 햅틱 피드백
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    onValueChange(diff)
                } else {
                    // 프로그래밍 업데이트 중에도 previousValue는 동기화
                    previousValue = newValue
                }
            }
    }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(180.dp)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 60.dp)
        ) {
            items(totalItems) { index ->
                val itemValue = index % itemCount
                // displayRange가 1..12면 itemValue 0 -> 12, 1 -> 1, ..., 11 -> 11
                // displayMultiplier로 실제 표시값 계산 (예: 5분 단위)
                val baseValue = if (displayRange.first == 1) {
                    if (itemValue == 0) 12 else itemValue
                } else {
                    itemValue
                }
                val displayValue = baseValue * displayMultiplier
                val isSelected = itemValue == currentIndex

                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", displayValue),
                        style = AdegoTheme.typography.headlineLarge,
                        color = if (isSelected) AdegoTheme.colors.main500 else AdegoTheme.colors.onBackground.copy(
                            alpha = 0.3f
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Highlight box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp, 60.dp)
                .background(
                    color = AdegoTheme.colors.main500.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}
