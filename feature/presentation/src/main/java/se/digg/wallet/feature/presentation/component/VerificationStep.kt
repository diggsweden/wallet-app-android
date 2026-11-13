// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R

private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

@Composable
internal fun VerificationStep(
    overline: String,
    headline: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    showConnector: Boolean = true,
    revealDelayMillis: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val check = remember { Animatable(if (revealDelayMillis == null) 1f else 0f) }
    val connector = remember { Animatable(if (revealDelayMillis == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (revealDelayMillis != null) {
            launch {
                check.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = revealDelayMillis,
                        easing = PopEasing,
                    ),
                )
            }
            connector.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, delayMillis = revealDelayMillis + 200),
            )
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind {
                        if (check.value < 1f) {
                            val ring = 2.dp.toPx()
                            drawCircle(
                                color = colors.outline,
                                radius = (size.minDimension - ring) / 2,
                                style = Stroke(width = ring),
                            )
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = check.value
                            scaleY = check.value
                        }
                        .background(color = colors.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .heightIn(min = 24.dp)
                        .weight(1f)
                        .graphicsLayer {
                            scaleY = connector.value
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                        .background(
                            color = colors.outlineVariant,
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = if (showConnector) 24.dp else 0.dp),
        ) {
            Text(
                text = overline,
                style = MaterialTheme.typography.labelMedium,
                color = colors.tertiary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = headline, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@PreviewsWallet
@Composable
private fun VerificationStepPreview() {
    ApprovalPreview {
        Column(modifier = Modifier.padding(24.dp)) {
            VerificationStep(
                overline = "Tillitsregister",
                headline = "Tillitsregistret för plånböcker",
                supportingText = "Den officiella listan som alla parter nedan kontrolleras mot.",
            )
            VerificationStep(
                overline = "Mottagare",
                headline = "Nordbil Hyrbilar AB",
                supportingText = "Registrerad och får be om de här uppgifterna.",
            )
            VerificationStep(
                overline = "Den här telefonen",
                headline = "Din plånbok",
                supportingText = "Appen och dina nycklar är äkta och oförändrade.",
                showConnector = false,
            )
        }
    }
}
