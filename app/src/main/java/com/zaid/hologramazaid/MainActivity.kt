package com.zaid.hologramazaid

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val sky = Color.rgb(21, 158, 235)
    private val ink = Color.rgb(21, 47, 70)
    private val muted = Color.rgb(92, 119, 143)
    private val canvas = Color.rgb(241, 249, 255)
    private val lineColor = Color.rgb(207, 232, 250)
    private val terminalBg = Color.rgb(5, 24, 39)

    private lateinit var screenHost: LinearLayout
    private var rootStatus: TextView? = null
    private var storageStatus: TextView? = null
    private var downloadStatus: TextView? = null
    private var logsView: TextView? = null
    private var terminalView: TextView? = null
    private var terminalInput: EditText? = null
    private var currentScreen = "home"
    private var running = false
    private val lock = Any()
    private val logBuffer = StringBuilder()
    private val logFile by lazy { File(filesDir, "holograma-zaid.log") }
    private val remoteScript = "/data/local/tmp/krissgamervipHoloo.sh"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        try {
            if (logFile.exists()) logBuffer.append(logFile.readText(Charsets.UTF_8).takeLast(90000))
        } catch (_: Exception) { }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(canvas)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(11), dp(16), dp(11))
            setBackgroundColor(Color.WHITE)
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.holograma_logo)
            contentDescription = "Logo Holograma Zaid"
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        heading.addView(label("Holograma Zaid", 19f, ink, true))
        heading.addView(label("ROOT SCRIPT CONTROL", 10f, muted, true))
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("ROOT", 11f, sky, true).apply {
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(Color.rgb(225, 244, 255), 24, lineColor)
        })
        root.addView(header)
        root.addView(View(this).apply { setBackgroundColor(lineColor) }, LinearLayout.LayoutParams(-1, dp(1)))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            setBackgroundColor(Color.WHITE)
        }
        nav.addView(button("⌂  Inicio", false) { showScreen("home") }, weightParams())
        nav.addView(button("▤  Registros", false) { showScreen("logs") }, weightParams().apply { leftMargin = dp(6) })
        nav.addView(button(">_  Terminal", false) { showScreen("terminal") }, weightParams().apply { leftMargin = dp(6) })
        root.addView(nav)

        screenHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(12))
        }
        root.addView(screenHost, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        addLog("Holograma Zaid v1.0.0 iniciado.")
        addLog("Script integrado: krissgamervipHoloo.sh")
        showScreen("home")
        verifyRoot()
    }

    private fun showScreen(screen: String) {
        currentScreen = screen
        screenHost.removeAllViews()
        logsView = null
        terminalView = null
        terminalInput = null
        when (screen) {
            "logs" -> screenHost.addView(buildLogsScreen(), LinearLayout.LayoutParams(-1, -1))
            "terminal" -> screenHost.addView(buildTerminalScreen(), LinearLayout.LayoutParams(-1, -1))
            else -> screenHost.addView(buildHomeScreen(), LinearLayout.LayoutParams(-1, -1))
        }
    }

    private fun buildHomeScreen(): View {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(Color.WHITE, 24, lineColor)
        }
        hero.addView(ImageView(this).apply {
            setImageResource(R.drawable.holograma_logo)
            contentDescription = "Holograma Zaid"
        }, LinearLayout.LayoutParams(dp(82), dp(82)))
        hero.addView(label("Holograma Zaid", 24f, ink, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("Ejecuta el script con privilegios root", 13f, muted, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(8))
        })
        hero.addView(label("SCRIPT INTEGRADO  •  MODO VERBOSE", 10f, sky, true).apply { gravity = Gravity.CENTER })
        col.addView(hero, marginParams(12))

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(231, 246, 255), 18, lineColor)
        }
        statusCard.addView(label("Estado de permisos", 14f, ink, true))
        rootStatus = label("Comprobando acceso root…", 13f, muted, false)
        statusCard.addView(rootStatus)
        statusCard.addView(button("Verificar root", false) { verifyRoot() }, LinearLayout.LayoutParams(-1, dp(42)))
        col.addView(statusCard, marginParams(10))

        val storageCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 18, lineColor)
        }
        storageCard.addView(label("Acceso al almacenamiento", 14f, ink, true))
        storageCard.addView(label("El permiso de Android permite gestionar archivos compartidos; la terminal root puede comprobar el acceso a rutas protegidas.", 11f, muted, false).apply {
            setPadding(0, dp(3), 0, dp(5))
        })
        storageStatus = label(storagePermissionSummary(), 12f, muted, true)
        storageCard.addView(storageStatus)
        storageCard.addView(button("Conceder acceso a todos los archivos", true) {
            requestStorageAccess()
        }, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(7) })
        storageCard.addView(button("Probar almacenamiento mediante root", false) {
            verifyStorageAccess()
        }, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(6) })
        col.addView(storageCard, marginParams(12))

        val downloadCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 18, lineColor)
        }
        downloadCard.addView(label("Descarga desde GitHub Releases", 14f, ink, true))
        downloadCard.addView(label("Descarga el archivo, calcula su SHA-256 y lo guarda en /data/local/tmp. No sobrescribe archivos del juego ni ejecuta scripts automáticamente.", 11f, muted, false).apply {
            setPadding(0, dp(3), 0, dp(5))
        })
        downloadStatus = label("Archivo no descargado por esta versión.", 12f, muted, true)
        downloadCard.addView(downloadStatus)
        downloadCard.addView(button("Descargar y verificar APK", true) {
            downloadFromGithubRelease()
        }, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(7) })
        col.addView(downloadCard, marginParams(14))

        col.addView(label("Acciones del script integrado", 18f, ink, true), marginParams(8))
        col.addView(actionCard("1 · Guardar copia original", "Crea el respaldo si todavía no existe.") {
            runScriptAction(1)
        }, marginParams(8))
        col.addView(actionCard("2 · Holo + Color Forte", "Activa ambos cambios del script.", true) {
            confirm("Activar Holo + Color Forte", "Se detendrá Free Fire antes de modificar el archivo. ¿Continuar?") {
                runScriptAction(2)
            }
        }, marginParams(8))
        col.addView(actionCard("4 · Solo Color Forte", "Activa únicamente el cambio del shader.") {
            confirm("Activar Color Forte", "Se detendrá Free Fire antes de modificar el archivo. ¿Continuar?") {
                runScriptAction(4)
            }
        }, marginParams(8))
        col.addView(actionCard("3 · Restaurar original", "Recupera el respaldo creado anteriormente.") {
            confirm("Restaurar original", "Esta acción sobrescribirá el archivo actual con la copia de respaldo. ¿Continuar?") {
                runScriptAction(3)
            }
        }, marginParams(12))
        col.addView(label("El script espera com.dts.freefireth y su archivo .pdcache en la ruta configurada. Revisa los registros si la instalación no coincide.", 11f, muted, false))
        scroll.addView(col)
        return scroll
    }

    private fun actionCard(title: String, subtitle: String, primary: Boolean = false, action: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(if (primary) Color.rgb(231, 246, 255) else Color.WHITE, 17, lineColor)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        card.addView(label(title, 14f, ink, true))
        card.addView(label(subtitle, 11f, muted, false).apply { setPadding(0, dp(4), 0, 0) })
        return card
    }

    private fun buildLogsScreen(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label("Registros de ejecución", 18f, ink, true), marginParams(8))
        val output = logTextView()
        logsView = output
        val scroll = ScrollView(this).apply {
            background = rounded(terminalBg, 16, terminalBg)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            addView(output)
        }
        col.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Copiar", false) { copyLogs() }, weightParams())
        actions.addView(button("Limpiar", false) { clearLogs() }, weightParams().apply { leftMargin = dp(8) })
        col.addView(actions, marginParams(4))
        updateLogViews()
        return col
    }

    private fun buildTerminalScreen(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label("Terminal root · verbose", 18f, ink, true), marginParams(5))
        col.addView(label("Los comandos se ejecutan mediante su -c. Úsala solo con comandos que entiendas.", 11f, muted, false), marginParams(8))
        val output = logTextView()
        terminalView = output
        val scroll = ScrollView(this).apply {
            background = rounded(terminalBg, 16, terminalBg)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            addView(output)
        }
        col.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val input = EditText(this).apply {
            hint = "Escribe un comando root…"
            setTextColor(ink)
            setHintTextColor(muted)
            textSize = 13f
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(Color.WHITE, 12, lineColor)
        }
        terminalInput = input
        col.addView(input, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(8)
            bottomMargin = dp(7)
        })
        col.addView(button("▶ Ejecutar comando", true) {
            val command = input.text.toString().trim()
            if (command.isEmpty()) {
                toast("Escribe un comando primero")
            } else {
                addLog("$ " + command)
                input.setText("")
                executeRoot("Terminal", command)
            }
        }, LinearLayout.LayoutParams(-1, dp(45)))
        updateLogViews()
        return col
    }

    private fun logTextView(): TextView = TextView(this).apply {
        textSize = 11f
        typeface = Typeface.MONOSPACE
        setTextColor(Color.rgb(166, 235, 255))
        setTextIsSelectable(true)
        text = synchronized(lock) { logBuffer.toString() }
    }

    private fun verifyRoot() {
        rootStatus?.text = "Solicitando/verificando acceso root…"
        executeRoot("Verificación de root", "id -u") { code, output ->
            val isRoot = code == 0 && output.trim().lineSequence().lastOrNull() == "0"
            rootStatus?.text = if (isRoot) "Root disponible · uid=0" else "Root no concedido o su no disponible"
            rootStatus?.setTextColor(if (isRoot) Color.rgb(18, 137, 85) else Color.rgb(190, 75, 65))
        }
    }

    private fun storagePermissionSummary(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                "Permiso de todos los archivos concedido"
            } else {
                "Pendiente: habilítalo en Ajustes de Android"
            }
        } else {
            val readOk = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val writeOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (readOk && writeOk) "Permisos de archivos concedidos"
            else "Pendiente: concede lectura y escritura de archivos"
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                toast("El permiso de todos los archivos ya está concedido")
                updateStorageStatus()
                return
            }
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (e: Exception) {
                    addLog("[ERROR] No se pudo abrir los ajustes de almacenamiento: " + (e.message ?: "sin detalle"))
                    toast("Abre Ajustes > Acceso especial > Gestionar todos los archivos")
                }
            }
        } else {
            val missing = mutableListOf<String>()
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (missing.isEmpty()) {
                updateStorageStatus()
                toast("Permisos de almacenamiento concedidos")
            } else {
                requestPermissions(missing.toTypedArray(), 2301)
            }
        }
    }

    private fun updateStorageStatus() {
        storageStatus?.text = storagePermissionSummary()
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        }
        storageStatus?.setTextColor(if (ok) Color.rgb(18, 137, 85) else Color.rgb(190, 75, 65))
    }

    private val githubReleaseDownloadUrl =
        "https://github.com/Fernan20881208/Zaid-Holograma/releases/download/Release/split_asset_pack_install_time.apk"
    private val stagedDownloadPath = "/data/local/tmp/split_asset_pack_install_time.apk"

    private fun downloadFromGithubRelease() {
        synchronized(lock) {
            if (running) {
                toast("Ya hay una operación en curso")
                return
            }
            running = true
        }
        downloadStatus?.text = "Descargando archivo desde GitHub Releases…"
        downloadStatus?.setTextColor(sky)
        addLog("[DESCARGA] Inicio de descarga desde GitHub Releases.")
        addLog("[DESCARGA] Destino final: " + stagedDownloadPath)
        Thread {
            var success = false
            val cached = File(cacheDir, "split_asset_pack_install_time.apk.download")
            var connection: HttpURLConnection? = null
            try {
                if (cached.exists() && !cached.delete()) {
                    throw IOException("No se pudo limpiar el archivo temporal previo.")
                }
                addLog("[DESCARGA] URL de GitHub Releases: " + githubReleaseDownloadUrl)
                connection = (URL(githubReleaseDownloadUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 30000
                    readTimeout = 60000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                    setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                }
                val httpCode = connection.responseCode
                val contentType = connection.contentType ?: "desconocido"
                val declaredBytes = connection.contentLengthLong
                addLog("[DESCARGA] HTTP " + httpCode + " · tipo=" + contentType + " · tamaño declarado=" + declaredBytes)
                if (httpCode !in 200..299) {
                    throw IOException("GitHub Releases respondió HTTP " + httpCode)
                }
                if (contentType.lowercase(Locale.ROOT).contains("text/html")) {
                    throw IOException("GitHub devolvió HTML en lugar del APK; verifica que el recurso de Release siga disponible.")
                }
                var received = 0L
                var nextReport = 25L * 1024L * 1024L
                connection.inputStream.use { input ->
                    cached.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            if (n == 0) continue
                            output.write(buffer, 0, n)
                            received += n
                            if (received >= nextReport) {
                                addLog("[DESCARGA] Recibidos " + (received / (1024 * 1024)) + " MiB.")
                                runOnUiThread {
                                    downloadStatus?.text = "Descargados " + (received / (1024 * 1024)) + " MiB…"
                                }
                                nextReport += 25L * 1024L * 1024L
                            }
                        }
                        output.flush()
                    }
                }
                val downloadedBytes = cached.length()
                if (downloadedBytes < 1024L * 1024L) {
                    throw IOException("El archivo descargado es demasiado pequeño (" + downloadedBytes + " bytes).")
                }
                if (declaredBytes > 0 && downloadedBytes != declaredBytes) {
                    throw IOException("Descarga incompleta: se esperaban " + declaredBytes + " bytes y llegaron " + downloadedBytes + ".")
                }
                val sourceSha = sha256File(cached)
                addLog("[DESCARGA] Descarga completa: " + downloadedBytes + " bytes.")
                addLog("[SHA-256] Origen: " + sourceSha)
                addLog("[ROOT] Copiando el archivo descargado a " + stagedDownloadPath)
                val cachePath = shellQuote(cached.absolutePath)
                val targetPath = shellQuote(stagedDownloadPath)
                val rootCommand =
                    "set -e; mkdir -p /data/local/tmp; " +
                    "cp -f " + cachePath + " " + targetPath + "; " +
                    "chmod 600 " + targetPath + "; " +
                    "if command -v sha256sum >/dev/null 2>&1; then " +
                    "echo STAGED_SHA=\$(sha256sum " + targetPath + " 2>/dev/null | awk '{print \$1}'); " +
                    "elif toybox sha256sum " + targetPath + " >/dev/null 2>&1; then " +
                    "echo STAGED_SHA=\$(toybox sha256sum " + targetPath + " 2>/dev/null | awk '{print \$1}'); " +
                    "else echo STAGED_SHA=UNAVAILABLE; fi; " +
                    "echo STAGED_SIZE=\$(wc -c < " + targetPath + " | tr -d '[:space:]')"
                val process = ProcessBuilder("su", "-c", rootCommand).redirectErrorStream(true).start()
                val rootOutput = StringBuilder()
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        rootOutput.append(line).append('\n')
                        addLog("[ROOT] " + line)
                    }
                }
                val rootCode = process.waitFor()
                if (rootCode != 0) {
                    throw IOException("No se pudo guardar en /data/local/tmp; su terminó con código " + rootCode + ".")
                }
                val stagedSize = Regex("STAGED_SIZE=(\\d+)").find(rootOutput.toString())?.groupValues?.get(1)?.toLongOrNull()
                val stagedSha = Regex("STAGED_SHA=([0-9a-fA-F]{64})").find(rootOutput.toString())?.groupValues?.get(1)
                if (stagedSize != downloadedBytes) {
                    throw IOException("La verificación del tamaño copiado falló. Esperado=" + downloadedBytes + ", copiado=" + stagedSize)
                }
                if (stagedSha != null && !stagedSha.equals(sourceSha, ignoreCase = true)) {
                    throw IOException("El SHA-256 no coincide después de copiar a /data/local/tmp.")
                }
                addLog("[OK] Archivo guardado en " + stagedDownloadPath)
                addLog("[OK] Tamaño verificado: " + downloadedBytes + " bytes.")
                if (stagedSha != null) addLog("[OK] SHA-256 del destino coincide.")
                else addLog("[AVISO] sha256sum no está disponible en el shell; se verificó el tamaño.")
                addLog("[INFO] No se modificó la carpeta privada del juego ni se ejecutaron scripts.")
                success = true
                cached.delete()
            } catch (e: Exception) {
                addLog("[ERROR] Descarga/validación fallida: " + (e.message ?: e.javaClass.simpleName))
                addLog("[INFO] El archivo temporal, si existe, permanece en: " + cached.absolutePath)
            } finally {
                connection?.disconnect()
                runOnUiThread {
                    synchronized(lock) { running = false }
                    downloadStatus?.text = if (success) {
                        "Descargado y verificado: /data/local/tmp/split_asset_pack_install_time.apk"
                    } else {
                        "No se completó; revisa Registros."
                    }
                    downloadStatus?.setTextColor(if (success) Color.rgb(18, 137, 85) else Color.rgb(190, 75, 65))
                    addLog(if (success) "[FIN] Descarga completada correctamente." else "[FIN] Operación de descarga finalizada con error.")
                    updateLogViews()
                }
            }
        }.start()
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun verifyStorageAccess() {
        val command = "id -u\n" +
            "echo '[CHECK] Directorios principales'\n" +
            "ls -ld /data /storage /storage/emulated/0 2>&1\n" +
            "echo '[CHECK] Lectura de almacenamiento compartido'\n" +
            "if [ -r /storage/emulated/0 ]; then echo '[OK] /storage/emulated/0 es legible'; else echo '[ERROR] Sin lectura de /storage/emulated/0'; fi\n" +
            "echo '[CHECK] Escritura de almacenamiento compartido'\n" +
            "if [ -w /storage/emulated/0 ]; then echo '[OK] /storage/emulated/0 informa escritura permitida'; else echo '[AVISO] El punto de montaje no anuncia escritura'; fi\n" +
            "echo '[CHECK] Primeros elementos visibles'\n" +
            "ls -A /storage/emulated/0 2>&1 | head -n 20\n" +
            "echo '[CHECK] Archivo del script y destino de Free Fire'\n" +
            "ls -l /data/local/tmp/krissgamervipHoloo.sh /data/user/0/com.dts.freefireth/files/split_asset_pack_install_time.apk.pdcache 2>&1"
        executeRoot("Verificación de almacenamiento por root", command)
    }

    override fun onResume() {
        super.onResume()
        updateStorageStatus()
    }

    @Deprecated("Deprecated in Android API; retained for permission result handling")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2301) updateStorageStatus()
    }

    private fun runScriptAction(action: Int) {
        val script = try {
            assets.open("krissgamervipHoloo.sh").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            addLog("[ERROR] No se pudo leer el script: " + (e.message ?: "sin detalle"))
            toast("No se pudo abrir el script")
            return
        }
        val command = StringBuilder()
        command.append("mkdir -p /data/local/tmp || exit 20\n")
        command.append("echo '[APP] Copiando script a ").append(remoteScript).append("'\n")
        command.append("cat > '").append(remoteScript).append("' <<'__HZAID_SCRIPT_END__'\n")
        command.append(script)
        if (!script.endsWith("\n")) command.append('\n')
        command.append("__HZAID_SCRIPT_END__\n")
        command.append("COPY_STATUS=\$?\n")
        command.append("if [ \"\$COPY_STATUS\" -ne 0 ]; then echo '[ERROR] No se pudo copiar el script'; exit 21; fi\n")
        command.append("chmod 755 '").append(remoteScript).append("' || exit 22\n")
        command.append("echo '[APP] Permisos establecidos: 755'\n")
        command.append("echo '[APP] UID efectivo:'\nid\n")
        command.append("echo '[APP] Ejecutando acción ").append(action).append("'\n")
        command.append("sh '").append(remoteScript).append("' ").append(action).append("\n")
        command.append("SCRIPT_STATUS=\$?\n")
        command.append("echo '[APP] Script finalizó con código '\"\$SCRIPT_STATUS\"\n")
        command.append("exit \$SCRIPT_STATUS\n")
        executeRoot("Acción del script " + action, command.toString())
    }

    private fun executeRoot(label: String, command: String, complete: ((Int, String) -> Unit)? = null) {
        synchronized(lock) {
            if (running) {
                addLog("[AVISO] Ya hay un proceso en ejecución; espera a que termine.")
                return
            }
            running = true
        }
        addLog("▶ " + label)
        Thread {
            var code = 255
            val output = StringBuilder()
            try {
                val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
                process.outputStream.close()
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        output.append(line).append('\n')
                        addLog(line)
                    }
                }
                code = process.waitFor()
            } catch (e: Exception) {
                val message = "[ERROR] No se pudo ejecutar su: " + e.javaClass.simpleName + ": " + (e.message ?: "sin detalle")
                output.append(message).append('\n')
                addLog(message)
            }
            val finalCode = code
            runOnUiThread {
                synchronized(lock) { running = false }
                addLog("Proceso finalizado · código " + finalCode)
                complete?.invoke(finalCode, output.toString())
                updateLogViews()
            }
        }.start()
    }

    private fun addLog(message: String) {
        val time = synchronized(SimpleDateFormat::class.java) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        }
        val line = "[" + time + "] " + message + "\n"
        synchronized(lock) {
            logBuffer.append(line)
            if (logBuffer.length > 150000) logBuffer.delete(0, logBuffer.length - 120000)
            try {
                logFile.appendText(line, Charsets.UTF_8)
                if (logFile.length() > 1000000) {
                    val tail = logFile.readText(Charsets.UTF_8).takeLast(600000)
                    logFile.writeText(tail, Charsets.UTF_8)
                }
            } catch (_: Exception) { }
        }
        runOnUiThread { updateLogViews() }
    }

    private fun updateLogViews() {
        val snapshot = synchronized(lock) { logBuffer.toString() }
        logsView?.text = snapshot
        terminalView?.text = snapshot
        logsView?.post {
            val p = logsView?.parent
            if (p is ScrollView) p.post { p.fullScroll(View.FOCUS_DOWN) }
        }
        terminalView?.post {
            val p = terminalView?.parent
            if (p is ScrollView) p.post { p.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun clearLogs() {
        synchronized(lock) {
            logBuffer.clear()
            try { logFile.writeText("", Charsets.UTF_8) } catch (_: Exception) { }
        }
        addLog("Registros limpiados.")
        toast("Registros limpiados")
    }

    private fun copyLogs() {
        val snapshot = synchronized(lock) { logBuffer.toString() }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Holograma Zaid logs", snapshot))
        toast("Registros copiados")
    }

    private fun button(textValue: String, primary: Boolean, action: () -> Unit): TextView =
        label(textValue, 12f, if (primary) Color.WHITE else sky, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(if (primary) sky else Color.WHITE, 14, if (primary) sky else lineColor)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun label(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            includeFontPadding = true
        }

    private fun rounded(fill: Int, radius: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun weightParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(42), 1f)

    private fun marginParams(bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(bottom) }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar") { _, _ -> action() }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onBackPressed() {
        if (currentScreen != "home") showScreen("home") else super.onBackPressed()
    }
}
