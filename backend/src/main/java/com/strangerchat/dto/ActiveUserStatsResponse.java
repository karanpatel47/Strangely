package com.strangerchat.dto;

/**
 * Aggregate statistics returned by the admin active-users endpoint.
 * Contains only counts — no user IDs, session IDs, or personal information.
 *
 * Invariant: {@code totalActiveUsers == male + female}
 */
public class ActiveUserStatsResponse {

    private int totalActiveUsers;
    private int male;
    private int female;

    public ActiveUserStatsResponse() {}

    public ActiveUserStatsResponse(int totalActiveUsers, int male, int female) {
        this.totalActiveUsers = totalActiveUsers;
        this.male = male;
        this.female = female;
    }

    public int getTotalActiveUsers() { return totalActiveUsers; }
    public void setTotalActiveUsers(int totalActiveUsers) { this.totalActiveUsers = totalActiveUsers; }
    public int getMale() { return male; }
    public void setMale(int male) { this.male = male; }
    public int getFemale() { return female; }
    public void setFemale(int female) { this.female = female; }
}
