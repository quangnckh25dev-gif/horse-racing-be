package com.horseracing.dto;

import java.util.List;

public class HorseOptionsResponse {
    private List<OptionResponse> statuses;
    private List<OptionResponse> colors;
    private List<OptionResponse> breeds;

    public HorseOptionsResponse(List<OptionResponse> statuses, List<OptionResponse> colors, List<OptionResponse> breeds) {
        this.statuses = statuses;
        this.colors = colors;
        this.breeds = breeds;
    }

    public List<OptionResponse> getStatuses() {
        return statuses;
    }

    public List<OptionResponse> getColors() {
        return colors;
    }

    public List<OptionResponse> getBreeds() {
        return breeds;
    }
}
