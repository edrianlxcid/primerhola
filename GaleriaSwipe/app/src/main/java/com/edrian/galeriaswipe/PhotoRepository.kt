package com.edrian.galeriaswipe

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un álbum (carpeta) de la galería, con cuántas fotos le faltan por revisar. */
data class Album(
    val id: Long,
    val nombre: String,
    val total: Int,
    val porRevisar: Int,
    val portada: Uri,
)

enum class Orden(val texto: String) {
    RECIENTES("Más recientes primero"),
    ANTIGUAS("Más antiguas primero"),
}

data class Photo(
    val id: Long,
    val uri: Uri,
    val nombre: String,
    val albumId: Long,
    val album: String,
    /** Milisegundos desde 1970. */
    val fecha: Long,
)

enum class Acceso { TOTAL, PARCIAL, NINGUNO }

object Permisos {

    fun requeridos(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        Build.VERSION.SDK_INT >= 30 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    fun acceso(context: Context): Acceso {
        fun tiene(p: String) =
            ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= 33 && tiene(Manifest.permission.READ_MEDIA_IMAGES) -> Acceso.TOTAL
            Build.VERSION.SDK_INT >= 34 &&
                tiene(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Acceso.PARCIAL
            Build.VERSION.SDK_INT >= 33 -> Acceso.NINGUNO
            Build.VERSION.SDK_INT >= 30 &&
                tiene(Manifest.permission.READ_EXTERNAL_STORAGE) -> Acceso.TOTAL
            Build.VERSION.SDK_INT < 30 &&
                tiene(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                tiene(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> Acceso.TOTAL
            else -> Acceso.NINGUNO
        }
    }
}

object PhotoRepository {

    /** Devuelve todas las fotos, de la más nueva a la más vieja, o null si falló la consulta. */
    suspend fun cargarFotos(context: Context): List<Photo>? = withContext(Dispatchers.IO) {
        val coleccion = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val columnas = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val orden = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val fotos = ArrayList<Photo>()
            context.contentResolver.query(coleccion, columnas, null, null, orden)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val iNombre = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val iTomada = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val iAgregada = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val tomada = if (c.isNull(iTomada)) 0L else c.getLong(iTomada)
                    val fecha = if (tomada > 0) tomada else c.getLong(iAgregada) * 1000L
                    fotos += Photo(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        ),
                        nombre = c.getString(iNombre) ?: "",
                        albumId = if (c.isNull(iAlbumId)) 0L else c.getLong(iAlbumId),
                        album = c.getString(iAlbum) ?: "",
                        fecha = fecha,
                    )
                }
            } ?: return@withContext null
            fotos
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Borrado directo, solo para Android 10 o anterior (en Android 11+ se usa
     * MediaStore.createDeleteRequest, que muestra el diálogo del sistema).
     * Devuelve cuántas se borraron.
     */
    suspend fun borrarDirecto(context: Context, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var borradas = 0
        for (uri in uris) {
            try {
                borradas += context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
        }
        borradas
    }
}
