package com.campus.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class WalletModesRequest {

    @NotNull
    private List<String> modes;

    public List<String> getModes() {
        return modes;
    }

    public void setModes(List<String> modes) {
        this.modes = modes;
    }
}
