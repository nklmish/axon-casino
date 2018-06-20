package com.nklmish.demo.game


import com.nklmish.demo.wallet.BetPlacedEvent
import com.nklmish.demo.wallet.RegisterBetLostCommand
import com.nklmish.demo.wallet.RegisterBetWonCommand
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ThreadLocalRandom

@Component
@Profile("game")
class Game(private val commandGateway: CommandGateway, private val eventScheduler: EventScheduler) {

    @EventHandler
    fun on(evt: BetPlacedEvent) {
        eventScheduler.schedule(Duration.of(5L, ChronoUnit.SECONDS), BetReadyToPlayEvent(evt))
    }

    /*
     * Game: 30% chance to get a payout of 2x the bet, 70% chance to lose the bet.
     */
    @EventHandler
    private fun play(evt: BetReadyToPlayEvent) {
        val bet = evt.bet
        val won = ThreadLocalRandom.current().nextDouble() < 0.3
        if (won) {
            commandGateway.send<Any>(RegisterBetWonCommand(bet.walletId, bet.betId, bet.amount,
                    bet.amount.multiply(BigDecimal.valueOf(2L))))
        } else {
            commandGateway.send<Any>(RegisterBetLostCommand(bet.walletId, bet.betId, bet.amount))
        }
    }

}
