package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.CometRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CometService {
    private final CometRepository cometRepository;
    private final StarSystemService starSystemService;

    public CometService(CometRepository cometRepository, StarSystemService starSystemService) {
        this.cometRepository = cometRepository;
        this.starSystemService = starSystemService;
    }

    public List<Comet> findAll(User user) {
        return cometRepository.findByUserOrderByDateAscStartTimeAsc(user);
    }

    public List<Comet> upcoming(User user) {
        return cometRepository.findByUserAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(user, LocalDate.now());
    }

    public List<Comet> findForStarSystem(User user, StarSystem starSystem) {
        return cometRepository.findByUserAndRelatedStarSystemOrderByDateAscStartTimeAsc(user, starSystem);
    }

    public List<Comet> remindersDue(User user, LocalDateTime now) {
        return cometRepository.findByUserAndRemindAtLessThanEqualOrderByRemindAtAsc(user, now);
    }

    public Comet getOwned(Long id, User user) {
        return cometRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Comet getOwned(UUID publicId, User user) {
        return cometRepository.findByPublicIdAndUser(publicId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Comet save(Comet comet, Long starSystemId, User user) {
        comet.setUser(user);
        comet.setRelatedStarSystem(resolveStarSystem(starSystemId, user));
        return cometRepository.save(comet);
    }

    public void update(UUID publicId, Comet form, Long starSystemId, User user) {
        Comet comet = getOwned(publicId, user);
        comet.setTitle(form.getTitle());
        comet.setDescription(form.getDescription());
        comet.setType(form.getType());
        comet.setDate(form.getDate());
        comet.setStartTime(form.getStartTime());
        comet.setEndTime(form.getEndTime());
        comet.setRemindAt(form.getRemindAt());
        comet.setPriority(form.getPriority());
        comet.setRelatedStarSystem(resolveStarSystem(starSystemId, user));
        cometRepository.save(comet);
    }

    public void delete(UUID publicId, User user) {
        cometRepository.delete(getOwned(publicId, user));
    }

    private StarSystem resolveStarSystem(Long starSystemId, User user) {
        if (starSystemId == null) {
            return null;
        }
        return starSystemService.getOwned(starSystemId, user);
    }
}
