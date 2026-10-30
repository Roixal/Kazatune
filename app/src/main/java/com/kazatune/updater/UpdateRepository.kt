package com.kazatune.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import com.kazatune.updater.model.ReleaseInfo
import com.kazatune.updater.util.SemanticVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class UpdateRepository(
    private val context: Context,
    private val preferences: UpdatePreferences
) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getCurrentVersionName(): String {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    suspend fun fetchLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Kazatune-Android-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            val responseBody = response.body
            val bodyString = responseBody.string()

            if (code == 403) {
                return@withContext Result.failure(
                    IllegalStateException("GitHub API rate limit exceeded. Please try again later.")
                )
            }

            if (!response.isSuccessful || bodyString.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Failed to query GitHub release: HTTP $code")
                )
            }

            val json = JSONObject(bodyString)
            val release = parseReleaseJson(json)
            preferences.recordCheck()
            Result.success(release)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseReleaseJson(json: JSONObject): ReleaseInfo {
        val tagName = json.optString("tag_name", "")
        val name = json.optString("name", "")
        val body = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "https://github.com/Roixal/Kazatune/releases")
        val publishedAt = json.optString("published_at", "")
        val isPrerelease = json.optBoolean("prerelease", false)

        // Parse semantic version from tag_name first, then fallback to name
        val parsedVersion = SemanticVersion.parse(tagName)
            ?: SemanticVersion.parse(name)
            ?: SemanticVersion(1, 0, 0, rawString = tagName)

        val versionName = parsedVersion.toString()

        // Inspect assets for the APK
        var apkUrl: String? = null
        var apkFileName: String? = null
        var apkSize: Long = 0L
        var sha256Digest: String? = null

        val assetsArray: JSONArray? = json.optJSONArray("assets")
        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", 0L)

                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = downloadUrl
                    apkFileName = assetName
                    apkSize = size
                } else if (assetName.contains("sha256", ignoreCase = true) || assetName.endsWith(".txt", ignoreCase = true)) {
                    // Possible checksum file
                    val digestFromAsset = asset.optString("digest", "")
                    if (digestFromAsset.isNotBlank()) {
                        sha256Digest = digestFromAsset
                    }
                }
            }
        }

        // If no digest was in asset metadata, try extracting from release notes body: e.g. "SHA256: <hash>"
        if (sha256Digest == null) {
            val shaRegex = Regex("""(?i)(?:sha-?256|checksum)[\s:=]+([a-fA-F0-9]{64})""")
            sha256Digest = shaRegex.find(body)?.groups?.get(1)?.value
        }

        // Check if update is explicitly marked as mandatory
        val isMandatory = body.contains("[MANDATORY]", ignoreCase = true) ||
                body.contains("MANDATORY_UPDATE: true", ignoreCase = true) ||
                body.contains("mandatory: true", ignoreCase = true)

        return ReleaseInfo(
            tagName = tagName,
            name = if (name.isNotBlank()) name else "Kazatune $versionName",
            versionName = versionName,
            body = body,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            apkDownloadUrl = apkUrl,
            apkFileName = apkFileName,
            apkSize = apkSize,
            sha256Digest = sha256Digest,
            isPrerelease = isPrerelease,
            isMandatory = isMandatory
        )
    }

    suspend fun downloadApk(
        release: ReleaseInfo,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speedFormatted: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = release.apkDownloadUrl
            ?: return@withContext Result.failure(IllegalStateException("No APK asset found in release ${release.versionName}"))

        // Security check: Must be HTTPS from trusted GitHub release servers
        if (!downloadUrl.startsWith("https://", ignoreCase = true) ||
            (!downloadUrl.contains("github.com", ignoreCase = true) &&
             !downloadUrl.contains("githubusercontent.com", ignoreCase = true))
        ) {
            return@withContext Result.failure(SecurityException("Untrusted download URL: $downloadUrl"))
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFileName = release.apkFileName ?: "Kazatune-${release.versionName}.apk"
        val outputFile = File(updatesDir, targetFileName)
        val tempFile = File(updatesDir, "$targetFileName.download")

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Kazatune-Android-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException("Download failed with HTTP ${response.code}")
                )
            }

            val body = response.body

            val totalBytes = if (body.contentLength() > 0) body.contentLength() else release.apkSize

            // Check storage space (StatFs)
            val stat = StatFs(updatesDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            if (totalBytes > 0 && availableBytes < (totalBytes + 10 * 1024 * 1024)) {
                return@withContext Result.failure(
                    IllegalStateException("Insufficient storage space. Required: ${totalBytes / (1024 * 1024)} MB")
                )
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesReadTotal: Long = 0
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastSpeed = 0L

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        bytesReadTotal += read
                        bytesSinceLastSpeed += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 300) {
                            val speedBps = (bytesSinceLastSpeed * 1000) / elapsed
                            val speedFormatted = formatSpeed(speedBps)
                            val progress = if (totalBytes > 0) {
                                (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            onProgress(progress, bytesReadTotal, totalBytes, speedFormatted)
                            lastUpdateTime = now
                            bytesSinceLastSpeed = 0L
                        }
                    }
                    output.flush()
                }
            }

            // Verify SHA-256 if provided in release metadata
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!release.sha256Digest.isNullOrBlank()) {
                if (!calculatedHash.equals(release.sha256Digest.trim(), ignoreCase = true)) {
                    tempFile.delete()
                    return@withContext Result.failure(
                        SecurityException("SHA-256 mismatch! Expected ${release.sha256Digest}, got $calculatedHash")
                    )
                }
            }

            // Move temp file to target output file
            if (outputFile.exists()) outputFile.delete()
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }

            // Verify APK package identity with Android PackageManager
            val validationResult = validateApk(outputFile)
            if (validationResult.isFailure) {
                outputFile.delete()
                return@withContext Result.failure(validationResult.exceptionOrNull()!!)
            }

            Result.success(outputFile)
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }

    private fun validateApk(apkFile: File): Result<Unit> {
        return try {
            val archiveInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            ) ?: return Result.failure(IllegalStateException("Invalid or corrupt APK file"))

            val currentPkg = context.packageName.removeSuffix(".debug")
            val targetPkg = archiveInfo.packageName.removeSuffix(".debug")

            if (currentPkg != targetPkg) {
                return Result.failure(
                    SecurityException("Package name mismatch! Expected $currentPkg but APK has ${archiveInfo.packageName}")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/Roixal/Kazatune/releases/latest"
        const val RELEASES_PAGE_URL = "https://github.com/Roixal/Kazatune/releases"
    }
}
