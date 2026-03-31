package com.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class ReorderItemsRequest {

    @NotNull(message = "listId is required")
    private UUID listId;

    @NotNull(message = "orderedIds is required")
    @Size(min = 1, max = 500, message = "orderedIds must contain between 1 and 500 items")
    private List<@NotNull UUID> orderedIds;

    public UUID getListId() {
        return listId;
    }

    public void setListId(UUID listId) {
        this.listId = listId;
    }

    public List<UUID> getOrderedIds() {
        return orderedIds;
    }

    public void setOrderedIds(List<UUID> orderedIds) {
        this.orderedIds = orderedIds;
    }
}
