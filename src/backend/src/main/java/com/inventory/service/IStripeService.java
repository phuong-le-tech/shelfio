package com.inventory.service;

import com.inventory.model.User;

public interface IStripeService {

    String createCheckoutSession(User user);

    void handleWebhookEvent(String payload, String sigHeader);
}
