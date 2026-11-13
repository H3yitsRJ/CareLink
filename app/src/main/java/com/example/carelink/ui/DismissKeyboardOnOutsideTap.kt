package com.example.carelink.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

private class TapTarget {
    var isTextField = false
    var focusedField: Any? = null
}

private val LocalTapTarget = staticCompositionLocalOf<TapTarget?> { null }

@Composable
internal fun DismissKeyboardOnOutsideTapColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tapTarget = remember { TapTarget() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    CompositionLocalProvider(LocalTapTarget provides tapTarget) {
        Column(
            modifier.pointerInput(tapTarget, focusManager, keyboard) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    tapTarget.isTextField = false
                    do {
                        // Read after children handle the event and leave their clicks intact.
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.singleOrNull() ?: break
                        if (tapTarget.isTextField || change.id != down.id ||
                            (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                        ) break

                        if (change.changedToUpIgnoreConsumed()) {
                            if (tapTarget.focusedField != null &&
                                change.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis
                            ) {
                                focusManager.clearFocus()
                                keyboard?.hide()
                            }
                            break
                        }
                    } while (change.pressed)
                }
            },
            content = content
        )
    }
}

@Composable
internal fun Modifier.keepKeyboardOnTap(enabled: Boolean = true): Modifier {
    val tapTarget = LocalTapTarget.current
    if (!enabled || tapTarget == null) return this

    val field = remember { Any() }
    DisposableEffect(tapTarget, field) {
        onDispose {
            if (tapTarget.focusedField === field) tapTarget.focusedField = null
        }
    }
    return onFocusChanged {
        if (it.hasFocus) {
            tapTarget.focusedField = field
        } else if (tapTarget.focusedField === field) {
            tapTarget.focusedField = null
        }
    }.pointerInput(tapTarget) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            tapTarget.isTextField = true
        }
    }
}
