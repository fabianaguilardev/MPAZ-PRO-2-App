package cl.duoc.mpazpro.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Envoltorio sencillo de TextToSpeech para leer las instrucciones en español.
 *
 * - Se inicializa de forma asíncrona: hasta que el motor avisa que está listo, no lee.
 * - Intenta español de Chile y, si no está instalado, español genérico.
 * - [onTerminar] se llama cuando termina de leer o si falla, para actualizar el botón.
 */
class LectorInstrucciones(
    context: Context,
    private val onTerminar: () -> Unit
) {
    private var listo = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { estado ->
            val motor = tts
            if (estado == TextToSpeech.SUCCESS && motor != null) {
                val idiomaChile = motor.setLanguage(Locale("es", "CL"))
                val resultado = if (idiomaChile >= TextToSpeech.LANG_AVAILABLE) {
                    idiomaChile
                } else {
                    motor.setLanguage(Locale("es"))
                }
                listo = resultado >= TextToSpeech.LANG_AVAILABLE

                motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = onTerminar()

                    @Deprecated("Requerido por la clase base")
                    override fun onError(utteranceId: String?) = onTerminar()
                })
            }
        }
    }

    /** Lee el texto. Devuelve false si el motor o el idioma español no están disponibles. */
    fun leer(texto: String): Boolean {
        if (!listo) return false
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "instruccion")
        return true
    }

    /** Corta la lectura en curso. */
    fun detener() {
        tts?.stop()
    }

    /** Libera el motor; debe llamarse al salir de la pantalla. */
    fun liberar() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        listo = false
    }
}
