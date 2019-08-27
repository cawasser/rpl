(ns explaining-solver)

;
; As I started to think about how we might use both the GRID and REQUESTS
; in a more real-world setting (users at a browser formulating requests, which
; are sent to a service that performs the allocation transformation and
; applying it to the GRID, I realized there are some significant limitations
; in out current approach.
;
; Yes, I did say initially that we WANTED a limited approach, so this isn't
; unexpected. We knew going in that we would need to keep refining/expanding
; the problem we were trying to solve, that's why I've been referring to
; things as 'experiments.'
;
;
;
;
; So what is the problem? There seem to be several, and I'll enumerate them
; here:
;
; 1) Do users submit ALL the requests, including prior requests?
;
;      This affect the set of constraints provided to the solver. Currently,
;      we sort of ARE passing everything in, so the solver really is finding
;      a acceptable solution for ALL requests. Is this how we really want it
;      to work?
;
; 2) Do we consider existing allocations (made during prior applications) as
;    sacrosanct, i.e., can never change them?
;
;      What if a given allocation was made from a flexible request? If a
;      newer request needs that particular slot and one of the OTHER
;      acceptable slots was still available, do we reject the request (as
;      would happen currently) or do we have the option of moving the older
;      request (assuming prioritization, some other sort of weighting, etc).
;
;      Now the big question - How would we know? Currently, the solver does
;      NOT explain how any particular 'binding' was made, i.e., which
;      constraints applied. We have no idea, after the fact, that we gave
;      :b cell [0 0] instead of [0 1]. If later :q needs [0 0], we could get
;      a solution because :b could be allocated [0 1], but :b is locked to
;      [0 0] the initial GRID, so the :q constraint will fail and we'll
;      get {}.
;
;      This reasoning depends on the subsequent requests only including the
;      new requests. We 'figure out' the older requests by processing the
;      initial GRID, which creates 100% FIXED requests. The flexibility is
;      NOT recoverable.
;
; 3) On the other hand, we could preserve the entire "request state" and pass
;    THAT to the solver. We're more likely to get an answer, but we then add
;    the situation that some allocations might get changed AFTER we've applied
;    them on an earlier pass!
;
;
;
;
;
;