package com.nklmish.demo.playerui

import com.nklmish.demo.wallet.*
import com.nklmish.demo.walletsummary.WalletSummaryCreatedEvt
import com.nklmish.demo.walletsummary.WalletSummaryUpdatedEvt
import org.axonframework.eventhandling.EventHandler
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

interface PlayerEventListener {
    fun on(evt: WalletSummaryCreatedEvt)
    fun on(evt: WalletSummaryUpdatedEvt)
    fun on(evt: WithdrawalApprovedEvent)
    fun on(evt: WithdrawalDeniedEvent)
    fun on(evt: BetPlacedEvent)
    fun on(evt: BetWonEvent)
    fun on(evt: BetLostEvent)
}

@Component
@Profile("gui")
class PlayerEventDistributor {

    private val playerEventListeners = Collections.newSetFromMap(
            WeakHashMap<PlayerEventListener, Boolean>());

    @EventHandler
    fun on(evt: WalletSummaryCreatedEvt) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: WalletSummaryUpdatedEvt) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: WithdrawalApprovedEvent) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: WithdrawalDeniedEvent) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: BetPlacedEvent) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: BetWonEvent) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    @EventHandler
    fun on(evt: BetLostEvent) {
        playerEventListeners.forEach { x -> x.on(evt) }
    }

    fun register(listener: PlayerEventListener) {
        playerEventListeners.add(listener)
    }

    fun unregister(listener: PlayerEventListener) {
        playerEventListeners.remove(listener)
    }
}