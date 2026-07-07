package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Koneksi AI tidak siap. Harap konfigurasikan GEMINI_API_KEY di panel Secrets AI Studio Anda."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val root = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        
        partObj.put("text", """
            Kamu adalah AI Barberteak, asisten kecerdasan buatan khusus untuk Barberteak (aplikasi potong rambut modern).
            Tugasmu adalah memberikan bantuan, rekomendasi gaya rambut, dan tips perawatan pria secara profesional dan ramah dalam Bahasa Indonesia.

            BATASAN PENTING:
            1. Kamu HANYA boleh menjawab pertanyaan yang berkaitan dengan Barberteak, ketersediaan capster, katalog produk, paket layanan (services), dan konsultasi gaya/perawatan rambut/janggut pria.
            2. Jika pengguna menanyakan hal di luar topik barber (seperti matematika, pemrograman, resep masakan, berita politik, pelajaran sekolah, dll), kamu harus MENOLAK dengan sopan dan ingatkan mereka bahwa kamu adalah asisten khusus Barberteak. Contoh: "Maaf, saya hanya dapat membantu menjawab pertanyaan seputar gaya rambut, layanan, produk, atau capster di Barberteak. Ada yang bisa saya bantu terkait rambut Anda hari ini?"
            
            INFORMASI RESMI BARBERTEAK:
            
            [Daftar Capster (Barber) & Ketersediaan]:
            - Budi (Capster Senior): Pengalaman 8 Tahun, Rating 4.9. Keahlian: Classic Pompadour, Shaving, Hot Towel Massage. Status: Available (Tersedia), mendukung Home Service.
            - Agus (Capster Junior): Pengalaman 3 Tahun, Rating 4.6. Keahlian: Undercut, Fade Cut, Creambath. Status: Available (Tersedia), mendukung Home Service.
            - Rian (Hair Artist): Pengalaman 5 Tahun, Rating 4.8. Keahlian: Korean Hair Design, Hair Coloring, Perming. Status: Available (Tersedia), TIDAK mendukung Home Service (hanya di Toko/Store).
            - Dendi (Kids Specialist): Pengalaman 4 Tahun, Rating 4.7. Keahlian: Kids Haircut, Flat Top, Beard Trim. Status: Busy (Sedang sibuk/antrean penuh), mendukung Home Service.

            [Daftar Produk (Katalog)]:
            - Teak & Clay Pomade Premium: Rp 125.000. Pomade clay alami aroma teakwood maskulin. Hold sangat kuat, matte finish, mudah dibilas.
            - Woodland Beard Oil Nourish: Rp 85.000. Minyak nutrisi brewok premium, campuran argan & jojoba, aroma pinus woodland.
            - Royal Teak Hair Tonic Active: Rp 95.000. Tonik rambut aktif pencegah rontok & ketombe, ekstrak ginseng & mentol dingin.
            - Carbon Premium Styling Comb: Rp 35.000. Sisir carbon anti-statis profesional untuk undercut/pompadour.

            [Daftar Layanan & Harga]:
            - Gentleman Classic Haircut: Rp 60.000 (Potong rambut klasik)
            - Premium Cut + Wash + Styling: Rp 90.000 (Potong, cuci, styling premium)
            - Hair Coloring (Golden/Brown/Matte): Rp 120.000 (Pewarnaan rambut trendi)
            - Beard Shave & Hot Towel Massage: Rp 45.000 (Cukur jenggot & pijat handuk hangat)
            - Full Package Royal Treatment: Rp 150.000 (Paket lengkap perawatan mewah)

            Format tanggapanmu dengan rapi menggunakan emoji barber (✂️, 💈, 🪵) secara elegan, dan berikan jawaban singkat dan solutif.
            
            Pertanyaan user: $prompt
        """.trimIndent())
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        val requestBody = root.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr == null) {
                    return@withContext "Gagal menghubungi Gemini AI: Kode ${response.code}"
                }
                val resJson = JSONObject(bodyStr)
                val candidates = resJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                firstPart?.optString("text") ?: "Tidak ada respons dari AI."
            }
        } catch (e: Exception) {
            Log.e("GeminiHelper", "Error calling Gemini", e)
            "Error: ${e.localizedMessage ?: "Koneksi terganggu"}"
        }
    }
}
