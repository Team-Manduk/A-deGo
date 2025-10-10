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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchPlaceRoute(
    onBackClick: () -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    viewModel: SelectPlaceViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "장소 검색",
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
        SearchPlaceScreen(
            onPlaceClick = onPlaceClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SearchPlaceScreen(
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // 임시 검색 결과 데이터 (실제로는 ViewModel에서 가져와야 함)
    val searchResults = listOf(
        "스타벅스 강남역점",
        "카페 베네 서울역점",
        "투썸플레이스 홍대입구점",
        "이디야커피 신촌점"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(AdegoTheme.colors.background)
            .padding(16.dp)
    ) {
        // 검색 입력창
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
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
                focusedBorderColor = AdegoTheme.colors.main500,
                unfocusedBorderColor = AdegoTheme.colors.line500,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 결과 리스트
        if (searchQuery.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { place ->
                    SearchResultItem(
                        placeName = place,
                        onClick = { onPlaceClick(place) }
                    )
                }
            }
        } else {
            // 검색 전 안내 메시지
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "검색어를 입력해주세요",
                    style = AdegoTheme.typography.bodyLarge,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    placeName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = placeName,
                style = AdegoTheme.typography.titleLarge,
                color = AdegoTheme.colors.onBackground
            )
        }
    }
}
