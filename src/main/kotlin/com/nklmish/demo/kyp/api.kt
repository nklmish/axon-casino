package com.nklmish.demo.kyp

import org.axonframework.commandhandling.TargetAggregateIdentifier
import java.math.BigDecimal
import java.util.*

data class PerformKypValidationCommand(@TargetAggregateIdentifier val validationRequestId: UUID, val walletId: String, val withdrawalId: UUID, val amount: BigDecimal)
data class RegisterKypValidationOkCommand(@TargetAggregateIdentifier val validationRequestId: UUID)
data class RegisterKypValidationNotOkCommand(@TargetAggregateIdentifier val validationRequestId: UUID)

data class KypValidationRequestedEvent(val validationRequestId: UUID, val walletId: String, val withdrawalId: UUID, val amount: BigDecimal)
data class KypValidationOkEvent(val validationRequestId: UUID)
data class KypValidationNotOkEvent(val validationRequestId: UUID)

