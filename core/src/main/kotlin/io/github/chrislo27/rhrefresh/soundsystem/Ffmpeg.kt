package io.github.chrislo27.rhrefresh.soundsystem

import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.toolboks.Toolboks
import io.github.chrislo27.toolboks.logging.Logger
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.ArgType
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.encode.ValueArgument
import ws.schild.jave.info.MultimediaInfo
import ws.schild.jave.process.ProcessLocator
import ws.schild.jave.progress.EncoderProgressListener
import java.io.File
import java.util.Locale
import java.util.Optional
import kotlin.math.pow


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
    fun createEncoder(): Encoder{
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

    /**
     * Returns a WAV output stream with SoundStretch having applied the result.
     * Throws an error if the change parameters are not in bounds.
     * @param executableDir The File object pointing to the directory of the executables
     * @param input A WAV file
     * @param output The file to which to output the modified WAV data
     * @param tempoPercent Changes the sound tempo by this amount of percents. See [TEMPO_CHANGE_RANGE]
     * @param pitchSemitones Changes the sound pitch by this amount of semitones. See [PITCH_CHANGE_RANGE]
     * @param ratePercent Changes the sound rate by this amount of percents. See [RATE_CHANGE_RANGE]
     * @param quick Enables the -quick parameter. Gains speed but will probably lose quality.
     */
    fun processStreams(input: File, output: File, tempoPercent: Float, pitchSemitones: Float, ratePercent: Float, quick: Boolean) {

        val audio = AudioAttributes();
        audio.setCodec("pcm_s16le")
        val attrs = EncodingAttributes()
        attrs.setAudioAttributes(audio)

        val encoder = createEncoder()
        var filterChain = ""
        if(tempoPercent>1f){
            filterChain += "atempo=$tempoPercent"
        } else if(tempoPercent<1f) {
            val tempoMultiplier = tempoPercent.toDouble().pow(0.2)
            for(i in 1..5){
                filterChain += "atempo=$tempoMultiplier,"
            }
        }
        if(pitchSemitones!=0f){
            if(!filterChain.endsWith(",")) filterChain+=","
            filterChain += "rubberband=pitch="+(2.0.pow(pitchSemitones.div(12).toDouble()))
        }
        if(filterChain.isNotEmpty()){
            Encoder.setOptionAtIndex(ValueArgument(ArgType.OUTFILE, "-af") { Optional.of(filterChain) }, 33)
        } else{
            Encoder.removeOptionAtIndex(33)
        }
        val multimediaFile = createMultimediaObject(input)
        Toolboks.LOGGER.info("FFMPEG ran for file ${input.path} with arguments `$filterChain`")
        encoder.encode(multimediaFile, output, attrs)
    }
}