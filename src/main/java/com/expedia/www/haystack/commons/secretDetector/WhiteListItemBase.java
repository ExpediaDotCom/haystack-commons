package com.expedia.www.haystack.commons.secretDetector;

import java.util.Arrays;

abstract class WhiteListItemBase {
    final String[] items;

    WhiteListItemBase(String... items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhiteListItemBase that = (WhiteListItemBase) o;
        return Arrays.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }
}
