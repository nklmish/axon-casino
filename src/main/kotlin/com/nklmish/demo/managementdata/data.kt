package com.nklmish.demo.managementdata

import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class TotalDeposited(
        @Id var currency: String? = null,
        var amount: BigDecimal? = null
)

@Entity
data class SamplesHolder (
        @Id var id: String? = null,
        @Lob var json: String? = null
)
