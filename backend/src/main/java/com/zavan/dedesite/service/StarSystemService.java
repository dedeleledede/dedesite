package com.zavan.dedesite.service;

import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.CometRepository;
import com.zavan.dedesite.repository.OrbitRepository;
import com.zavan.dedesite.repository.StarSystemRepository;
import com.zavan.dedesite.repository.StarRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StarSystemService {
    private final StarSystemRepository starSystemRepository;
    private final StarRepository starRepository;
    private final CometRepository cometRepository;
    private final OrbitRepository orbitRepository;

    public StarSystemService(StarSystemRepository starSystemRepository, StarRepository starRepository, CometRepository cometRepository, OrbitRepository orbitRepository) {
        this.starSystemRepository = starSystemRepository;
        this.starRepository = starRepository;
        this.cometRepository = cometRepository;
        this.orbitRepository = orbitRepository;
    }

    public List<StarSystem> findAll(User user) {
        return starSystemRepository.findByUserOrderByStatusAscPriorityDescCreatedAtDesc(user);
    }

    public List<StarSystem> findOpen(User user) {
        return starSystemRepository.findByUserAndStatusNotOrderByPriorityDescCreatedAtDesc(user, StarSystem.Status.ARCHIVED);
    }

    public StarSystem getOwned(Long id, User user) {
        return starSystemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public StarSystem getOwned(UUID publicId, User user) {
        return starSystemRepository.findByPublicIdAndUser(publicId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public StarSystem save(StarSystem starSystem, User user) {
        starSystem.setUser(user);
        return starSystemRepository.save(starSystem);
    }

    public void update(UUID publicId, StarSystem form, User user) {
        StarSystem starSystem = getOwned(publicId, user);
        starSystem.setName(form.getName());
        starSystem.setDescription(form.getDescription());
        starSystem.setStatus(form.getStatus());
        starSystem.setPriority(form.getPriority());
        starSystem.setEstimatedWeeklyHours(form.getEstimatedWeeklyHours());
        starSystem.setDeadline(form.getDeadline());
        starSystem.setEnergyType(form.getEnergyType());
        starSystemRepository.save(starSystem);
    }

    public void archive(UUID publicId, User user) {
        StarSystem starSystem = getOwned(publicId, user);
        starSystem.setStatus(StarSystem.Status.ARCHIVED);
        starSystemRepository.save(starSystem);
    }

    public void delete(UUID publicId, User user) {
        StarSystem starSystem = getOwned(publicId, user);
        List<Star> stars = starRepository.findByUserAndStarSystemOrderByStatusAscDueDateAsc(user, starSystem);
        stars.forEach(star -> star.setStarSystem(null));
        starRepository.saveAll(stars);
        List<Comet> comets = cometRepository.findByUserAndRelatedStarSystemOrderByDateAscStartTimeAsc(user, starSystem);
        comets.forEach(comet -> comet.setRelatedStarSystem(null));
        cometRepository.saveAll(comets);
        List<com.zavan.dedesite.model.Orbit> orbits = orbitRepository.findByUserAndStarSystem(user, starSystem);
        orbits.forEach(orbit -> orbit.setStarSystem(null));
        orbitRepository.saveAll(orbits);
        starSystemRepository.delete(starSystem);
    }
}
