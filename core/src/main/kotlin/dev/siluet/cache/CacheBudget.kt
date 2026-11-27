package dev.siluet.cache

import dev.siluet.theme.SiluetTheme
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource

internal class CacheBudget(
    private val maxTotalEntries: Int,
    private val evictIdleAfter: Duration,
    private val timeSource: TimeSource.WithComparableMarks,
) : CacheSizeListener {
    private class Registration(theme: SiluetTheme, val cache: ThemeCache) {
        private val theme = WeakReference(theme)
        val isAbandoned: Boolean get() = theme.get() == null
    }

    private val registrations = ConcurrentHashMap.newKeySet<Registration>()

    private val sweepInterval: Duration = evictIdleAfter / 10

    private val total = AtomicInteger()
    private val tick = AtomicLong()
    private val target = (maxTotalEntries * 0.9).toInt()
    private val shrinkLock = ReentrantLock()
    private val sweepLock = ReentrantLock()
    @Volatile
    private var lastSweepAt: ComparableTimeMark = timeSource.markNow()

    fun register(theme: SiluetTheme, cache: ThemeCache) {
        registrations.add(Registration(theme, cache))
    }

    fun beforeRender(cache: ThemeCache) {
        val now = timeSource.markNow()

        if(now - cache.lastUsedAt > evictIdleAfter) {
            cache.invalidateAll()
        }

        cache.touch(now, tick.incrementAndGet())

        sweepIdleIfDue(now)
    }

    override fun sizeChanged(delta: Int) {
        if(total.addAndGet(delta) > maxTotalEntries) {
            shrink()
        }
    }

    private fun shrink() {
        if(!shrinkLock.tryLock()) {
            return
        }
        try {
            while (total.get() > target) {
                val victim = pickVictim() ?: return
                while (total.get() > target && victim.evictEldest()) {

                }
            }
        } finally {
            shrinkLock.unlock()
        }
    }

    private fun pickVictim(): ThemeCache? {
        var victim: ThemeCache? = null
        var victimAccess = Long.MAX_VALUE

        for(registration in registrations){
            if(registration.isAbandoned) {
                release(registration)
                continue
            }
            val cache = registration.cache
            if(cache.size == 0) {
                continue
            }

            val access = cache.access
            if(access < victimAccess) {
                victim = cache
                victimAccess = access
            }
        }
        return victim
    }

    private fun sweepIdleIfDue(now: ComparableTimeMark) {
        if(evictIdleAfter.isInfinite()){
            return
        }
        if(now - lastSweepAt < sweepInterval) {
            return
        }
        if(!sweepLock.tryLock()) {
            return
        }

        try {
            if(now - lastSweepAt < sweepInterval) {
                return
            }
            lastSweepAt = now

            for(registration in registrations) {
                when {
                    registration.isAbandoned -> release(registration)
                    now - registration.cache.lastUsedAt > evictIdleAfter -> registration.cache.invalidateAll()
                }
            }
        } finally {
            sweepLock.unlock()
        }
    }
    private fun release(registration: Registration) {
        if(registrations.remove(registration)) {
            registration.cache.invalidateAll()
        }
    }
}