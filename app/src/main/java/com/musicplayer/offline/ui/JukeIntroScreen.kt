package com.musicplayer.offline.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A short brand moment after Android's native splash, once per app process. */
@Composable
fun JukeIntroScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.96f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { alpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
            launch { scale.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
        }
        delay(620)
        alpha.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        JukeHorizontalLogo(
            Modifier
                .width(260.dp)
                .height(96.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}
