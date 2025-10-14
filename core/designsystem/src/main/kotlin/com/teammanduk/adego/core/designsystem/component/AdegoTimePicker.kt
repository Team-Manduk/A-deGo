package com.teammanduk.adego.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

    // initialHour (0-23) -> selectedHour (0-11, 0=12시)
    val initialHour12 = initialHour % 12
    var selectedHour by remember { mutableIntStateOf(initialHour12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var isPM by remember { mutableStateOf(initialHour >= 12) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "시간 선택",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onBackground,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = AdegoTheme.colors.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Time Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AM/PM Picker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp, 40.dp)
                            .background(
                                color = if (!isPM) AdegoTheme.colors.main500 else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { isPM = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM",
                            style = AdegoTheme.typography.bodyLarge,
                            color = if (!isPM) Color.White else AdegoTheme.colors.onBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp, 40.dp)
                            .background(
                                color = if (isPM) AdegoTheme.colors.main500 else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { isPM = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM",
                            style = AdegoTheme.typography.bodyLarge,
                            color = if (isPM) Color.White else AdegoTheme.colors.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Hour Picker
                ScrollableTimePickerColumn(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    range = 0..11,
                    displayRange = 1..12
                )

                Text(
                    text = ":",
                    style = AdegoTheme.typography.headlineLarge,
                    color = AdegoTheme.colors.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Minute Picker
                ScrollableTimePickerColumn(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    range = 0..59,
                    displayRange = 0..59
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm Button
            Button(
                onClick = {
                    // selectedHour: 0=12시, 1=1시, 2=2시, ..., 11=11시
                    val hour12 = if (selectedHour == 0) 12 else selectedHour
                    val hour24 = if (isPM) {
                        if (hour12 == 12) 12 else hour12 + 12
                    } else {
                        if (hour12 == 12) 0 else hour12
                    }
                    onConfirm(hour24, selectedMinute)
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
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    displayRange: IntRange
) {
    val itemCount = range.count()
    val repeatCount = 1000
    val totalItems = itemCount * repeatCount
    val middleStart = totalItems / 2 - (totalItems / 2) % itemCount

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleStart + value
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index % itemCount }
            .distinctUntilChanged()
            .collect { normalizedIndex ->
                onValueChange(normalizedIndex)
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
                val displayValue = if (displayRange.first == 1) {
                    if (itemValue == 0) 12 else itemValue
                } else {
                    itemValue
                }
                val isSelected = itemValue == value

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
                        color = if (isSelected) AdegoTheme.colors.main500 else AdegoTheme.colors.onBackground.copy(alpha = 0.3f),
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
