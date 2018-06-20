package com.nklmish.demo.kyp

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.model.AggregateIdentifier
import org.axonframework.commandhandling.model.AggregateLifecycle
import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.context.annotation.Profile
import java.util.*

@Aggregate
@Profile("kyp")
class KypValidation {

    @AggregateIdentifier
    private var id: UUID? = null

    constructor() {
    }

    @CommandHandler
    constructor(cmd: PerformKypValidationCommand) {
        apply(KypValidationRequestedEvent(cmd.validationRequestId, cmd.walletId, cmd.withdrawalId, cmd.amount))
    }

    @CommandHandler
    fun handle(cmd: RegisterKypValidationOkCommand) {
        apply(KypValidationOkEvent(cmd.validationRequestId))
    }

    @CommandHandler
    fun handle(cmd: RegisterKypValidationNotOkCommand) {
        apply(KypValidationNotOkEvent(cmd.validationRequestId))
    }

    @EventSourcingHandler
    fun on(evt: KypValidationRequestedEvent) {
        id = evt.validationRequestId
    }

    @EventSourcingHandler
    fun on(evt: KypValidationOkEvent) {
        AggregateLifecycle.markDeleted()
    }

    @EventSourcingHandler
    fun on(evt: KypValidationNotOkEvent) {
        AggregateLifecycle.markDeleted()
    }
}