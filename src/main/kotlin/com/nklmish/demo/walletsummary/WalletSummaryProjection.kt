package com.nklmish.demo.walletsummary

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nklmish.demo.kyp.PerformKypValidationCommand
import com.nklmish.demo.process.ValidationDelayExpiredEvent
import com.nklmish.demo.wallet.*
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.EntityManager
import javax.persistence.Id
import javax.persistence.Lob


@Entity
data class TopWalletSummaryHolder(
        @Id var id: String? = null,
        @Lob var json: String? = null
)

@Component
@Profile("query")
class WalletSummaryProjection(
        val entityManager: EntityManager,
        val queryUpdateEmitter: QueryUpdateEmitter,
        val objectMapper: ObjectMapper) {

    private val USD2EUR = BigDecimal("0.85")
    private val TOPN = 5

    @EventHandler
    fun on(evt: WalletCreatedEvent) {
        val wallet = WalletSummary(evt.walletId, evt.currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        entityManager.persist(wallet)
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: DepositedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! + evt.amount
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: WithdrawalRequestedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! - evt.amount
        wallet.withdrawing = wallet.withdrawing!! + evt.amount
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: WithdrawalApprovedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.withdrawing = wallet.withdrawing!! - evt.amount
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: WithdrawalDeniedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! + evt.amount
        wallet.withdrawing = wallet.withdrawing!! - evt.amount
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: BetPlacedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! - evt.amount
        wallet.betted = wallet.betted!! + evt.amount
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: BetWonEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! + evt.amountPayout
        wallet.betted = wallet.betted!! - evt.amountBetted
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @EventHandler
    fun on(evt: BetLostEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.betted = wallet.betted!! - evt.amount
        queryUpdateEmitter.emit(SingleWalletSummaryQuery::class.java, { q -> q.walletId == wallet.walletId }, WalletSummaryUpdate(wallet, evt))
        updateTop(wallet)
    }

    @QueryHandler
    fun handle(query: SingleWalletSummaryQuery): WalletSummary? {
        return entityManager.find(WalletSummary::class.java, query.walletId)
    }

    @QueryHandler
    fun handle(query: TopWalletSummaryQuery): List<TopWalletSummary> {
        return objectMapper.readValue(topholder().json!!)
    }

    private fun topholder(): TopWalletSummaryHolder {
        var holder = entityManager.find(TopWalletSummaryHolder::class.java, "1")
        if(holder == null) {
           holder = TopWalletSummaryHolder("1", "[]")
            entityManager.persist(holder)
        }
        return holder
    }

    private fun updateTop(walletSummary: WalletSummary) {
        val holder = topholder()
        val currentTop: List<TopWalletSummary> = objectMapper.readValue(holder.json!!)
        val newTop: MutableList<TopWalletSummary> = currentTop.toMutableList()
        val candidate = topCandidate(walletSummary)
        var wasAlreadyThere = false
        for(i in newTop.indices) {
            if(newTop[i].walletId == candidate.walletId) {
                wasAlreadyThere = true
                newTop[i] = candidate;
            }
        }
        if(!wasAlreadyThere) {
            newTop.add(candidate)
        }
        newTop.sortBy { x -> -x.total }
        while(newTop.size > TOPN) newTop.removeAt(TOPN)
        var memberChange = false
        var valueChange = false
        var valueChangeLocation = -1;

        if(newTop.size == currentTop.size) {
            for(i in newTop.indices) {
                if(newTop[i].walletId != currentTop[i].walletId) {
                    memberChange = true
                } else if(newTop[i].available != currentTop[i].available) {
                    valueChange = true
                    valueChangeLocation = i
                } else if(newTop[i].betted != currentTop[i].betted) {
                    valueChange = true
                    valueChangeLocation = i
                } else if(newTop[i].withdrawing != currentTop[i].withdrawing) {
                    valueChange = true
                    valueChangeLocation = i
                }
            }
        } else {
            memberChange = true
        }

        if(valueChange || memberChange) {
            holder.json = objectMapper.writeValueAsString(newTop)
        }

        if(memberChange) {
            queryUpdateEmitter.emit(TopWalletSummaryQuery::class.java, { _ -> true }, TopWalletsMemberChange(newTop))
        } else if(valueChange) {
            queryUpdateEmitter.emit(TopWalletSummaryQuery::class.java, { _ -> true }, TopWalletsValueChange(valueChangeLocation, candidate))
        }
    }

    private fun topCandidate(walletSummary: WalletSummary): TopWalletSummary {
        val multiplier = when (walletSummary.currency) {
            "EUR" -> BigDecimal.ONE
            "USD" -> USD2EUR
            else -> throw IllegalArgumentException("Unknown currency")
        }
        val available = multiplier.times(walletSummary.available!!)
        val betted = multiplier.times(walletSummary.betted!!)
        val withdrawing = multiplier.times(walletSummary.withdrawing!!)
        val total = available + betted + withdrawing
        return TopWalletSummary(walletSummary.walletId!!, available, betted, withdrawing, total)
    }


}