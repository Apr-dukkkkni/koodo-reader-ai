package com.koodoagent.controller;

import com.koodoagent.dto.ConceptProfileDTO;
import com.koodoagent.memory.ProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/profile")
public class ProfileTestController {

    private final ProfileService profileService;

    public ProfileTestController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{concept}")
    public Map<String, Object> get(@PathVariable String concept) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conceptName", concept);
        result.put("familiarityLevel", profileService.getFamiliarity(concept));
        result.put("profile", profileService.get(concept).orElse(null));
        return result;
    }

    @DeleteMapping("/{concept}")
    public Map<String, Object> reset(@PathVariable String concept) {
        profileService.resetConcept(concept);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("concept", concept);
        result.put("reset", true);
        return result;
    }

    @PostMapping("/{concept}/ask")
    public Map<String, Object> ask(@PathVariable String concept) {
        profileService.recordAsk(concept);
        return get(concept);
    }

    @PostMapping("/{concept}/familiarity")
    public Map<String, Object> setFamiliarity(@PathVariable String concept,
                                              @RequestParam int level) {
        profileService.setFamiliarity(concept, level);
        return get(concept);
    }

    @GetMapping("/list")
    public List<ConceptProfileDTO> list() {
        return profileService.listAll();
    }
}