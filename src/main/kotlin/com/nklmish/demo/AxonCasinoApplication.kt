package com.nklmish.demo

import com.nklmish.demo.process.WithdrawalApprovalSaga
import org.axonframework.config.EventHandlingConfiguration
import org.axonframework.config.SagaConfiguration
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.axonframework.eventhandling.scheduling.java.SimpleEventScheduler
import org.axonframework.serialization.json.JacksonSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*
import java.util.concurrent.Executors


@SpringBootApplication
@EnableScheduling
class AxonCasinoApplication {

    @Autowired
    fun config(eventHandlingConfiguration: EventHandlingConfiguration) {
        eventHandlingConfiguration.usingTrackingProcessors()
    }

    @Bean
    @Profile("process")
    fun withdrawalApprovalSagaConfiguration(): SagaConfiguration<WithdrawalApprovalSaga> {
        return SagaConfiguration.trackingSagaManager(WithdrawalApprovalSaga::class.java)
    }

    @Bean
    @Profile("process", "game")
    fun eventScheduler(eventBus: EventBus): EventScheduler {
        return SimpleEventScheduler(Executors.newSingleThreadScheduledExecutor(), eventBus)
    }

    @Bean
    fun eventSerializer() = JacksonSerializer()

}

fun main(args: Array<String>) {
    runApplication<AxonCasinoApplication>(*args)
}
