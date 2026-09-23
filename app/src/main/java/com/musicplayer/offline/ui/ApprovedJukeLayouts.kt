package com.musicplayer.offline.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

/**
 * Visual foundation for the approved JUKE layouts.
 *
 * The approved PNGs are the visual source of truth and must not be
 * reinterpreted as a loose reference. Screens may place live/dynamic controls
 * over the PNG where required, while preserving the approved composition.
 */
object ApprovedJukeLayoutAssets {
    const val LIBRARY = "juke_layout_library"
    const val NOW_PLAYING = "juke_layout_now_playing"
    const val PLAYLISTS = "juke_layout_playlists"
    const val EQUALIZER = "juke_layout_equalizer"
    const val QUEUE = "juke_layout_queue"
    const val SETTINGS = "juke_layout_settings"
}

@Composable
fun ApprovedLayoutBackdrop(
    assetName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
): Boolean {
    val context = LocalContext.current
    @DrawableRes val id = context.resources.getIdentifier(assetName, "drawable", context.packageName)
    if (id == 0) return false

    Image(
        painter = painterResource(id),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = contentScale
    )
    return true
}
