package org.lab1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.lab1.dto.InvestmentOfferCreateRequest;
import org.lab1.dto.InvestmentOfferResponse;
import org.lab1.service.InvestmentService;
import org.lab1.web.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Investment offers", description = "Offers made by investors to published projects")
@RestController
@RequestMapping("/api/investment-offers")
@RequiredArgsConstructor
public class InvestmentOfferController {

    private final InvestmentService investmentService;

    @Operation(summary = "Make an investment offer")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentOfferResponse create(@Valid @RequestBody InvestmentOfferCreateRequest request) {
        return investmentService.createOffer(request);
    }

    @Operation(summary = "Get an offer by id")
    @GetMapping("/{id}")
    public InvestmentOfferResponse getById(@PathVariable Long id) {
        return investmentService.getById(id);
    }

    @Operation(summary = "List offers filtered by project or investor; total count is returned in the X-Total-Count header")
    @GetMapping
    public ResponseEntity<List<InvestmentOfferResponse>> findAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long investorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_PAGE_SIZE) int size) {
        Page<InvestmentOfferResponse> result = investmentService.findAll(projectId, investorId,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return ResponseEntity.ok()
                .header(Pagination.TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .header(Pagination.TOTAL_PAGES_HEADER, String.valueOf(result.getTotalPages()))
                .body(result.getContent());
    }

    @Operation(summary = "Accept an offer: raised amount grows, project becomes FUNDED when the goal is reached")
    @PostMapping("/{id}/accept")
    public InvestmentOfferResponse accept(@PathVariable Long id) {
        return investmentService.accept(id);
    }

    @Operation(summary = "Reject an offer (by the project team)")
    @PostMapping("/{id}/reject")
    public InvestmentOfferResponse reject(@PathVariable Long id) {
        return investmentService.reject(id);
    }

    @Operation(summary = "Withdraw an offer (by the investor)")
    @PostMapping("/{id}/withdraw")
    public InvestmentOfferResponse withdraw(@PathVariable Long id) {
        return investmentService.withdraw(id);
    }
}
