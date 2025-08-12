package se.digg.wallet.feature.dashboard

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import se.digg.wallet.R
import se.digg.wallet.core.ui.theme.WalletTheme

@Composable
fun DashboardScreen(navController: NavController) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        DashboardContent(
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun DashboardContent(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_logo_full),
                contentDescription = "App icon",
                colorFilter = null
            )
            Text(
                fontSize = 24.sp,
                text = stringResource(R.string.homescreen_welcome),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                text = stringResource(R.string.homescreen_content)
                        + stringResource(R.string.homescreen_content2),
            )
            TextWithClickableLink()
            Spacer(modifier = Modifier.height(16.dp))
            TextWithClickableLinkTwo()
        }
    }
}

@Composable
fun TextWithClickableLink() {
    val context = LocalContext.current

    val annotatedText = buildAnnotatedString {
        append(stringResource(R.string.homescreen_more_info))
        pushStringAnnotation(
            tag = "URL",
            annotation = stringResource(R.string.homescreen_more_info_url)
        )

        withStyle(
            style = SpanStyle(
                color = Color.Blue,
                fontSize = 16.sp,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(stringResource(R.string.homescreen_more_info_here))
        }
        pop()
    }

    Text(
        text = annotatedText,
        modifier = Modifier.clickable {
            val url = annotatedText.getStringAnnotations("URL", 0, annotatedText.length)
                .firstOrNull()?.item
            url?.let {
                val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                context.startActivity(intent)
            }
        }
    )
}

@Composable
fun TextWithClickableLinkTwo() {
    val context = LocalContext.current

    val annotatedText = buildAnnotatedString {
        append(stringResource(R.string.homescreen_pid_info))
        pushStringAnnotation(
            tag = "URL",
            annotation = stringResource(R.string.homescreen_pid_info_url)
        )

        withStyle(
            style = SpanStyle(
                color = Color.Blue,
                fontSize = 16.sp,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(stringResource(R.string.homescreen_more_info_here))
        }
        pop()
    }

    Text(
        text = annotatedText,
        modifier = Modifier.clickable {
            val url = annotatedText.getStringAnnotations("URL", 0, annotatedText.length)
                .firstOrNull()?.item
            url?.let {
                val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                context.startActivity(intent)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WalletTheme {
        DashboardContent()
    }
}