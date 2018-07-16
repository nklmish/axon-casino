package com.nklmish.demo.react;

import com.nklmish.demo.managementdata.TotalDeposited;
import com.nklmish.demo.managementdata.TotalDepositedQuery;
import com.nklmish.demo.managementdata.TotalDepositedSample;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.axonframework.queryhandling.responsetypes.ResponseTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/rest")
public class RestEndpoints {

    private final QueryGateway queryGateway;

    public RestEndpoints(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

//    @GetMapping(produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
//    public Flux<TotalDepositedSample> streamResult() {
//        return queryGateway.subscriptionQuery(new TotalDepositedQuery(),
//                ResponseTypes.multipleInstancesOf(TotalDepositedSample.class),
//                ResponseTypes.instanceOf(TotalDepositedSample.class)).updates();
//    }

    @GetMapping(produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<TotalDepositedSample> streamResult() {
        SubscriptionQueryResult<List<TotalDepositedSample>, TotalDepositedSample> queryResult = queryGateway.subscriptionQuery(new TotalDepositedQuery(),
                ResponseTypes.multipleInstancesOf(TotalDepositedSample.class),
                ResponseTypes.instanceOf(TotalDepositedSample.class));

        return Flux.concat(
                queryResult.initialResult().flatMapMany(x -> Flux.fromIterable(x)),
                queryResult.updates()
        );
    }
}
