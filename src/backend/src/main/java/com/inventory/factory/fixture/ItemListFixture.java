package com.inventory.factory.fixture;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.enums.CustomFieldType;
import com.inventory.model.ItemList;
import com.inventory.model.User;

import java.util.ArrayList;
import java.util.List;

public final class ItemListFixture {

    private ItemListFixture() {}

    public static List<ItemList> createAll(User owner) {
        List<ItemList> lists = new ArrayList<>();
        lists.add(createElectronicsList(owner));
        lists.add(createKitchenPantryList(owner));
        lists.add(createOfficeSuppliesList(owner));
        lists.add(createBookCollectionList(owner));
        lists.add(createGarageToolsList(owner));
        lists.add(createCampingGearList(owner));
        return lists;
    }

    public static ItemList createElectronicsList(User owner) {
        ItemList list = new ItemList();
        list.setName("Electronics Warehouse");
        list.setDescription("Consumer electronics, peripherals, and accessories inventory");
        list.setCategory("Electronics");
        list.setUser(owner);
        list.setCustomFieldDefinitions(List.of(
                new CustomFieldDefinition("brand", "Brand", CustomFieldType.TEXT, true, 1),
                new CustomFieldDefinition("price", "Price ($)", CustomFieldType.NUMBER, true, 2),
                new CustomFieldDefinition("warranty_expiry", "Warranty Expiry", CustomFieldType.DATE, false, 3),
                new CustomFieldDefinition("refurbished", "Refurbished", CustomFieldType.BOOLEAN, false, 4)
        ));
        return list;
    }

    public static ItemList createKitchenPantryList(User owner) {
        ItemList list = new ItemList();
        list.setName("Kitchen Pantry");
        list.setDescription("Food, beverages, and cooking ingredients");
        list.setCategory("Food & Beverage");
        list.setUser(owner);
        list.setCustomFieldDefinitions(List.of(
                new CustomFieldDefinition("expiry_date", "Expiry Date", CustomFieldType.DATE, true, 1),
                new CustomFieldDefinition("calories", "Calories (per serving)", CustomFieldType.NUMBER, false, 2),
                new CustomFieldDefinition("organic", "Organic", CustomFieldType.BOOLEAN, false, 3)
        ));
        return list;
    }

    public static ItemList createOfficeSuppliesList(User owner) {
        ItemList list = new ItemList();
        list.setName("Office Supplies");
        list.setDescription("Stationery, printer supplies, and desk accessories");
        list.setCategory("Office");
        list.setUser(owner);
        list.setCustomFieldDefinitions(List.of(
                new CustomFieldDefinition("supplier", "Supplier", CustomFieldType.TEXT, false, 1),
                new CustomFieldDefinition("unit_price", "Unit Price ($)", CustomFieldType.NUMBER, true, 2),
                new CustomFieldDefinition("reorder", "Needs Reorder", CustomFieldType.BOOLEAN, false, 3)
        ));
        return list;
    }

    public static ItemList createBookCollectionList(User owner) {
        ItemList list = new ItemList();
        list.setName("Book Collection");
        list.setDescription("Personal library of fiction, non-fiction, and reference books");
        list.setCategory("Books");
        list.setUser(owner);
        list.setCustomFieldDefinitions(List.of(
                new CustomFieldDefinition("author", "Author", CustomFieldType.TEXT, true, 1),
                new CustomFieldDefinition("pages", "Pages", CustomFieldType.NUMBER, false, 2),
                new CustomFieldDefinition("published_date", "Published Date", CustomFieldType.DATE, false, 3),
                new CustomFieldDefinition("ebook_available", "E-book Available", CustomFieldType.BOOLEAN, false, 4)
        ));
        return list;
    }

    public static ItemList createGarageToolsList(User owner) {
        ItemList list = new ItemList();
        list.setName("Garage Tools");
        list.setDescription("Hand tools, power tools, and workshop equipment");
        list.setCategory("Tools");
        list.setUser(owner);
        list.setCustomFieldDefinitions(List.of(
                new CustomFieldDefinition("brand", "Brand", CustomFieldType.TEXT, true, 1),
                new CustomFieldDefinition("purchase_date", "Purchase Date", CustomFieldType.DATE, false, 2),
                new CustomFieldDefinition("power_tool", "Power Tool", CustomFieldType.BOOLEAN, false, 3)
        ));
        return list;
    }

    public static ItemList createCampingGearList(User owner) {
        ItemList list = new ItemList();
        list.setName("Camping Gear");
        list.setDescription("Tents, sleeping bags, cooking gear, and outdoor equipment");
        list.setCategory("Outdoor");
        list.setUser(owner);
        list.setCustomFieldDefinitions(List.of(
                new CustomFieldDefinition("weight_kg", "Weight (kg)", CustomFieldType.NUMBER, false, 1),
                new CustomFieldDefinition("waterproof", "Waterproof", CustomFieldType.BOOLEAN, false, 2),
                new CustomFieldDefinition("last_used", "Last Used", CustomFieldType.DATE, false, 3)
        ));
        return list;
    }
}
