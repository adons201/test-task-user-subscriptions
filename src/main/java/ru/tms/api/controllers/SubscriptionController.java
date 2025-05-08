package ru.tms.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tms.dto.Subscription;
import ru.tms.services.SubscriptionService;
import ru.tms.services.SubscriptionServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user-subscriptions/v1")
@Tag(name = "Subscription", description = "the Subscription in service user-subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionServiceImpl subscriptionService) {
        this.subscriptionService = subscriptionService;
        log.info("SubscriptionController initialized");
    }

    @Operation(summary = "Get all Subscriptions by user",
            description = "Return all Subscriptions by userId", tags = {"Subscription"})
    @GetMapping("/users/{id}/subscriptions")
    public ResponseEntity<List<Subscription>> getSubscriptionByUserId(@PathVariable("id") Long userId) {
        log.debug("Received request to fetch subscriptions by userId {}", userId);
        List<Subscription> subscriptions = this.subscriptionService.getSubscriptionsByUserId(userId);
        return new ResponseEntity<>(subscriptions, HttpStatus.OK);
    }

    @Operation(summary = "Get top three Subscriptions",
            description = "Return top three Subscriptions", tags = {"Subscription"})
    @GetMapping("/subscriptions/top")
    public ResponseEntity<List<String>> findTopThreeSubscriptions() {
        log.debug("Received request to fetch top three subscriptions");
        List<String> subscriptions = this.subscriptionService.findTopThreeSubscriptions();
        return new ResponseEntity<>(subscriptions, HttpStatus.OK);
    }

    @Operation(summary = "Add Subscription", description = "Return create subscription", tags = {"Subscription"})
    @PostMapping("/users/{id}/subscriptions")
    public ResponseEntity<Subscription> createSubscription(@PathVariable("id") Long userId,
                                                           @RequestBody @Valid Subscription subscription) {
        log.debug("Creating new subscription {} for userId {}", subscription, userId);
        Subscription createSubscription = this.subscriptionService.createSubscription(subscription, userId);
        log.debug("Successfully create subscription with id {}", subscription.id());
        return new ResponseEntity<>(createSubscription, HttpStatus.OK);
    }

    @Operation(summary = "Delete Subscription", description = "Delete subscription", tags = {"Subscription"})
    @DeleteMapping("/users/{id}/subscriptions/{sub_id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable("id") Long userId,
                                                   @PathVariable("sub_id") Long subscriptionId) {
        log.debug("Delete subscription with id {} for userId {}", subscriptionId, userId);
        this.subscriptionService.deleteSubscription(userId, subscriptionId);
        log.debug("Successfully deleted subscription with ID {}", subscriptionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
