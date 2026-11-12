package mx.loperstack.tokkiphealth.exclusion.core.types

data class DefaultExclusion(
    val reason: String,
    override val exclusion: Exclusion,
) : ExclusionHolder, Exclusion by exclusion