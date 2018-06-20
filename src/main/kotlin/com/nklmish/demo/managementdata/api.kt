package com.nklmish.demo.managementdata

import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id

class TotalDepositedQuery

@Entity
data class TotalDeposited(
        @Id var currency: String? = null,
        var amount: BigDecimal? = null
)

