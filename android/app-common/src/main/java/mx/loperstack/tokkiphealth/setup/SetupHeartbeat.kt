package mx.loperstack.tokkiphealth.setup

fun interface SetupHeartbeat {
    /**
     * @throws Exception if setup is incomplete
     */
    suspend fun checkOrThrow()
}
