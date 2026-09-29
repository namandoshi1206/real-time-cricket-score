package com.cricket.controller;

import com.cricket.dto.DeliveryRequest;
import com.cricket.dto.DeliveryResponse;
import com.cricket.dto.InningsRequest;
import com.cricket.dto.InningsResponse;
import com.cricket.service.DeliveryService;
import com.cricket.service.InningsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class InningsController {

    private final InningsService inningsService;
    private final DeliveryService deliveryService;

    public InningsController(InningsService inningsService, DeliveryService deliveryService) {
        this.inningsService = inningsService;
        this.deliveryService = deliveryService;
    }

    @PostMapping("/api/matches/{matchId}/innings")
    public ResponseEntity<InningsResponse> createInnings(@PathVariable Long matchId,
                                                         @Valid @RequestBody InningsRequest request) {
        InningsResponse response = inningsService.create(matchId, request);
        return ResponseEntity.created(URI.create("/api/innings/" + response.id())).body(response);
    }

    @GetMapping("/api/matches/{matchId}/innings")
    public List<InningsResponse> findByMatch(@PathVariable Long matchId) {
        return inningsService.findByMatchId(matchId);
    }

    @GetMapping("/api/innings/{inningsId}")
    public InningsResponse findById(@PathVariable Long inningsId) {
        return inningsService.findById(inningsId);
    }

    @PostMapping("/api/innings/{inningsId}/deliveries")
    public DeliveryResponse recordDelivery(@PathVariable Long inningsId,
                                           @Valid @RequestBody DeliveryRequest request) {
        return deliveryService.record(inningsId, request);
    }

    @GetMapping("/api/innings/{inningsId}/deliveries")
    public List<DeliveryResponse> findDeliveries(@PathVariable Long inningsId) {
        return deliveryService.findByInningsId(inningsId);
    }

    @GetMapping("/api/innings/{inningsId}/score")
    public InningsResponse score(@PathVariable Long inningsId) {
        return inningsService.findById(inningsId);
    }
}