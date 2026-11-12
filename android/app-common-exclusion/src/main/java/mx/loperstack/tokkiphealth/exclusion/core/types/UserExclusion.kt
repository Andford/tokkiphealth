package mx.loperstack.tokkiphealth.exclusion.core.types

data class UserExclusion(
    override val exclusion: Exclusion,
) : ExclusionHolder, Exclusion by exclusion