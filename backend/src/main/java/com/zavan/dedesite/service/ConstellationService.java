package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Constellation;
import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.CometRepository;
import com.zavan.dedesite.repository.ConstellationRepository;
import com.zavan.dedesite.repository.StarRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConstellationService {
    private final ConstellationRepository constellationRepository;
    private final StarRepository starRepository;
    private final CometRepository cometRepository;

    public ConstellationService(ConstellationRepository constellationRepository, StarRepository starRepository, CometRepository cometRepository) {
        this.constellationRepository = constellationRepository;
        this.starRepository = starRepository;
        this.cometRepository = cometRepository;
    }

    public List<Constellation> findAll(User user) {
        return constellationRepository.findByUserOrderByStatusAscPriorityDescNameAsc(user);
    }

    public List<Constellation> findOpen(User user) {
        return constellationRepository.findByUserAndStatusNotOrderByPriorityDescNameAsc(user, Constellation.Status.ARCHIVED);
    }

    public Constellation getOwned(Long id, User user) {
        return constellationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Constellation save(Constellation constellation, User user) {
        constellation.setUser(user);
        return constellationRepository.save(constellation);
    }

    public void update(Long id, Constellation form, User user) {
        Constellation constellation = getOwned(id, user);
        constellation.setName(form.getName());
        constellation.setDescription(form.getDescription());
        constellation.setStatus(form.getStatus());
        constellation.setPriority(form.getPriority());
        constellation.setEstimatedWeeklyHours(form.getEstimatedWeeklyHours());
        constellation.setDeadline(form.getDeadline());
        constellation.setEnergyType(form.getEnergyType());
        constellationRepository.save(constellation);
    }

    public void archive(Long id, User user) {
        Constellation constellation = getOwned(id, user);
        constellation.setStatus(Constellation.Status.ARCHIVED);
        constellationRepository.save(constellation);
    }

    public void delete(Long id, User user) {
        Constellation constellation = getOwned(id, user);
        List<Star> stars = starRepository.findByUserAndConstellationOrderByStatusAscDueDateAsc(user, constellation);
        stars.forEach(star -> star.setConstellation(null));
        starRepository.saveAll(stars);
        List<Comet> comets = cometRepository.findByUserAndRelatedConstellationOrderByDateAscStartTimeAsc(user, constellation);
        comets.forEach(comet -> comet.setRelatedConstellation(null));
        cometRepository.saveAll(comets);
        constellationRepository.delete(constellation);
    }
}
