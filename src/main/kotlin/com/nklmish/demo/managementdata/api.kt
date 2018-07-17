package com.nklmish.demo.managementdata

import java.math.BigDecimal
import java.time.Instant

class TotalDepositedQuery

data class TotalDepositedSample(val timestamp: Instant, val totalEUR: BigDecimal, val totalUSD: BigDecimal)

