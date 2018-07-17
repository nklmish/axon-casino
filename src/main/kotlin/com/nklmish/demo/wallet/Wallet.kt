package com.nklmish.demo.wallet

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.model.AggregateIdentifier
import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.util.*

@Aggregate
@Profile("wallet")
class Wallet {

    @AggregateIdentifier
    private lateinit var walletId: String
    private lateinit var currency: String
    private lateinit var available: BigDecimal
    private val currencies: Set<String> = setOf("EUR", "USD")
    private val requestedWithdrawals = HashMap<UUID, BigDecimal>()
    private val bets = HashMap<String, BigDecimal>()

    constructor() {
    }

    @CommandHandler
    constructor(cmd: CreateWalletCommand) {
        if (!currencies.contains(cmd.currency.toUpperCase())) {
            throw IllegalArgumentException("Invalid currency '" + cmd.currency + "', supported currencies:" + currencies)
        }
        apply(WalletCreatedEvent(cmd.walletId, cmd.currency.toUpperCase()))
    }

    @CommandHandler
    fun handle(cmd: DepositCommand) {
        if (cmd.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("amount <= 0")
        }
        if (cmd.amount >= BigDecimal("10000000")) {
            throw IllegalArgumentException("amount too large")
        }
        apply(DepositedEvent(cmd.walletId, cmd.amount, currency))
    }

    @CommandHandler
    fun handle(cmd: RequestWithdrawCommand) {
        if (cmd.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("amount <= 0")
        }
        if (cmd.amount > available) {
            throw IllegalArgumentException("Error withdrawing, insufficient funds")
        }
        apply(WithdrawalRequestedEvent(cmd.walletId, UUID.randomUUID(), cmd.amount, currency))
    }

    @CommandHandler
    fun handle(cmd: ApproveWithdrawCommand) {
        if (!requestedWithdrawals.containsKey(cmd.withdrawalId)) {
            throw IllegalArgumentException("withdrawalId unknown")
        }
        apply(WithdrawalApprovedEvent(cmd.walletId, cmd.withdrawalId, requestedWithdrawals.get(cmd.withdrawalId)!!, currency))
    }

    @CommandHandler
    fun handle(cmd: DenyWithdrawCommand) {
        if (!requestedWithdrawals.containsKey(cmd.withdrawalId)) {
            throw IllegalArgumentException("withdrawalId unknown")
        }
        apply(WithdrawalDeniedEvent(cmd.walletId, cmd.withdrawalId, requestedWithdrawals.get(cmd.withdrawalId)!!, currency))
    }

    @CommandHandler
    fun placeBet(cmd: PlaceBetCommand) {
        if (cmd.amount > available) {
            throw IllegalArgumentException("Cannot place bet, insufficient funds")
        }
        if (bets.containsKey(cmd.betId)) { // idempotent behavior
            throw IllegalArgumentException("Bet already placed")
        }
        apply(BetPlacedEvent(cmd.walletId, cmd.betId, cmd.amount, currency))
    }

    @CommandHandler
    fun handle(cmd: RegisterBetWonCommand) {
        if (!bets.containsKey(cmd.betId)) {
            throw IllegalStateException("Bet not placed")
        }
        apply(BetWonEvent(cmd.walletId, cmd.betId, cmd.amountBetted, currency, cmd.amountPayout))
    }

    @CommandHandler
    fun handle(cmd: RegisterBetLostCommand) {
        if (!bets.containsKey(cmd.betId)) {
            throw IllegalStateException("Bet not placed")
        }
        apply(BetLostEvent(cmd.walletId, cmd.betId, cmd.amount, currency))
    }

    @EventSourcingHandler
    fun on(event: WalletCreatedEvent) {
        walletId = event.walletId
        currency = event.currency
        available = BigDecimal.ZERO
    }

    @EventSourcingHandler
    fun on(event: DepositedEvent) {
        available += event.amount
    }

    @EventSourcingHandler
    fun on(event: WithdrawalRequestedEvent) {
        available -= event.amount
        requestedWithdrawals.put(event.withdrawalId, event.amount)
    }

    @EventSourcingHandler
    fun on(event: WithdrawalApprovedEvent) {
        requestedWithdrawals.remove(event.withdrawalId)
    }

    @EventSourcingHandler
    fun on(event: WithdrawalDeniedEvent) {
        available += requestedWithdrawals.remove(event.withdrawalId)!!
    }

    @EventSourcingHandler
    fun on(event: BetPlacedEvent) {
        available -= event.amount
        bets.put(event.betId, event.amount)
    }

    @EventSourcingHandler
    fun on(event: BetWonEvent) {
        available += event.amountPayout
        bets.remove(event.betId)
    }

    @EventSourcingHandler
    fun on(event: BetLostEvent) {
        bets.remove(event.betId)
    }
}