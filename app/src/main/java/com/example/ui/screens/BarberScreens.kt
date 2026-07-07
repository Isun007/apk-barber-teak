package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarberViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

// Modern bounce click scale animation for buttons and cards
@Composable
fun Modifier.clickableWithBounce(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}



@Composable
fun BarberAppContent(viewModel: BarberViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeRole by viewModel.currentActiveRole.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("home") } // "home", "book_store", "book_home", "catalog", "capsters", "ask_ai", "history", "complaints"
    var selectedCapsterIdForBooking by remember { mutableStateOf<Int?>(null) }
    var isAiBubbleOpen by remember { mutableStateOf(false) }

    // Intercept ask_ai and open floating bubble instead of navigating away
    LaunchedEffect(currentScreen) {
        if (currentScreen == "ask_ai") {
            isAiBubbleOpen = true
            currentScreen = "home"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentUser == null) {
                LoginScreen(
                    viewModel = viewModel,
                    onLogin = { email, name, phone ->
                        viewModel.loginUser(email, name, phone)
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        BarberTopBar(
                            user = currentUser,
                            activeRole = activeRole,
                            currentScreen = currentScreen,
                            onBackClick = { currentScreen = "home" },
                            onLogout = {
                                viewModel.logout()
                                currentScreen = "home"
                            }
                        )
                    },
                    modifier = Modifier.testTag("app_scaffold")
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Routing screens based on activeRole and currentScreen
                        when (activeRole) {
                            "ADMIN" -> {
                                AdminDashboardScreen(viewModel = viewModel)
                            }
                            "CAPSTER" -> {
                                CapsterDashboardScreen(viewModel = viewModel)
                            }
                            else -> {
                                // Client Flows
                                when (currentScreen) {
                                    "home" -> ClientHomeScreen(
                                        viewModel = viewModel,
                                        onNavigate = { screen, capsterId ->
                                            if (screen == "ask_ai") {
                                                isAiBubbleOpen = true
                                            } else {
                                                currentScreen = screen
                                                selectedCapsterIdForBooking = capsterId
                                            }
                                        }
                                    )
                                    "book_store" -> BookingFormScreen(
                                        viewModel = viewModel,
                                        isHomeService = false,
                                        preselectedCapsterId = selectedCapsterIdForBooking,
                                        onSuccess = { currentScreen = "history" },
                                        onCancel = { currentScreen = "home" }
                                    )
                                    "book_home" -> BookingFormScreen(
                                        viewModel = viewModel,
                                        isHomeService = true,
                                        preselectedCapsterId = selectedCapsterIdForBooking,
                                        onSuccess = { currentScreen = "history" },
                                        onCancel = { currentScreen = "home" }
                                    )
                                    "catalog" -> ProductCatalogScreen(viewModel = viewModel)
                                    "capsters" -> CapstersListScreen(
                                        viewModel = viewModel,
                                        onSelectCapsterForBooking = { id, type ->
                                            selectedCapsterIdForBooking = id
                                            currentScreen = if (type == "HOME") "book_home" else "book_store"
                                        }
                                    )
                                    "ask_ai" -> AskAiScreen(viewModel = viewModel)
                                    "history" -> BookingHistoryScreen(viewModel = viewModel)
                                    "complaints" -> ComplaintsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }

                // Modern Floating AI Chat Bubble
                if (activeRole == "CLIENT") {
                    FloatingAiBubble(
                        viewModel = viewModel,
                        isOpen = isAiBubbleOpen,
                        onToggle = { isAiBubbleOpen = !isAiBubbleOpen }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// COMPONENTS: REUSABLE HEADERS, ROLE SELECTORS, ETC
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberTopBar(
    user: UserEntity?,
    activeRole: String,
    currentScreen: String,
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Logo",
                    tint = TeakGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "BARBERTEAK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp
                    ),
                    color = TeakGold
                )
            }
        },
        navigationIcon = {
            if (activeRole == "CLIENT" && currentScreen != "home") {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = when (activeRole) {
                        "ADMIN" -> "ADMIN MODE"
                        "CAPSTER" -> "CAPSTER MODE"
                        else -> "PELANGGAN"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TeakHoney
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Keluar",
                    tint = StatusRed
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun RoleSelectorBar(
    activeRole: String,
    onRoleSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = "EVALUASI MULTI-ROLE (Pilih peran untuk review instan):",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = TeakHoney,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val roles = listOf(
                "CLIENT" to "1. Pelanggan",
                "CAPSTER" to "2. Capster (Budi)",
                "ADMIN" to "3. Admin Utama"
            )
            roles.forEach { (roleKey, label) ->
                val isSelected = activeRole == roleKey
                Button(
                    onClick = { onRoleSelected(roleKey) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) TeakWoodPrimary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("role_btn_$roleKey"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// LOGIN SCREEN
// ---------------------------------------------------------------------------------

@Composable
fun LoginScreen(
    viewModel: BarberViewModel? = null,
    onLogin: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var email by remember { mutableStateOf("yudhaactaffian007@gmail.com") }
    var name by remember { mutableStateOf("Yudha Actaffian") }
    var phone by remember { mutableStateOf("08556677889") }
    var password by remember { mutableStateOf("123456") }
    var isNewUser by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val authError = viewModel?.authError?.collectAsStateWithLifecycle()?.value
    val unregisteredGmail = viewModel?.unregisteredGmail?.collectAsStateWithLifecycle()?.value

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(BackgroundDark, Color(0xFF1E1712))
    )

    // Clear error states on switch between login/register
    LaunchedEffect(isNewUser) {
        viewModel?.clearAuthErrors()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Hero Branding
        Card(
            modifier = Modifier
                .size(110.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Barberteak",
                    tint = TeakGold,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Text(
            text = "BARBERTEAK",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 4.sp
            ),
            color = TeakGold
        )
        Text(
            text = "Layanan Barber On-Demand & Home Service Modern",
            style = MaterialTheme.typography.bodySmall,
            color = OnBackgroundDark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Tampilan jika akun gmail belum terdaftar
        if (unregisteredGmail != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Unregistered",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Akun Gmail Belum Terdaftar!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "Email \"$unregisteredGmail\" belum terdaftar di sistem Barberteak. Silakan buat akun baru dengan mengklik tombol daftar di bawah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isNewUser = true
                                email = unregisteredGmail
                                viewModel.clearAuthErrors()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Daftar Sekarang", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearAuthErrors() },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        // 2. Notifikasi Gagal (Kolom Kosong / Password atau Email salah)
        if (authError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel?.clearAuthErrors() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isNewUser) "Daftar Akun Baru" else "Masuk Aplikasi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackgroundDark
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        viewModel?.clearAuthErrors()
                    },
                    label = { Text("Email (Gmail)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeakGold,
                        focusedLabelColor = TeakGold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = TeakGold)
                    }
                )

                if (isNewUser) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            viewModel?.clearAuthErrors()
                        },
                        label = { Text("Nama Lengkap") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeakGold,
                            focusedLabelColor = TeakGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_name_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TeakGold)
                        }
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { 
                            phone = it
                            viewModel?.clearAuthErrors()
                        },
                        label = { Text("Nomor HP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeakGold,
                            focusedLabelColor = TeakGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = TeakGold)
                        }
                    )
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        viewModel?.clearAuthErrors()
                    },
                    label = { Text("Kata Sandi") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeakGold,
                        focusedLabelColor = TeakGold
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TeakGold)
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = TeakGold)
                        }
                    }
                )

                if (!isNewUser) {
                    Text(
                        text = "*Gunakan kata sandi \"123456\" untuk demo/quick accounts.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TeakHoney
                    )
                }

                Button(
                    onClick = {
                        if (viewModel != null) {
                            viewModel.loginUserWithValidation(email, password, isNewUser, name, phone)
                        } else {
                            onLogin(email, if (isNewUser) name else "", if (isNewUser) phone else "")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_submit_button")
                ) {
                    Text(
                        text = if (isNewUser) "Daftar" else "Masuk",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                // Switch Sign in / Sign up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNewUser) "Sudah punya akun?" else "Pengguna baru?",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackgroundDark.copy(alpha = 0.6f)
                    )
                    TextButton(onClick = { isNewUser = !isNewUser }) {
                        Text(
                            text = if (isNewUser) "Masuk di sini" else "Daftar di sini",
                            color = TeakGold,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Predefined Quick Accounts Selector (STELAR UX FOR DEMO EVALUATION)
        Text(
            text = "DEMO QUICK LOGIN (Pilih email khusus):",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TeakHoney,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val specialAccounts = listOf(
                Triple("yudhaactaffian007@gmail.com", "Client (VIP)", "vip_btn"),
                Triple("capster@barberteak.com", "Capster Budi", "capster_btn"),
                Triple("admin@barberteak.com", "Admin Utama", "admin_btn")
            )
            specialAccounts.forEach { (specEmail, label, tag) ->
                Button(
                    onClick = {
                        email = specEmail
                        password = "123456"
                        isNewUser = false
                        if (viewModel != null) {
                            viewModel.loginUserWithValidation(specEmail, "123456", false, "", "")
                        } else {
                            onLogin(specEmail, "", "")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("quick_$tag"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TeakGold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT HOME SCREEN (MOBILE JKN STYLE)
// ---------------------------------------------------------------------------------

@Composable
fun ClientHomeScreen(
    viewModel: BarberViewModel,
    onNavigate: (String, Int?) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val reservations by viewModel.userReservations.collectAsStateWithLifecycle()

    val activeReservation = reservations.firstOrNull {
        it.status != "COMPLETED" && it.status != "REJECTED"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // GREETING & MEMBERSHIP CARD (Mobile JKN Style)
        item {
            ClientMembershipCard(user = currentUser)
        }

        // ACTIVE QUEUE WIDGET (If booking exists)
        if (activeReservation != null) {
            item {
                ActiveQueueWidget(
                    reservation = activeReservation,
                    onClick = { onNavigate("history", null) }
                )
            }
        }

        // SERVICE GRID MENU
        item {
            Text(
                text = "Menu Layanan Utama",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceMenuItem(
                    title = "Reservasi Toko",
                    icon = Icons.Default.Store,
                    color = TeakWoodPrimary,
                    description = "Antre di toko",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("book_store", null) }
                )
                ServiceMenuItem(
                    title = "Home Service",
                    icon = Icons.Default.DirectionsCar,
                    color = TeakGold,
                    description = "Panggil ke rumah",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("book_home", null) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceMenuItem(
                    title = "Katalog Produk",
                    icon = Icons.Default.ShoppingBag,
                    color = TeakHoney,
                    description = "Pomade & Hair Tonic",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("catalog", null) }
                )
                ServiceMenuItem(
                    title = "Pilih Capster",
                    icon = Icons.Default.ContentCut,
                    color = Color(0xFF6D4C41),
                    description = "Daftar pemotong",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("capsters", null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceMenuItem(
                    title = "Tanya AI Barber",
                    icon = Icons.Default.AutoAwesome,
                    color = StatusBlue,
                    description = "Saran gaya & rambut",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("ask_ai", null) }
                )
                ServiceMenuItem(
                    title = "Riwayat Reservasi",
                    icon = Icons.Default.History,
                    color = StatusGreen,
                    description = "Cek status booking",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("history", null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceMenuItem(
                    title = "Pengaduan & Kritik",
                    icon = Icons.Default.Feedback,
                    color = StatusAmber,
                    description = "Kotak kritik/saran demi peningkatan kualitas layanan kami",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("complaints", null) }
                )
            }
        }

        // HERO BANNER SHOWCASE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_barber_hero),
                        contentDescription = "Barber Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Text(
                        text = "Barberteak: Potongan Rapi, Tepat Waktu.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClientMembershipCard(user: UserEntity?) {
    val brush = Brush.linearGradient(
        colors = listOf(TeakWoodDark, TeakWoodPrimary, Color(0xFF5D4037)),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(20.dp)
        ) {
            // Background branding lines
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawLine(
                            color = TeakGold.copy(alpha = 0.1f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 10f
                        )
                    }
            )

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KARTU ANGGOTA VIRTUAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = TeakGold
                        )
                        Text(
                            text = "BARBERTEAK LUXURY CLUB",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = Color.White
                        )
                    }
                    // Tier Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TeakGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = user?.membershipTier?.uppercase() ?: "BRONZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "NAMA ANGGOTA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = user?.name ?: "PELANGGAN",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = user?.phone ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // QR Code icon representing virtual JKN card style
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Card",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveQueueWidget(
    reservation: ReservationEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableWithBounce(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(TeakGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reservation.serviceType == "HOME") Icons.Default.DirectionsCar else Icons.Default.Store,
                    contentDescription = "Tipe",
                    tint = TeakGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ANTREAN AKTIF: ${reservation.queueNo ?: "-"}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TeakHoney
                )
                Text(
                    text = "${reservation.serviceName} dengan ${reservation.capsterName}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (reservation.status) {
                                "PENDING" -> StatusAmber.copy(alpha = 0.2f)
                                "APPROVED" -> StatusGreen.copy(alpha = 0.2f)
                                "ON_THE_WAY" -> StatusBlue.copy(alpha = 0.2f)
                                "IN_PROGRESS" -> TeakWoodPrimary.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = reservation.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (reservation.status) {
                                    "PENDING" -> StatusAmber
                                    "APPROVED" -> StatusGreen
                                    "ON_THE_WAY" -> StatusBlue
                                    "IN_PROGRESS" -> TeakHoney
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint = TeakGold
            )
        }
    }
}

@Composable
fun ServiceMenuItem(
    title: String,
    icon: ImageVector,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickableWithBounce(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: BOOKING FORM
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    viewModel: BarberViewModel,
    isHomeService: Boolean,
    preselectedCapsterId: Int?,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val capsters by viewModel.allCapsters.collectAsStateWithLifecycle()
    val availableCapsters = capsters.filter { !isHomeService || it.supportsHomeService }

    var selectedCapster by remember {
        mutableStateOf(availableCapsters.find { it.id == preselectedCapsterId } ?: availableCapsters.firstOrNull())
    }

    // Preselected services
    val services = listOf(
        Pair("Gentleman Classic Haircut", 60000.0),
        Pair("Premium Cut + Wash + Styling", 90000.0),
        Pair("Hair Coloring (Golden/Brown/Matte)", 120000.0),
        Pair("Beard Shave & Hot Towel Massage", 45000.0),
        Pair("Full Package Royal Treatment", 150000.0)
    )

    var selectedService by remember { mutableStateOf(services.first()) }
    var address by remember { mutableStateOf("") }
    var dateSelection by remember { mutableStateOf("Besok (Rabu, 8 Jul 2026)") }
    var timeSelection by remember { mutableStateOf("10:00 WIB") }

    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isHomeService) "Form On-Demand Home Service" else "Form Reservasi Antrean Toko",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = if (isHomeService) "Capster kami akan datang ke alamat Anda lengkap dengan peralatan cukur steril." 
                   else "Sistem antrean digital akan memberi Anda nomor antrean setelah reservasi disetujui.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Select Service Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Jenis Layanan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                services.forEach { service ->
                    val isSelected = selectedService.first == service.first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableWithBounce { selectedService = service }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedService = service },
                            colors = RadioButtonDefaults.colors(selectedColor = TeakWoodPrimary)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = service.first,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = formatCurrency.format(service.second),
                                style = MaterialTheme.typography.bodySmall,
                                color = TeakHoney
                            )
                        }
                    }
                }
            }
        }

        // Select Capster Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Capster / Barber",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableCapsters) { capster ->
                        val isSelected = selectedCapster?.id == capster.id
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickableWithBounce { selectedCapster = capster },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TeakWoodPrimary.copy(alpha = 0.15f) 
                                                 else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TeakWoodPrimary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(TeakWoodPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Capster",
                                        tint = TeakWoodPrimary
                                    )
                                }
                                Text(
                                    text = capster.name.split(" ")[0],
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Text(
                                    text = "⭐ ${capster.rating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TeakHoney
                                )
                                Text(
                                    text = capster.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = if (capster.status == "Available") StatusGreen else StatusAmber
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Home Service Address if needed
        if (isHomeService) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Alamat Lengkap Kunjungan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("Tulis alamat rumah lengkap Anda...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("home_address_input")
                    )
                }
            }
        }

        // Schedule Picker Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pilih Waktu Kunjungan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Mock schedules
                val dates = listOf("Hari Ini (Selasa, 7 Jul 2026)", "Besok (Rabu, 8 Jul 2026)", "Kamis, 9 Jul 2026")
                val times = listOf("09:00 WIB", "10:30 WIB", "13:00 WIB", "15:00 WIB", "17:30 WIB")

                Text(text = "Tanggal:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(dates) { date ->
                        val isSel = dateSelection == date
                        FilterChip(
                            selected = isSel,
                            onClick = { dateSelection = date },
                            label = { Text(date, fontSize = 11.sp) }
                        )
                    }
                }

                Text(text = "Jam Mulai:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(times) { time ->
                        val isSel = timeSelection == time
                        FilterChip(
                            selected = isSel,
                            onClick = { timeSelection = time },
                            label = { Text(time, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Summary Price
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Biaya Layanan",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatCurrency.format(
                            selectedService.second + (if (isHomeService) 20000.0 else 0.0) // Add Home Service transport fee
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TeakHoney
                    )
                    if (isHomeService) {
                        Text(
                            text = "(Termasuk biaya transport Rp 20.000)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = {
                            selectedCapster?.let {
                                viewModel.createReservation(
                                    capsterId = it.id,
                                    capsterName = it.name,
                                    serviceName = selectedService.first,
                                    servicePrice = selectedService.second + (if (isHomeService) 20000.0 else 0.0),
                                    serviceType = if (isHomeService) "HOME" else "STORE",
                                    homeAddress = if (isHomeService) address else null,
                                    date = dateSelection,
                                    time = timeSelection
                                )
                                onSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                        enabled = selectedCapster != null && (!isHomeService || address.isNotEmpty()),
                        modifier = Modifier.testTag("confirm_booking_button")
                    ) {
                        Text("Konfirmasi Booking", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onCancel) {
                        Text("Batalkan", color = StatusRed)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: PRODUCTS CATALOG
// ---------------------------------------------------------------------------------

@Composable
fun ProductCatalogScreen(viewModel: BarberViewModel) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Katalog Produk Barberteak",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = "Dapatkan produk perawatan rambut dan pomade berkualitas salon profesional langsung dari genggaman Anda.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Product image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl == "img_hair_pomade") {
                                Image(
                                    painter = painterResource(id = R.drawable.img_hair_pomade),
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(TeakWoodPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Produk",
                                        tint = TeakWoodPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = product.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TeakHoney
                            )
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "⭐ ${product.rating}  •  Stok: ${product.stock}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatCurrency.format(product.price),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TeakWoodPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.buyProduct(product)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                shape = RoundedCornerShape(8.dp),
                                enabled = product.stock > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("buy_${product.id}"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (product.stock > 0) "Beli Langsung" else "Habis",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: CAPSTERS LIST
// ---------------------------------------------------------------------------------

@Composable
fun CapstersListScreen(
    viewModel: BarberViewModel,
    onSelectCapsterForBooking: (Int, String) -> Unit
) {
    val capsters by viewModel.allCapsters.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Daftar Capster Ahli Barberteak",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = "Pilih penata rambut profesional andalan Anda untuk mendapatkan model potongan yang paling sesuai.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(capsters) { capster ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(TeakWoodPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = capster.name,
                                tint = TeakWoodPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = capster.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (capster.status == "Available") StatusGreen.copy(alpha = 0.15f) 
                                                         else StatusAmber.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = capster.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (capster.status == "Available") StatusGreen else StatusAmber
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Keahlian: ${capster.specialties}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Pengalaman: ${capster.experience}  •  ⭐ ${capster.rating} (${capster.reviewsCount} ulasan)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSelectCapsterForBooking(capster.id, "STORE") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Booking Toko", style = MaterialTheme.typography.labelSmall)
                                }

                                if (capster.supportsHomeService) {
                                    Button(
                                        onClick = { onSelectCapsterForBooking(capster.id, "HOME") },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeakGold),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Home Service", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: TANYA AI BARBER
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiScreen(viewModel: BarberViewModel) {
    var query by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<Pair<String, Boolean>>() } // Pair(message, isUser)
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Add initial welcome message if empty
    LaunchedEffect(Unit) {
        if (chatHistory.isEmpty()) {
            chatHistory.add(
                "Halo! Saya AI Barberteak, asisten kecerdasan buatan khusus Barberteak. ✂️\n\nAda yang bisa saya bantu? Saya dapat membantu Anda mengecek ketersediaan & jadwal capster, menginformasikan katalog produk, menjelaskan daftar paket layanan, serta memberikan saran gaya rambut pria yang cocok untuk Anda!" to false
            )
        }
    }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty() || isLoading) return
        chatHistory.add(text to true)
        query = ""
        isLoading = true

        coroutineScope.launch {
            val response = com.example.data.remote.GeminiHelper.askGemini(text)
            chatHistory.add(response to false)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Tanya AI Barberteak",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                color = TeakGold
            )
            Text(
                text = "Konsultasi gaya rambut & perawatan pria secara langsung didukung kecerdasan buatan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Suggestions when there is only the welcome message
        if (chatHistory.size == 1) {
            Text(
                text = "Rekomendasi Pertanyaan Cepat:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TeakHoney)
            )
            val suggestions = listOf(
                "Siapa saja capster yang tersedia saat ini?",
                "Berapa harga paket Royal Treatment & isinya apa saja?",
                "Apa keunggulan Teak & Clay Pomade Premium dan harganya?",
                "Rekomendasi model rambut yang cocok untuk wajah bulat?"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions) { suggestion ->
                    Card(
                        modifier = Modifier
                            .clickableWithBounce { sendMessage(suggestion) }
                            .testTag("ai_suggestion_${suggestion.take(10)}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatHistory) { (message, isUser) ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 2.dp,
                                bottomEnd = if (isUser) 2.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) TeakWoodPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Card(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.widthIn(max = 200.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = TeakGold
                                    )
                                    Text(
                                        text = "AI sedang mengetik...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Tanyakan rekomendasi rambut Anda...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeakGold,
                    focusedLabelColor = TeakGold
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            IconButton(
                onClick = { sendMessage(query) },
                enabled = query.trim().isNotEmpty() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (query.trim().isNotEmpty() && !isLoading) TeakWoodPrimary else MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (query.trim().isNotEmpty() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: BOOKING HISTORY / STATUS TRACKING
// ---------------------------------------------------------------------------------

@Composable
fun BookingHistoryScreen(viewModel: BarberViewModel) {
    val reservations by viewModel.userReservations.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Daftar Reservasi & Antrean Anda",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )

        if (reservations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Empty",
                        tint = TeakWoodLight.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Belum Ada Riwayat Reservasi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Gunakan tombol Reservasi Toko atau Home Service untuk mulai potong rambut.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(reservations) { reservation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = if (reservation.serviceType == "HOME") Icons.Default.DirectionsCar else Icons.Default.Store,
                                        contentDescription = "Tipe",
                                        tint = TeakWoodPrimary
                                    )
                                    Text(
                                        text = if (reservation.serviceType == "HOME") "HOME SERVICE" else "DI TOKO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (reservation.status) {
                                            "PENDING" -> StatusAmber.copy(alpha = 0.15f)
                                            "APPROVED" -> StatusGreen.copy(alpha = 0.15f)
                                            "ON_THE_WAY" -> StatusBlue.copy(alpha = 0.15f)
                                            "IN_PROGRESS" -> TeakWoodPrimary.copy(alpha = 0.15f)
                                            "COMPLETED" -> StatusGreen.copy(alpha = 0.15f)
                                            else -> StatusRed.copy(alpha = 0.15f)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = reservation.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (reservation.status) {
                                                "PENDING" -> StatusAmber
                                                "APPROVED" -> StatusGreen
                                                "ON_THE_WAY" -> StatusBlue
                                                "IN_PROGRESS" -> TeakHoney
                                                "COMPLETED" -> StatusGreen
                                                else -> StatusRed
                                            }
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                            Text(
                                text = reservation.serviceName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Text(
                                text = "Capster: ${reservation.capsterName}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Jadwal: ${reservation.date} • ${reservation.time}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            if (reservation.serviceType == "HOME" && reservation.homeAddress != null) {
                                Text(
                                    text = "Alamat Kunjungan: ${reservation.homeAddress}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Harga Layanan",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = formatCurrency.format(reservation.servicePrice),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney
                                    )
                                }

                                if (reservation.queueNo != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Tiket Antrean: ${reservation.queueNo}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TeakWoodPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: COMPLAINTS & FEEDBACK (MOBILE JKN STYLE)
// ---------------------------------------------------------------------------------

@Composable
fun ComplaintsScreen(viewModel: BarberViewModel) {
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pusat Layanan Pengaduan & Kritik",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = "Kami sangat menghargai masukan Anda untuk meningkatkan kualitas layanan home service dan in-store Barberteak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kirim Pengaduan Baru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Tuliskan keluhan atau saran Anda di sini...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("complaint_input")
                )

                Button(
                    onClick = {
                        if (feedbackText.trim().isNotEmpty()) {
                            viewModel.addComplaint(feedbackText)
                            feedbackText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = feedbackText.trim().isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("submit_complaint_button")
                ) {
                    Text("Kirim Masukan", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Kritik & Saran Masuk Terbaru",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 10.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(complaints) { complaint ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = complaint.first,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TeakHoney
                            )
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Feedback",
                                tint = TeakWoodLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = complaint.second,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CAPSTER ROLE: DASHBOARD
// ---------------------------------------------------------------------------------

@Composable
fun CapsterDashboardScreen(viewModel: BarberViewModel) {
    val activeCapster by viewModel.activeCapster.collectAsStateWithLifecycle()
    val reservations by viewModel.capsterReservations.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Split into tabs: 0 = Tugas Hari Ini, 1 = Jadwal & Status
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = "Capster",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selamat Bekerja,",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = activeCapster?.name ?: "Capster Barberteak",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Rating Anda: ⭐ ${activeCapster?.rating ?: 4.8f}  • Keahlian: ${activeCapster?.specialties ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TeakGold
                    )
                }
            }
        }

        // Tab Navigation for incoming appointments vs daily schedule and status
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TeakWoodPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = TeakGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tugas Aktif (${reservations.filter { it.status != "COMPLETED" && it.status != "REJECTED" }.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(imageVector = Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Jadwal & Status", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        if (selectedTab == 0) {
            // Active incoming and processing appointments
            val activeTasks = reservations.filter { it.status != "COMPLETED" && it.status != "REJECTED" }
            
            Text(
                text = "Tugas Cukur Aktif (${activeTasks.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (activeTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "No tasks",
                            tint = TeakWoodLight.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada tugas cukur aktif saat ini",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Antrean baru dari pelanggan akan otomatis tampil di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeTasks) { reservation ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = if (reservation.serviceType == "HOME") Icons.Default.DirectionsCar else Icons.Default.Store,
                                            contentDescription = "Layanan",
                                            tint = TeakGold
                                        )
                                        Text(
                                            text = if (reservation.serviceType == "HOME") "HOME SERVICE (KUNJUNGAN)" else "DI TOKO / BARBER",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TeakHoney
                                        )
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = "Antrean: ${reservation.queueNo ?: "-"}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TeakWoodPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                                Text(
                                    text = reservation.serviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Pelanggan:", style = MaterialTheme.typography.labelSmall)
                                        Text(text = reservation.userName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(text = "HP: ${reservation.userPhone}", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Waktu Booking:", style = MaterialTheme.typography.labelSmall)
                                        Text(text = reservation.time, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(text = reservation.date, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (reservation.serviceType == "HOME" && reservation.homeAddress != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "📍 ALAMAT RUMAH PELANGGAN:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TeakWoodPrimary)
                                            Text(text = reservation.homeAddress, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                // Dynamic on-demand actions based on booking status
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status Tugas: ${reservation.status}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney,
                                        modifier = Modifier.weight(1f)
                                    )

                                    when (reservation.status) {
                                        "PENDING" -> {
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "APPROVED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Terima Tugas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "REJECTED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Tolak", fontSize = 11.sp)
                                            }
                                        }
                                        "APPROVED" -> {
                                            Button(
                                                onClick = {
                                                    val nextStatus = if (reservation.serviceType == "HOME") "ON_THE_WAY" else "IN_PROGRESS"
                                                    viewModel.updateReservationStatus(reservation.id, nextStatus)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("start_trip_${reservation.id}")
                                            ) {
                                                Text(
                                                    text = if (reservation.serviceType == "HOME") "Mulai Perjalanan (Otw)" else "Mulai Cukur",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        "ON_THE_WAY" -> {
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("start_cut_${reservation.id}")
                                            ) {
                                                Text("Tiba & Mulai Cukur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "IN_PROGRESS" -> {
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "COMPLETED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("finish_cut_${reservation.id}")
                                            ) {
                                                Text("Selesai & Bayar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Schedule & Status update view
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. Update Availability Status Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Circle, contentDescription = null, tint = TeakGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Status Ketersediaan Anda",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                
                                val statusText = activeCapster?.status ?: "Available"
                                val statusColor = when (statusText) {
                                    "Available" -> StatusGreen
                                    "Busy" -> StatusAmber
                                    else -> StatusRed
                                }
                                Card(colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f))) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Ubah status ketersediaan Anda agar pelanggan mengetahui apakah Anda dapat dipesan untuk reservasi toko atau kunjungan rumah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statuses = listOf(
                                    "Available" to StatusGreen,
                                    "Busy" to StatusAmber,
                                    "Off" to StatusRed
                                )
                                statuses.forEach { (status, color) ->
                                    val isSelected = activeCapster?.status == status
                                    Button(
                                        onClick = { viewModel.updateActiveCapsterStatus(status) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = if (!isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Schedule timeline & completed tasks
                item {
                    Text(
                        text = "Jadwal Tugas Harian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val dailySchedule = reservations
                if (dailySchedule.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada jadwal tugas tercatat untuk Anda.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(dailySchedule) { reservation ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (reservation.status == "COMPLETED") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                                    Text(text = reservation.time, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TeakHoney))
                                    Text(text = reservation.date.take(6), style = MaterialTheme.typography.labelSmall)
                                }
                                
                                Divider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = reservation.serviceName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "Pelanggan: ${reservation.userName}", style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val isHome = reservation.serviceType == "HOME"
                                        Icon(
                                            imageVector = if (isHome) Icons.Default.DirectionsCar else Icons.Default.Store,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = TeakGold
                                        )
                                        Text(text = if (isHome) "Home Service" else "In-Store", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val color = when (reservation.status) {
                                        "COMPLETED" -> StatusGreen
                                        "PENDING" -> StatusAmber
                                        "REJECTED" -> StatusRed
                                        else -> StatusBlue
                                    }
                                    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
                                        Text(
                                            text = reservation.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color, fontSize = 9.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(text = formatCurrency.format(reservation.servicePrice), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// ADMIN ROLE: DASHBOARD
// ---------------------------------------------------------------------------------

@Composable
fun AdminDashboardScreen(viewModel: BarberViewModel) {
    val reservations by viewModel.allReservations.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DASHBOARD ANALISIS KEUANGAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TeakHoney
                )

                val totalEarnings = reservations.filter { it.status == "COMPLETED" }.sumOf { it.servicePrice }
                Text(
                    text = formatCurrency.format(totalEarnings),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TeakGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Transaksi", style = MaterialTheme.typography.labelSmall)
                        Text("${reservations.size} Pesanan", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Home Service", style = MaterialTheme.typography.labelSmall)
                        val homeCount = reservations.count { it.serviceType == "HOME" }
                        Text("$homeCount Panggilan", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Master Booking List
        Text(
            text = "Semua Riwayat Reservasi Masuk",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(reservations) { reservation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ORDER ID: #${reservation.id}",
                                fontWeight = FontWeight.Bold,
                                color = TeakGold,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (reservation.status) {
                                        "PENDING" -> StatusAmber.copy(alpha = 0.15f)
                                        "APPROVED" -> StatusGreen.copy(alpha = 0.15f)
                                        "ON_THE_WAY" -> StatusBlue.copy(alpha = 0.15f)
                                        "IN_PROGRESS" -> TeakWoodPrimary.copy(alpha = 0.15f)
                                        "COMPLETED" -> StatusGreen.copy(alpha = 0.15f)
                                        else -> StatusRed.copy(alpha = 0.15f)
                                    }
                                )
                            ) {
                                Text(
                                    text = reservation.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when (reservation.status) {
                                            "PENDING" -> StatusAmber
                                            "APPROVED" -> StatusGreen
                                            "ON_THE_WAY" -> StatusBlue
                                            "IN_PROGRESS" -> TeakHoney
                                            "COMPLETED" -> StatusGreen
                                            else -> StatusRed
                                        }
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        Text(
                            text = "${reservation.serviceName} (${if (reservation.serviceType == "HOME") "Home Service" else "Store"})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Pelanggan:", style = MaterialTheme.typography.labelSmall)
                                Text(reservation.userName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(reservation.userEmail, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Capster Terpilih:", style = MaterialTheme.typography.labelSmall)
                                Text(reservation.capsterName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("Tiket: ${reservation.queueNo ?: "-"}", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (reservation.status == "PENDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.updateReservationStatus(reservation.id, "APPROVED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp).testTag("approve_btn_${reservation.id}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Setujui", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.updateReservationStatus(reservation.id, "REJECTED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Tolak", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// FLOATING AI BUBBLE WIDGET
// ---------------------------------------------------------------------------------

@Composable
fun FloatingAiBubble(
    viewModel: BarberViewModel,
    isOpen: Boolean,
    onToggle: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<Pair<String, Boolean>>() } // Pair(message, isUser)
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Add initial welcome message if empty
    LaunchedEffect(Unit) {
        if (chatHistory.isEmpty()) {
            chatHistory.add(
                "Halo! Saya AI Barberteak. ✂️\n\nAda yang bisa saya bantu? Silakan tanyakan ketersediaan capster, katalog produk, harga layanan, atau saran model rambut!" to false
            )
        }
    }

    // Auto scroll to bottom
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty() || isLoading) return
        chatHistory.add(text to true)
        query = ""
        isLoading = true

        coroutineScope.launch {
            val response = com.example.data.remote.GeminiHelper.askGemini(text)
            chatHistory.add(response to false)
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (isOpen) {
            // Chat window card
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .height(440.dp)
                    .padding(bottom = 70.dp) // Leave room for the bubble button below
                    .border(1.dp, TeakGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .testTag("floating_ai_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TeakWoodPrimary, Color(0xFF1E1712))))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TeakGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = TeakGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "AI Barberteak",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusGreen)
                                    )
                                    Text(
                                        text = "Asisten Online",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory) { (message, isUser) ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 2.dp,
                                        bottomEnd = if (isUser) 2.dp else 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) TeakWoodPrimary else SurfaceVariantDark
                                    ),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isUser) Color.White else OnBackgroundDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = TeakGold
                                    )
                                    Text(
                                        text = "AI sedang mengetik...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnBackgroundDark.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Suggestions row
                    if (chatHistory.size == 1) {
                        val suggestions = listOf("Cek Capster?", "Katalog Produk?", "Layanan & Harga?")
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                Card(
                                    modifier = Modifier.clickableWithBounce {
                                        val fullText = when(suggestion) {
                                            "Cek Capster?" -> "Siapa saja capster yang tersedia saat ini?"
                                            "Katalog Produk?" -> "Apa saja katalog produk premium Barberteak?"
                                            else -> "Berapa harga paket layanan potong rambut?"
                                        }
                                        sendMessage(fullText)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, TeakGold.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TeakGold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Tanya AI...", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TeakGold,
                                focusedLabelColor = TeakGold,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("floating_ai_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        IconButton(
                            onClick = { sendMessage(query) },
                            enabled = query.trim().isNotEmpty() && !isLoading,
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (query.trim().isNotEmpty()) TeakWoodPrimary else Color.Gray.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Kirim",
                                tint = if (query.trim().isNotEmpty()) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Circular Bubble Button
        Card(
            modifier = Modifier
                .size(56.dp)
                .clickableWithBounce { onToggle() }
                .testTag("floating_ai_bubble_button"),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(TeakWoodPrimary, Color(0xFF1E1712))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOpen) Icons.Default.Close else Icons.Default.SmartToy,
                    contentDescription = "Tanya AI",
                    tint = TeakGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
