package com.nklmish.demo.kyp

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventhandling.EventHandler
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("kyp")
class KypValidator(private val commandGateway: CommandGateway) {

    @EventHandler
    fun on(evt: KypValidationRequestedEvent) {
        if (evt.amount.toString().contains("6")) {
            commandGateway.send<Any>(RegisterKypValidationNotOkCommand(evt.validationRequestId))
        } else {
            commandGateway.send<Any>(RegisterKypValidationOkCommand(evt.validationRequestId))
        }
    }

}