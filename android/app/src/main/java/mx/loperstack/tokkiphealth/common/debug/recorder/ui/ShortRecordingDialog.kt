package mx.loperstack.tokkiphealth.common.debug.recorder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.compose.dialog.SdmConfirmDialog
import mx.loperstack.tokkiphealth.common.compose.dialog.SdmDialogAction
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper

@Composable
fun ShortRecordingDialog(
    onContinue: () -> Unit,
    onStopAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    SdmConfirmDialog(
        title = stringResource(R.string.debug_debuglog_short_recording_title),
        message = stringResource(R.string.debug_debuglog_short_recording_desc),
        onDismissRequest = {
            onContinue()
            onDismiss()
        },
        positive = SdmDialogAction(
            label = stringResource(R.string.debug_debuglog_short_recording_continue_action),
            onClick = {
                onContinue()
                onDismiss()
            },
        ),
        negative = SdmDialogAction(
            label = stringResource(R.string.debug_debuglog_short_recording_stop_action),
            onClick = {
                onStopAnyway()
                onDismiss()
            },
        ),
    )
}

@Preview2
@Composable
private fun ShortRecordingDialogPreview() {
    PreviewWrapper {
        ShortRecordingDialog(
            onContinue = {},
            onStopAnyway = {},
            onDismiss = {},
        )
    }
}
