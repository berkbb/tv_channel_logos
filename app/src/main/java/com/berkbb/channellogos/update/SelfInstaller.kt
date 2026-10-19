package com.berkbb.channellogos.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.berkbb.channellogos.MainActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/**
 * Installs a newer Channel Logos APK over the app with the system PackageInstaller (the TV asks
 * for confirmation), as Pitwall Updater does. The app reopens after the update.
 */
object SelfInstaller {

    /** Result of the system installer, for the ViewModel. Success rarely arrives: the app is replaced first. */
    sealed interface Event {
        data object Success : Event
        data class Failure(val message: String) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    internal fun emit(event: Event) {
        _events.tryEmit(event)
    }

    /** Starts the installation; false when the session could not be written. */
    fun install(context: Context, apk: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        var session: PackageInstaller.Session? = null
        return try {
            val sessionId = installer.createSession(params)
            session = installer.openSession(sessionId)
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            RestartAfterUpdateReceiver.arm(context)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )
            session.commit(pendingIntent.intentSender)
            true
        } catch (e: Exception) {
            runCatching { session?.abandon() }
            RestartAfterUpdateReceiver.disarm(context)
            false
        } finally {
            runCatching { session?.close() }
        }
    }
}

/** Not exported: only the app's own PendingIntent (handed to the system installer) reaches it. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { context.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> SelfInstaller.emit(SelfInstaller.Event.Success)
            else -> {
                RestartAfterUpdateReceiver.disarm(context)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Code $status"
                SelfInstaller.emit(SelfInstaller.Event.Failure(message))
            }
        }
    }
}

/**
 * Reopens the app after it updated itself. Only when the update was started in the app
 * ([arm]), so an adb install stays quiet.
 */
class RestartAfterUpdateReceiver : BroadcastReceiver() {
    companion object {
        private const val PREFS = "self_update"
        private const val KEY_PENDING = "restart_after_update"

        fun arm(context: Context) = prefs(context).edit().putBoolean(KEY_PENDING, true).apply()
        fun disarm(context: Context) = prefs(context).edit().remove(KEY_PENDING).apply()
        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!prefs(context).getBoolean(KEY_PENDING, false)) return
        disarm(context)
        runCatching {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
