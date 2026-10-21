package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.*;
import com.travelbuddy.api.model.Models.*;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final TravelBuddyStateService stateService;

    public ExpenseController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping("/groups")
    public ResponseEntity<ExpenseGroup> createGroup(@RequestBody CreateGroupRequest request) {
        ExpenseGroup group = stateService.createExpenseGroup(request);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<ExpenseGroup>> getGroups() {
        return ResponseEntity.ok(stateService.getExpenseGroups());
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable String groupId) {
        return stateService.getExpenseGroup(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@RequestBody AddExpenseRequest request) {
        Expense expense = stateService.addExpense(request);
        return ResponseEntity.ok(expense);
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String tripId
    ) {
        if (groupId != null && !groupId.isBlank()) {
            return ResponseEntity.ok(stateService.getExpensesByGroupId(groupId));
        }
        if (tripId != null && !tripId.isBlank()) {
            return ResponseEntity.ok(stateService.getExpensesByTripId(tripId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/groups/{groupId}/settlements")
    public ResponseEntity<List<Settlement>> getGroupSettlements(@PathVariable String groupId) {
        List<Settlement> settlements = stateService.calculateSettlements(groupId);
        return ResponseEntity.ok(settlements);
    }

    @GetMapping("/trips/{tripId}/settlements")
    public ResponseEntity<List<Settlement>> getTripSettlements(@PathVariable String tripId) {
        return stateService.getExpenseGroupByTripId(tripId)
                .map(group -> ResponseEntity.ok(stateService.calculateSettlements(group.id())))
                .orElse(ResponseEntity.ok(List.of()));
    }

    @PostMapping("/settle")
    public ResponseEntity<Settlement> settleDebt(@RequestBody SettleRequest request) {
        Settlement settlement = stateService.recordSettlementPayment(request);
        return ResponseEntity.ok(settlement);
    }
}
