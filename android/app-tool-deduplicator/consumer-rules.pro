# Arbiter criteria are logged by class name (ArbiterConfigViewModel); keep the names readable in
# obfuscated gplay logs. Names only, members and shrinking unaffected.
-keepnames class * implements mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCriterium
