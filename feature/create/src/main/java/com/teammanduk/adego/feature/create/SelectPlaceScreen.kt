package com.teammanduk.adego.feature.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun SelectPlaceRoute() {
    SelectPlaceScreen()
}

@Composable
private fun SelectPlaceScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "모임 장소 선택"
        )
    }
}
