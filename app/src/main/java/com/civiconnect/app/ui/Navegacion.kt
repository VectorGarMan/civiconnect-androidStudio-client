package com.civiconnect.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.civiconnect.app.ui.screens.CrearReporteScreen
import com.civiconnect.app.ui.screens.DetalleScreen
import com.civiconnect.app.ui.screens.LoginScreen
import com.civiconnect.app.ui.screens.RegistroScreen
import com.civiconnect.app.ui.screens.ReportesScreen

@Composable
fun CiviApp(vm: CiviViewModel = viewModel()) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                vm = vm,
                onIrRegistro = { nav.navigate("registro") },
                onLoginExitoso = {
                    nav.navigate("reportes") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("registro") {
            RegistroScreen(vm = vm, onVolver = { nav.popBackStack() })
        }
        composable("reportes") {
            ReportesScreen(
                vm = vm,
                onDetalle = { id -> nav.navigate("detalle/$id") },
                onCrear = { nav.navigate("crear") },
                onSalir = {
                    vm.cerrarSesion()
                    nav.navigate("login") { popUpTo("reportes") { inclusive = true } }
                }
            )
        }
        composable(
            route = "detalle/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            DetalleScreen(
                vm = vm,
                idReporte = entrada.arguments?.getLong("id") ?: -1L,
                onVolver = { nav.popBackStack() }
            )
        }
        composable("crear") {
            CrearReporteScreen(vm = vm, onVolver = { nav.popBackStack() })
        }
    }
}
