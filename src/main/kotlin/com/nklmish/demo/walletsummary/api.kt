package com.nklmish.demo.walletsummary

import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id

data class SingleWalletSummaryQuery(val walletId: String)

@Entity
data class WalletSummary(
        @Id var walletId: String? = null,
        var currency: String? = null,
        var available: BigDecimal? = null,
        var betted: BigDecimal? = null,
        var withdrawing: BigDecimal? = null
)

data class WalletSummaryUpdate(val walletSummary: WalletSummary, val event: Any?)





class TopWalletSummaryQuery()

data class TopWalletSummary(
        val walletId: String,
        val available: BigDecimal,
        val betted: BigDecimal,
        val withdrawing: BigDecimal,
        val total: BigDecimal
)

interface TopWalletsChange
data class TopWalletsMemberChange(val summaries: List<TopWalletSummary>) : TopWalletsChange
data class TopWalletsValueChange(val position: Int, val summary: TopWalletSummary) : TopWalletsChange
