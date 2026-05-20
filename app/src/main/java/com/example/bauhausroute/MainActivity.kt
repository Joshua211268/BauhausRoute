package com.example.bauhausroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bauhausroute.ui.theme.BauhausBlue
import com.example.bauhausroute.ui.theme.BauhausCarbonBlack
import com.example.bauhausroute.ui.theme.BauhausGeometryBlue
import com.example.bauhausroute.ui.theme.BauhausGeometryRed
import com.example.bauhausroute.ui.theme.BauhausGeometryYellow
import com.example.bauhausroute.ui.theme.BauhausRed
import com.example.bauhausroute.ui.theme.BauhausTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BauhausTheme {
                BauhausBackground {
                    BauhausStageOnePreview(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BauhausBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 右上角：以淡色圓形與線框方塊建立包浩斯海報感。
            drawCircle(
                color = BauhausGeometryRed,
                radius = size.minDimension * 0.24f,
                center = Offset(size.width * 0.92f, size.height * 0.08f)
            )
            drawRect(
                color = BauhausGeometryBlue,
                topLeft = Offset(size.width * 0.68f, size.height * 0.08f),
                size = Size(size.width * 0.25f, size.width * 0.25f),
                style = Stroke(width = 5.dp.toPx())
            )

            // 左下角：錯位矩形與圓形呼應抽象幾何背景。
            drawRect(
                color = BauhausGeometryYellow,
                topLeft = Offset(-size.width * 0.08f, size.height * 0.72f),
                size = Size(size.width * 0.34f, size.width * 0.24f),
                style = Stroke(width = 6.dp.toPx())
            )
            drawCircle(
                color = BauhausGeometryBlue,
                radius = size.minDimension * 0.18f,
                center = Offset(size.width * 0.12f, size.height * 0.88f),
                style = Stroke(width = 7.dp.toPx())
            )
        }

        content()
    }
}

@Composable
fun BauhausStageOnePreview(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Project\nBauhaus Route",
            color = BauhausCarbonBlack,
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Theme and geometric background ready.",
            color = BauhausBlue,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "RED / BLUE / YELLOW",
            color = BauhausRed,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BauhausPreview() {
    BauhausTheme {
        BauhausBackground {
            BauhausStageOnePreview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            )
        }
    }
}
