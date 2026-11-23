package com.caddieapp.loop;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AuthUser;
import com.caddieapp.common.Enums.BagType;
import com.caddieapp.queue.QueueDtos.BoardResponse;
import com.caddieapp.queue.QueueQueryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The admin's buttons on the board. Admin-only; the board actions return the updated board. */
@RestController
@RequestMapping("/api/admin")
public class BoardController {

    public record CallRequest(@NotNull BagType bagType) {
    }

    /** lineNumber null = undo a manual move. */
    public record MoveRequest(@Min(1) Integer lineNumber) {
    }

    public record BagTypeRequest(@NotNull BagType bagType) {
    }

    public record DoubleLoopRequest(@NotNull Boolean doubleLoop) {
    }

    public record AddBagRequest(@NotNull Long caddieId, @NotNull LocalDate date, @NotNull BagType bagType) {
    }

    private final AssignmentService assignments;
    private final QueueQueryService queries;

    public BoardController(AssignmentService assignments, QueueQueryService queries) {
        this.assignments = assignments;
        this.queries = queries;
    }

    @PostMapping("/board/{caddieId}/call")
    public BoardResponse call(@AuthenticationPrincipal AuthUser admin, @PathVariable long caddieId,
                              @Valid @RequestBody CallRequest request) {
        assignments.call(admin.id(), caddieId, request.bagType());
        return queries.board();
    }

    @PostMapping("/board/{caddieId}/send-out")
    public BoardResponse sendOut(@AuthenticationPrincipal AuthUser admin, @PathVariable long caddieId) {
        assignments.sendOut(admin.id(), caddieId);
        return queries.board();
    }

    @PostMapping("/board/{caddieId}/no-reply")
    public BoardResponse noReply(@AuthenticationPrincipal AuthUser admin, @PathVariable long caddieId) {
        assignments.noReply(admin.id(), caddieId);
        return queries.board();
    }

    @PostMapping("/board/{caddieId}/back-in")
    public BoardResponse backIn(@AuthenticationPrincipal AuthUser admin, @PathVariable long caddieId) {
        assignments.backIn(admin.id(), caddieId);
        return queries.board();
    }

    @PostMapping("/board/{caddieId}/move")
    public BoardResponse move(@AuthenticationPrincipal AuthUser admin, @PathVariable long caddieId,
                              @Valid @RequestBody MoveRequest request) {
        assignments.moveInLine(admin.id(), caddieId, request.lineNumber(), queries.lineNumberOf(caddieId));
        return queries.board();
    }

    /** Mark / unmark a double loop (round twice on the same bag: counts twice). */
    @PostMapping("/loops/{loopId}/double-loop")
    public BoardResponse doubleLoop(@AuthenticationPrincipal AuthUser admin, @PathVariable long loopId,
                                    @Valid @RequestBody DoubleLoopRequest request) {
        assignments.markDoubleLoop(admin.id(), loopId, request.doubleLoop());
        return queries.board();
    }

    /** Enter a bag by hand (from the Caddies tab, so no board comes back). */
    @PostMapping("/loops")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBag(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody AddBagRequest request) {
        assignments.addBag(admin.id(), request.caddieId(), request.date(), request.bagType());
    }

    /** Correct a bag: it was really a single / double. */
    @PostMapping("/loops/{loopId}/bag-type")
    public BoardResponse changeBagType(@AuthenticationPrincipal AuthUser admin, @PathVariable long loopId,
                                       @Valid @RequestBody BagTypeRequest request) {
        assignments.changeBagType(admin.id(), loopId, request.bagType());
        return queries.board();
    }
}
