package com.nklmish.demo.managementdata

import com.nklmish.demo.wallet.BetLostEvent
import com.nklmish.demo.wallet.BetWonEvent
import com.nklmish.demo.wallet.DepositedEvent
import com.nklmish.demo.wallet.WithdrawalApprovedEvent
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import javax.persistence.EntityManager

@Component
@Profile("query")
class TotalDepositedProjection(val entityManager: EntityManager) {

    private fun findOrCreateTotal(currency: String): TotalDeposited {
        var totalDeposited = entityManager.find(TotalDeposited::class.java, currency)
        if (totalDeposited == null) {
            totalDeposited = TotalDeposited(currency, BigDecimal.ZERO)
            entityManager.persist(totalDeposited)
        }
        return totalDeposited
    }

    @EventHandler
    fun on(evt: DepositedEvent) {
        val totalDeposited = findOrCreateTotal(evt.currency)
        totalDeposited.amount = totalDeposited.amount!! + evt.amount
    }

    @EventHandler
    fun on(evt: WithdrawalApprovedEvent) {
        val totalDeposited = findOrCreateTotal(evt.currency)
        totalDeposited.amount = totalDeposited.amount!! - evt.amount
    }

    @EventHandler
    fun on(evt: BetWonEvent) {
        val totalDeposited = findOrCreateTotal(evt.currency)
        totalDeposited.amount = totalDeposited.amount!! + evt.amountPayout - evt.amountBetted
    }

    @EventHandler
    fun on(evt: BetLostEvent) {
        val totalDeposited = findOrCreateTotal(evt.currency)
        totalDeposited.amount = totalDeposited.amount!! - evt.amount
    }

    @QueryHandler
    fun handle(query: TotalDepositedQuery): List<TotalDeposited> {
        val results = ArrayList<TotalDeposited>()
        for (currency in arrayOf("EUR", "USD")) {
            results.add(findOrCreateTotal(currency))
        }
        return results
    }
}