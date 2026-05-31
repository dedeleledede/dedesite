package com.zavan.dedesite.service;

import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.StarRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StarService {
    private final StarRepository starRepository;
    private final StarSystemService starSystemService;

    public StarService(StarRepository starRepository, StarSystemService starSystemService) {
        this.starRepository = starRepository;
        this.starSystemService = starSystemService;
    }

    public List<Star> findAll(User user) {
        return starRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Star> findByStatus(User user, Star.Status status) {
        return starRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status);
    }

    public List<Star> findOpen(User user) {
        return starRepository.findByUserAndStatusNotOrderByDueDateAscCreatedAtDesc(user, Star.Status.DONE);
    }

    public List<Star> findForStarSystem(User user, StarSystem starSystem) {
        return starRepository.findByUserAndStarSystemOrderByStatusAscDueDateAsc(user, starSystem);
    }

    public long countOpen(User user, StarSystem starSystem) {
        return starRepository.countByUserAndStarSystemAndStatusNot(user, starSystem, Star.Status.DONE);
    }

    public long countAll(User user, StarSystem starSystem) {
        return starRepository.countByUserAndStarSystem(user, starSystem);
    }

    public Optional<Star> nextOpen(User user, StarSystem starSystem) {
        return starRepository.findFirstByUserAndStarSystemAndStatusNotOrderByDueDateAscCreatedAtAsc(user, starSystem, Star.Status.DONE);
    }

    public Star getOwned(Long id, User user) {
        return starRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Star getOwned(UUID publicId, User user) {
        return starRepository.findByPublicIdAndUser(publicId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Star save(Star star, Long starSystemId, User user) {
        star.setUser(user);
        star.setStarSystem(resolveStarSystem(starSystemId, user));
        syncStatusFromSchedule(star);
        return starRepository.save(star);
    }

    public void update(UUID publicId, Star form, Long starSystemId, User user) {
        Star star = getOwned(publicId, user);
        star.setTitle(form.getTitle());
        star.setDescription(form.getDescription());
        star.setStatus(form.getStatus());
        star.setPriority(form.getPriority());
        star.setEnergyType(form.getEnergyType());
        star.setDueDate(form.getDueDate());
        star.setEstimatedMinutes(form.getEstimatedMinutes());
        star.setScheduledStart(form.getScheduledStart());
        star.setScheduledEnd(form.getScheduledEnd());
        star.setStarSystem(resolveStarSystem(starSystemId, user));
        if (star.getStatus() == Star.Status.DONE && star.getCompletedAt() == null) {
            star.setCompletedAt(LocalDateTime.now());
        } else if (star.getStatus() != Star.Status.DONE) {
            star.setCompletedAt(null);
        }
        syncStatusFromSchedule(star);
        starRepository.save(star);
    }

    public void complete(UUID publicId, User user) {
        Star star = getOwned(publicId, user);
        star.setStatus(Star.Status.DONE);
        star.setCompletedAt(LocalDateTime.now());
        starRepository.save(star);
    }

    public void delete(UUID publicId, User user) {
        starRepository.delete(getOwned(publicId, user));
    }

    private StarSystem resolveStarSystem(Long starSystemId, User user) {
        if (starSystemId == null) {
            return null;
        }
        return starSystemService.getOwned(starSystemId, user);
    }

    private void syncStatusFromSchedule(Star star) {
        if (star.getStatus() != Star.Status.DONE
                && star.getScheduledStart() != null
                && star.getScheduledEnd() != null
                && star.getStatus() == Star.Status.READY) {
            star.setStatus(Star.Status.SCHEDULED);
        }
    }
}
