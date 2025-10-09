package com.teammanduk.adego.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.GetSelectedPlaceUseCase
import com.teammanduk.adego.core.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val getSelectedPlaceUseCase: GetSelectedPlaceUseCase
) : ViewModel() {

    val selectedPlace: StateFlow<Place?> = getSelectedPlaceUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
