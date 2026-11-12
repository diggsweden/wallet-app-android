// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.digg.wallet.feature.presentation.R

@Composable
internal fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    trailingText: String? = null,
    collapsedSummary: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val borderModifier = if (dashed) {
        Modifier.dashedBorder(width = 1.5.dp, color = colors.outline, cornerRadius = 16.dp)
    } else {
        Modifier.border(width = 1.dp, color = colors.outlineVariant, shape = shape)
    }
    val expandedState = stringResource(
        if (expanded) R.string.presentation_a11y_expanded else R.string.presentation_a11y_collapsed,
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(borderModifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                .semantics { stateDescription = expandedState }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                leading()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (collapsedSummary != null) {
                    AnimatedVisibility(visible = !expanded) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            CompositionLocalProvider(
                                LocalContentColor provides colors.onSurfaceVariant,
                            ) {
                                ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                                    collapsedSummary()
                                }
                            }
                        }
                    }
                }
            }
            trailingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.chevron_down),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                content()
            }
        }
    }
}

private fun Modifier.dashedBorder(width: Dp, color: Color, cornerRadius: Dp) = drawWithCache {
    val strokeWidth = width.toPx()
    val stroke = Stroke(
        width = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
    )
    onDrawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(cornerRadius.toPx() - strokeWidth / 2),
            style = stroke,
        )
    }
}
