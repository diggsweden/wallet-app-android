package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun BaseScreen(
    floatingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var floatingContentHeight by remember { mutableStateOf(value = 0.dp) }
    val fixedContentHeight = 4.dp

    Box(modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            content()
            Spacer(Modifier.height(floatingContentHeight + fixedContentHeight))
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(bottom = 24.dp, top = 12.dp)
                .onSizeChanged { floatingContentHeight = with(density) { it.height.toDp() } },
        ) {
            floatingContent()
        }
    }
}
