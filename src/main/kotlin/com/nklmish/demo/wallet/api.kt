package com.nklmish.demo.wallet

import org.axonframework.commandhandling.TargetAggregateIdentifier
import java.math.BigDecimal
import java.util.*

data class CreateWalletCommand(@TargetAggregateIdentifier val walletId: String, val currency: String)
data class DepositCommand(@TargetAggregateIdentifier val walletId: String, val amount: BigDecimal)
data class RequestWithdrawCommand(@TargetAggregateIdentifier val walletId: String, val amount: BigDecimal)
data class ApproveWithdrawCommand(@TargetAggregateIdentifier val walletId: String, val withdrawalId: UUID)
data class DenyWithdrawCommand(@TargetAggregateIdentifier val walletId: String, val withdrawalId: UUID)
data class PlaceBetCommand(@TargetAggregateIdentifier val walletId: String, val betId: String, val amount: BigDecimal)
data class RegisterBetWonCommand(@TargetAggregateIdentifier val walletId: String, val betId: String, val amountBetted: BigDecimal, val amountPayout: BigDecimal)
data class RegisterBetLostCommand(@TargetAggregateIdentifier val walletId: String, val betId: String, val amount: BigDecimal)

data class WalletCreatedEvent(val walletId: String, val currency: String)
data class DepositedEvent(val walletId: String, val amount: BigDecimal, val currency: String)
data class WithdrawalRequestedEvent(val walletId: String, val withdrawalId: UUID, val amount: BigDecimal, val currency: String)
data class WithdrawalApprovedEvent(val walletId: String, val withdrawalId: UUID, val amount: BigDecimal, val currency: String)
data class WithdrawalDeniedEvent(val walletId: String, val withdrawalId: UUID, val amount: BigDecimal, val currency: String)
data class BetPlacedEvent(val walletId: String, val betId: String, val amount: BigDecimal, val currency: String)
data class BetWonEvent(val walletId: String, val betId: String, val amountBetted: BigDecimal, val currency: String, val amountPayout: BigDecimal)
data class BetLostEvent(val walletId: String, val betId: String, val amount: BigDecimal, val currency: String)
