package com.inventory.service;

import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.CsvExportResult;
import com.inventory.model.ItemList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface IItemListService {

    Page<ItemList> getAllLists(@NonNull Pageable pageable);

    ItemList getListById(@NonNull UUID id);

    ItemList createList(@NonNull ItemListRequest request);

    ItemList updateList(@NonNull UUID id, @NonNull ItemListRequest request);

    void deleteList(@NonNull UUID id);

    CsvExportResult exportListAsCsv(@NonNull UUID id);
}
