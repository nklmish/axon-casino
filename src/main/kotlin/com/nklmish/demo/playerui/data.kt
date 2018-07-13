package com.nklmish.demo.playerui

import java.math.BigDecimal

data class BetData(
        var betId: String,
        var amountBetted: BigDecimal,
        var amountWon: BigDecimal?
)
