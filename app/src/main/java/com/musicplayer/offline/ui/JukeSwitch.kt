package com.musicplayer.offline.ui

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable

@Composable
fun JukeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = JukeTextPrimary,
            checkedTrackColor = JukePrimary,
            checkedBorderColor = JukePrimary,
            uncheckedThumbColor = JukeInactiveThumb,
            uncheckedTrackColor = JukeInactiveTrack,
            uncheckedBorderColor = JukeInactiveBorder,
            disabledCheckedThumbColor = JukeTextPrimary.copy(alpha = 0.72f),
            disabledCheckedTrackColor = JukePrimary.copy(alpha = 0.46f),
            disabledCheckedBorderColor = JukePrimary.copy(alpha = 0.46f),
            disabledUncheckedThumbColor = JukeInactiveThumb.copy(alpha = 0.52f),
            disabledUncheckedTrackColor = JukeInactiveTrack.copy(alpha = 0.52f),
            disabledUncheckedBorderColor = JukeInactiveBorder.copy(alpha = 0.52f)
        )
    )
}
