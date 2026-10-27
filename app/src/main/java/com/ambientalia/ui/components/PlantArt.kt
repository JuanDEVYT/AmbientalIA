package com.ambientalia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifierimport androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ambientalia.R
import java.io.File

/**
 * Ilustración de cada planta del catálogo (drawables planos, sin red).
 * Si una planta escaneada no está en el catálogo se adivina por el nombre
 * y, si no, se usa el brote genérico.
 */
object PlantArt {

    private val porId = mapOf(
        "monstera" to R.drawable.planta_tropical,
        "ficus" to R.drawable.planta_tropical,
        "calatea" to R.drawable.planta_tropical,
        "kentia" to R.drawable.planta_tropical,
        "poto" to R.drawable.planta_colgante,
        "suculenta" to R.drawable.planta_suculenta,
        "aloe" to R.drawable.planta_suculenta,
        "cactus" to R.drawable.planta_cactus,
        "helecho" to R.drawable.planta_helecho,
        "orquidea" to R.drawable.planta_flor,
        "lavanda" to R.drawable.planta_flor,
        "rosa" to R.drawable.planta_flor,
        "pensamiento" to R.drawable.planta_flor,
        "sansevieria" to R.drawable.planta_hojas_altas,
        "bambu" to R.drawable.planta_hojas_altas,
        "tomate" to R.drawable.planta_hierbas,
        "albahaca" to R.drawable.planta_hierbas,
        "menta" to R.drawable.planta_hierbas,
    )

    private val porNombre = listOf(
        "cactus" to R.drawable.planta_cactus,
        "suculent" to R.drawable.planta_suculenta,
        "aloe" to R.drawable.planta_suculenta,
        "helech" to R.drawable.planta_helecho,
        "flor" to R.drawable.planta_flor,
        "rosa" to R.drawable.planta_flor,
        "orquid" to R.drawable.planta_flor,
        "lavand" to R.drawable.planta_flor,
        "colgant" to R.drawable.planta_colgante,
        "poto" to R.drawable.planta_colgante,
        "sansevier" to R.drawable.planta_hojas_altas,
        "bamb" to R.drawable.planta_hojas_altas,
        "lengua" to R.drawable.planta_hojas_altas,
        "ment" to R.drawable.planta_hierbas,
        "albahac" to R.drawable.planta_hierbas,
        "tomat" to R.drawable.planta_hierbas,
        "monstera" to R.drawable.planta_tropical,
        "ficus" to R.drawable.planta_tropical,
        "palmer" to R.drawable.planta_tropical,
        "calatea" to R.drawable.planta_tropical,
    )

    fun de(catalogId: String?, nombre: String = ""): Int {
        val id = catalogId?.lowercase()
        if (id != null) porId[id]?.let { return it }
        val n = nombre.lowercase()
        porNombre.forEach { (clave, recurso) -> if (n.contains(clave)) return recurso }
        return R.drawable.planta_default
    }
}

/**
 * Imagen de la planta (foto si el usuario la escaneó, ilustración si no).
 */
@Composable
fun PlantArt(
    nombre: String,
    catalogId: String?,
    foto: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null,
) {
    val ruta = foto
    if (ruta != null) {
        AsyncImage(
            model = File(ruta),
            contentDescription = nombre,
            contentScale = contentScale,
            colorFilter = colorFilter,
            modifier = modifier.size(size),
        )
        return
    }
    Image(
        painter = painterResource(id = PlantArt.de(catalogId, nombre)),
        contentDescription = nombre,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter,
        modifier = modifier.size(size),
    )
}
