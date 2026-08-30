package com.sezeros.speedboost

import com.sezeros.speedboost.model.RuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppStateStore {
    private val mutableState = MutableStateFlow(RuntimeState())
    val state = mutableState.asStateFlow()

    fun update(block: (RuntimeState) -> RuntimeState) {
        mutableState.value = block(mutableState.value)
    }

    fun reset() {
        mutableState.value = RuntimeState()
    }
}
