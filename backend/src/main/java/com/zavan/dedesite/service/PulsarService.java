package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.OrbitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PulsarService {
    private final OrbitRepository orbitRepository;
    private final StarSystemService starSystemService;

    public PulsarService(OrbitRepository orbitRepository, StarSystemService starSystemService) {
        this.orbitRepository = orbitRepository;
        this.starSystemService = starSystemService;
    }

    public List<Orbit> findAll(User user) {
        return orbitRepository.findByUserAndKindOrderByActiveDescCreatedAtDesc(user, Orbit.Kind.PULSAR);
    }

    public List<Orbit> findActive(User user) {
        return orbitRepository.findByUserAndKindAndActiveTrueOrderByCreatedAtDesc(user, Orbit.Kind.PULSAR);
    }

    public Orbit getOwned(UUID publicId, User user) {
        Orbit orbit = orbitRepository.findByPublicIdAndUser(publicId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!orbit.isPulsar()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return orbit;
    }

    public Orbit save(Orbit pulsar, Long starSystemId, User user) {
        applyPulsarDefaults(pulsar, user, starSystemId);
        return orbitRepository.save(pulsar);
    }

    public void update(UUID publicId, Orbit form, Long starSystemId, User user) {
        Orbit pulsar = getOwned(publicId, user);
        pulsar.setTitle(form.getTitle());
        pulsar.setDescription(form.getDescription());
        pulsar.setTargetMinutesPerWeek(form.getTargetMinutesPerWeek());
        pulsar.setMinimumSessionMinutes(form.getMinimumSessionMinutes());
        pulsar.setMaximumSessionMinutes(form.getMaximumSessionMinutes());
        pulsar.setEnergyType(form.getEnergyType());
        pulsar.setPriority(form.getPriority());
        pulsar.setStarSystem(resolveStarSystem(starSystemId, user));
        pulsar.setAutoSchedule(form.isAutoSchedule());
        pulsar.setActive(form.isActive());
        orbitRepository.save(pulsar);
    }

    public void toggle(UUID publicId, User user) {
        Orbit pulsar = getOwned(publicId, user);
        pulsar.setActive(!pulsar.isActive());
        orbitRepository.save(pulsar);
    }

    public void delete(UUID publicId, User user) {
        orbitRepository.delete(getOwned(publicId, user));
    }

    private void applyPulsarDefaults(Orbit pulsar, User user, Long starSystemId) {
        pulsar.setUser(user);
        pulsar.setKind(Orbit.Kind.PULSAR);
        pulsar.setFlexibility(Orbit.Flexibility.FLEXIBLE);
        pulsar.setStarSystem(resolveStarSystem(starSystemId, user));
    }

    private StarSystem resolveStarSystem(Long starSystemId, User user) {
        return starSystemId == null ? null : starSystemService.getOwned(starSystemId, user);
    }
}
