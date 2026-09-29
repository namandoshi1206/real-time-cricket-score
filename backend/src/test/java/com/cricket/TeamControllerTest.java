package com.cricket;

import com.cricket.controller.TeamController;
import com.cricket.dto.TeamResponse;
import com.cricket.exception.GlobalExceptionHandler;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(TeamController.class)
@Import(GlobalExceptionHandler.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    @Test
    void createsTeam() throws Exception {
        when(teamService.create(any())).thenReturn(new TeamResponse(1L, "India", "IND", "India"));

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"India\",\"shortName\":\"IND\",\"country\":\"India\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/teams/1"))
                .andExpect(jsonPath("$.name").value("India"));
    }

    @Test
    void retrievesTeams() throws Exception {
        when(teamService.findAll()).thenReturn(List.of(new TeamResponse(1L, "India", "IND", "India")));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortName").value("IND"));
    }

    @Test
    void rejectsInvalidTeamRequest() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"shortName\":\"IND\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    void returnsNotFoundForMissingTeam() throws Exception {
        when(teamService.findById(eq(99L))).thenThrow(new ResourceNotFoundException("Team not found: 99"));

        mockMvc.perform(get("/api/teams/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found: 99"));
    }
}