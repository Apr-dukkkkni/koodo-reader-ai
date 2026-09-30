package com.koodoagent.controller;

import com.koodoagent.agent.QueryRouterService;
import com.koodoagent.dto.RouteDecisionDTO;
import com.koodoagent.dto.RouteTestRequestDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent")
public class RouterTestController {

    private final QueryRouterService queryRouterService;

    public RouterTestController(QueryRouterService queryRouterService) {
        this.queryRouterService = queryRouterService;
    }

    @PostMapping("/route-test")
    public RouteDecisionDTO routeTest(@RequestBody RouteTestRequestDTO request) {
        return queryRouterService.route(
                request.term(),
                request.question(),
                request.context()
        );
    }
}