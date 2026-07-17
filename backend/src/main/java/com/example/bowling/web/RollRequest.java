package com.example.bowling.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to record a bowling roll")
public class RollRequest {

    @Schema(description = "Number of pins knocked down", example = "7", minimum = "0", maximum = "10", required = true)
    @NotNull
    @Min(0)
    @Max(10)
    private Integer pins;

    public Integer getPins() {
        return pins;
    }

    public void setPins(Integer pins) {
        this.pins = pins;
    }
}
