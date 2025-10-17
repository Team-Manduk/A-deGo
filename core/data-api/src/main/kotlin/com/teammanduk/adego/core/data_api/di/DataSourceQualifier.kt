package com.teammanduk.adego.core.data_api.di

import javax.inject.Qualifier

/**
 * TMAP 역지오코딩 DataSource를 구분하기 위한 Qualifier
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmapReverseGeocoding

/**
 * Android Geocoder DataSource를 구분하기 위한 Qualifier
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AndroidGeocoder
