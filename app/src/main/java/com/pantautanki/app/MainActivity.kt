package com.pantautanki.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem"),
    LIGHT("Terang"),
    DARK("Gelap")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PantauTankiApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantauTankiApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("pantautanki_preferences", Context.MODE_PRIVATE)
    }
    var themeMode by remember {
        mutableStateOf(
            runCatching {
                ThemeMode.valueOf(preferences.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM)
        )
    }
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
        currency = java.util.Currency.getInstance("IDR")
    }}
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val db = remember { AppDb(context) }
    var vehicles by remember { mutableStateOf(emptyList<Vehicle>()) }
    var activeVehicleId by remember { mutableLongStateOf(-1L) }
    var fuelEntries by remember { mutableStateOf(emptyList<FuelEntry>()) }

    LaunchedEffect(Unit) {
        vehicles = db.vehicles()
        if (vehicles.isNotEmpty()) activeVehicleId = vehicles.first().id
    }

    LaunchedEffect(activeVehicleId) {
        fuelEntries = if (activeVehicleId != -1L) db.entries(activeVehicleId) else emptyList()
    }
    var showAddFuel by remember { mutableStateOf(false) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var showVehiclePicker by remember { mutableStateOf(false) }

    val active = vehicles.firstOrNull { it.id == activeVehicleId }
    val activeEntries = if (active != null) fuelEntries.filter { it.vehicleId == active.id } else emptyList()
    val monthCost = activeEntries.sumOf { it.total }
    val monthLiters = activeEntries.sumOf { it.liters }
    val avgConsumption = if (activeEntries.size >= 2) {
        val first = activeEntries.minOf { it.odometer }
        val last = activeEntries.maxOf { it.odometer }
        val distance = (last - first).coerceAtLeast(0)
        if (monthLiters > 0) distance / monthLiters else 0.0
    } else 0.0

    MaterialTheme(
        colorScheme = if (useDarkTheme) {
            darkColorScheme(
                primary = Color(0xFF62D4BE),
                onPrimary = Color(0xFF00382F),
                secondary = Color(0xFFB0CCC4),
                background = Color(0xFF101412),
                surface = Color(0xFF171C1A)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF176B5B),
                secondary = Color(0xFF5C6F68),
                background = Color(0xFFF7F8FA),
                surface = Color.White
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("PantauTanki", fontWeight = FontWeight.Bold)
                            Text("100% offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showVehiclePicker = true }) {
                            Icon(Icons.Default.DirectionsCar, "Pilih kendaraan")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddFuel = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) { Icon(Icons.Default.Add, "Tambah pengisian") }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Beranda") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.BarChart, null) },
                        label = { Text("Statistik") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.DirectionsCar, null) },
                        label = { Text("Kendaraan") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("Riwayat") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.MoreHoriz, null) },
                        label = { Text("Lainnya") }
                    )
                }
            }
        ) { padding ->
            // Scaffold provides the space occupied by the TopAppBar and NavigationBar.
            // Keep each screen inside that inset so content cannot render underneath the app bar.
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                0 -> if (active != null) HomeScreen(active, monthCost, monthLiters, avgConsumption, activeEntries, currency)
                     else EmptyHomeScreen(onAddVehicle = { showAddVehicle = true })

                1 -> if (active != null) StatsScreen(active, activeEntries, currency)
                     else EmptyHomeScreen(onAddVehicle = { showAddVehicle = true })
                2 -> VehicleScreen(
                    vehicles = vehicles,
                    activeId = activeVehicleId,
                    onSelect = { activeVehicleId = it.id },
                    onAdd = { showAddVehicle = true },
                    onDelete = { id ->
                        scope.launch {
                            db.deleteVehicle(id)
                            vehicles = db.vehicles()
                            fuelEntries = if (activeVehicleId != id && activeVehicleId != -1L) db.entries(activeVehicleId) else emptyList()
                            if (activeVehicleId == id) activeVehicleId = vehicles.firstOrNull()?.id ?: -1L
                        }
                    }
                )
                    3 -> HistoryScreen(
                    vehicle = active,
                    entries = activeEntries,
                    currency = currency
                )
                4 -> MoreScreen(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        preferences.edit().putString("theme_mode", it.name).apply()
                    },
                    onAddFuel = { showAddFuel = true },
                    onAddVehicle = { showAddVehicle = true }
                )
                }
            }
        }

        if (showAddFuel) {
            AddFuelDialog(
                onDismiss = { showAddFuel = false },
                onSave = { odo, liters, price ->
                    if (active != null) {
                        scope.launch {
                            db.addFuel(active.id, odo, liters, price)
                            vehicles = db.vehicles()
                            fuelEntries = db.entries(active.id)
                            showAddFuel = false
                        }
                    }
                }
            )
        }

        if (showAddVehicle) {
            AddVehicleDialog(
                onDismiss = { showAddVehicle = false },
                onSave = { name, odo, tank ->
                    scope.launch {
                        val id = db.addVehicle(name, odo, tank)
                        vehicles = db.vehicles()
                        activeVehicleId = id
                        showAddVehicle = false
                    }
                }
            )
        }

        if (showVehiclePicker) {
            AlertDialog(
                onDismissRequest = { showVehiclePicker = false },
                title = { Text("Pilih kendaraan") },
                text = {
                    Column {
                        vehicles.forEach { v ->
                            ListItem(
                                headlineContent = { Text(v.name) },
                                supportingContent = { Text("${v.odometer} km") },
                                leadingContent = { Icon(Icons.Default.DirectionsCar, null) },
                                modifier = Modifier.clickable {
                                    activeVehicleId = v.id
                                    scope.launch { fuelEntries = db.entries(v.id) }
                                    showVehiclePicker = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}


@Composable
fun EmptyHomeScreen(onAddVehicle: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Belum ada kendaraan", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Buat profil kendaraan pertama untuk mulai mencatat BBM.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddVehicle) { Text("Tambah kendaraan") }
        }
    }
}

@Composable
fun HomeScreen(
    vehicle: Vehicle,
    monthCost: Long,
    monthLiters: Double,
    avgConsumption: Double,
    entries: List<FuelEntry>,
    currency: NumberFormat
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(vehicle.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${vehicle.odometer} km", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric("Konsumsi", if (avgConsumption > 0) "%.2f km/L".format(avgConsumption) else "Belum ada data")
                        Metric("BBM bulan ini", currency.format(monthCost))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Total liter", "%.1f L".format(monthLiters), Modifier.weight(1f))
                SummaryCard("Kapasitas tangki", "%.1f L".format(vehicle.tankCapacity), Modifier.weight(1f))
            }
        }
        item {
            Text("Riwayat terakhir", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (entries.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Belum ada pengisian BBM.", fontWeight = FontWeight.SemiBold)
                        Text("Tekan tombol + untuk mencatat pengisian pertama.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(entries.takeLast(5).reversed()) { e ->
                ListItem(
                    headlineContent = { Text("${e.liters} L") },
                    supportingContent = { Text("${e.odometer} km • ${currency.format(e.total)}") },
                    leadingContent = {
                        Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}

@Composable
fun Metric(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryScreen(
    vehicle: Vehicle?,
    entries: List<FuelEntry>,
    currency: NumberFormat
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Text("Riwayat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                vehicle?.name?.let { "Riwayat pengisian BBM • $it" } ?: "Belum ada kendaraan aktif",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (vehicle == null || entries.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Belum ada riwayat pengisian BBM.", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Riwayat akan muncul setelah Anda mencatat pengisian BBM.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(entries.reversed(), key = { it.id }) { entry ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    dateFormat.format(Date(entry.timestamp)),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${entry.odometer} km",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                currency.format(entry.total),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HistoryMetric("Liter", "%.2f L".format(entry.liters))
                            HistoryMetric("Harga/L", currency.format(entry.pricePerLiter))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatsScreen(vehicle: Vehicle, entries: List<FuelEntry>, currency: NumberFormat) {
    val liters = entries.sumOf { it.liters }
    val cost = entries.sumOf { it.total }
    val distance = if (entries.size >= 2) {
        entries.maxOf { it.odometer } - entries.minOf { it.odometer }
    } else 0
    val kmPerL = if (liters > 0) distance / liters else 0.0
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Statistik ${vehicle.name}", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Semua data lokal di perangkat ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SummaryCard("Rata-rata konsumsi", if (kmPerL > 0) "%.2f km/L".format(kmPerL) else "Belum cukup data") }
        item { SummaryCard("Total BBM", "%.1f L".format(liters)) }
        item { SummaryCard("Total biaya BBM", currency.format(cost)) }
        item { SummaryCard("Jarak tercatat", "$distance km") }
    }
}

@Composable
fun VehicleScreen(
    vehicles: List<Vehicle>,
    activeId: Long,
    onSelect: (Vehicle) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<Vehicle?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text("Kendaraan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Setiap profil menyimpan track record sendiri.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, "Tambah kendaraan")
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vehicles) { v ->
                Card(
                    Modifier.fillMaxWidth().clickable { onSelect(v) },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${v.odometer} km • ${v.tankCapacity} L", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (v.id == activeId) Text("Kendaraan aktif", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                        IconButton(onClick = { deleteTarget = v }) {
                            Icon(Icons.Default.DeleteOutline, "Hapus")
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { v ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus ${v.name}?") },
            text = { Text("Seluruh riwayat BBM dan data profil kendaraan ini akan ikut terhapus dari perangkat. Data hanya dapat dipulihkan jika sebelumnya sudah dibackup.") },
            confirmButton = {
                TextButton(onClick = { onDelete(v.id); deleteTarget = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun MoreScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAddFuel: () -> Unit,
    onAddVehicle: () -> Unit
) {
    var showThemePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Lainnya", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("Tema") },
                    supportingContent = { Text("Saat ini: ${themeMode.label}") },
                    leadingContent = { Icon(Icons.Default.Brightness6, null) },
                    modifier = Modifier.clickable { showThemePicker = true }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Backup & Restore") },
                    supportingContent = { Text("Data tetap berada di tangan Anda.") },
                    leadingContent = { Icon(Icons.Default.Backup, null) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Export CSV") },
                    supportingContent = { Text("Untuk dibaca atau diolah di spreadsheet.") },
                    leadingContent = { Icon(Icons.Default.FileDownload, null) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Tentang PantauTanki") },
                    supportingContent = { Text("Versi 1.0.0 • Android 8+") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Pilih tema") },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        ListItem(
                            headlineContent = { Text(mode.label) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Brightness6,
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onThemeModeChange(mode)
                                showThemePicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun AddFuelDialog(onDismiss: () -> Unit, onSave: (Int, Double, Long) -> Unit) {
    var odo by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat pengisian BBM") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(odo, { odo = it }, label = { Text("Odometer (km)") }, singleLine = true)
                OutlinedTextField(liters, { liters = it }, label = { Text("Liter") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("Harga per liter (Rp)") }, singleLine = true)
                if (liters.toDoubleOrNull() != null && price.toLongOrNull() != null)
                    Text("Total: Rp${(liters.toDouble() * price.toLong()).roundToInt()}", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val o = odo.toIntOrNull()
                    val l = liters.toDoubleOrNull()
                    val p = price.toLongOrNull()
                    if (o != null && l != null && p != null && l > 0 && p > 0) onSave(o, l, p)
                }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onSave: (String, Int, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf("") }
    var tank by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil kendaraan baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama kendaraan") }, singleLine = true)
                OutlinedTextField(odo, { odo = it }, label = { Text("Odometer saat ini") }, singleLine = true)
                OutlinedTextField(tank, { tank = it }, label = { Text("Kapasitas tangki (L)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val o = odo.toIntOrNull()
                val t = tank.toDoubleOrNull()
                if (name.isNotBlank() && o != null && t != null && t > 0) onSave(name.trim(), o, t)
            }) { Text("Buat profil") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
