package com.safeshield.app.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.security.PinManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the screen should show right now, distinct from the pure [PinManager.VerifyResult] so the UI doesn't have to interpret raw enum states itself. */
sealed class PinScreenMessage {
    object None : PinScreenMessage()
    object PinSaved : PinScreenMessage()
    object PinsDoNotMatch : PinScreenMessage()
    object InvalidPinFormat : PinScreenMessage()
    object IncorrectCurrentPin : PinScreenMessage()
    data class LockedOut(val retryAfterSeconds: Long) : PinScreenMessage()
}

class AdminSecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val pinManager = PinManager(application)

    private val _isPinSet = MutableStateFlow(pinManager.isPinSet())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _message = MutableStateFlow<PinScreenMessage>(PinScreenMessage.None)
    val message: StateFlow<PinScreenMessage> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = PinScreenMessage.None
    }

    /** Used the first time a PIN is set — there is nothing to verify against yet. */
    fun setInitialPin(newPin: String, confirmPin: String) {
        if (newPin != confirmPin) {
            _message.value = PinScreenMessage.PinsDoNotMatch
            return
        }
        if (!isValidFormat(newPin)) {
            _message.value = PinScreenMessage.InvalidPinFormat
            return
        }
        viewModelScope.launch {
            pinManager.setPin(newPin)
            _isPinSet.value = true
            _message.value = PinScreenMessage.PinSaved
        }
    }

    fun changePin(currentPin: String, newPin: String, confirmPin: String) {
        if (newPin != confirmPin) {
            _message.value = PinScreenMessage.PinsDoNotMatch
            return
        }
        if (!isValidFormat(newPin)) {
            _message.value = PinScreenMessage.InvalidPinFormat
            return
        }
        viewModelScope.launch {
            when (val result = pinManager.changePin(currentPin, newPin)) {
                is PinManager.VerifyResult.Success -> _message.value = PinScreenMessage.PinSaved
                is PinManager.VerifyResult.IncorrectPin -> _message.value = PinScreenMessage.IncorrectCurrentPin
                is PinManager.VerifyResult.LockedOut ->
                    _message.value = PinScreenMessage.LockedOut(result.retryAfterMillis / 1000)
                is PinManager.VerifyResult.NotConfigured -> setInitialPin(newPin, confirmPin)
            }
        }
    }

    private fun isValidFormat(pin: String): Boolean =
        pin.length in PinManager.MIN_PIN_LENGTH..PinManager.MAX_PIN_LENGTH && pin.all { it.isDigit() }
}
