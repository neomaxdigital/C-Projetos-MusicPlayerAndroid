package com.musicplayer.offline.ui

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun JukeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accent = PrimaryBlue
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = JukeTextPrimary,
            checkedTrackColor = accent,
            checkedBorderColor = accent,
            uncheckedThumbColor = JukeInactiveThumb,
            uncheckedTrackColor = JukeInactiveTrack,
            uncheckedBorderColor = JukeInactiveBorder,
            disabledCheckedThumbColor = JukeTextPrimary.copy(alpha = 0.72f),
            disabledCheckedTrackColor = accent.copy(alpha = 0.46f),
            disabledCheckedBorderColor = accent.copy(alpha = 0.46f),
            disabledUncheckedThumbColor = JukeInactiveThumb.copy(alpha = 0.52f),
            disabledUncheckedTrackColor = JukeInactiveTrack.copy(alpha = 0.52f),
            disabledUncheckedBorderColor = JukeInactiveBorder.copy(alpha = 0.52f)
        )
    )
}
