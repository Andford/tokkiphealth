package mx.loperstack.tokkiphealth.common.sieve

import mx.loperstack.tokkiphealth.common.files.Segments

interface FileSieve {

    fun Collection<Criterium>.match(target: Segments): Boolean = any { crit ->
        when (crit) {
            is NameCriterium -> crit.match(target.last())
            is SegmentCriterium -> crit.match(target)
            is CriteriaOperator -> crit.match(target)
        }
    }

    fun Collection<NameCriterium>.matchAny(target: String): Boolean = any { crit -> crit.match(target) }

    fun Collection<SegmentCriterium>.matchAny(target: Segments): Boolean = any { crit -> crit.match(target) }

}