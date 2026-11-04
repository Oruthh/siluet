package dev.siluet.reflect

interface SecurityPolicy {
    fun checkAccess(targetClass: Class<*>, memberName: String)

    companion object {
        val Default: SecurityPolicy = DefaultSecurityPolicy()
    }
}