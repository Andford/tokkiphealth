package mx.loperstack.tokkiphealth.analyzer.ui.storage.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkiphealth.analyzer.R
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper

@Composable
internal fun ContentInfoBanner(
    modifier: Modifier = Modifier,
    text: CaString,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            text = text.get(LocalContext.current),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview2
@Composable
private fun ContentInfoBannerPreview() {
    PreviewWrapper {
        ContentInfoBanner(text = R.string.analyzer_storage_content_type_system_info.toCaString())
    }
}

@Preview2
@Composable
private fun ContentInfoBannerMediaReadOnlyPreview() {
    PreviewWrapper {
        ContentInfoBanner(text = R.string.analyzer_storage_content_type_media_readonly_info.toCaString())
    }
}
