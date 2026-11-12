package mx.loperstack.tokkiphealth.common.sieve

import mx.loperstack.tokkiphealth.common.files.Segments

sealed interface CriteriaOperator : Criterium {

    fun match(target: Segments): Boolean

    data class And(
        val criteria: List<Criterium>,
    ) : CriteriaOperator {
        constructor(vararg crits: Criterium) : this(crits.asList())

        override fun match(target: Segments): Boolean = criteria.all { crit ->
            when (crit) {
                is NameCriterium -> crit.match(target.last())
                is SegmentCriterium -> crit.match(target)
                is CriteriaOperator -> crit.match(target)
            }
        }
    }

    data class Or(
        val criteria: List<Criterium>,
    ) : CriteriaOperator {
        constructor(vararg crits: Criterium) : this(crits.asList())

        override fun match(target: Segments): Boolean = criteria.any { crit ->
            when (crit) {
                is NameCriterium -> crit.match(target.last())
                is SegmentCriterium -> crit.match(target)
                is CriteriaOperator -> crit.match(target)
            }
        }
    }
}