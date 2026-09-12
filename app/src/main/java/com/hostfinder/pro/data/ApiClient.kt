package com.hostfinder.pro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient(baseUrl: String) {
    private val root = baseUrl.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$root$path").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw Exception(parseError(body, resp.code))
            body
        }
    }

    private suspend fun post(path: String, json: String): String = withContext(Dispatchers.IO) {
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$root$path").post(body).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw Exception(parseError(text, resp.code))
            text
        }
    }

    private fun parseError(body: String, code: Int): String {
        return try {
            val o = JSONObject(body)
            when (val d = o.opt("detail")) {
                is String -> d
                else -> "HTTP $code"
            }
        } catch (_: Exception) {
            "HTTP $code"
        }
    }

    suspend fun health(): Boolean = try {
        get("/health").contains("ok")
    } catch (_: Exception) {
        try { get("/").contains("Host Finder") } catch (_: Exception) { false }
    }

    suspend fun listDomains(): List<DomainItem> {
        val arr = JSONArray(get("/api/domains"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DomainItem(
                id = o.getInt("id"),
                name = o.getString("name"),
                status = o.optString("status", "pending"),
                registrar = o.optString("registrar", ""),
                lastScanned = o.optString("last_scanned", ""),
            )
        }
    }

    suspend fun createDomain(name: String) {
        post("/api/domains", JSONObject().put("name", name).toString())
    }

    suspend fun triggerScan(domain: String): ScanJob {
        val o = JSONObject(post("/api/scan", JSONObject().put("domain", domain).toString()))
        return ScanJob(
            id = o.getInt("id"),
            domainName = o.getString("domain_name"),
            status = o.getString("status"),
            progress = o.optDouble("progress", 0.0),
            findingsCount = o.optInt("findings_count", 0),
            errorMessage = o.optString("error_message", ""),
        )
    }

    suspend fun listScans(): List<ScanJob> {
        val arr = JSONArray(get("/api/scan"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ScanJob(
                id = o.getInt("id"),
                domainName = o.getString("domain_name"),
                status = o.getString("status"),
                progress = o.optDouble("progress", 0.0),
                findingsCount = o.optInt("findings_count", 0),
                errorMessage = o.optString("error_message", ""),
            )
        }
    }

    suspend fun liveEnumerate(domain: String): EnumResult {
        val o = JSONObject(get("/api/subdomains/enumerate?domain=${java.net.URLEncoder.encode(domain, "UTF-8")}"))
        val arr = o.optJSONArray("subdomains") ?: JSONArray()
        val list = (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            val ips = s.optJSONArray("ip_addresses")
            val ipList = if (ips != null) (0 until ips.length()).map { ips.getString(it) } else emptyList()
            SubItem(s.getString("name"), ipList, s.optBoolean("is_alive", false))
        }
        return EnumResult(o.optString("domain", domain), o.optInt("count", list.size), list)
    }

    suspend fun listSubdomains(domainId: Int): List<SubItem> {
        val arr = JSONArray(get("/api/subdomains?domain_id=$domainId"))
        return (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            val ips = s.optJSONArray("ip_addresses")
            val ipList = if (ips != null) (0 until ips.length()).map { ips.getString(it) } else emptyList()
            SubItem(s.getString("name"), ipList, s.optBoolean("is_alive", false))
        }
    }
}

data class DomainItem(
    val id: Int,
    val name: String,
    val status: String,
    val registrar: String,
    val lastScanned: String,
)

data class ScanJob(
    val id: Int,
    val domainName: String,
    val status: String,
    val progress: Double,
    val findingsCount: Int,
    val errorMessage: String,
)

data class SubItem(val name: String, val ips: List<String>, val alive: Boolean)
data class EnumResult(val domain: String, val count: Int, val subdomains: List<SubItem>)
