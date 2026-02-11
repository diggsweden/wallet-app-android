package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.data.DisclosureLocal

@Composable
fun DisclosureList(disclosures: List<DisclosureLocal>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFC3C3C2),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            disclosures.forEach { item ->
                DisclosureItem(
                    title = item.claim.display.first().name ?: "-",
                    content = item.value,
                )
            }
        }
    }
}

@Composable
fun DisclosureItem(title: String, content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 20.dp),
    ) {
        Text(text = title, style = DiggTextStyle.H5)
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = content, style = DiggTextStyle.BodyMD)
    }
}
