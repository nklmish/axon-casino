package com.nklmish.demo.walletsummary

import com.nklmish.demo.wallet.*
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.GenericEventMessage.asEventMessage
import org.axonframework.queryhandling.QueryHandler
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import javax.persistence.EntityManager

@Component
@Profile("query")
class WalletSummaryProjection(
        val entityManager: EntityManager,
        val eventBus: EventBus) {

    @EventHandler
    fun on(evt: WalletCreatedEvent) {
        val wallet = WalletSummary(evt.walletId, evt.currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        entityManager.persist(wallet)
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryCreatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: DepositedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! + evt.amount
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: WithdrawalRequestedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! - evt.amount
        wallet.withdrawing = wallet.withdrawing!! + evt.amount
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: WithdrawalApprovedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.withdrawing = wallet.withdrawing!! - evt.amount
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: WithdrawalDeniedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! + evt.amount
        wallet.withdrawing = wallet.withdrawing!! - evt.amount
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: BetPlacedEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! - evt.amount
        wallet.betted = wallet.betted!! + evt.amount
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: BetWonEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.available = wallet.available!! + evt.amountPayout
        wallet.betted = wallet.betted!! - evt.amountBetted
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @EventHandler
    fun on(evt: BetLostEvent) {
        val wallet = entityManager.find(WalletSummary::class.java, evt.walletId)
        wallet.betted = wallet.betted!! - evt.amount
        eventBus.publish(asEventMessage<WalletSummary>(WalletSummaryUpdatedEvt(wallet)))
    }

    @QueryHandler
    fun handle(query: SingleWalletSummaryQuery): WalletSummary {
        return entityManager.find(WalletSummary::class.java, query.walletId)
    }

    @QueryHandler
    fun handle(query: AllWalletSummaryQuery): List<WalletSummary> {
        return entityManager.createQuery("SELECT w FROM WalletSummary w ORDER BY w.walletId", WalletSummary::class.java)
                .resultList
    }


}