package liquibase.lockservice

import liquibase.database.Database

/**
 * Desktop application can use database without safe locks
 * Spring doesn't have any API for add custom realisation
 * Liquibase scan only liquibase.* packages by default
 */
class EmptyLockService : LockService {

    override fun getPriority(): Int = 1000

    override fun supports(database: Database?): Boolean = true

    override fun setDatabase(database: Database?) {}

    override fun setChangeLogLockWaitTime(changeLogLockWaitTime: Long) {}

    override fun setChangeLogLockRecheckTime(changeLogLocRecheckTime: Long) {}

    override fun hasChangeLogLock(): Boolean = true

    override fun waitForLock() {}

    override fun acquireLock(): Boolean = true

    override fun releaseLock() {}

    override fun listLocks(): Array<DatabaseChangeLogLock> = emptyArray()

    override fun forceReleaseLock() {}

    override fun reset() {}

    override fun init() {}

    override fun destroy() {}
}