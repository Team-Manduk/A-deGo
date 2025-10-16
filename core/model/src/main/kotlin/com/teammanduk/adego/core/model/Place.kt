package com.teammanduk.adego.core.model

data class Place(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isPOI: Boolean = true  // POI 검색 결과인지 여부 (기본값: true, 역지오코딩은 false)
)
