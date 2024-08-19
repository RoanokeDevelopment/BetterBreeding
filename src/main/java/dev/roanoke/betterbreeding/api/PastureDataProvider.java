package dev.roanoke.betterbreeding.api;

import dev.roanoke.betterbreeding.pastures.real.RealPastureData;

public interface PastureDataProvider {
    RealPastureData getPastureData();
    void setPastureData(RealPastureData data);
}
