package com.nklmish.demo.managementui

import com.nklmish.demo.managementdata.TotalDeposited
import com.nklmish.demo.managementdata.TotalDepositedQuery
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.responsetypes.ResponseTypes
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

interface ManagementEventListener {
    fun updateTotals(now: Long, totals: List<TotalDeposited>)
}

@Component
@Profile("gui")
class ManagementDataCollector(private val queryGateway: QueryGateway) {
    private val managementEventListeners = Collections.newSetFromMap(
            WeakHashMap<ManagementEventListener, Boolean>());

    @Scheduled(fixedRate = 2000)
    fun queryData() {
        val totals = queryGateway.query(TotalDepositedQuery(),
                ResponseTypes.multipleInstancesOf(TotalDeposited::class.java))
        val totalsResult = totals.join()
        val now = System.currentTimeMillis()
        managementEventListeners.forEach { x -> x.updateTotals(now, totalsResult) }

    }

    fun register(listener: ManagementEventListener) {
        managementEventListeners.add(listener)
    }

    fun unregister(listener: ManagementEventListener) {
        managementEventListeners.remove(listener)
    }

}
