package dev.opendroid.device

import android.content.Context
import com.zed.nullshell.Prefs
import com.zed.nullshell.RootfsManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes shell commands directly inside RawDog's Kali Linux userspace (PRoot)
 * or device shell, returning execution output, exit code, and timing.
 */
object RawDogShellExecutor {

    suspend fun execute(context: Context, commandStr: String, timeoutSec: Long = 60): String = withContext(Dispatchers.IO) {
        val rootfsInstalled = RootfsManager.isInstalled(context)
        val startTime = System.currentTimeMillis()

        try {
            val pb: ProcessBuilder
            if (rootfsInstalled) {
                val rootfs = RootfsManager.dir(context)
                val cmdList = mutableListOf<String>()
                val nativeDir = File(context.applicationInfo.nativeLibraryDir)
                val prootBinary = File(nativeDir, "libproot.so")

                if (prootBinary.isFile) {
                    cmdList.add(prootBinary.absolutePath)
                    cmdList.add("--kill-on-exit")
                    cmdList.add("--link2symlink")
                    cmdList.add("-0")
                    cmdList.add("-r")
                    cmdList.add(rootfs.absolutePath)
                    cmdList.add("-b")
                    cmdList.add("/dev")
                    cmdList.add("-b")
                    cmdList.add("/proc")
                    if (Prefs.bindSys(context)) {
                        cmdList.add("-b")
                        cmdList.add("/sys")
                    }
                    if (Prefs.bindSdcard(context)) {
                        cmdList.add("-b")
                        cmdList.add("/sdcard")
                    }
                    cmdList.add("-w")
                    cmdList.add(Prefs.workdir(context))

                    // Guest command
                    cmdList.add("/usr/bin/env")
                    cmdList.add("-i")
                    cmdList.add("HOME=/root")
                    cmdList.add("USER=root")
                    cmdList.add("LOGNAME=root")
                    cmdList.add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
                    cmdList.add("LANG=C.UTF-8")
                    cmdList.add("TMPDIR=/tmp")
                    cmdList.add("/bin/sh")
                    cmdList.add("-c")
                    cmdList.add(commandStr)

                    pb = ProcessBuilder(cmdList)
                    pb.environment()["PROOT_LOADER"] = File(nativeDir, "libproot-loader.so").absolutePath
                    val loader32 = File(nativeDir, "libproot-loader32.so")
                    if (loader32.isFile) {
                        pb.environment()["PROOT_LOADER_32"] = loader32.absolutePath
                    }
                    pb.environment()["LD_LIBRARY_PATH"] = nativeDir.absolutePath
                    pb.environment()["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
                    pb.directory(context.filesDir)
                } else {
                    pb = ProcessBuilder("/system/bin/sh", "-c", commandStr)
                    pb.directory(context.filesDir)
                }
            } else {
                pb = ProcessBuilder("/system/bin/sh", "-c", commandStr)
                pb.directory(context.filesDir)
            }

            pb.redirectErrorStream(true)
            val process = pb.start()

            val outputBuffer = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val maxChars = 32768

            while (reader.readLine().also { line = it } != null) {
                if (outputBuffer.length < maxChars) {
                    outputBuffer.append(line).append("\n")
                } else if (outputBuffer.length == maxChars) {
                    outputBuffer.append("\n[... output truncated after 32KB ...]\n")
                }
            }

            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            val durationMs = System.currentTimeMillis() - startTime

            if (!finished) {
                process.destroyForcibly()
                return@withContext "[RawDog Shell: Command timed out after ${timeoutSec}s]\n${outputBuffer}"
            }

            val exitCode = process.exitValue()
            val statusHeader = "[RawDog Shell | Exit: $exitCode | ${durationMs}ms]\n"
            statusHeader + if (outputBuffer.isEmpty()) "(no output)" else outputBuffer.toString().trimEnd()
        } catch (e: Exception) {
            "[RawDog Shell Error: ${e.message}]"
        }
    }
}
