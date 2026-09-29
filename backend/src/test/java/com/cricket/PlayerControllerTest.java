package com.cricket;

import com.cricket.controller.PlayerController;
import com.cricket.dto.PlayerResponse;
import com.cricket.entity.PlayerRole;
import com.cricket.exception.GlobalExceptionHandler;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.service.PlayerService;
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

@WebMvcTest(PlayerController.class)
@Import(GlobalExceptionHandler.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    @Test
    void createsPlayerWithTeamRelationship() throws Exception {
        when(playerService.create(any())).thenReturn(
                new PlayerResponse(1L, "Virat", "Kohli", PlayerRole.BATSMAN, null, null, 10L, "India"));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Virat\",\"lastName\":\"Kohli\",\"role\":\"BATSMAN\",\"teamId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/players/1"))
                .andExpect(jsonPath("$.teamId").value(10))
                .andExpect(jsonPath("$.teamName").value("India"));
    }

    @Test
    void retrievesPlayerById() throws Exception {
        when(playerService.findById(1L)).thenReturn(
                new PlayerResponse(1L, "Virat", "Kohli", PlayerRole.BATSMAN, null, null, 10L, "India"));

        mockMvc.perform(get("/api/players/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Virat"))
                .andExpect(jsonPath("$.teamId").value(10));
    }

    @Test
    void retrievesPlayersByTeam() throws Exception {
        when(playerService.findByTeamId(10L)).thenReturn(List.of(
                new PlayerResponse(1L, "Virat", "Kohli", PlayerRole.BATSMAN, null, null, 10L, "India")));

        mockMvc.perform(get("/api/players/team/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastName").value("Kohli"));
    }

    @Test
    void rejectsPlayerWithoutRequiredRole() throws Exception {
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Virat\",\"lastName\":\"Kohli\",\"teamId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.role").exists());
    }

    @Test
    void returnsNotFoundForMissingPlayer() throws Exception {
        when(playerService.findById(eq(99L))).thenThrow(new ResourceNotFoundException("Player not found: 99"));

        mockMvc.perform(get("/api/players/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Player not found: 99"));
    }
}