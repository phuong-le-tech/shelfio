package com.inventory.dto.request;

import jakarta.validation.constraints.AssertTrue;

public record CheckoutRequest(
    @AssertTrue(message = "Withdrawal waiver must be accepted")
    boolean withdrawalWaiverAccepted
) {}
