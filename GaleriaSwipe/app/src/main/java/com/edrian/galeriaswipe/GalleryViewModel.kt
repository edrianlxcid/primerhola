package com.edrian.galeriaswipe

import android.app.Application
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Una decisión tomada al deslizar, para poder deshacerla. */
private data class Accion(val id: Long, val aPapelera: Boolean)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("decisiones", Context.MODE_PRIVATE)

    var fotos by mutableStateOf<List<Photo>>(emptyList())
        private set
    var cargando by mutableStateOf(true)
        private set

    /** Fotos que decidiste conservar (deslizar a la derecha). Se guardan entre sesiones. */
    var conservadas by mutableStateOf(leer(CLAVE_CONSERVADAS))
        private set

    /** Fotos en la papelera (deslizar a la izquierda), pendientes de confirmar. */
    var enPapelera by mutableStateOf(leer(CLAVE_PAPELERA))
        private set

    private val historial = mutableStateListOf<Accion>()

    /** Mensaje corto para mostrar abajo (snackbar). */
    var mensaje by mutableStateOf<String?>(null)

    val porRevisar: List<Photo> by derivedStateOf {
        val c = conservadas
        val p = enPapelera
        fotos.filter { it.id !in c && it.id !in p }
    }

    val papelera: List<Photo> by derivedStateOf {
        val p = enPapelera
        fotos.filter { it.id in p }
    }

    val puedeDeshacer: Boolean get() = historial.isNotEmpty()

    private var trabajoCarga: Job? = null

    fun cargar() {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            if (fotos.isEmpty()) cargando = true
            val lista = PhotoRepository.cargarFotos(getApplication())
            if (lista != null) {
                fotos = lista
                // Olvidar fotos que ya no existen (p. ej. borradas desde otra app).
                val ids = lista.mapTo(HashSet()) { it.id }
                val c = conservadas.filterTo(LinkedHashSet()) { it in ids }
                if (c.size != conservadas.size) {
                    conservadas = c
                    guardar(CLAVE_CONSERVADAS, c)
                }
                val p = enPapelera.filterTo(LinkedHashSet()) { it in ids }
                if (p.size != enPapelera.size) {
                    enPapelera = p
                    guardar(CLAVE_PAPELERA, p)
                }
                historial.removeAll { it.id !in ids }
            }
            cargando = false
        }
    }

    fun conservar(foto: Photo) {
        conservadas = conservadas + foto.id
        guardar(CLAVE_CONSERVADAS, conservadas)
        registrar(Accion(foto.id, aPapelera = false))
    }

    fun mandarAPapelera(foto: Photo) {
        enPapelera = enPapelera + foto.id
        guardar(CLAVE_PAPELERA, enPapelera)
        registrar(Accion(foto.id, aPapelera = true))
    }

    fun deshacer() {
        val ultima = historial.removeLastOrNull() ?: return
        if (ultima.aPapelera) {
            enPapelera = enPapelera - ultima.id
            guardar(CLAVE_PAPELERA, enPapelera)
        } else {
            conservadas = conservadas - ultima.id
            guardar(CLAVE_CONSERVADAS, conservadas)
        }
    }

    /** Saca una foto de la papelera y la marca como conservada. */
    fun restaurar(id: Long) {
        enPapelera = enPapelera - id
        conservadas = conservadas + id
        guardar(CLAVE_PAPELERA, enPapelera)
        guardar(CLAVE_CONSERVADAS, conservadas)
        historial.removeAll { it.id == id }
    }

    fun restaurarTodas() {
        val ids = enPapelera
        enPapelera = emptySet()
        conservadas = conservadas + ids
        guardar(CLAVE_PAPELERA, enPapelera)
        guardar(CLAVE_CONSERVADAS, conservadas)
        historial.removeAll { it.id in ids }
        mensaje = "Se recuperaron ${ids.size} fotos"
    }

    /** Vuelve a mostrar las fotos que ya habías conservado. */
    fun revisarDeNuevo() {
        conservadas = emptySet()
        guardar(CLAVE_CONSERVADAS, conservadas)
        historial.removeAll { !it.aPapelera }
    }

    /** Se llama cuando vuelve el diálogo del sistema (Android 11+). */
    fun despuesDeEliminar(confirmado: Boolean) {
        if (confirmado) {
            mensaje = "Fotos eliminadas"
            cargar()
        } else {
            mensaje = "No se eliminó nada"
        }
    }

    /** Android 10 o anterior: borra sin diálogo del sistema. */
    fun eliminarDirecto() {
        val uris = papelera.map { it.uri }
        viewModelScope.launch {
            val borradas = PhotoRepository.borrarDirecto(getApplication(), uris)
            mensaje = if (borradas == uris.size) {
                "Se eliminaron $borradas fotos"
            } else {
                "Se eliminaron $borradas de ${uris.size} fotos"
            }
            cargar()
        }
    }

    private fun registrar(a: Accion) {
        historial.add(a)
        if (historial.size > MAX_HISTORIAL) historial.removeAt(0)
    }

    private fun leer(clave: String): Set<Long> =
        prefs.getStringSet(clave, emptySet())
            .orEmpty()
            .mapNotNullTo(LinkedHashSet()) { it.toLongOrNull() }

    private fun guardar(clave: String, ids: Set<Long>) {
        prefs.edit().putStringSet(clave, ids.mapTo(HashSet()) { it.toString() }).apply()
    }

    private companion object {
        const val CLAVE_CONSERVADAS = "conservadas"
        const val CLAVE_PAPELERA = "papelera"
        const val MAX_HISTORIAL = 100
    }
}
