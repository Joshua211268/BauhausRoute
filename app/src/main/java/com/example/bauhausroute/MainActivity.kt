package com.example.bauhausroute

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.example.bauhausroute.ui.theme.BauhausBlue
import com.example.bauhausroute.ui.theme.BauhausCarbonBlack
import com.example.bauhausroute.ui.theme.BauhausGeometryBlue
import com.example.bauhausroute.ui.theme.BauhausGeometryRed
import com.example.bauhausroute.ui.theme.BauhausGeometryYellow
import com.example.bauhausroute.ui.theme.BauhausRed
import com.example.bauhausroute.ui.theme.BauhausTheme
import com.example.bauhausroute.ui.theme.BauhausWarmWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GeoPointData(
    val latitude: Double,
    val longitude: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BauhausTheme {
                BauhausBackground {
                    RouteDiscoveryScreen(
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
fun RouteDiscoveryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf<List<GeoPointData>>(emptyList()) }
    var hasMissingGps by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        scope.launch {
            isParsing = true
            val result = parseGeoPoints(context.contentResolver, uris)
            points = result.points
            hasMissingGps = result.hasMissingGps
            isParsing = false
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "ROUTE\nDISCOVERY",
            color = BauhausCarbonBlack,
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(96.dp))

        BauhausPhotoButton(
            text = if (isParsing) "PARSING..." else "SELECT PHOTOS",
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = !isParsing
        )

        if (hasMissingGps) {
            Text(
                text = "Some photos lack GPS data",
                color = BauhausRed,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            itemsIndexed(points) { index, point ->
                Text(
                    text = "${index + 1}. ${point.latitude}, ${point.longitude}",
                    color = BauhausBlue,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun BauhausPhotoButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(BorderStroke(borderWidth, BauhausCarbonBlack))
            .background(if (enabled) BauhausYellowButtonColor else BauhausWarmWhite)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = text,
            color = BauhausCarbonBlack,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

private val BauhausYellowButtonColor = Color(0xFFF2B705)

private data class GeoParseResult(
    val points: List<GeoPointData>,
    val hasMissingGps: Boolean
)

private suspend fun parseGeoPoints(
    contentResolver: ContentResolver,
    uris: List<Uri>
): GeoParseResult = withContext(Dispatchers.IO) {
    val points = mutableListOf<GeoPointData>()
    var hasMissingGps = false

    uris.forEach { uri ->
        val point = readGeoPoint(contentResolver, uri)
        if (point == null) {
            hasMissingGps = true
        } else {
            points += point
        }
    }

    GeoParseResult(points = points, hasMissingGps = hasMissingGps)
}

private fun readGeoPoint(
    contentResolver: ContentResolver,
    uri: Uri
): GeoPointData? {
    return contentResolver.openInputStream(uri)?.use { inputStream ->
        val coordinates = ExifInterface(inputStream).latLong
        if (coordinates != null) {
            GeoPointData(
                latitude = coordinates[0].toDouble(),
                longitude = coordinates[1].toDouble()
            )
        } else {
            null
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BauhausPreview() {
    BauhausTheme {
        BauhausBackground {
            RouteDiscoveryScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            )
        }
    }
}
