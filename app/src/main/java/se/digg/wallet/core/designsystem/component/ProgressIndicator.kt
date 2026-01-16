package se.digg.wallet.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.Progress
import se.digg.wallet.core.designsystem.theme.ProgressDarkMode
import se.digg.wallet.core.designsystem.theme.Track
import se.digg.wallet.core.designsystem.theme.TrackDarkMode

@Composable
fun ProgressIndicator(modifier: Modifier = Modifier, progress: Float) {

    val isDarkMode = isSystemInDarkTheme()
    val color = if (isDarkMode) ProgressDarkMode else Progress
    val trackColor = if (isDarkMode) TrackDarkMode else Track

    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(11.dp)
            .clip(RoundedCornerShape(8.dp)),
        progress = { progress },
        color = color,
        trackColor = trackColor
    )
}

@Composable
fun AnimatedLinearProgress(modifier: Modifier, targetProgress: Float) {
    val isDarkMode = isSystemInDarkTheme()
    val color = if (isDarkMode) ProgressDarkMode else Progress
    val trackColor = if (isDarkMode) TrackDarkMode else Track

    val safeTarget = targetProgress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = safeTarget,
        animationSpec = tween(durationMillis = 400),
        label = "linearProgress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(11.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = color,
        trackColor = trackColor
    )
}