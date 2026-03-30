package com.inventory.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class BulkDeleteRequest {

    @NotEmpty(message = "ids must not be empty")
    @Size(max = 100, message = "Cannot delete more than 100 items at once")
    private List<@NotNull UUID> ids;

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }
}
