package com.cricket;

import com.cricket.dto.InningsResponse;
import com.cricket.dto.MatchResponse;
import com.cricket.dto.MatchSummaryResponse;
import com.cricket.entity.InningsStatus;
import com.cricket.entity.MatchStatus;
import com.cricket.entity.MatchType;
import com.cricket.service.DeliveryService;
import com.cricket.service.InningsService;
import com.cricket.service.MatchService;
import com.cricket.service.MatchSummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchSummaryServiceTest {

    @Mock
    private MatchService matchService;

    @Mock
    private InningsService inningsService;

    @Mock
    private DeliveryService deliveryService;

    @Test
    void determinesWinnerByRunsForCompletedMatch() {
        MatchSummaryService service = new MatchSummaryService(matchService, inningsService, deliveryService);
        MatchResponse match = new MatchResponse(1L, "Falcons vs Lions", 10L, "Falcons", 20L, "Lions",
                "Oval", MatchType.T20, LocalDateTime.now(), MatchStatus.COMPLETED);
        InningsResponse first = innings(11L, 10L, "Falcons", 175, 8, null);
        InningsResponse second = innings(12L, 20L, "Lions", 170, 10, 176);
        when(matchService.findById(1L)).thenReturn(match);
        when(inningsService.findByMatchId(1L)).thenReturn(List.of(first, second));
        when(deliveryService.scorecard(11L)).thenReturn(first);
        when(deliveryService.scorecard(12L)).thenReturn(second);

        MatchSummaryResponse summary = service.getSummary(1L);

        assertEquals(10L, summary.winnerTeamId());
        assertEquals("Falcons won by 5 runs", summary.result());
    }

    private InningsResponse innings(Long id, Long teamId, String teamName, int totalRuns,
                                    int wickets, Integer targetRuns) {
        return new InningsResponse(id, 1L, teamId, teamName, teamId == 10L ? 20L : 10L,
                teamId == 10L ? "Lions" : "Falcons", teamId == 10L ? 1 : 2, targetRuns,
                totalRuns, wickets, 120, "20.0", InningsStatus.COMPLETED, null, null, null);
    }
}