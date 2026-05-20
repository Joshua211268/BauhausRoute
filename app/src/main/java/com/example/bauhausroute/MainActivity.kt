package com.example.bauhausroute

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
    var selectedCount by remember { mutableStateOf(0) }
    var missingGpsCount by remember { mutableStateOf(0) }
    var readErrorCount by remember { mutableStateOf(0) }
    var diagnostics by remember { mutableStateOf<List<String>>(emptyList()) }
    var isParsing by remember { mutableStateOf(false) }

    fun handlePickedUris(uris: List<Uri>) {
        scope.launch {
            isParsing = true
            val result = parseGeoPoints(context.contentResolver, uris)
            points = result.points
            selectedCount = result.selectedCount
            missingGpsCount = result.missingGpsCount
            readErrorCount = result.readErrorCount
            diagnostics = result.diagnostics
            isParsing = false
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        handlePickedUris(uris)
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        handlePickedUris(uris)
    }

    Column(modifier = modifier) {
        Text(
            text = "ROUTE\nDISCOVERY",
            color = BauhausCarbonBlack,
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(56.dp))

        BauhausPhotoButton(
            text = if (isParsing) "PARSING..." else "SELECT PHOTOS",
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = !isParsing
        )

        Spacer(modifier = Modifier.height(12.dp))

        BauhausPhotoButton(
            text = "SELECT HEIF FILES",
            onClick = {
                filePicker.launch(arrayOf("image/*", "image/heic", "image/heif"))
            },
            enabled = !isParsing
        )

        if (selectedCount > 0) {
            Text(
                text = "Selected $selectedCount files / Found ${points.size} GPS points",
                color = BauhausCarbonBlack,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (missingGpsCount > 0) {
            Text(
                text = "Some photos lack GPS data",
                color = BauhausRed,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (readErrorCount > 0) {
            Text(
                text = "$readErrorCount files could not be read as EXIF images",
                color = BauhausRed,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (points.isNotEmpty()) {
            BauhausRouteMap(
                points = points.map { it.toGeoPoint() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(top = 24.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(top = 24.dp)
        ) {
            itemsIndexed(diagnostics) { _, item ->
                Text(
                    text = item,
                    color = BauhausCarbonBlack,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

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
fun BauhausRouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orderedPoints = remember(points) { sortByNearestNeighbor(points) }
    val markerIcon = remember { createBauhausMarkerIcon(context) }
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier.border(BorderStroke(3.dp, BauhausCarbonBlack)),
        factory = { mapView },
        update = { view ->
            view.overlays.clear()

            if (orderedPoints.isNotEmpty()) {
                view.controller.setCenter(orderedPoints.first())
                view.controller.setZoom(15.0)

                val routeLine = Polyline().apply {
                    setPoints(orderedPoints)
                    outlinePaint.color = BauhausBlue.toArgb()
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(routeLine)

                orderedPoints.forEachIndexed { index, point ->
                    val marker = Marker(view).apply {
                        position = point
                        icon = markerIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Stop ${index + 1}"
                    }
                    view.overlays.add(marker)
                }
            }

            view.invalidate()
        }
    )
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
    val selectedCount: Int,
    val missingGpsCount: Int,
    val readErrorCount: Int,
    val diagnostics: List<String>
)

private suspend fun parseGeoPoints(
    contentResolver: ContentResolver,
    uris: List<Uri>
): GeoParseResult = withContext(Dispatchers.IO) {
    val points = mutableListOf<GeoPointData>()
    val diagnostics = mutableListOf<String>()
    var missingGpsCount = 0
    var readErrorCount = 0

    uris.forEach { uri ->
        val displayName = contentResolver.getDisplayName(uri)
        val mimeType = contentResolver.getType(uri) ?: "unknown type"

        try {
            val point = readGeoPoint(contentResolver, uri)
            if (point == null) {
                missingGpsCount += 1
                diagnostics += "NO GPS: $displayName ($mimeType)"
            } else {
                points += point
                diagnostics += "GPS OK: $displayName ($mimeType)"
            }
        } catch (exception: Exception) {
            readErrorCount += 1
            diagnostics += "READ FAIL: $displayName ($mimeType)"
        }
    }

    GeoParseResult(
        points = points,
        selectedCount = uris.size,
        missingGpsCount = missingGpsCount,
        readErrorCount = readErrorCount,
        diagnostics = diagnostics
    )
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

private fun ContentResolver.getDisplayName(uri: Uri): String {
    return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (displayNameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(displayNameIndex)
        } else {
            uri.lastPathSegment ?: "unknown file"
        }
    } ?: (uri.lastPathSegment ?: "unknown file")
}

private fun GeoPointData.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}

fun sortByNearestNeighbor(points: List<GeoPoint>): List<GeoPoint> {
    if (points.size <= 2) return points

    val ordered = mutableListOf(points.first())
    val remaining = points.drop(1).toMutableList()

    while (remaining.isNotEmpty()) {
        val current = ordered.last()
        val next = remaining.minBy { current.distanceInMetersTo(it) }
        ordered += next
        remaining -= next
    }

    return ordered
}

private fun GeoPoint.distanceInMetersTo(other: GeoPoint): Double {
    val earthRadiusMeters = 6_371_000.0
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val deltaLat = Math.toRadians(other.latitude - latitude)
    val deltaLon = Math.toRadians(other.longitude - longitude)
    val haversine = sin(deltaLat / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    return earthRadiusMeters * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

private fun createBauhausMarkerIcon(context: Context): BitmapDrawable {
    val size = 44
    val center = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.style = Paint.Style.FILL
    paint.color = Color.Transparent.toArgb()
    canvas.drawCircle(center, center, center, paint)

    paint.color = BauhausCarbonBlack.toArgb()
    canvas.drawCircle(center, center, 18f, paint)

    paint.color = Color.White.toArgb()
    canvas.drawCircle(center, center, 13f, paint)

    paint.color = BauhausRed.toArgb()
    canvas.drawCircle(center, center, 8f, paint)

    return BitmapDrawable(context.resources, bitmap)
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
