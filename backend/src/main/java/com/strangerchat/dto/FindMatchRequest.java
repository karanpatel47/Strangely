package com.strangerchat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request to enter the matchmaking queue with a specified gender.
 */
public class FindMatchRequest {
    @NotNull(message = "Gender is required")
    private Gender gender;

    public FindMatchRequest() {}

    public FindMatchRequest(Gender gender) {
        this.gender = gender;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }
}
