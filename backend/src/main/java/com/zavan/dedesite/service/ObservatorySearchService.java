package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ObservatorySearchService {
    private final StarSystemService starSystemService;
    private final StarService starService;
    private final CometService cometService;

    public ObservatorySearchService(StarSystemService starSystemService, StarService starService, CometService cometService) {
        this.starSystemService = starSystemService;
        this.starService = starService;
        this.cometService = cometService;
    }

    public List<Result> search(User user, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<Result> results = new ArrayList<>();
        for (StarSystem starSystem : starSystemService.findAll(user)) {
            if (matches(needle, starSystem.getName(), starSystem.getDescription())) {
                results.add(new Result("Star System", starSystem.getName(), starSystem.getDescription(), "/observatory/star-systems/" + starSystem.getPublicId()));
            }
        }
        for (Star star : starService.findAll(user)) {
            if (matches(needle, star.getTitle(), star.getDescription())) {
                results.add(new Result("Star", star.getTitle(), star.getDescription(), "/observatory/stars/" + star.getPublicId() + "/edit"));
            }
        }
        for (Comet comet : cometService.findAll(user)) {
            if (matches(needle, comet.getTitle(), comet.getDescription())) {
                results.add(new Result("Comet", comet.getTitle(), comet.getDescription(), "/observatory/comets/" + comet.getPublicId() + "/edit"));
            }
        }
        return results;
    }

    private boolean matches(String needle, String... fields) {
        for (String field : fields) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record Result(String type, String title, String description, String url) {}
}
