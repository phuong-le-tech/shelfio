package com.inventory.factory.fixture;

import com.inventory.enums.ItemStatus;
import com.inventory.model.Item;
import com.inventory.model.ItemList;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ItemFixture {

    private static final Random RANDOM = new Random(42);
    private static final ItemStatus[] STATUSES = ItemStatus.values();

    private ItemFixture() {}

    public static List<Item> createItemsFor(ItemList list) {
        return switch (list.getCategory()) {
            case "Electronics" -> createElectronicsItems(list);
            case "Food & Beverage" -> createKitchenItems(list);
            case "Office" -> createOfficeItems(list);
            case "Books" -> createBookItems(list);
            case "Tools" -> createToolItems(list);
            case "Outdoor" -> createCampingItems(list);
            default -> List.of();
        };
    }

    private static List<Item> createElectronicsItems(ItemList list) {
        return List.of(
                createItem(list, "MacBook Pro 16\"", 5, Map.of(
                        "brand", "Apple", "price", 2499.99, "warranty_expiry", "2027-03-15", "refurbished", false)),
                createItem(list, "Dell UltraSharp 27\" Monitor", 12, Map.of(
                        "brand", "Dell", "price", 549.99, "warranty_expiry", "2026-11-20", "refurbished", false)),
                createItem(list, "Logitech MX Master 3S", 30, Map.of(
                        "brand", "Logitech", "price", 99.99, "warranty_expiry", "2026-06-01", "refurbished", false)),
                createItem(list, "Samsung Galaxy S24", 8, Map.of(
                        "brand", "Samsung", "price", 899.99, "warranty_expiry", "2027-01-10", "refurbished", false)),
                createItem(list, "Sony WH-1000XM5 Headphones", 15, Map.of(
                        "brand", "Sony", "price", 349.99, "warranty_expiry", "2026-08-22", "refurbished", true)),
                createItem(list, "Anker USB-C Hub", 45, Map.of(
                        "brand", "Anker", "price", 35.99, "warranty_expiry", "2026-04-30", "refurbished", false)),
                createItem(list, "Keychron K2 Keyboard", 20, Map.of(
                        "brand", "Keychron", "price", 89.99, "warranty_expiry", "2026-09-15", "refurbished", false)),
                createItem(list, "iPad Air M2", 3, Map.of(
                        "brand", "Apple", "price", 799.99, "warranty_expiry", "2027-05-01", "refurbished", true))
        );
    }

    private static List<Item> createKitchenItems(ItemList list) {
        return List.of(
                createItem(list, "Basmati Rice (5kg)", 4, Map.of(
                        "expiry_date", "2027-02-15", "calories", 350, "organic", false)),
                createItem(list, "Extra Virgin Olive Oil", 6, Map.of(
                        "expiry_date", "2026-12-01", "calories", 120, "organic", true)),
                createItem(list, "Canned Chickpeas", 18, Map.of(
                        "expiry_date", "2028-06-30", "calories", 210, "organic", false)),
                createItem(list, "Organic Honey (500g)", 3, Map.of(
                        "expiry_date", "2028-01-15", "calories", 64, "organic", true)),
                createItem(list, "Black Peppercorns", 8, Map.of(
                        "expiry_date", "2027-09-01", "calories", 5, "organic", false)),
                createItem(list, "Green Tea (100 bags)", 10, Map.of(
                        "expiry_date", "2027-04-20", "calories", 0, "organic", true))
        );
    }

    private static List<Item> createOfficeItems(ItemList list) {
        return List.of(
                createItem(list, "A4 Copy Paper (5 reams)", 25, Map.of(
                        "supplier", "Staples", "unit_price", 24.99, "reorder", false)),
                createItem(list, "Black Ink Cartridge HP 61", 6, Map.of(
                        "supplier", "HP Direct", "unit_price", 29.99, "reorder", true)),
                createItem(list, "Post-it Notes (12 pack)", 14, Map.of(
                        "supplier", "3M", "unit_price", 11.49, "reorder", false)),
                createItem(list, "Ballpoint Pens (Box of 50)", 8, Map.of(
                        "supplier", "BIC", "unit_price", 8.99, "reorder", false)),
                createItem(list, "Desk Organizer Tray", 3, Map.of(
                        "supplier", "Amazon Basics", "unit_price", 15.99, "reorder", true)),
                createItem(list, "Whiteboard Markers (8 pack)", 10, Map.of(
                        "supplier", "Expo", "unit_price", 12.49, "reorder", false))
        );
    }

    private static List<Item> createBookItems(ItemList list) {
        return List.of(
                createItem(list, "Clean Code", 2, Map.of(
                        "author", "Robert C. Martin", "pages", 464, "published_date", "2008-08-01", "ebook_available", true)),
                createItem(list, "Designing Data-Intensive Applications", 3, Map.of(
                        "author", "Martin Kleppmann", "pages", 616, "published_date", "2017-03-16", "ebook_available", true)),
                createItem(list, "The Pragmatic Programmer", 2, Map.of(
                        "author", "David Thomas & Andrew Hunt", "pages", 352, "published_date", "2019-09-23", "ebook_available", true)),
                createItem(list, "Sapiens: A Brief History of Humankind", 4, Map.of(
                        "author", "Yuval Noah Harari", "pages", 498, "published_date", "2015-02-10", "ebook_available", true)),
                createItem(list, "Dune", 5, Map.of(
                        "author", "Frank Herbert", "pages", 688, "published_date", "1965-08-01", "ebook_available", true)),
                createItem(list, "Project Hail Mary", 3, Map.of(
                        "author", "Andy Weir", "pages", 496, "published_date", "2021-05-04", "ebook_available", false)),
                createItem(list, "Atomic Habits", 6, Map.of(
                        "author", "James Clear", "pages", 320, "published_date", "2018-10-16", "ebook_available", true))
        );
    }

    private static List<Item> createToolItems(ItemList list) {
        return List.of(
                createItem(list, "DeWalt Cordless Drill", 4, Map.of(
                        "brand", "DeWalt", "purchase_date", "2024-03-15", "power_tool", true)),
                createItem(list, "Stanley Socket Set (150pc)", 2, Map.of(
                        "brand", "Stanley", "purchase_date", "2023-11-20", "power_tool", false)),
                createItem(list, "Bosch Circular Saw", 1, Map.of(
                        "brand", "Bosch", "purchase_date", "2024-07-08", "power_tool", true)),
                createItem(list, "Craftsman Adjustable Wrench Set", 3, Map.of(
                        "brand", "Craftsman", "purchase_date", "2023-06-01", "power_tool", false)),
                createItem(list, "Makita Random Orbit Sander", 2, Map.of(
                        "brand", "Makita", "purchase_date", "2025-01-12", "power_tool", true))
        );
    }

    private static List<Item> createCampingItems(ItemList list) {
        return List.of(
                createItem(list, "REI Half Dome 2-Person Tent", 2, Map.of(
                        "weight_kg", 2.36, "waterproof", true, "last_used", "2025-09-14")),
                createItem(list, "Therm-a-Rest NeoAir Sleeping Pad", 3, Map.of(
                        "weight_kg", 0.34, "waterproof", false, "last_used", "2025-09-14")),
                createItem(list, "Jetboil Flash Cooking System", 1, Map.of(
                        "weight_kg", 0.37, "waterproof", false, "last_used", "2025-08-20")),
                createItem(list, "Black Diamond Headlamp", 6, Map.of(
                        "weight_kg", 0.075, "waterproof", true, "last_used", "2025-11-02")),
                createItem(list, "Osprey Atmos 65L Backpack", 1, Map.of(
                        "weight_kg", 2.01, "waterproof", false, "last_used", "2025-09-14"))
        );
    }

    private static Item createItem(ItemList list, String name, int stock, Map<String, Object> customFieldValues) {
        Item item = new Item();
        item.setName(name);
        item.setStatus(STATUSES[RANDOM.nextInt(STATUSES.length)]);
        item.setStock(stock);
        item.setCustomFieldValues(customFieldValues);
        item.setItemList(list);
        list.getItems().add(item);
        return item;
    }
}
