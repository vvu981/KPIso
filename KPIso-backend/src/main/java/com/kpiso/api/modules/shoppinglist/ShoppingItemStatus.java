package com.kpiso.api.modules.shoppinglist;

public enum ShoppingItemStatus {
    PENDING("Pendiente"),
    BOUGHT("Comprado");

    private final String displayName;

    ShoppingItemStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
