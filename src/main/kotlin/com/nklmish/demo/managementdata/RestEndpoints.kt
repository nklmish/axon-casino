package com.nklmish.demo.managementdata

import com.nklmish.demo.managementdata.TotalDepositedQuery
import com.nklmish.demo.managementdata.TotalDepositedSample
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.SubscriptionQueryResult
import org.axonframework.queryhandling.responsetypes.ResponseTypes
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/rest")
class RestEndpoints(private val queryGateway: QueryGateway) {

    @GetMapping(produces = arrayOf(MediaType.APPLICATION_STREAM_JSON_VALUE))
    fun streamResult1(): Flux<TotalDepositedSample> {
        return queryGateway.subscriptionQuery(TotalDepositedQuery(),
                ResponseTypes.multipleInstancesOf(TotalDepositedSample::class.java),
                ResponseTypes.instanceOf(TotalDepositedSample::class.java)).updates()
    }

//    @GetMapping(produces = arrayOf(MediaType.APPLICATION_STREAM_JSON_VALUE))
//    fun streamResult2(): Flux<TotalDepositedSample> {
//        val queryResult = queryGateway.subscriptionQuery(TotalDepositedQuery(),
//                ResponseTypes.multipleInstancesOf(TotalDepositedSample::class.java),
//                ResponseTypes.instanceOf(TotalDepositedSample::class.java))
//
//        return Flux.concat(
//                queryResult.initialResult().flatMapMany { x -> Flux.fromIterable(x) },
//                queryResult.updates()
//        )
//    }
}
