package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Constellation;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.StarRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StarService {
    private final StarRepository starRepository;
    private final ConstellationService constellationService;

    public StarService(StarRepository starRepository, ConstellationService constellationService) {
        this.starRepository = starRepository;
        this.constellationService = constellationService;
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

    public List<Star> findForConstellation(User user, Constellation constellation) {
        return starRepository.findByUserAndConstellationOrderByStatusAscDueDateAsc(user, constellation);
    }

    public long countOpen(User user, Constellation constellation) {
        return starRepository.countByUserAndConstellationAndStatusNot(user, constellation, Star.Status.DONE);
    }

    public Optional<Star> nextOpen(User user, Constellation constellation) {
        return starRepository.findFirstByUserAndConstellationAndStatusNotOrderByDueDateAscCreatedAtAsc(user, constellation, Star.Status.DONE);
    }

    public Star getOwned(Long id, User user) {
        return starRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Star save(Star star, Long constellationId, User user) {
        star.setUser(user);
        star.setConstellation(resolveConstellation(constellationId, user));
        syncStatusFromSchedule(star);
        return starRepository.save(star);
    }

    public void update(Long id, Star form, Long constellationId, User user) {
        Star star = getOwned(id, user);
        star.setTitle(form.getTitle());
        star.setDescription(form.getDescription());
        star.setStatus(form.getStatus());
        star.setPriority(form.getPriority());
        star.setDueDate(form.getDueDate());
        star.setEstimatedMinutes(form.getEstimatedMinutes());
        star.setScheduledStart(form.getScheduledStart());
        star.setScheduledEnd(form.getScheduledEnd());
        star.setConstellation(resolveConstellation(constellationId, user));
        if (star.getStatus() == Star.Status.DONE && star.getCompletedAt() == null) {
            star.setCompletedAt(LocalDateTime.now());
        } else if (star.getStatus() != Star.Status.DONE) {
            star.setCompletedAt(null);
        }
        syncStatusFromSchedule(star);
        starRepository.save(star);
    }

    public void complete(Long id, User user) {
        Star star = getOwned(id, user);
        star.setStatus(Star.Status.DONE);
        star.setCompletedAt(LocalDateTime.now());
        starRepository.save(star);
    }

    public void delete(Long id, User user) {
        starRepository.delete(getOwned(id, user));
    }

    private Constellation resolveConstellation(Long constellationId, User user) {
        if (constellationId == null) {
            return null;
        }
        return constellationService.getOwned(constellationId, user);
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
