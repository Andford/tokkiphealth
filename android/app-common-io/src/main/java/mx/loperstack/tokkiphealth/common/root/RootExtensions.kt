package mx.loperstack.tokkiphealth.common.root

import kotlinx.coroutines.flow.first

suspend fun RootManager.canUseRootNow(): Boolean = useRoot.first()