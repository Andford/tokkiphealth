package mx.loperstack.tokkiphealth.main.ui.settings.support.contactform.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper

@Composable
fun ContactFormExpectedCard(
    modifier: Modifier = Modifier,
    expected: String,
    wordCount: Int,
    minWords: Int,
    onExpectedChange: (String) -> Unit,
) {
    ContactFormTextFieldCard(
        value = expected,
        labelRes = R.string.support_contact_expected_label,
        hint = stringResource(R.string.support_contact_expected_hint),
        wordCount = wordCount,
        minWords = minWords,
        minLines = 3,
        onValueChange = onExpectedChange,
        modifier = modifier,
    )
}

@Preview2
@Composable
private fun ContactFormExpectedCardPreview() {
    PreviewWrapper {
        ContactFormExpectedCard(
            expected = "",
            wordCount = 0,
            minWords = 10,
            onExpectedChange = {},
        )
    }
}
