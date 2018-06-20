package com.nklmish.demo.game

import com.nklmish.demo.wallet.BetPlacedEvent

data class BetReadyToPlayEvent(val bet: BetPlacedEvent)