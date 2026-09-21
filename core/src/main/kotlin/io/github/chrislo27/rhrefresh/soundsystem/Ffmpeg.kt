package io.github.chrislo27.rhrefresh.soundsystem

import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.soundsystem.SoundStretch.currentOS
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.process.ProcessLocator
import java.io.File
import java.util.Locale


/**
 * A simple wrapper around the [SoundStretch](https://www.surina.net/soundtouch/soundstretch.html) executables.
 */
object Ffmpeg{

    enum class OS(val supported: Boolean, val executableName: String) {
        UNSUPPORTED(false, ""),
        WINDOWS(true, "ffmpeg_win.exe"),
        MACOS(true, "ffmpeg_macOS"),
        LINUX(true, "ffmpeg_linux");

        companion object {
            val ALL_VALUES: List<OS> = values().toList()
            val SUPPORTED: List<OS> = ALL_VALUES - UNSUPPORTED
        }
    }

    val currentOS: OS = try {
        val osName: String = System.getProperty("os.name", "???")?.toLowerCase(Locale.ROOT) ?: "???"
        when {
            "win" in osName -> OS.WINDOWS
            "mac" in osName -> OS.MACOS
            osName.startsWith("linux") -> OS.LINUX
            else -> OS.UNSUPPORTED
        }
    } catch (e: Exception) {
        e.printStackTrace()
        OS.UNSUPPORTED
    }
    val isSupported: Boolean get() = currentOS.supported

    // Return the encoder built for the platform, otherwise uhhhhh
    fun makeEncoder(): Encoder{
        return if(isSupported){
            Encoder(ProcessLocator { RHREfresh.FFMPEG_FOLDER.child(currentOS.executableName).file().absolutePath })
        } else{
            Encoder()
        }
    }

    fun createMultimediaObject(file: File): MultimediaObject{
        return if(isSupported){
            MultimediaObject(file, ProcessLocator { RHREfresh.FFMPEG_FOLDER.child(currentOS.executableName).file().absolutePath })
        }else {
            MultimediaObject(file)
        }
    }
}