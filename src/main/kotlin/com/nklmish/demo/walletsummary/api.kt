package com.nklmish.demo.walletsummary

import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id

data class SingleWalletSummaryQuery(val walletId: String)
class AllWalletSummaryQuery

@Entity
data class WalletSummary(
        @Id var walletId: String? = null,
        var currency: String? = null,
        var available: BigDecimal? = null,
        var betted: BigDecimal? = null,
        var withdrawing: BigDecimal? = null
)

/* Technical events to push changes to the UI. Will become obsolete as soon
   as SubscriptionQueries arrive in Axon Framework. */
data class WalletSummaryCreatedEvt(val walletSummary: WalletSummary)

data class WalletSummaryUpdatedEvt(val walletSummary: WalletSummary)
