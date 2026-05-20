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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.bauhausroute.ui.theme.ExpressiveAmber
import com.example.bauhausroute.ui.theme.ExpressiveCoral
import com.example.bauhausroute.ui.theme.ExpressiveInk
import com.example.bauhausroute.ui.theme.ExpressiveMuted
import com.example.bauhausroute.ui.theme.ExpressivePeach
import com.example.bauhausroute.ui.theme.ExpressiveSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
                            .padding(18.dp)
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
    var isRouting by remember { mutableStateOf(false) }
    var roadRoute by remember { mutableStateOf<RoadRouteResult?>(null) }

    fun handlePickedUris(uris: List<Uri>) {
        scope.launch {
            isParsing = true
            val result = parseGeoPoints(context.contentResolver, uris)
            points = result.points
            selectedCount = result.selectedCount
            missingGpsCount = result.missingGpsCount
            readErrorCount = result.readErrorCount
            diagnostics = result.diagnostics
            roadRoute = null
            isParsing = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        handlePickedUris(uris)
    }

    LaunchedEffect(points) {
        val routePoints = points.map { it.toGeoPoint() }
        if (routePoints.isEmpty()) {
            roadRoute = null
            return@LaunchedEffect
        }

        isRouting = true
        roadRoute = RoadRouteService.planRoute(routePoints)
        isRouting = false
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeaderPanel()

        ExpressiveActionButton(
            text = if (isParsing) "PARSING" else "IMPORT HEIF",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                filePicker.launch(arrayOf("image/heic", "image/heif"))
            },
            enabled = !isParsing
        )

        StatusPanel(
            selectedCount = selectedCount,
            gpsCount = points.size,
            missingGpsCount = missingGpsCount,
            readErrorCount = readErrorCount,
            isRouting = isRouting,
            roadRoute = roadRoute
        )

        if (points.isNotEmpty()) {
            val fallbackStops = sortByNearestNeighbor(points.map { it.toGeoPoint() })
            val route = roadRoute
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                shape = RoundedCornerShape(30.dp),
                color = ExpressiveSurface,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp
            ) {
                BauhausRouteMap(
                    stopPoints = route?.orderedStops ?: fallbackStops,
                    routePoints = route?.path ?: fallbackStops,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        } else {
            EmptyMapPanel()
        }

        DebugPanel(
            roadDiagnostics = roadRoute?.diagnostics.orEmpty(),
            fileDiagnostics = diagnostics,
            points = points,
            modifier = Modifier.weight(1f, fill = true)
        )
    }
}

@Composable
fun HeaderPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = ExpressiveSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ROUTE\nDISCOVERY",
                    color = ExpressiveInk,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "EXIF to road route",
                    color = ExpressiveMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(ExpressiveCoral),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OSM",
                    color = ExpressiveInk,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun StatusPanel(
    selectedCount: Int,
    gpsCount: Int,
    missingGpsCount: Int,
    readErrorCount: Int,
    isRouting: Boolean,
    roadRoute: RoadRouteResult?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = ExpressiveSurface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip("$selectedCount files", ExpressivePeach)
                SummaryChip("$gpsCount GPS", ExpressiveAmber)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val routeLabel = when {
                    isRouting -> "planning"
                    roadRoute == null -> "waiting"
                    roadRoute.isFallback -> "fallback"
                    else -> roadRoute.distanceMeters.toKilometerLabel()
                }
                SummaryChip(routeLabel, if (roadRoute?.isFallback == true) ExpressivePeach else BauhausBlue, invert = roadRoute?.isFallback != true && roadRoute != null)

                if (missingGpsCount > 0) {
                    SummaryChip("$missingGpsCount no GPS", ExpressivePeach)
                }
                if (readErrorCount > 0) {
                    SummaryChip("$readErrorCount failed", ExpressivePeach)
                }
            }
        }
    }
}

@Composable
fun SummaryChip(
    text: String,
    color: Color,
    invert: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color
    ) {
        Text(
            text = text,
            color = if (invert) Color.White else ExpressiveInk,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun EmptyMapPanel() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(30.dp),
        color = ExpressivePeach,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select geotagged photos",
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun DebugPanel(
    roadDiagnostics: List<String>,
    fileDiagnostics: List<String>,
    points: List<GeoPointData>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ExpressiveSurface.copy(alpha = 0.92f)
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            item {
                Text(
                    text = "DEBUG",
                    color = ExpressiveMuted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            itemsIndexed(roadDiagnostics) { _, item ->
                Text(
                    text = item,
                    color = if (item.contains("FAIL")) BauhausRed else BauhausBlue,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }

            itemsIndexed(fileDiagnostics) { _, item ->
                Text(
                    text = item,
                    color = ExpressiveInk,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }

            itemsIndexed(points) { index, point ->
                Text(
                    text = "${index + 1}. ${point.latitude}, ${point.longitude}",
                    color = BauhausBlue,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun BauhausRouteMap(
    stopPoints: List<GeoPoint>,
    routePoints: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
        factory = { mapView },
        update = { view ->
            view.overlays.clear()

            if (stopPoints.isNotEmpty()) {
                view.controller.setCenter(stopPoints.first())
                view.controller.setZoom(15.0)

                val routeLine = Polyline().apply {
                    setPoints(routePoints)
                    outlinePaint.color = BauhausBlue.toArgb()
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(routeLine)

                stopPoints.forEachIndexed { index, point ->
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
fun ExpressiveActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = ExpressiveAmber
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) containerColor else ExpressivePeach.copy(alpha = 0.7f),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

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
