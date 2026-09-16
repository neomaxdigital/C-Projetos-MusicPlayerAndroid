package com.musicplayer.offline.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.musicplayer.offline.R

@Composable
fun JukeCircularLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.juke_splash_logo),
        contentDescription = "Logo circular JUKE",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun JukeHorizontalLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.juke_logo_horizontal),
        contentDescription = "JUKE",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
