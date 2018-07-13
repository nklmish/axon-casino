package com.nklmish.demo

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nklmish.demo.process.WithdrawalApprovalSaga
import com.thoughtworks.xstream.XStream
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.EventProcessingConfiguration
import org.axonframework.config.SagaConfiguration
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.axonframework.eventhandling.scheduling.java.SimpleEventScheduler
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.json.JacksonSerializer
import org.axonframework.serialization.xml.XStreamSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.concurrent.Executors


@SpringBootApplication
@EnableScheduling
class AxonCasinoApplication {

    @Autowired
    fun config(eventProcessingConfiguration: EventProcessingConfiguration) {
        eventProcessingConfiguration.usingTrackingProcessors()
    }

    @Bean
    @Profile("process")
    fun withdrawalApprovalSagaConfiguration(): SagaConfiguration<WithdrawalApprovalSaga> {
        return SagaConfiguration.trackingSagaManager(WithdrawalApprovalSaga::class.java)
    }

    @Bean
    @Profile("process", "game")
    fun eventScheduler(eventBus: EventBus, transactionManager: TransactionManager): EventScheduler {
        return SimpleEventScheduler(Executors.newSingleThreadScheduledExecutor(), eventBus, transactionManager)
    }

    @Bean
    fun eventSerializer(objectMapper: ObjectMapper): JacksonSerializer {
        return JacksonSerializer(objectMapper)
    }

    @Bean
    fun kotlinModule(): Module {
        return KotlinModule()
    }

    @Autowired
    fun configure(serializer: Serializer) {
        if (serializer is XStreamSerializer) {
            val xStream = serializer.xStream
            XStream.setupDefaultSecurity(xStream)
            xStream.allowTypesByWildcard(arrayOf("org.axonframework.**", "com.nklmish.demo.**"))
        }
    }

}

fun main(args: Array<String>) {
    runApplication<AxonCasinoApplication>(*args)
}
