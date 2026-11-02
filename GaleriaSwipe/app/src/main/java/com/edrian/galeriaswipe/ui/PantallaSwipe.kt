package com.edrian.galeriaswipe.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.edrian.galeriaswipe.GalleryViewModel
import com.edrian.galeriaswipe.Photo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

/** Qué tan lejos (en fracción del ancho) hay que arrastrar para decidir. */
private const val UMBRAL = 0.28f

/** Posición de una tarjeta mientras se arrastra. */
class EstadoTarjeta {
    val x = Animatable(0f)
    val y = Animatable(0f)
    var ancho by mutableFloatStateOf(1000f)

    /** true en cuanto la tarjeta empieza a salir, para no decidir dos veces. */
    var decidida = false

    suspend fun salir(izquierda: Boolean) {
        decidida = true
        x.animateTo(if (izquierda) -ancho * 1.5f else ancho * 1.5f, tween(220))
    }

    suspend fun volver() = coroutineScope {
        launch { x.animateTo(0f, spring()) }
        launch { y.animateTo(0f, spring()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSwipe(
    vm: GalleryViewModel,
    snackbar: SnackbarHostState,
    accesoParcial: Boolean,
    onElegirMasFotos: () -> Unit,
    onAbrirPapelera: () -> Unit,
) {
    val cola = vm.porRevisar
    val enPapelera = vm.enPapelera.size
    val scope = rememberCoroutineScope()
    val estados = remember { HashMap<Long, EstadoTarjeta>() }

    fun decidir(foto: Photo, izquierda: Boolean) {
        estados.remove(foto.id)
        if (izquierda) vm.mandarAPapelera(foto) else vm.conservar(foto)
    }

    fun botonSwipe(izquierda: Boolean) {
        val foto = cola.firstOrNull() ?: return
        val estado = estados.getOrPut(foto.id) { EstadoTarjeta() }
        if (estado.decidida) return
        scope.launch {
            estado.salir(izquierda)
            decidir(foto, izquierda)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Galería Swipe", fontWeight = FontWeight.Bold)
                        Text(
                            if (vm.cargando) "Cargando…" else "${cola.size} por revisar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAbrirPapelera) {
                        BadgedBox(badge = {
                            if (enPapelera > 0) Badge { Text("$enPapelera") }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Papelera")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (accesoParcial) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Solo ves las fotos que elegiste.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onElegirMasFotos) { Text("Elegir más") }
                    }
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    vm.cargando && vm.fotos.isEmpty() -> CircularProgressIndicator()

                    vm.fotos.isEmpty() -> Text(
                        "No se encontraron fotos en tu teléfono.",
                        textAlign = TextAlign.Center,
                    )

                    cola.isEmpty() -> Terminado(
                        enPapelera = enPapelera,
                        hayConservadas = vm.conservadas.isNotEmpty(),
                        onAbrirPapelera = onAbrirPapelera,
                        onRevisarDeNuevo = vm::revisarDeNuevo,
                    )

                    else -> {
                        // Se dibujan 2 tarjetas: la de arriba y la siguiente debajo.
                        val visibles = cola.take(2)
                        val tope = visibles.first()
                        for (foto in visibles.asReversed()) {
                            key(foto.id) {
                                val estado = estados.getOrPut(foto.id) { EstadoTarjeta() }
                                TarjetaFoto(
                                    foto = foto,
                                    estado = estado,
                                    arriba = foto.id == tope.id,
                                    onDecision = { izquierda -> decidir(foto, izquierda) },
                                )
                            }
                        }
                    }
                }
            }

            // Botones de abajo
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = { botonSwipe(izquierda = true) },
                    enabled = cola.isNotEmpty(),
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Rojo),
                ) {
                    Icon(Icons.Filled.Close, "A la papelera", Modifier.size(34.dp))
                }
                OutlinedIconButton(
                    onClick = vm::deshacer,
                    enabled = vm.puedeDeshacer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.Filled.Refresh, "Deshacer")
                }
                FilledIconButton(
                    onClick = { botonSwipe(izquierda = false) },
                    enabled = cola.isNotEmpty(),
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Verde),
                ) {
                    Icon(Icons.Filled.Favorite, "Conservar", Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun TarjetaFoto(
    foto: Photo,
    estado: EstadoTarjeta,
    arriba: Boolean,
    onDecision: (izquierda: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val decisionActual by rememberUpdatedState(onDecision)
    val forma = RoundedCornerShape(24.dp)
    val escala by animateFloatAsState(if (arriba) 1f else 0.94f, label = "escala")

    val gestos = if (arriba) {
        Modifier.pointerInput(foto.id) {
            detectDragGestures(
                onDragEnd = {
                    scope.launch {
                        val dx = estado.x.value
                        if (abs(dx) > estado.ancho * UMBRAL && !estado.decidida) {
                            val izquierda = dx < 0
                            estado.salir(izquierda)
                            decisionActual(izquierda)
                        } else {
                            estado.volver()
                        }
                    }
                },
                onDragCancel = { scope.launch { estado.volver() } },
                onDrag = { cambio, arrastre ->
                    cambio.consume()
                    scope.launch {
                        estado.x.snapTo(estado.x.value + arrastre.x)
                        estado.y.snapTo(estado.y.value + arrastre.y)
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { estado.ancho = it.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationX = estado.x.value
                translationY = estado.y.value
                rotationZ = estado.x.value / estado.ancho * 12f
                scaleX = escala
                scaleY = escala
            }
            .then(gestos)
            .shadow(10.dp, forma)
            .clip(forma)
            .background(Color.Black)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(foto.uri)
                .crossfade(true)
                .build(),
            contentDescription = foto.nombre,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        // Fecha y álbum abajo
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                DateFormat.getDateInstance(DateFormat.LONG).format(Date(foto.fecha)),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            if (foto.album.isNotBlank()) {
                Text(
                    foto.album,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Etiquetas mientras arrastras
        if (arriba) {
            val progreso = (estado.x.value / (estado.ancho * UMBRAL)).coerceIn(-1f, 1f)
            if (progreso < 0f) {
                Etiqueta(
                    "PAPELERA", Rojo, -progreso, 15f,
                    Modifier.align(Alignment.TopEnd).padding(24.dp),
                )
            } else if (progreso > 0f) {
                Etiqueta(
                    "SE QUEDA", Verde, progreso, -15f,
                    Modifier.align(Alignment.TopStart).padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color, alfa: Float, giro: Float, modifier: Modifier) {
    Surface(
        modifier = modifier
            .rotate(giro)
            .alpha(alfa),
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(3.dp, color),
    ) {
        Text(
            texto,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Terminado(
    enPapelera: Int,
    hayConservadas: Boolean,
    onAbrirPapelera: () -> Unit,
    onRevisarDeNuevo: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎉", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "¡Revisaste todas tus fotos!",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (enPapelera > 0) {
            Button(onClick = onAbrirPapelera) {
                Text("Ver papelera ($enPapelera)")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (hayConservadas) {
            OutlinedButton(onClick = onRevisarDeNuevo) {
                Text("Volver a revisar las que conservé")
            }
        }
    }
}
