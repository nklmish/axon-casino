package com.nklmish.demo.managementui

import com.nklmish.demo.managementdata.TotalDeposited
import com.nklmish.demo.managementdata.TotalDepositedQuery
import com.nklmish.demo.walletsummary.AllWalletSummaryQuery
import com.nklmish.demo.walletsummary.WalletSummary
import com.nklmish.demo.walletsummary.WalletSummaryCreatedEvt
import com.nklmish.demo.walletsummary.WalletSummaryUpdatedEvt
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.responsetypes.ResponseTypes
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

interface ManagementEventListener {
    fun updateTotals(now: Long, totals: List<TotalDeposited>)
    fun on(evt: WalletSummaryCreatedEvt)
    fun on(evt: WalletSummaryUpdatedEvt)
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

    fun findWallets(): List<WalletSummary> {
        return queryGateway.query(AllWalletSummaryQuery(), ResponseTypes.multipleInstancesOf(WalletSummary::class.java)).join()
    }

    @EventHandler
    fun on(evt: WalletSummaryCreatedEvt) {
        managementEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: WalletSummaryUpdatedEvt) {
        managementEventListeners.forEach { x -> x.on(evt) }
    }

    fun register(listener: ManagementEventListener) {
        managementEventListeners.add(listener)
    }

    fun unregister(listener: ManagementEventListener) {
        managementEventListeners.remove(listener)
    }

}
