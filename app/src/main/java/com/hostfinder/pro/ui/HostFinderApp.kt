package com.hostfinder.pro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostfinder.pro.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Accent = Color(0xFF3B9EFF)
private val Panel = Color(0xFF111820)
private val Panel2 = Color(0xFF18212C)
private val Muted = Color(0xFF7A8A9E)
private val Success = Color(0xFF22C55E)
private val Danger = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostFinderApp() {
    var apiBase by remember { mutableStateOf("http://10.0.2.2:8000") }
    var screen by remember { mutableStateOf("home") }
    var message by remember { mutableStateOf<String?>(null) }
    var domains by remember { mutableStateOf<List<DomainItem>>(emptyList()) }
    var scans by remember { mutableStateOf<List<ScanJob>>(emptyList()) }
    var domainInput by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var liveDomain by remember { mutableStateOf("") }
    var liveResult by remember { mutableStateOf<EnumResult?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember(apiBase) { ApiClient(apiBase) }

    fun toast(msg: String) { message = msg }

    fun refresh() {
        scope.launch {
            try {
                domains = client.listDomains()
                scans = client.listScans()
            } catch (e: Exception) {
                toast("API: ${e.message}")
            }
        }
    }

    LaunchedEffect(apiBase) { refresh() }

    LaunchedEffect(scans) {
        if (scans.any { it.status == "running" || it.status == "queued" }) {
            delay(3000)
            refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Host Finder Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Attack surface \u00b7 mobile", color = Muted, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Panel)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(selected = screen == "home", onClick = { screen = "home"; refresh() },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = screen == "domains", onClick = { screen = "domains"; refresh() },
                    icon = { Icon(Icons.Default.Language, null) }, label = { Text("Domains") })
                NavigationBarItem(selected = screen == "scans", onClick = { screen = "scans"; refresh() },
                    icon = { Icon(Icons.Default.Autorenew, null) }, label = { Text("Scans") })
                NavigationBarItem(selected = screen == "live", onClick = { screen = "live" },
                    icon = { Icon(Icons.Default.Bolt, null) }, label = { Text("Live") })
                NavigationBarItem(selected = screen == "settings", onClick = { screen = "settings" },
                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text("API") })
            }
        },
        containerColor = Color(0xFF070B10)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            message?.let { msg ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel2),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Accent)
                        TextButton(onClick = { message = null }) { Text("OK") }
                    }
                }
            }

            when (screen) {
                "home" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Domains", domains.size.toString(), Accent, Modifier.weight(1f))
                        StatCard("Scans", scans.size.toString(), Color.White, Modifier.weight(1f))
                        StatCard("Done", scans.count { it.status == "completed" }.toString(), Success, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionCard("Quick Scan") {
                        OutlinedTextField(value = domainInput, onValueChange = { domainInput = it },
                            placeholder = { Text("example.com") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (domainInput.isBlank()) return@Button
                                loading = true
                                scope.launch {
                                    try {
                                        try { client.createDomain(domainInput.trim()) } catch (_: Exception) {}
                                        val job = client.triggerScan(domainInput.trim())
                                        toast("Scan queued #${job.id}")
                                        domainInput = ""
                                        refresh()
                                        screen = "scans"
                                    } catch (e: Exception) {
                                        toast(e.message ?: "Failed")
                                    } finally { loading = false }
                                }
                            },
                            enabled = !loading && domainInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text(if (loading) "Working\u2026" else "Add & Scan") }
                        Text("WHOIS \u2192 DNS \u2192 subdomains \u2192 tech \u2192 paths",
                            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionCard("Recent Scans") {
                        if (scans.isEmpty()) Text("No scans yet", color = Muted)
                        else scans.take(5).forEach { s ->
                            ScanRow(s)
                            HorizontalDivider(color = Color(0xFF243041), modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
                "domains" -> {
                    SectionCard("Tracked Domains (${domains.size})") {
                        if (domains.isEmpty()) Text("No domains \u2014 run a quick scan", color = Muted)
                        else LazyColumn {
                            items(domains) { d ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text(d.name, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                    Text("${d.status} \u00b7 ${d.registrar.ifBlank { "\u2014" }}", color = Muted, fontSize = 12.sp)
                                    TextButton(onClick = {
                                        loading = true
                                        scope.launch {
                                            try {
                                                client.triggerScan(d.name)
                                                toast("Rescan queued")
                                                refresh()
                                            } catch (e: Exception) { toast(e.message ?: "Failed") }
                                            finally { loading = false }
                                        }
                                    }) { Text("Rescan") }
                                }
                                HorizontalDivider(color = Color(0xFF243041))
                            }
                        }
                    }
                }
                "scans" -> {
                    SectionCard("Scan Jobs") {
                        if (scans.isEmpty()) Text("No jobs", color = Muted)
                        else LazyColumn {
                            items(scans) { s ->
                                ScanRow(s)
                                HorizontalDivider(color = Color(0xFF243041), modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
                "live" -> {
                    SectionCard("Live Subdomain Enum") {
                        OutlinedTextField(value = liveDomain, onValueChange = { liveDomain = it },
                            placeholder = { Text("example.com") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (liveDomain.isBlank()) return@Button
                                loading = true
                                liveResult = null
                                scope.launch {
                                    try { liveResult = client.liveEnumerate(liveDomain.trim()) }
                                    catch (e: Exception) { toast(e.message ?: "Failed") }
                                    finally { loading = false }
                                }
                            },
                            enabled = !loading && liveDomain.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text(if (loading) "Enumerating\u2026" else "Run Enum") }
                        liveResult?.let { r ->
                            Text("Found ${r.count} for ${r.domain}", color = Accent,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(r.subdomains) { s ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(s.name, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                            if (s.ips.isNotEmpty()) Text(s.ips.joinToString(), color = Muted, fontSize = 11.sp)
                                        }
                                        StatusChip(if (s.alive) "alive" else "down", if (s.alive) Success else Muted)
                                    }
                                    HorizontalDivider(color = Color(0xFF243041))
                                }
                            }
                        }
                    }
                }
                "settings" -> {
                    SectionCard("API Server") {
                        Text("Emulator \u2192 host uses 10.0.2.2. Physical device: your PC LAN IP.",
                            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(value = apiBase, onValueChange = { apiBase = it },
                            label = { Text("Base URL") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    try {
                                        val ok = client.health()
                                        toast(if (ok) "Backend reachable" else "No response")
                                        if (ok) refresh()
                                    } catch (e: Exception) { toast(e.message ?: "Unreachable") }
                                    finally { loading = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text("Test Connection") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title.uppercase(), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
private fun ScanRow(s: ScanJob) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.domainName, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
            StatusChip(s.status, when (s.status) { "completed" -> Success; "failed" -> Danger; else -> Accent })
        }
        LinearProgressIndicator(
            progress = { (s.progress / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
            color = Accent, trackColor = Panel2,
        )
        Text("${s.progress.toInt()}% \u00b7 findings ${s.findingsCount}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        if (s.errorMessage.isNotBlank()) Text(s.errorMessage, color = Danger, fontSize = 11.sp)
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(text.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF243041),
    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Accent,
    focusedContainerColor = Panel2, unfocusedContainerColor = Panel2,
)
