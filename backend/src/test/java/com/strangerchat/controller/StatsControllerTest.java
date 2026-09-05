package com.strangerchat.controller;

import com.strangerchat.service.ActiveUserTracker;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest {

    @Test
    void activeUsersReturnsCurrentCountAsJson() throws Exception {
        ActiveUserTracker tracker = new ActiveUserTracker();
        tracker.register("session-1", "user-1");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatsController(tracker)).build();

        mockMvc.perform(get("/api/stats/active-users"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"activeUsers\":1}"));
    }

    @Test
    void adminActiveUsersReturnsTotalEvenWhenGenderIsUnknown() throws Exception {
        ActiveUserTracker tracker = new ActiveUserTracker();
        tracker.register("session-1", "user-1");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatsController(tracker)).build();

        mockMvc.perform(get("/api/admin/stats/active-users"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"totalActiveUsers\":1,\"male\":0,\"female\":0}"));
    }
}