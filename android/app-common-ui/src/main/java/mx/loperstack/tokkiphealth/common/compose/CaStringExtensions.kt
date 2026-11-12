package mx.loperstack.tokkiphealth.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import mx.loperstack.tokkiphealth.common.ca.CaString

@Composable
@ReadOnlyComposable
fun CaString.asComposable(): String = get(LocalContext.current)
