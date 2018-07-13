package com.nklmish.demo.managementdata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nklmish.demo.wallet.BetLostEvent
import com.nklmish.demo.wallet.BetWonEvent
import com.nklmish.demo.wallet.DepositedEvent
import com.nklmish.demo.wallet.WithdrawalApprovedEvent
import com.nklmish.demo.walletsummary.TopWalletSummaryHolder
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import javax.persistence.EntityManager

@Component
@Profile("query")
class TotalDepositedProjection(val entityManager: EntityManager, val objectMapper: ObjectMapper, val queryUpdateEmitter: QueryUpdateEmitter) {

    private val SAMPLES = 200

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

    @Scheduled(fixedRate = 1000)
    @Transactional
    fun sample() {
        val holder = samplesholder()
        val samples: MutableList<TotalDepositedSample> = objectMapper.readValue(holder.json!!)
        val eur = findOrCreateTotal("EUR")
        val usd = findOrCreateTotal("USD")
        val newSample = TotalDepositedSample(Instant.now(), eur.amount!!, usd.amount!!)
        samples.add(newSample)
        samples.removeAt(0)
        holder.json = objectMapper.writeValueAsString(samples)
        queryUpdateEmitter.emit(TotalDepositedQuery::class.java, { _ -> true}, newSample)
    }

    private fun samplesholder(): SamplesHolder {
        var holder = entityManager.find(SamplesHolder::class.java, "1")
        if(holder == null) {
            val now = Instant.now()
            val samples = MutableList<TotalDepositedSample>(SAMPLES) { i ->
                val instant = now.minusSeconds((SAMPLES - i).toLong())
                TotalDepositedSample(instant, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2))
            }
            holder = SamplesHolder("1", objectMapper.writeValueAsString(samples))
            entityManager.persist(holder)
        }
        return holder
    }

    @QueryHandler
    fun handle(query: TotalDepositedQuery): List<TotalDepositedSample> {
        return objectMapper.readValue(samplesholder().json!!)
    }
}