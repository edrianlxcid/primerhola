package com.edrian.galeriaswipe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.edrian.galeriaswipe.Album
import com.edrian.galeriaswipe.GalleryViewModel
import com.edrian.galeriaswipe.Orden

/** Fila con los botones "Orden" y "Álbum" que va arriba de las tarjetas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraFiltros(vm: GalleryViewModel, modifier: Modifier = Modifier) {
    var menuOrden by remember { mutableStateOf(false) }
    var hojaAlbumes by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            AssistChip(
                onClick = { menuOrden = true },
                label = {
                    Text(if (vm.orden == Orden.RECIENTES) "Recientes" else "Antiguas")
                },
                leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp)) },
            )
            DropdownMenu(expanded = menuOrden, onDismissRequest = { menuOrden = false }) {
                Orden.entries.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(o.texto) },
                        onClick = {
                            vm.cambiarOrden(o)
                            menuOrden = false
                        },
                        leadingIcon = {
                            if (o == vm.orden) Icon(Icons.Filled.Check, null)
                            else Spacer(Modifier.size(24.dp))
                        },
                    )
                }
            }
        }

        AssistChip(
            onClick = { hojaAlbumes = true },
            label = {
                Text(
                    vm.nombreAlbum ?: "Todos los álbumes",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f, fill = false),
        )
    }

    if (hojaAlbumes) {
        ModalBottomSheet(
            onDismissRequest = { hojaAlbumes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        ) {
            ListaAlbumes(
                albumes = vm.albumes,
                seleccionado = vm.albumSeleccionado,
                totalPorRevisar = vm.albumes.sumOf { it.porRevisar },
                totalFotos = vm.fotos.size,
                onElegir = { id ->
                    vm.elegirAlbum(id)
                    hojaAlbumes = false
                },
            )
        }
    }
}

@Composable
private fun ListaAlbumes(
    albumes: List<Album>,
    seleccionado: Long?,
    totalPorRevisar: Int,
    totalFotos: Int,
    onElegir: (Long?) -> Unit,
) {
    Column(Modifier.navigationBarsPadding()) {
        Text(
            "Elige un álbum",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn {
            item {
                FilaAlbum(
                    nombre = "Todos los álbumes",
                    detalle = "$totalPorRevisar por revisar de $totalFotos",
                    portada = albumes.firstOrNull()?.portada,
                    elegido = seleccionado == null,
                    onClick = { onElegir(null) },
                )
            }
            items(albumes, key = { it.id }) { a ->
                FilaAlbum(
                    nombre = a.nombre,
                    detalle = if (a.porRevisar == 0) {
                        "✓ Revisado · ${a.total} fotos"
                    } else {
                        "${a.porRevisar} por revisar de ${a.total}"
                    },
                    portada = a.portada,
                    elegido = seleccionado == a.id,
                    onClick = { onElegir(a.id) },
                )
            }
        }
    }
}

@Composable
private fun FilaAlbum(
    nombre: String,
    detalle: String,
    portada: android.net.Uri?,
    elegido: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (elegido) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.DarkGray)
        ) {
            if (portada != null) {
                AsyncImage(
                    model = portada,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                nombre,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (elegido) {
            Icon(Icons.Filled.Check, "Elegido", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
