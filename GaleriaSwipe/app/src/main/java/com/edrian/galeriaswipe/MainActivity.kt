package com.edrian.galeriaswipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edrian.galeriaswipe.ui.PantallaPermiso
import com.edrian.galeriaswipe.ui.PantallaPapelera
import com.edrian.galeriaswipe.ui.PantallaSwipe
import com.edrian.galeriaswipe.ui.TemaGaleria

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaGaleria {
                App()
            }
        }
    }
}

@Composable
fun App(vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var acceso by remember { mutableStateOf(Permisos.acceso(context)) }
    var verPapelera by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        acceso = Permisos.acceso(context)
        if (acceso != Acceso.NINGUNO) vm.cargar()
    }

    // Cada vez que la app vuelve a primer plano: revisar permisos y recargar la galería.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        acceso = Permisos.acceso(context)
        if (acceso != Acceso.NINGUNO) vm.cargar()
    }

    val dialogoBorrar = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { resultado ->
        vm.despuesDeEliminar(resultado.resultCode == Activity.RESULT_OK)
    }

    val eliminarPapelera: () -> Unit = {
        val uris = vm.papelera.map { it.uri }
        if (uris.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= 30) {
                val pendiente = MediaStore.createDeleteRequest(context.contentResolver, uris)
                dialogoBorrar.launch(IntentSenderRequest.Builder(pendiente.intentSender).build())
            } else {
                vm.eliminarDirecto()
            }
        }
    }

    LaunchedEffect(vm.mensaje) {
        vm.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensaje = null
        }
    }

    when {
        acceso == Acceso.NINGUNO -> PantallaPermiso(
            onPedir = { pedirPermiso.launch(Permisos.requeridos()) },
            onAbrirAjustes = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                )
            },
        )

        verPapelera -> PantallaPapelera(
            vm = vm,
            snackbar = snackbar,
            onVolver = { verPapelera = false },
            onEliminar = eliminarPapelera,
        )

        else -> PantallaSwipe(
            vm = vm,
            snackbar = snackbar,
            accesoParcial = acceso == Acceso.PARCIAL,
            onElegirMasFotos = { pedirPermiso.launch(Permisos.requeridos()) },
            onAbrirPapelera = { verPapelera = true },
        )
    }
}
