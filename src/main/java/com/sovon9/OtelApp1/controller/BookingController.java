package com.sovon9.OtelApp1.controller;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class BookingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingController.class);
    private final WebClient webClient;

    private final Tracer tracer;

    public BookingController(WebClient webClient, Tracer tracer)
    {
        this.webClient = webClient;
        this.tracer = tracer;
    }

    @PostMapping("/booking/{userId}/qty/{qty}")
    public ResponseEntity<String> gasBooking(@PathVariable("userId") String userId, @PathVariable("qty") int qty)
    {
        Boolean paymentSuccess=null;
        try
        {
            paymentSuccess = webClient.get().uri("/payment/amount/{amount}", 900).retrieve().bodyToMono(Boolean.class).block();
        }
        catch (Exception e)
        {
            LOGGER.error("Payment Failure: "+e.getMessage(), e);
        }
        if (paymentSuccess==null || !paymentSuccess) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body("Gas booking is failed for " + userId + " with " + qty + " cylinders due to payment failure");
        }
        return ResponseEntity.ok("Gas booking is completed for "+userId+" with "+qty+" cylinders");
    }

    /**
     * Manual span creation
     * @param userId
     * @param qty
     * @return
     */
    @PostMapping("/booking/manual/{userId}/qty/{qty}")
    public ResponseEntity<String> gasBookingWithmanualSpan(@PathVariable("userId") String userId, @PathVariable("qty") int qty)
    {
        Boolean paymentSuccess=null;
        try
        {
            paymentSuccess = webClient.get().uri("/payment/amount/{amount}", 900).retrieve().bodyToMono(Boolean.class).block();
            
            // mock DB update
            Span parentSpan = tracer.currentSpan(); // get parent span from tracer
            Span childSpan = tracer.nextSpan(parentSpan).name("mock-db-update").start(); // create child span
            try (Tracer.SpanInScope ws = tracer.withSpan(childSpan)) { // this will make child span as current span,
                // when we close this span it will reset the current span to parent span
                Thread.sleep(1000);
            } finally {
                // This guarantees the span is closed whether an exception occurs or not
                childSpan.end();
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Payment Failure: "+e.getMessage(), e);
        }
        if (paymentSuccess==null || !paymentSuccess) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body("Gas booking is failed for " + userId + " with " + qty + " cylinders due to payment failure");
        }
        return ResponseEntity.ok("Gas booking is completed for "+userId+" with "+qty+" cylinders");
    }

}
