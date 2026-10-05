package com.cricket;

import com.cricket.controller.MatchController;
import com.cricket.dto.MatchResponse;
import com.cricket.entity.MatchStatus;
import com.cricket.entity.MatchType;
import com.cricket.exception.GlobalExceptionHandler;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.service.MatchService;
import com.cricket.service.MatchSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MatchControllerTest {

    private static final String REQUEST = "{"
            + "\"title\":\"India vs Australia\","
            + "\"teamAId\":1,"
            + "\"teamBId\":2,"
            + "\"venue\":\"Mumbai\","
            + "\"matchType\":\"T20\","
            + "\"scheduledDate\":\"2030-01-01T18:00:00\","
            + "\"status\":\"UPCOMING\""
            + "}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private MatchSummaryService matchSummaryService;

    @Test
    void createsMatchSuccessfully() throws Exception {
        when(matchService.create(any())).thenReturn(match(1L, MatchType.T20, MatchStatus.UPCOMING));

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/matches/1"))
                .andExpect(jsonPath("$.title").value("India vs Australia"));
    }

    @Test
    void getsMatchById() throws Exception {
        when(matchService.findById(1L)).thenReturn(match(1L, MatchType.T20, MatchStatus.UPCOMING));

        mockMvc.perform(get("/api/matches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamAId").value(1))
                .andExpect(jsonPath("$.teamBId").value(2));
    }

    @Test
    void getsAllMatches() throws Exception {
        when(matchService.findAll()).thenReturn(List.of(match(1L, MatchType.T20, MatchStatus.UPCOMING)));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchType").value("T20"));
    }

    @Test
    void updatesMatch() throws Exception {
        when(matchService.update(eq(1L), any())).thenReturn(match(1L, MatchType.ODI, MatchStatus.LIVE));

        mockMvc.perform(put("/api/matches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("T20", "ODI").replace("UPCOMING", "LIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchType").value("ODI"))
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void deletesMatch() throws Exception {
        doNothing().when(matchService).delete(1L);

        mockMvc.perform(delete("/api/matches/1"))
                .andExpect(status().isNoContent());

        verify(matchService).delete(1L);
    }

    @Test
    void rejectsNonExistentTeam() throws Exception {
        when(matchService.create(any())).thenThrow(new ResourceNotFoundException("Team not found: 99"));

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"teamAId\":1", "\"teamAId\":99")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found: 99"));
    }

    @Test
    void rejectsSameTeamForBothSides() throws Exception {
        when(matchService.create(any())).thenThrow(new IllegalArgumentException("A match must use two different teams"));

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"teamBId\":2", "\"teamBId\":1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A match must use two different teams"));
    }

    @Test
    void rejectsInvalidMatchData() throws Exception {
        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"teamAId\":1,\"teamBId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    void filtersByStatus() throws Exception {
        when(matchService.findByStatus(MatchStatus.LIVE)).thenReturn(List.of(match(1L, MatchType.T20, MatchStatus.LIVE)));

        mockMvc.perform(get("/api/matches/status/LIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("LIVE"));
    }

    @Test
    void filtersByType() throws Exception {
        when(matchService.findByType(MatchType.TEST)).thenReturn(List.of(match(1L, MatchType.TEST, MatchStatus.UPCOMING)));

        mockMvc.perform(get("/api/matches/type/TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchType").value("TEST"));
    }

    @Test
    void returnsNotFoundForMissingMatch() throws Exception {
        when(matchService.findById(eq(99L))).thenThrow(new ResourceNotFoundException("Match not found: 99"));

        mockMvc.perform(get("/api/matches/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match not found: 99"));
    }

    private MatchResponse match(Long id, MatchType type, MatchStatus status) {
        return new MatchResponse(id, "India vs Australia", 1L, "India", 2L, "Australia",
                "Mumbai", type, LocalDateTime.of(2030, 1, 1, 18, 0), status);
    }
}