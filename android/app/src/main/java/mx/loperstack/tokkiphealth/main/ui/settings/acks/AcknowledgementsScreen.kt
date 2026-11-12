package mx.loperstack.tokkiphealth.main.ui.settings.acks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.MaterialTheme
import mx.loperstack.tokkiphealth.common.compose.layout.SdmScaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.common.compose.layout.SdmTooltipIconButton
import mx.loperstack.tokkiphealth.common.compose.settings.SettingsCategoryHeader
import mx.loperstack.tokkiphealth.common.compose.settings.SettingsPreferenceItem
import mx.loperstack.tokkiphealth.common.error.ErrorEventHandler
import mx.loperstack.tokkiphealth.common.navigation.NavigationEventHandler

@Composable
fun AcknowledgementsScreenHost(
    vm: AcknowledgementsViewModel = hiltViewModel(),
) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    AcknowledgementsScreen(
        onNavigateUp = vm::navUp,
        onOpenUrl = vm::openUrl,
    )
}

@Composable
internal fun AcknowledgementsScreen(
    onNavigateUp: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {

    SdmScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_acknowledgements_label)) },
                navigationIcon = {
                    SdmTooltipIconButton(
                        icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                        label = stringResource(CommonR.string.general_navigate_up_action),
                        onClick = onNavigateUp,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Where Tokkip Health comes from
            SettingsCategoryHeader(text = stringResource(R.string.settings_acknowledgements_category_thanks_title))
            SettingsPreferenceItem(
                title = "SD Maid SE",
                subtitle = stringResource(R.string.settings_acknowledgements_sdmaid_body),
                onClick = { onOpenUrl("https://github.com/d4rken-org/sdmaid-se") },
            )
            SettingsPreferenceItem(
                title = "Tokkip",
                subtitle = stringResource(R.string.settings_acknowledgements_tokkip_body),
                onClick = { onOpenUrl("https://github.com/Andford/tokkiphealth") },
            )

            // Licenses
            SettingsCategoryHeader(text = stringResource(R.string.settings_acknowledgements_category_licenses_title))
            Text(
                text = stringResource(R.string.settings_acknowledgements_category_licenses_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            LicenseItem("libRootJava", "Run Java (and Kotlin) code as root! (APACHE 2.0)", "https://github.com/Chainfire/librootjava", onOpenUrl)
            LicenseItem("librootkotlinx", "Run rooted Kotlin JVM code made easy with coroutines. (APACHE 2.0)", "https://github.com/Mygod/librootkotlinx", onOpenUrl)
            LicenseItem("Shizuku", "Using system APIs directly with adb/root privileges from normal apps through a Java process started with app_process. (APACHE 2.0)", "https://github.com/RikkaApps/Shizuku", onOpenUrl)
            LicenseItem("Porter", "Privileged access for normal apps via adb, with a Shizuku-compatible client SDK. (APACHE 2.0)", "https://github.com/d4rken-org/porter", onOpenUrl)
            LicenseItem("Material Design Icons", "materialdesignicons.com (SIL Open Font License 1.1 / Attribution 4.0 International)", "https://github.com/Templarian/MaterialDesign", onOpenUrl)
            LicenseItem("Lottie", "Airbnb's Lottie for Android. (APACHE 2.0)", "https://github.com/airbnb/lottie-android", onOpenUrl)
            LicenseItem("Reorderable", "Reorderable Composables for Jetpack Compose. (APACHE 2.0)", "https://github.com/Calvin-LL/Reorderable", onOpenUrl)
            LicenseItem("ZoomImage", "Library for zoom images, supported Android View, Compose and Compose Multiplatform. (APACHE 2.0)", "https://github.com/panpf/zoomimage", onOpenUrl)
            LicenseItem("Kotlin", "The Kotlin Programming Language. (APACHE 2.0)", "https://github.com/JetBrains/kotlin", onOpenUrl)
            LicenseItem("Dagger", "A fast dependency injector for Android and Java. (APACHE 2.0)", "https://github.com/google/dagger", onOpenUrl)
            LicenseItem("Android", "Android Open Source Project (APACHE 2.0)", "https://source.android.com/source/licenses.html", onOpenUrl)
            LicenseItem("Android", "The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.", "https://developer.android.com/distribute/tools/promote/brand.html", onOpenUrl)
        }
    }
}

@Composable
private fun LicenseItem(
    title: String,
    subtitle: String,
    url: String,
    onOpenUrl: (String) -> Unit,
) {
    SettingsPreferenceItem(
        title = title,
        subtitle = subtitle,
        onClick = { onOpenUrl(url) },
    )
}

@Preview2
@Composable
private fun AcknowledgementsScreenPreview() {
    PreviewWrapper {
        AcknowledgementsScreen()
    }
}
