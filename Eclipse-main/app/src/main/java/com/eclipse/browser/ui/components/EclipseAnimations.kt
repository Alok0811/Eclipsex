package com.eclipse.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EclipseEnterAnimation(
    index: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
            initialOffsetY = { (it * 0.28f).toInt() }
        )
    ) {
        content()
    }
}

@Composable
fun Modifier.eclipsePress(onClick: () -> Unit): Modifier {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scale.animateTo(0.88f, spring(dampingRatio = 1.0f, stiffness = 1000f))
                    }
                    tryAwaitRelease()
                    scope.launch {
                        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
                    }
                    onClick()
                }
            )
        }
}
