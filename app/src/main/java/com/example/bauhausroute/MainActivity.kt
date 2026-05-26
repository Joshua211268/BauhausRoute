package com.example.bauhausroute

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bauhausroute.ui.theme.BauhausTheme
import com.example.bauhausroute.ui.theme.ExpressiveAmber
import com.example.bauhausroute.ui.theme.ExpressiveInk
import com.example.bauhausroute.ui.theme.ExpressiveMuted
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

private val SoftGreen = Color(0xFFEAF7EF)
private val LeafGreen = Color(0xFF32C86E)
private val DeepGreen = Color(0xFF168A4A)
private val MintGreen = Color(0xFFDDF7E8)
private val WarningRed = Color(0xFFE84B5F)
private val GlassWhite = Color.White.copy(alpha = 0.78f)
private val GlassStroke = Color.White.copy(alpha = 0.72f)

data class GeoPointData(
    val latitude: Double,
    val longitude: Double
)

private data class CleanStop(
    val farmlandId: Long?,
    val name: String,
    val area: String,
    val code: String,
    val status: String,
    val items: Int,
    val priorityNumber: Int,
    val severity: String,
    val routePoint: GeoPointData?
)

private data class PriorityMapMarker(
    val point: GeoPoint,
    val priorityNumber: Int,
    val severity: String
)

private data class WeatherSnapshot(
    val locationLabel: String,
    val condition: String,
    val temperatureC: Int,
    val humidityPercent: Int,
    val windKmh: Int,
    val rainChancePercent: Int,
    val isGpsBased: Boolean
)

private data class FlightAdvice(
    val title: String,
    val detail: String,
    val iconText: String,
    val containerColor: Color,
    val contentColor: Color
)

private enum class InspectionFilter(val label: String) {
    All("全部"),
    HasTrash("有垃圾"),
    NoTrash("無垃圾")
}

private enum class DashboardTab(
    val label: String,
    val route: String
) {
    Home("首頁", "home"),
    Status("狀態", "status"),
    Settings("設定", "settings")
}

private object MainRoutes {
    const val DetailPattern = "detail/{farmlandId}"

    fun detail(farmlandId: Long): String = "detail/$farmlandId"
}

private enum class AuthRoute {
    Login,
    RoleSelection,
    Register,
    CleanerRegister,
    Main
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BauhausTheme {
                GlassAppBackground {
                    FarmerAuthApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassAppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftGreen)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.42f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width * 0.08f, size.height * 0.02f)
            )
            drawCircle(
                color = MintGreen.copy(alpha = 0.62f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.96f, size.height * 0.36f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.34f),
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.24f, size.height * 0.92f)
            )
        }
        content()
    }
}

@Composable
private fun FarmerAuthApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { FarmerLocalStore(context) }
    val scope = rememberCoroutineScope()
    val database = remember { FarmlandInspectionDatabase.get(context) }
    val teamProfileDao = remember(database) { database.teamProfileDao() }
    var route by remember { mutableStateOf(AuthRoute.Login) }
    var farmer by remember { mutableStateOf<FarmerProfile?>(null) }

    when (route) {
        AuthRoute.Login -> LoginScreen(
            onLogin = { account, password ->
                val profile = store.login(account, password)
                if (profile == null) {
                    Toast.makeText(context, "帳號或密碼錯誤", Toast.LENGTH_SHORT).show()
                } else {
                    farmer = profile
                    route = AuthRoute.Main
                }
            },
            onSkipLogin = {
                farmer = FarmerProfile(
                    name = "測試使用者",
                    phone = "",
                    email = "tester@local",
                    password = "",
                    address = "",
                    role = UserRole.FARMER
                )
                route = AuthRoute.Main
            },
            onRegisterClick = { route = AuthRoute.RoleSelection },
            modifier = modifier
        )

        AuthRoute.RoleSelection -> RoleSelectionScreen(
            onBack = { route = AuthRoute.Login },
            onFarmerClick = { route = AuthRoute.Register },
            onCleanerClick = { route = AuthRoute.CleanerRegister },
            modifier = modifier
        )

        AuthRoute.Register -> RegisterScreen(
            onBack = { route = AuthRoute.RoleSelection },
            onRegister = { profile ->
                store.register(profile.copy(role = UserRole.FARMER))
                Toast.makeText(context, "註冊成功，請登入", Toast.LENGTH_SHORT).show()
                route = AuthRoute.Login
            },
            modifier = modifier
        )

        AuthRoute.CleanerRegister -> CleanerTeamRegisterScreen(
            onBack = { route = AuthRoute.RoleSelection },
            onSkip = {
                farmer = FarmerProfile(
                    name = "清潔團隊測試",
                    phone = "",
                    email = "cleaner@local",
                    password = "",
                    address = "",
                    role = UserRole.CLEANER
                )
                route = AuthRoute.Main
            },
            onRegister = { profile ->
                scope.launch {
                    teamProfileDao.insert(profile)
                    farmer = FarmerProfile(
                        name = profile.contactName,
                        phone = profile.phone,
                        email = profile.email,
                        password = "",
                        address = "${profile.city}${profile.district}",
                        role = UserRole.CLEANER
                    )
                    Toast.makeText(context, "清潔團隊註冊完成", Toast.LENGTH_SHORT).show()
                    route = AuthRoute.Main
                }
            },
            modifier = modifier
        )

        AuthRoute.Main -> RouteDiscoveryScreen(
            farmer = farmer,
            modifier = modifier
        )
    }
}

@Composable
private fun RoleSelectionScreen(
    onBack: () -> Unit,
    onFarmerClick: () -> Unit,
    onCleanerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 14.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F6F4))
                .align(Alignment.TopStart)
        ) {
            Text(text = "<", color = ExpressiveInk, style = MaterialTheme.typography.titleLarge)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 118.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LeafLogo(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MintGreen)
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "農地巡檢系統",
                color = ExpressiveInk,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "無人機智慧巡檢・垃圾偵測分析",
                color = ExpressiveMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(58.dp))
            Text(
                text = "請選擇您的身分",
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(22.dp))
            RoleCard(
                title = "農民",
                subtitle = "查看農地巡檢結果與管理農地",
                accent = LeafGreen,
                onClick = onFarmerClick
            )
            Spacer(modifier = Modifier.height(14.dp))
            RoleCard(
                title = "清潔團隊",
                subtitle = "查看所有農地狀態與路徑規劃",
                accent = Color(0xFFFF9F43),
                onClick = onCleanerClick
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(text = title, color = ExpressiveInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(text = subtitle, color = ExpressiveMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text(text = ">", color = ExpressiveMuted, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onSkipLogin: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LeafLogo(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MintGreen)
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "農地巡檢系統",
                color = ExpressiveInk,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "無人機智慧巡檢・垃圾偵測分析",
                color = ExpressiveMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(34.dp))
            MintTextField(
                value = account,
                onValueChange = { account = it },
                label = "EMAIL",
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(14.dp))
            MintTextField(
                value = password,
                onValueChange = { password = it },
                label = "PASSWORD",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    EyeToggleButton(
                        visible = showPassword,
                        onClick = { showPassword = !showPassword }
                    )
                }
            )
            Spacer(modifier = Modifier.height(26.dp))
            MintButton(
                text = "登入",
                enabled = account.isNotBlank() && password.isNotBlank(),
                onClick = { onLogin(account, password) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = {}) {
                Text(text = "忘記密碼？", color = DeepGreen, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onRegisterClick) {
                Text(text = "還沒有帳號？ 立即註冊", color = DeepGreen, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(
            onClick = onSkipLogin,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        ) {
            Text(text = "略過 (Skip)", color = ExpressiveMuted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegisterScreen(
    onBack: () -> Unit,
    onRegister: (FarmerProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val canSubmit = name.isNotBlank() &&
        phone.isNotBlank() &&
        email.isNotBlank() &&
        password.isNotBlank() &&
        address.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Text(text = "<", color = ExpressiveInk, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "帳號註冊",
                    color = ExpressiveInk,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(text = "步驟 1/2", color = ExpressiveMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE5E8ED))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .background(LeafGreen)
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        FormLabel("姓名 *")
        MintTextField(value = name, onValueChange = { name = it }, label = "王小明")
        FormLabel("電話 *")
        MintTextField(value = phone, onValueChange = { phone = it }, label = "0912-345-678", keyboardType = KeyboardType.Phone)
        FormLabel("信箱 *")
        MintTextField(value = email, onValueChange = { email = it }, label = "example@email.com", keyboardType = KeyboardType.Email)
        FormLabel("密碼 *")
        MintTextField(
            value = password,
            onValueChange = { password = it },
            label = "請輸入密碼",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                EyeToggleButton(
                    visible = showPassword,
                    onClick = { showPassword = !showPassword }
                )
            }
        )
        FormLabel("聯絡地址 *")
        MintTextField(value = address, onValueChange = { address = it }, label = "縣市 / 區 / 路")
        Spacer(modifier = Modifier.height(22.dp))
        MintButton(
            text = "註冊",
            enabled = canSubmit,
            onClick = {
                onRegister(
                    FarmerProfile(
                        name = name,
                        phone = phone,
                        email = email,
                        password = password,
                        address = address
                    )
                )
            }
        )
    }
}

private val TaiwanDistricts = linkedMapOf(
    "台北市" to listOf("中正區", "大同區", "中山區", "松山區", "大安區", "信義區", "士林區", "北投區"),
    "新北市" to listOf("板橋區", "新莊區", "中和區", "永和區", "三重區", "新店區", "淡水區", "汐止區"),
    "桃園市" to listOf("桃園區", "中壢區", "平鎮區", "八德區", "楊梅區", "蘆竹區", "大溪區"),
    "台中市" to listOf("中區", "東區", "南區", "西區", "北區", "西屯區", "南屯區", "北屯區"),
    "台南市" to listOf("中西區", "東區", "南區", "北區", "安平區", "安南區", "永康區"),
    "高雄市" to listOf("新興區", "前金區", "苓雅區", "鹽埕區", "鼓山區", "左營區", "三民區"),
    "嘉義縣" to listOf("太保市", "朴子市", "民雄鄉", "水上鄉", "中埔鄉", "竹崎鄉"),
    "屏東縣" to listOf("屏東市", "潮州鎮", "東港鎮", "恆春鎮", "萬丹鄉", "內埔鄉")
)

@Composable
private fun CleanerTeamRegisterScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onRegister: (TeamProfileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var teamName by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var teamSize by remember { mutableStateOf(5) }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    val districts = TaiwanDistricts[city].orEmpty()
    val canSubmit = teamName.isNotBlank() &&
        contactName.isNotBlank() &&
        phone.isNotBlank() &&
        email.isNotBlank() &&
        city.isNotBlank() &&
        district.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Text(text = "<", color = ExpressiveInk, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "清潔團隊註冊",
                    color = ExpressiveInk,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(text = "填寫團隊基本資料", color = ExpressiveMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        FormLabel("團隊名稱 *")
        MintTextField(value = teamName, onValueChange = { teamName = it }, label = "例：綠淨清潔隊")
        FormLabel("聯絡人姓名 *")
        MintTextField(value = contactName, onValueChange = { contactName = it }, label = "請輸入姓名")
        FormLabel("電話 *")
        MintTextField(value = phone, onValueChange = { phone = it }, label = "0912-345-678", keyboardType = KeyboardType.Phone)
        FormLabel("信箱")
        MintTextField(value = email, onValueChange = { email = it }, label = "team@email.com", keyboardType = KeyboardType.Email)
        FormLabel("團隊人數")
        TeamSizeStepper(
            value = teamSize,
            onValueChange = { teamSize = it },
            modifier = Modifier.fillMaxWidth()
        )
        FormLabel("服務區域 *")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AreaDropdown(
                value = city,
                placeholder = "選擇縣市",
                options = TaiwanDistricts.keys.toList(),
                onSelected = {
                    city = it
                    district = ""
                },
                modifier = Modifier.weight(1f)
            )
            AreaDropdown(
                value = district,
                placeholder = "選擇區",
                options = districts,
                enabled = city.isNotBlank(),
                onSelected = { district = it },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(44.dp))
        MintButton(
            text = "完成註冊",
            enabled = canSubmit,
            onClick = {
                onRegister(
                    TeamProfileEntity(
                        teamName = teamName.trim(),
                        contactName = contactName.trim(),
                        phone = phone.trim(),
                        email = email.trim().lowercase(),
                        teamSize = teamSize,
                        city = city,
                        district = district,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
        )
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "略過 (Skip)", color = ExpressiveMuted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TeamSizeStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE3E6EA), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$value 人",
            color = ExpressiveInk,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        StepperButton(text = "-", enabled = value > 1, onClick = { onValueChange(value - 1) })
        Spacer(modifier = Modifier.width(6.dp))
        StepperButton(text = "+", enabled = value < 99, onClick = { onValueChange(value + 1) })
    }
}

@Composable
private fun StepperButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 42.dp, height = 30.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE9ECEF),
            contentColor = ExpressiveInk,
            disabledContainerColor = Color(0xFFF2F4F6),
            disabledContentColor = ExpressiveMuted.copy(alpha = 0.38f)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AreaDropdown(
    value: String,
    placeholder: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.58f))
                .border(1.dp, Color(0xFFE3E6EA), RoundedCornerShape(16.dp))
                .clickable(enabled = enabled && options.isNotEmpty()) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) ExpressiveMuted.copy(alpha = 0.48f) else DeepGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(text = "⌄", color = ExpressiveMuted, style = MaterialTheme.typography.titleMedium)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        color = ExpressiveInk,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 10.dp, bottom = 7.dp)
    )
}

@Composable
private fun MintTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        placeholder = { Text(text = label, color = ExpressiveMuted.copy(alpha = 0.42f)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White.copy(alpha = 0.78f),
            focusedBorderColor = LeafGreen,
            unfocusedBorderColor = Color(0xFFE3E6EA),
            cursorColor = DeepGreen,
            focusedTextColor = ExpressiveInk,
            unfocusedTextColor = ExpressiveInk
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon
    )
}

@Composable
private fun MintButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepGreen,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFA7DEB6),
            disabledContentColor = Color.White
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EyeToggleButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .aspectRatio(1f)
        ) {
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawOval(
                color = ExpressiveMuted,
                topLeft = Offset(size.width * 0.12f, size.height * 0.28f),
                size = Size(size.width * 0.76f, size.height * 0.44f),
                style = stroke
            )
            drawCircle(
                color = if (visible) DeepGreen else ExpressiveMuted,
                radius = size.minDimension * 0.13f,
                center = center,
                style = stroke
            )
            if (!visible) {
                drawLine(
                    color = ExpressiveMuted,
                    start = Offset(size.width * 0.2f, size.height * 0.82f),
                    end = Offset(size.width * 0.82f, size.height * 0.18f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun LeafLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawOval(
            color = LeafGreen,
            topLeft = Offset(size.width * 0.14f, size.height * 0.18f),
            size = Size(size.width * 0.72f, size.height * 0.58f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.26f, size.height * 0.46f),
            end = Offset(size.width * 0.78f, size.height * 0.58f),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = LeafGreen,
            start = Offset(size.width * 0.62f, size.height * 0.66f),
            end = Offset(size.width * 0.82f, size.height * 0.86f),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun RouteDiscoveryScreen(
    farmer: FarmerProfile? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val farmlandDao = remember { FarmlandInspectionDatabase.get(context).farmlandInspectionDao() }
    val farmlands by farmlandDao.observeAll().collectAsState(initial = emptyList())
    var points by remember { mutableStateOf(recentDemoPoints()) }
    var selectedCount by remember { mutableStateOf(2) }
    var missingGpsCount by remember { mutableStateOf(0) }
    var readErrorCount by remember { mutableStateOf(0) }
    var diagnostics by remember { mutableStateOf<List<String>>(emptyList()) }
    var isParsing by remember { mutableStateOf(false) }
    var isRouting by remember { mutableStateOf(false) }
    var roadRoute by remember { mutableStateOf<RoadRouteResult?>(null) }
    var weather by remember { mutableStateOf(createLocalWeather(null, farmer?.address)) }
    var farmerFilter by remember { mutableStateOf(InspectionFilter.All) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: DashboardTab.Home.route
    val selectedTab = currentRoute.toDashboardTab()
    val farmerAddress = farmer?.address.orEmpty()
    val currentRole = farmer?.role ?: UserRole.FARMER
    val canUseCleanerTools = currentRole == UserRole.CLEANER || currentRole == UserRole.ADMIN
    val availableTabs = if (canUseCleanerTools) {
        DashboardTab.entries
    } else {
        listOf(DashboardTab.Home, DashboardTab.Settings)
    }

    fun refreshWeatherFromDeviceLocation() {
        scope.launch {
            if (farmerAddress.isNotBlank()) {
                weather = withContext(Dispatchers.IO) {
                    createWeatherSnapshotFromAddress(farmerAddress)
                }
                return@launch
            }
            val location = context.awaitDeviceLocation()
            weather = withContext(Dispatchers.IO) {
                createWeatherSnapshot(location)
            }
        }
    }

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            refreshWeatherFromDeviceLocation()
        } else {
            weather = createLocalWeather(null, farmerAddress.ifBlank { null })
        }
    }

    LaunchedEffect(farmerAddress, canUseCleanerTools) {
        if (farmerAddress.isNotBlank()) {
            weather = withContext(Dispatchers.IO) {
                createWeatherSnapshotFromAddress(farmerAddress)
            }
        } else if (context.hasLocationPermission()) {
            refreshWeatherFromDeviceLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val priorityFarmlands = remember(farmlands) {
        farmlands.toPrioritySortedFarmlands().take(3)
    }
    val farmlandRoutePoints = remember(priorityFarmlands) {
        priorityFarmlands.mapNotNull { farmland ->
            val latitude = farmland.latitude
            val longitude = farmland.longitude
            if (latitude == null || longitude == null) {
                null
            } else {
                GeoPointData(latitude, longitude)
            }
        }
    }
    val displayRoutePoints = if (farmlandRoutePoints.isNotEmpty()) farmlandRoutePoints else points
    LaunchedEffect(displayRoutePoints) {
        val routePoints = displayRoutePoints.map { it.toGeoPoint() }
        if (routePoints.isEmpty()) {
            roadRoute = null
            return@LaunchedEffect
        }

        isRouting = true
        roadRoute = RoadRouteService.planRoute(routePoints)
        isRouting = false
    }

    val stops = remember(farmlands, points, roadRoute) {
        buildStops(farmlands, points, roadRoute)
    }
    val priorityMarkers = remember(stops) {
        stops.mapNotNull { stop ->
            stop.routePoint?.let { point ->
                PriorityMapMarker(
                    point = point.toGeoPoint(),
                    priorityNumber = stop.priorityNumber,
                    severity = stop.severity
                )
            }
        }
    }
    val flightAdvice = remember(weather) { weather.toFlightAdvice() }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardHeader(
                onBellClick = {},
                currentRole = currentRole,
                userName = farmer?.name,
                onScanClick = { filePicker.launch(arrayOf("image/heic", "image/heif")) },
                modifier = Modifier.padding(bottom = 2.dp)
            )

            if (selectedTab == DashboardTab.Home || (canUseCleanerTools && selectedTab == DashboardTab.Status)) {
                WeatherCard(
                    weather = weather,
                    flightAdvice = flightAdvice,
                    isParsing = isParsing,
                    isRouting = isRouting,
                    onImportClick = { filePicker.launch(arrayOf("image/heic", "image/heif")) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                SharedTransitionLayout {
                    NavHost(
                        navController = navController,
                        startDestination = DashboardTab.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(DashboardTab.Home.route) {
                            if (canUseCleanerTools) {
                                RouteCard(
                                    stops = stops,
                                    points = displayRoutePoints,
                                    roadRoute = roadRoute,
                                    isRouting = isRouting,
                                    missingGpsCount = missingGpsCount,
                                    readErrorCount = readErrorCount,
                                    diagnostics = diagnostics,
                                    priorityMarkers = priorityMarkers,
                                    onStopClick = { farmlandId ->
                                        navController.navigate(MainRoutes.detail(farmlandId))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                FarmerRoleHome(
                                    farmer = farmer,
                                    farmlands = farmlands,
                                    selectedFilter = farmerFilter,
                                    onFilterSelected = { farmerFilter = it },
                                    onFarmlandSelected = { farmlandId ->
                                        navController.navigate(MainRoutes.detail(farmlandId))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        composable(DashboardTab.Status.route) {
                            if (canUseCleanerTools) {
                                StatusScene(
                                    dao = farmlandDao,
                                    farmlands = farmlands,
                                    onFarmlandSelected = { farmlandId ->
                                        navController.navigate(MainRoutes.detail(farmlandId))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                FarmerRoleHome(
                                    farmer = farmer,
                                    farmlands = farmlands,
                                    selectedFilter = farmerFilter,
                                    onFilterSelected = { farmerFilter = it },
                                    onFarmlandSelected = { farmlandId ->
                                        navController.navigate(MainRoutes.detail(farmlandId))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        composable(DashboardTab.Settings.route) {
                            SettingsScene(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        composable(MainRoutes.DetailPattern) { entry ->
                            val farmlandId = entry.arguments?.getString("farmlandId")?.toLongOrNull()
                            val farmland = farmlands.firstOrNull { it.id == farmlandId }
                            if (farmland == null) {
                                MissingFarmlandScene(
                                    onBack = { navController.popBackStack() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                FarmlandDetailScene(
                                    dao = farmlandDao,
                                    farmland = farmland,
                                    onBack = { navController.popBackStack() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        BottomNavigationBar(
            selectedTab = selectedTab,
            tabs = availableTabs,
            onTabSelected = { tab ->
                navController.navigate(tab.route) {
                    popUpTo(DashboardTab.Home.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
private fun DashboardHeader(
    onBellClick: () -> Unit,
    currentRole: UserRole,
    userName: String?,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (currentRole) {
        UserRole.FARMER -> "我的農地巡檢"
        UserRole.CLEANER -> "待處理任務"
        UserRole.ADMIN -> "系統總覽"
    }
    val subtitle = when (currentRole) {
        UserRole.FARMER -> "${userName?.takeIf { it.isNotBlank() } ?: "使用者"} ，歡迎回來"
        UserRole.CLEANER -> "查看所有農地狀態與路徑規劃"
        UserRole.ADMIN -> "管理巡檢、任務與團隊資料"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = ExpressiveMuted,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val actionClick = if (currentRole == UserRole.FARMER) onScanClick else onBellClick
        Surface(
            modifier = Modifier
                .size(50.dp)
                .background(Color.White.copy(alpha = 0.54f), CircleShape)
                .clickable(onClick = actionClick),
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (currentRole == UserRole.FARMER) {
                    ScanCameraIcon(
                        color = DeepGreen,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(text = "!", color = DeepGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WarningRed)
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmerRoleHome(
    farmer: FarmerProfile?,
    farmlands: List<FarmlandInspectionEntity>,
    selectedFilter: InspectionFilter,
    onFilterSelected: (InspectionFilter) -> Unit,
    onFarmlandSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredFarmlands = remember(farmlands, selectedFilter) {
        when (selectedFilter) {
            InspectionFilter.All -> farmlands
            InspectionFilter.HasTrash -> farmlands.filter { it.trashCount > 0 }
            InspectionFilter.NoTrash -> farmlands.filter { it.trashCount == 0 }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FarmerFilterRow(
            selectedFilter = selectedFilter,
            allCount = farmlands.size,
            trashCount = farmlands.count { it.trashCount > 0 },
            cleanCount = farmlands.count { it.trashCount == 0 },
            onFilterSelected = onFilterSelected
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = GlassWhite,
            shadowElevation = 8.dp
        ) {
            if (filteredFarmlands.isEmpty()) {
                FarmerEmptyInspectionState(modifier = Modifier.fillMaxSize())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredFarmlands, key = { it.id }) { farmland ->
                        FarmlandGridCard(
                            farmland = farmland,
                            onClick = { onFarmlandSelected(farmland.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FarmerFilterRow(
    selectedFilter: InspectionFilter,
    allCount: Int,
    trashCount: Int,
    cleanCount: Int,
    onFilterSelected: (InspectionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FarmerFilterChip(
            label = "${InspectionFilter.All.label} ($allCount)",
            selected = selectedFilter == InspectionFilter.All,
            onClick = { onFilterSelected(InspectionFilter.All) },
            modifier = Modifier.weight(1f)
        )
        FarmerFilterChip(
            label = "${InspectionFilter.HasTrash.label} ($trashCount)",
            selected = selectedFilter == InspectionFilter.HasTrash,
            onClick = { onFilterSelected(InspectionFilter.HasTrash) },
            modifier = Modifier.weight(1f)
        )
        FarmerFilterChip(
            label = "${InspectionFilter.NoTrash.label} ($cleanCount)",
            selected = selectedFilter == InspectionFilter.NoTrash,
            onClick = { onFilterSelected(InspectionFilter.NoTrash) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FarmerFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) DeepGreen else Color.White.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = if (selected) DeepGreen else GlassStroke,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else ExpressiveInk,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FarmerEmptyInspectionState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center
        ) {
            AlbumIcon(
                color = LeafGreen.copy(alpha = 0.52f),
                modifier = Modifier
                    .size(82.dp)
                    .offset(x = (-16).dp, y = 12.dp)
            )
            ScanCameraIcon(
                color = DeepGreen,
                modifier = Modifier
                    .size(78.dp)
                    .offset(x = 14.dp, y = (-8).dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "尚無巡檢資料",
            color = ExpressiveInk,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "無人機巡檢結果將顯示在這裡",
            color = ExpressiveMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScanCameraIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.4.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.18f, size.height * 0.28f),
            size = Size(size.width * 0.64f, size.height * 0.5f),
            style = stroke,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.31f, size.height * 0.18f),
            size = Size(size.width * 0.22f, size.height * 0.12f),
            style = stroke,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.13f,
            center = Offset(size.width * 0.5f, size.height * 0.53f),
            style = stroke
        )
        listOf(
            Offset(size.width * 0.08f, size.height * 0.14f) to Offset(size.width * 0.25f, size.height * 0.14f),
            Offset(size.width * 0.08f, size.height * 0.14f) to Offset(size.width * 0.08f, size.height * 0.31f),
            Offset(size.width * 0.92f, size.height * 0.14f) to Offset(size.width * 0.75f, size.height * 0.14f),
            Offset(size.width * 0.92f, size.height * 0.14f) to Offset(size.width * 0.92f, size.height * 0.31f),
            Offset(size.width * 0.08f, size.height * 0.86f) to Offset(size.width * 0.25f, size.height * 0.86f),
            Offset(size.width * 0.08f, size.height * 0.86f) to Offset(size.width * 0.08f, size.height * 0.69f),
            Offset(size.width * 0.92f, size.height * 0.86f) to Offset(size.width * 0.75f, size.height * 0.86f),
            Offset(size.width * 0.92f, size.height * 0.86f) to Offset(size.width * 0.92f, size.height * 0.69f)
        ).forEach { (start, end) ->
            drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun AlbumIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.6.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.18f),
            size = Size(size.width * 0.74f, size.height * 0.64f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.34f, size.height * 0.38f)
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.72f),
            end = Offset(size.width * 0.44f, size.height * 0.54f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.44f, size.height * 0.54f),
            end = Offset(size.width * 0.78f, size.height * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun String.toDashboardTab(): DashboardTab {
    return when (this) {
        DashboardTab.Status.route,
        MainRoutes.DetailPattern -> DashboardTab.Status
        DashboardTab.Settings.route -> DashboardTab.Settings
        else -> DashboardTab.Home
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherSnapshot,
    flightAdvice: FlightAdvice,
    isParsing: Boolean,
    isRouting: Boolean,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(GlassWhite)
            .border(1.dp, GlassStroke, RoundedCornerShape(30.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                WeatherIcon(
                    condition = weather.condition,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Text(
                    text = weather.condition,
                    color = ExpressiveInk,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
                Text(
                    text = weather.locationLabel,
                    color = ExpressiveMuted,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeatherMetric("${weather.temperatureC}°C")
                    WeatherMetric("${weather.humidityPercent}%")
                    WeatherMetric("${weather.windKmh}km/h")
                }
            }

            Column(
                modifier = Modifier
                    .width(68.dp)
                    .clickable(onClick = onImportClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isParsing || isRouting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DeepGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = flightAdvice.iconText,
                        color = flightAdvice.contentColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = flightAdvice.title,
                        color = flightAdvice.contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherIcon(
    condition: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cloud = if (condition.contains("雨")) Color(0xFF74B8F3) else LeafGreen
        val sun = ExpressiveAmber
        if (condition.contains("晴")) {
            drawCircle(color = sun, radius = size.minDimension * 0.28f, center = center)
        } else {
            drawCircle(color = cloud, radius = size.minDimension * 0.23f, center = Offset(size.width * 0.38f, size.height * 0.48f))
            drawCircle(color = cloud, radius = size.minDimension * 0.29f, center = Offset(size.width * 0.58f, size.height * 0.42f))
            drawRoundRect(
                color = cloud,
                topLeft = Offset(size.width * 0.16f, size.height * 0.48f),
                size = Size(size.width * 0.72f, size.height * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx())
            )
            if (condition.contains("雨")) {
                drawLine(
                    color = DeepGreen,
                    start = Offset(size.width * 0.34f, size.height * 0.82f),
                    end = Offset(size.width * 0.24f, size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = DeepGreen,
                    start = Offset(size.width * 0.58f, size.height * 0.82f),
                    end = Offset(size.width * 0.48f, size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun WeatherMetric(text: String) {
    Text(
        text = text,
        color = ExpressiveMuted,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1
    )
}

@Composable
private fun RouteCard(
    stops: List<CleanStop>,
    points: List<GeoPointData>,
    roadRoute: RoadRouteResult?,
    isRouting: Boolean,
    missingGpsCount: Int,
    readErrorCount: Int,
    diagnostics: List<String>,
    priorityMarkers: List<PriorityMapMarker>,
    onStopClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassStroke, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = GlassWhite,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RouteCardHeader(
                stopCount = stops.size,
                routeLabel = routeStatusLabel(isRouting, roadRoute)
            )

            MapPreview(
                points = points,
                roadRoute = roadRoute,
                priorityMarkers = priorityMarkers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
            )

            GlassDivider()

            LazyColumn(
                modifier = Modifier.height(178.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(stops) { _, stop ->
                    StopRow(
                        stop = stop,
                        onClick = {
                            stop.farmlandId?.let(onStopClick)
                        }
                    )
                }

                if (missingGpsCount > 0 || readErrorCount > 0) {
                    item {
                        Text(
                            text = "未定位 $missingGpsCount 處 · 讀取失敗 $readErrorCount 處",
                            color = WarningRed,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                diagnostics.takeLast(2).forEach { message ->
                    item {
                        Text(
                            text = message,
                            color = ExpressiveMuted,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCardHeader(
    stopCount: Int,
    routeLabel: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "▶", color = LeafGreen, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "最佳清掃路線",
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Text(
                text = "已排配 $stopCount 處垃圾，路線已優化",
                color = ExpressiveMuted,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SummaryPill(text = routeLabel, color = MintGreen, contentColor = DeepGreen)
    }
}

@Composable
private fun GlassDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.72f))
    )
}

@Composable
private fun MapPreview(
    points: List<GeoPointData>,
    roadRoute: RoadRouteResult?,
    priorityMarkers: List<PriorityMapMarker>,
    modifier: Modifier = Modifier
) {
    val priorityPoints = priorityMarkers.map { it.point }
    val stopPoints = if (priorityPoints.isNotEmpty()) {
        priorityPoints
    } else {
        roadRoute?.orderedStops ?: sortByNearestNeighbor(points.map { it.toGeoPoint() })
    }
    val routePoints = roadRoute?.path ?: stopPoints
    val mapMarkers = priorityMarkers.ifEmpty {
        stopPoints.take(3).mapIndexed { index, point ->
            PriorityMapMarker(
                point = point,
                priorityNumber = index + 1,
                severity = fallbackSeverityForPriority(index + 1)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
    ) {
        if (stopPoints.isNotEmpty()) {
            BauhausRouteMap(
                markers = mapMarkers,
                routePoints = routePoints,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
            )
        } else {
            TemplateMap(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TemplateMap(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val roadColor = Color(0xFFFFF1C7).copy(alpha = 0.82f)

        drawLine(
            color = roadColor,
            start = Offset(size.width * 0.03f, size.height * 0.72f),
            end = Offset(size.width * 0.94f, size.height * 0.28f),
            strokeWidth = 13.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.68f),
            start = Offset(size.width * 0.05f, size.height * 0.2f),
            end = Offset(size.width * 0.9f, size.height * 0.82f),
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = LeafGreen.copy(alpha = 0.92f),
            start = Offset(size.width * 0.25f, size.height * 0.62f),
            end = Offset(size.width * 0.72f, size.height * 0.34f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

    }

    BoxWithConstraints(modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        MapZoomControls(modifier = Modifier.align(Alignment.TopStart))
        listOf(
            Triple("1", 0.31f, 0.62f),
            Triple("2", 0.55f, 0.37f),
            Triple("3", 0.76f, 0.64f)
        ).forEach { (label, x, y) ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxWidth * x - 14.dp, y = maxHeight * y - 14.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(WarningRed),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MapZoomControls(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.86f),
        shadowElevation = 5.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "+", color = ExpressiveInk, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 9.dp))
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .width(28.dp)
                    .background(Color(0xFFE0E7DF))
            )
            Text(text = "−", color = ExpressiveInk, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 9.dp))
        }
    }
}

@Composable
private fun StopRow(
    stop: CleanStop,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = stop.farmlandId != null, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(stop.severity.toPriorityMarkerColor()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${stop.priorityNumber}",
                color = if (stop.severity == "輕微") ExpressiveInk else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = stop.name,
                color = ExpressiveInk,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${stop.area} · 巡檢點 ${stop.code}",
                color = ExpressiveMuted,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val statusColors = stop.status.toStatusColors()
        SummaryPill(
            text = stop.status,
            color = statusColors.first,
            contentColor = statusColors.second
        )
        Text(
            text = "${stop.items}件",
            color = ExpressiveMuted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SummaryPill(
    text: String,
    color: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.82f),
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun StatusScene(
    dao: FarmlandInspectionDao,
    farmlands: List<FarmlandInspectionEntity>,
    onFarmlandSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    GlassPanel(modifier = modifier.fillMaxHeight()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddFarmlandButton(onClick = { showAddDialog = true })
                }

                if (farmlands.isEmpty()) {
                    EmptyFarmlandState(
                        onAddClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(farmlands, key = { it.id }) { farmland ->
                            FarmlandGridCard(
                                farmland = farmland,
                                onClick = { onFarmlandSelected(farmland.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFarmlandDialog(
            dao = dao,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddFarmlandButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(LeafGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MissingFarmlandScene(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "找不到巡檢紀錄",
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "該筆資料可能已被刪除。",
                color = ExpressiveMuted,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(18.dp))
            MintButton(text = "返回狀態頁", enabled = true, onClick = onBack)
        }
    }
}

@Composable
private fun EmptyFarmlandState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "尚未新增農地",
            color = ExpressiveInk,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "從相簿選取 HEIF 圖片並輸入地區名稱。",
            color = ExpressiveMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        MintButton(text = "新增農地", enabled = true, onClick = onAddClick)
    }
}

@Composable
private fun FarmlandGridCard(
    farmland: FarmlandInspectionEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.64f))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        FarmlandImage(
            imageUri = farmland.coverImageUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.22f)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = farmland.regionName,
            color = ExpressiveInk,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "最後更改 ${farmland.timestampMillis.toDateLabel()}",
            color = ExpressiveMuted,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun FarmlandDetailScene(
    dao: FarmlandInspectionDao,
    farmland: FarmlandInspectionEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val detailImages = farmland.detailImages
    val pagerState = rememberPagerState(
        initialPage = (detailImages.size - 1).coerceAtLeast(0),
        pageCount = { detailImages.size.coerceAtLeast(1) }
    )
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            persistImageReadPermission(context, uri)
            scope.launch {
                val updatedImages = (detailImages + uri.toString()).distinct()
                dao.updateDetailImages(farmland.id, updatedImages)
                Toast.makeText(context, "已加入新巡檢圖片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(detailImages.size) {
        if (detailImages.isNotEmpty()) {
            pagerState.animateScrollToPage(detailImages.lastIndex)
        }
    }

    GlassPanel(modifier = modifier.fillMaxHeight()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) {
                        Text(text = "返回", color = DeepGreen, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "詳細資訊",
                        color = ExpressiveInk,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    DeleteFarmlandButton(onClick = { showDeleteDialog = true })
                }
            }

            item {
                FarmlandImagePager(
                    imageUris = detailImages,
                    pagerState = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            item {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = farmland.regionName,
                            color = ExpressiveInk,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "地區名稱",
                            color = ExpressiveMuted,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    AddDetailImageButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    Text(
                        text = farmland.coordinateLabel(),
                        color = ExpressiveMuted,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InspectionIndicator(
                        icon = farmland.severityIcon(),
                        label = farmland.severity,
                        color = farmland.severityColor(),
                        modifier = Modifier.weight(1f)
                    )
                    InspectionIndicator(
                        icon = if (farmland.processStatus == "已處理") "✓" else "…",
                        label = farmland.processStatus,
                        color = if (farmland.processStatus == "已處理") DeepGreen else Color(0xFF4D8DF7),
                        modifier = Modifier.weight(1f)
                    )
                    InspectionIndicator(
                        icon = "◇",
                        label = "${farmland.trashCount} 件垃圾",
                        color = ExpressiveInk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                DescriptionBlock(description = farmland.description)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "詳細座標\n${farmland.coordinateLabel()}",
                        color = ExpressiveMuted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "巡檢日期時間\n${farmland.timestampMillis.toDateTimeLabel()}",
                        color = ExpressiveMuted,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "刪除巡檢紀錄",
                    color = ExpressiveInk,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    text = "確定要刪除 ${farmland.regionName} 的巡檢紀錄嗎？此動作將無法復原。",
                    color = ExpressiveMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dao.delete(farmland)
                            showDeleteDialog = false
                            Toast.makeText(context, "已成功刪除該農地紀錄", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                ) {
                    Text(text = "確定刪除", color = WarningRed, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "取消", color = ExpressiveMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun DeleteFarmlandButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(WarningRed.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val strokeWidth = 2.2.dp.toPx()
            drawLine(
                color = WarningRed,
                start = Offset(size.width * 0.26f, size.height * 0.28f),
                end = Offset(size.width * 0.74f, size.height * 0.28f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = WarningRed,
                start = Offset(size.width * 0.42f, size.height * 0.16f),
                end = Offset(size.width * 0.58f, size.height * 0.16f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawRoundRect(
                color = WarningRed,
                topLeft = Offset(size.width * 0.32f, size.height * 0.36f),
                size = Size(size.width * 0.36f, size.height * 0.46f),
                style = Stroke(width = strokeWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun AddDetailImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(MintGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(23.dp)) {
            val strokeWidth = 2.2.dp.toPx()
            drawRoundRect(
                color = DeepGreen,
                topLeft = Offset(size.width * 0.12f, size.height * 0.28f),
                size = Size(size.width * 0.62f, size.height * 0.52f),
                style = Stroke(width = strokeWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawCircle(
                color = DeepGreen,
                radius = size.minDimension * 0.11f,
                center = Offset(size.width * 0.43f, size.height * 0.54f),
                style = Stroke(width = strokeWidth)
            )
            drawLine(
                color = DeepGreen,
                start = Offset(size.width * 0.84f, size.height * 0.36f),
                end = Offset(size.width * 0.84f, size.height * 0.74f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = DeepGreen,
                start = Offset(size.width * 0.65f, size.height * 0.55f),
                end = Offset(size.width * 1.03f, size.height * 0.55f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InspectionIndicator(
    icon: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                color = color,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DescriptionBlock(description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "狀況描述",
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = description,
                color = ExpressiveMuted,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AddFarmlandDialog(
    dao: FarmlandInspectionDao,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var regionName by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistImageReadPermission(context, uri)
            imageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = regionName.isNotBlank() && imageUri != null,
                onClick = {
                    val pickedUri = imageUri ?: return@TextButton
                    scope.launch {
                        val point = withContext(Dispatchers.IO) {
                            readGeoPoint(context.contentResolver, pickedUri)
                        }
                        val trashCount = generateTrashCount(regionName)
                        dao.insert(
                            FarmlandInspectionEntity(
                                regionName = regionName.trim(),
                                coverImageUri = pickedUri.toString(),
                                detailImages = emptyList(),
                                latitude = point?.latitude,
                                longitude = point?.longitude,
                                severity = severityForTrashCount(trashCount),
                                processStatus = "待處理",
                                trashCount = trashCount,
                                description = buildAiInspectionDescription(regionName, trashCount, point),
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                        onDismiss()
                    }
                }
            ) {
                Text(text = "儲存", color = DeepGreen, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = ExpressiveMuted)
            }
        },
        title = {
            Text(text = "新增農地", color = ExpressiveInk, fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MintTextField(
                    value = regionName,
                    onValueChange = { regionName = it },
                    label = "地區名稱"
                )
                Button(
                    onClick = { picker.launch(arrayOf("image/heif", "image/heic", "image/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (imageUri == null) "選取 HEIF 圖片" else "已選取圖片")
                }
            }
        },
        containerColor = Color(0xFFF7FCF9),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FarmlandImagePager(
    imageUris: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(MintGreen)) {
        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "尚未加入圖片", color = DeepGreen, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                FarmlandImage(
                    imageUri = imageUris[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                imageUris.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.54f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmlandImage(
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (imageUri.isNullOrBlank()) {
        Box(
            modifier = modifier.background(MintGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "無法讀取圖片", color = DeepGreen, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        var fallbackBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

        if (fallbackBitmap != null) {
            Image(
                bitmap = fallbackBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(imageUri))
                    .crossfade(true)
                    .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    .listener(
                        onError = { _, _ ->
                            fallbackBitmap = runCatching {
                                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { stream ->
                                    BitmapFactory.decodeStream(stream)
                                }
                            }.getOrNull()
                        }
                    )
                    .build(),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun SettingsScene(modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "設定",
                color = ExpressiveInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            SettingsRow("定位模式", "GPS 優先")
            SettingsRow("資料儲存", "本機 Room Database")
            SettingsRow("路線模式", "最佳清掃路線")
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(GlassWhite)
            .border(1.dp, GlassStroke, RoundedCornerShape(32.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ExpressiveInk,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = ExpressiveMuted,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: DashboardTab,
    tabs: List<DashboardTab> = DashboardTab.entries,
    onTabSelected: (DashboardTab) -> Unit
) {
    val density = LocalDensity.current
    val bottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp + bottomPadding)
            .background(Color(0xFFF7FCF9).copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 10.dp,
                    top = 8.dp,
                    end = 10.dp,
                    bottom = 8.dp + bottomPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    label = tab.label,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: DashboardTab,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "bottomNavSelectedProgress"
    )
    val contentColor = if (selected) DeepGreen else ExpressiveMuted.copy(alpha = 0.76f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(0.72f + selectedProgress * 0.28f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MintGreen.copy(alpha = selectedProgress * 0.95f))
                )
                BottomNavIcon(
                    tab = tab,
                    color = contentColor,
                    modifier = Modifier.size(23.dp)
                )
            }
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BottomNavIcon(
    tab: DashboardTab,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.2.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        when (tab) {
            DashboardTab.Home -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.12f, size.height * 0.48f),
                    end = Offset(size.width * 0.5f, size.height * 0.16f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.16f),
                    end = Offset(size.width * 0.88f, size.height * 0.48f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.24f, size.height * 0.45f),
                    size = Size(size.width * 0.52f, size.height * 0.4f),
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            DashboardTab.Status -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.36f,
                    center = center,
                    style = stroke
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(size.width * 0.5f, size.height * 0.27f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(size.width * 0.68f, size.height * 0.6f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            DashboardTab.Settings -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.2f,
                    center = center,
                    style = stroke
                )
                listOf(
                    Offset(size.width * 0.5f, size.height * 0.08f) to Offset(size.width * 0.5f, size.height * 0.22f),
                    Offset(size.width * 0.5f, size.height * 0.78f) to Offset(size.width * 0.5f, size.height * 0.92f),
                    Offset(size.width * 0.08f, size.height * 0.5f) to Offset(size.width * 0.22f, size.height * 0.5f),
                    Offset(size.width * 0.78f, size.height * 0.5f) to Offset(size.width * 0.92f, size.height * 0.5f),
                    Offset(size.width * 0.2f, size.height * 0.2f) to Offset(size.width * 0.3f, size.height * 0.3f),
                    Offset(size.width * 0.7f, size.height * 0.7f) to Offset(size.width * 0.8f, size.height * 0.8f)
                ).forEach { (start, end) ->
                    drawLine(
                        color = color,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun BauhausRouteMap(
    markers: List<PriorityMapMarker>,
    routePoints: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
        modifier = modifier.clip(RoundedCornerShape(28.dp)),
        factory = { mapView },
        update = { view ->
            view.overlays.clear()

            if (markers.isNotEmpty()) {
                view.controller.setCenter(markers.first().point)
                view.controller.setZoom(15.0)

                val routeLine = Polyline().apply {
                    setPoints(routePoints)
                    outlinePaint.color = DeepGreen.toArgb()
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(routeLine)

                markers.forEach { mapMarker ->
                    val marker = Marker(view).apply {
                        position = mapMarker.point
                        icon = createPriorityMarkerIcon(
                            context = context,
                            priorityNumber = mapMarker.priorityNumber,
                            severity = mapMarker.severity
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Priority ${mapMarker.priorityNumber}"
                    }
                    view.overlays.add(marker)
                }
            }

            view.invalidate()
        }
    )
}

private fun buildStops(
    farmlands: List<FarmlandInspectionEntity>,
    points: List<GeoPointData>,
    roadRoute: RoadRouteResult?
): List<CleanStop> {
    if (farmlands.isNotEmpty()) {
        return farmlands.toPrioritySortedFarmlands().take(3).mapIndexed { index, farmland ->
            val priorityNumber = index + 1
            CleanStop(
                farmlandId = farmland.id,
                name = farmland.regionName,
                area = farmland.stopAreaLabel(),
                code = "%02d".format(priorityNumber),
                status = farmland.processStatus.toStopStatus(),
                items = farmland.trashCount,
                priorityNumber = priorityNumber,
                severity = farmland.severity,
                routePoint = farmland.toGeoPointDataOrNull()
            )
        }
    }

    if (points.isEmpty()) {
        return listOf(
            CleanStop(null, "西區花田", "西區", "D1", "清掃中", 2, 1, "嚴重", null),
            CleanStop(null, "北區水田", "北區", "A2", "待處理", 7, 2, "輕微", null),
            CleanStop(null, "東區果園", "東區", "E1", "待處理", 5, 3, "無", null)
        )
    }

    val ordered = roadRoute?.orderedStops ?: sortByNearestNeighbor(points.map { it.toGeoPoint() })
    return ordered.take(8).mapIndexed { index, point ->
        CleanStop(
            farmlandId = null,
            name = "清掃點",
            area = point.toAreaLabel(),
            code = "%02d".format(index + 1),
            status = if (index == 0) "清掃中" else "待處理",
            items = ((point.latitude * 10_000).toInt().mod(7) + 2),
            priorityNumber = index + 1,
            severity = fallbackSeverityForPriority(index + 1),
            routePoint = GeoPointData(point.latitude, point.longitude)
        )
    }
}

private fun List<FarmlandInspectionEntity>.toPrioritySortedFarmlands(): List<FarmlandInspectionEntity> {
    return sortedWith(
        compareBy<FarmlandInspectionEntity> { it.severity.toSeverityRank() }
            .thenBy { it.timestampMillis }
    )
}

private fun FarmlandInspectionEntity.toGeoPointDataOrNull(): GeoPointData? {
    val latitude = latitude ?: return null
    val longitude = longitude ?: return null
    return GeoPointData(latitude, longitude)
}

private fun String.toSeverityRank(): Int {
    return when (this) {
        "嚴重" -> 0
        "輕微" -> 1
        else -> 2
    }
}

private fun String.toPriorityMarkerColor(): Color {
    return when (this) {
        "嚴重" -> WarningRed
        "輕微" -> Color(0xFFF5C84C)
        else -> LeafGreen
    }
}

private fun fallbackSeverityForPriority(priorityNumber: Int): String {
    return when (priorityNumber) {
        1 -> "嚴重"
        2 -> "輕微"
        else -> "無"
    }
}

private fun FarmlandInspectionEntity.stopAreaLabel(): String {
    return when {
        latitude != null && longitude != null -> coordinateLabel()
        else -> "未讀取座標"
    }
}

private fun String.toStopStatus(): String {
    return when (this) {
        "已處理" -> "已處理"
        "清掃中" -> "清掃中"
        else -> "待處理"
    }
}

private fun String.toStatusColors(): Pair<Color, Color> {
    return when (this) {
        "已處理", "清掃中" -> MintGreen to DeepGreen
        else -> Color(0xFFFFF2DA) to Color(0xFFE59A23)
    }
}

private fun generateTrashCount(regionName: String): Int {
    return kotlin.math.abs(regionName.hashCode()).mod(9) + 1
}

private fun severityForTrashCount(trashCount: Int): String {
    return when {
        trashCount >= 7 -> "嚴重"
        trashCount >= 3 -> "輕微"
        else -> "無"
    }
}

private fun buildAiInspectionDescription(
    regionName: String,
    trashCount: Int,
    point: GeoPointData?
): String {
    val gpsText = if (point == null) {
        "圖片未包含可讀取的 GPS 座標。"
    } else {
        "已讀取 HEIF/圖片座標，位置約為 %.4f, %.4f。".format(point.latitude, point.longitude)
    }
    return "$regionName 偵測到 $trashCount 件疑似垃圾。$gpsText 建議安排無人機複查並依嚴重度排程清理。"
}

private fun FarmlandInspectionEntity.coordinateLabel(): String {
    return if (latitude == null || longitude == null) {
        "無法讀取"
    } else {
        "%.4f, %.4f".format(latitude, longitude)
    }
}

private fun persistImageReadPermission(
    context: Context,
    uri: Uri
) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private fun FarmlandInspectionEntity.severityColor(): Color {
    return when (severity) {
        "嚴重" -> WarningRed
        "輕微" -> Color(0xFFE59A23)
        else -> DeepGreen
    }
}

private fun FarmlandInspectionEntity.severityIcon(): String {
    return when (severity) {
        "嚴重" -> "!"
        "輕微" -> "△"
        else -> "✓"
    }
}

private fun Long.toDateLabel(): String {
    return SimpleDateFormat("MM/dd", Locale.TAIWAN).format(Date(this))
}

private fun Long.toDateTimeLabel(): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN).format(Date(this))
}

private fun recentDemoPoints(): List<GeoPointData> {
    return listOf(
        GeoPointData(latitude = 25.033964, longitude = 121.564468),
        GeoPointData(latitude = 25.03981, longitude = 121.55791)
    )
}

private fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun Context.awaitDeviceLocation(): Location? {
    if (!hasLocationPermission()) return null
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null

    val activeLocation = withTimeoutOrNull(8_000) {
        locationManager.awaitSingleUpdate(this@awaitDeviceLocation, LocationManager.GPS_PROVIDER)
            ?: locationManager.awaitSingleUpdate(this@awaitDeviceLocation, LocationManager.NETWORK_PROVIDER)
    }
    if (activeLocation != null) return activeLocation

    return getLastKnownDeviceLocation(locationManager)
}

private suspend fun LocationManager.awaitSingleUpdate(
    context: Context,
    provider: String
): Location? {
    if (!context.hasPermissionForProvider(provider) || !isProviderUsable(provider)) return null

    return suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                removeUpdates(this)
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }

            override fun onProviderDisabled(disabledProvider: String) {
                if (disabledProvider == provider) {
                    removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }

        try {
            requestSingleUpdate(provider, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation {
                removeUpdates(listener)
            }
        } catch (exception: SecurityException) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        } catch (exception: IllegalArgumentException) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}

private fun LocationManager.isProviderUsable(provider: String): Boolean {
    return try {
        allProviders.contains(provider) && isProviderEnabled(provider)
    } catch (exception: Exception) {
        false
    }
}

private fun Context.getLastKnownDeviceLocation(locationManager: LocationManager): Location? {
    return listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    ).mapNotNull { provider ->
        if (!hasPermissionForProvider(provider)) {
            null
        } else {
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (exception: SecurityException) {
                null
            }
        }
    }.maxByOrNull { it.time }
}

private fun Context.hasPermissionForProvider(provider: String): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return when (provider) {
        LocationManager.GPS_PROVIDER -> hasFineLocation
        else -> hasFineLocation || hasCoarseLocation
    }
}

private fun createLocalWeather(
    location: Location?,
    address: String? = null
): WeatherSnapshot {
    val latitude = location?.latitude ?: 25.033
    val longitude = location?.longitude ?: 121.565
    val addressSeed = address.orEmpty().fold(0) { total, char -> total + char.code }
    val seed = kotlin.math.abs(((latitude * 100) + (longitude * 10)).toInt() + addressSeed)
    val condition = when (seed % 4) {
        0 -> "晴時多雲"
        1 -> "多雲"
        2 -> "小雨"
        else -> "陰天"
    }
    val wind = 8 + seed % 18
    val humidity = 58 + seed % 35
    val rainChance = if (condition.contains("雨")) 72 else 22 + seed % 28
    val temperature = 22 + seed % 9

    return WeatherSnapshot(
        locationLabel = when {
            !address.isNullOrBlank() -> "${address.trim()} · 本地天氣"
            location == null -> "預設位置 · 中央氣象署"
            else -> "目前位置 %.3f, %.3f".format(latitude, longitude)
        },
        condition = condition,
        temperatureC = temperature,
        humidityPercent = humidity,
        windKmh = wind,
        rainChancePercent = rainChance,
        isGpsBased = location != null
    )
}

private suspend fun createWeatherSnapshotFromAddress(address: String): WeatherSnapshot {
    val fallback = createLocalWeather(location = null, address = address)
    val cwaWeather = CwaWeatherService.fetchWeather(address) ?: return fallback

    return WeatherSnapshot(
        locationLabel = "${address.trim()} · ${cwaWeather.locationName}",
        condition = cwaWeather.condition,
        temperatureC = cwaWeather.temperatureC,
        humidityPercent = fallback.humidityPercent,
        windKmh = fallback.windKmh,
        rainChancePercent = cwaWeather.rainChancePercent,
        isGpsBased = false
    )
}

private suspend fun createWeatherSnapshot(location: Location?): WeatherSnapshot {
    val fallback = createLocalWeather(location)
    if (location == null) return fallback

    val cwaWeather = CwaWeatherService.fetchWeather(
        latitude = location.latitude,
        longitude = location.longitude
    ) ?: return fallback

    val wind = fallback.windKmh
    return WeatherSnapshot(
        locationLabel = "${cwaWeather.locationName} · 中央氣象署",
        condition = cwaWeather.condition,
        temperatureC = cwaWeather.temperatureC,
        humidityPercent = fallback.humidityPercent,
        windKmh = wind,
        rainChancePercent = cwaWeather.rainChancePercent,
        isGpsBased = true
    )
}

private fun WeatherSnapshot.toFlightAdvice(): FlightAdvice {
    val unsafe = condition.contains("雨") || windKmh >= 20 || rainChancePercent >= 60
    return if (unsafe) {
        FlightAdvice(
            title = "不宜飛行",
            detail = "雨勢或風速偏高",
            iconText = "!",
            containerColor = Color(0xFFFFEFF1),
            contentColor = WarningRed
        )
    } else {
        FlightAdvice(
            title = "建議飛行",
            detail = "天氣穩定",
            iconText = "✓",
            containerColor = MintGreen,
            contentColor = DeepGreen
        )
    }
}

private fun GeoPoint.toAreaLabel(): String {
    return when {
        latitude >= 25.1 -> "北區"
        latitude <= 24.95 -> "南區"
        longitude >= 121.55 -> "東區"
        else -> "西區"
    }
}

private fun routeStatusLabel(
    isRouting: Boolean,
    roadRoute: RoadRouteResult?
): String {
    return when {
        isRouting -> "規劃中"
        roadRoute == null -> "3 處"
        roadRoute.isFallback -> "備援"
        else -> roadRoute.distanceMeters.toKilometerLabel()
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

private fun createPriorityMarkerIcon(
    context: Context,
    priorityNumber: Int,
    severity: String
): BitmapDrawable {
    val size = 58
    val center = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val markerColor = severity.toPriorityMarkerColor()

    paint.style = Paint.Style.FILL
    paint.color = Color.Transparent.toArgb()
    canvas.drawCircle(center, center, center, paint)

    paint.color = Color.White.toArgb()
    canvas.drawCircle(center, center, 25f, paint)

    paint.color = markerColor.toArgb()
    canvas.drawCircle(center, center, 21f, paint)

    paint.color = if (severity == "輕微") ExpressiveInk.toArgb() else Color.White.toArgb()
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 25f
    val textY = center - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(priorityNumber.toString(), center, textY, paint)

    return BitmapDrawable(context.resources, bitmap)
}

@Preview(showBackground = true)
@Composable
fun BauhausPreview() {
    BauhausTheme {
        GlassAppBackground {
            RouteDiscoveryScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            )
        }
    }
}
