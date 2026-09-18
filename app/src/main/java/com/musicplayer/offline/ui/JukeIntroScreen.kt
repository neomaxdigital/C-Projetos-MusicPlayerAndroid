package com.musicplayer.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * JUKE brand intro shown once per app process.
 *
 * Android's native splash is intentionally visually neutral; this is the only
 * branded intro the user should perceive.
 */
@Composable
fun JukeIntroScreen(onFinished: () -> Unit) {
    var activeDot by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(5_000)
        onFinished()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            activeDot = (activeDot + 1) % 3
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        val logoSize = minOf(maxWidth * 0.72f, 360.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            JukeCircularLogo(
                Modifier.size(logoSize)
            )

            Spacer(Modifier.height(34.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(if (index == activeDot) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == activeDot) PrimaryBlue
                                else PrimaryBlue.copy(alpha = 0.28f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Carregando...",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
