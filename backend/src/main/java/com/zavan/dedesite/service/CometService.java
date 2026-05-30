package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Constellation;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.CometRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CometService {
    private final CometRepository cometRepository;
    private final ConstellationService constellationService;

    public CometService(CometRepository cometRepository, ConstellationService constellationService) {
        this.cometRepository = cometRepository;
        this.constellationService = constellationService;
    }

    public List<Comet> findAll(User user) {
        return cometRepository.findByUserOrderByDateAscStartTimeAsc(user);
    }

    public List<Comet> upcoming(User user) {
        return cometRepository.findByUserAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(user, LocalDate.now());
    }

    public List<Comet> findForConstellation(User user, Constellation constellation) {
        return cometRepository.findByUserAndRelatedConstellationOrderByDateAscStartTimeAsc(user, constellation);
    }

    public Comet getOwned(Long id, User user) {
        return cometRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Comet save(Comet comet, Long constellationId, User user) {
        comet.setUser(user);
        comet.setRelatedConstellation(resolveConstellation(constellationId, user));
        return cometRepository.save(comet);
    }

    public void update(Long id, Comet form, Long constellationId, User user) {
        Comet comet = getOwned(id, user);
        comet.setTitle(form.getTitle());
        comet.setDescription(form.getDescription());
        comet.setType(form.getType());
        comet.setDate(form.getDate());
        comet.setStartTime(form.getStartTime());
        comet.setEndTime(form.getEndTime());
        comet.setPriority(form.getPriority());
        comet.setRelatedConstellation(resolveConstellation(constellationId, user));
        cometRepository.save(comet);
    }

    public void delete(Long id, User user) {
        cometRepository.delete(getOwned(id, user));
    }

    private Constellation resolveConstellation(Long constellationId, User user) {
        if (constellationId == null) {
            return null;
        }
        return constellationService.getOwned(constellationId, user);
    }
}
