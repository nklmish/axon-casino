package com.nklmish.demo.process

import com.nklmish.demo.kyp.KypValidationNotOkEvent
import com.nklmish.demo.kyp.KypValidationOkEvent
import com.nklmish.demo.kyp.PerformKypValidationCommand
import com.nklmish.demo.wallet.ApproveWithdrawCommand
import com.nklmish.demo.wallet.DenyWithdrawCommand
import com.nklmish.demo.wallet.WithdrawalRequestedEvent
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventhandling.saga.EndSaga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.axonframework.spring.stereotype.Saga
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

@Saga
@Profile("process")
class WithdrawalApprovalSaga {

    @Autowired
    @Transient
    private val commandGateway: CommandGateway? = null
    @Autowired
    @Transient
    private val eventScheduler: EventScheduler? = null

    private var amount: BigDecimal? = null
    private var walletId: String? = null
    private var validationRequestId: UUID? = null
    private var withdrawalId: UUID? = null

    @StartSaga
    @SagaEventHandler(associationProperty = "withdrawalId")
    fun on(event: WithdrawalRequestedEvent) {
        amount = event.amount
        walletId = event.walletId
        withdrawalId = event.withdrawalId
        validationRequestId = UUID.randomUUID()
        SagaLifecycle.associateWith("validationRequestId", validationRequestId!!.toString())
        eventScheduler!!.schedule(Duration.of(5L, ChronoUnit.SECONDS), ValidationDelayExpiredEvent(validationRequestId!!))
    }

    @SagaEventHandler(associationProperty = "validationRequestId")
    fun on(event: ValidationDelayExpiredEvent) {
        val cmd = PerformKypValidationCommand(validationRequestId!!, walletId!!, withdrawalId!!, amount!!)
        commandGateway!!.send<Any>(cmd)
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "validationRequestId")
    fun on(event: KypValidationOkEvent) {
        commandGateway!!.send<Any>(ApproveWithdrawCommand(walletId!!, withdrawalId!!))
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "validationRequestId")
    fun on(event: KypValidationNotOkEvent) {
        commandGateway!!.send<Any>(DenyWithdrawCommand(walletId!!, withdrawalId!!))
    }

}
