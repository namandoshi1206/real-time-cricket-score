package com.cricket;

import com.cricket.controller.InningsController;
import com.cricket.dto.DeliveryResponse;
import com.cricket.dto.InningsResponse;
import com.cricket.entity.InningsStatus;
import com.cricket.exception.GlobalExceptionHandler;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.service.DeliveryService;
import com.cricket.service.InningsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InningsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InningsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InningsService inningsService;

    @MockBean
    private DeliveryService deliveryService;

    @Test
    void createsInnings() throws Exception {
        when(inningsService.create(eq(10L), any())).thenReturn(innings(100L));

        mockMvc.perform(post("/api/matches/10/innings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"battingTeamId\":1,\"bowlingTeamId\":2,\"inningsNumber\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/innings/100"))
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void retrievesInningsAndScore() throws Exception {
        when(inningsService.findById(100L)).thenReturn(innings(100L));
                when(deliveryService.scorecard(100L)).thenReturn(innings(100L));

        mockMvc.perform(get("/api/innings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overs").value("0.0"));

        mockMvc.perform(get("/api/innings/100/score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRuns").value(0));
    }

    @Test
    void retrievesDeliveries() throws Exception {
        when(deliveryService.findByInningsId(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/innings/100/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returnsMissingInnings() throws Exception {
        when(inningsService.findById(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Innings not found: 999"));

        mockMvc.perform(get("/api/innings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Innings not found: 999"));
    }

    private InningsResponse innings(Long id) {
        return new InningsResponse(id, 10L, 1L, "Batting Team", 2L, "Bowling Team", 1,
                null, 0, 0, 0, "0.0", InningsStatus.LIVE, null, null, null);
    }
}