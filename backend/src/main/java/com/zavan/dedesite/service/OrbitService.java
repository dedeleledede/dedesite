package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.OrbitRepository;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrbitService {
    private final OrbitRepository orbitRepository;

    public OrbitService(OrbitRepository orbitRepository) {
        this.orbitRepository = orbitRepository;
    }

    public List<Orbit> findAll(User user) {
        return orbitRepository.findByUserOrderByDayOfWeekAscStartTimeAsc(user);
    }

    public List<Orbit> findActiveForDay(User user, DayOfWeek dayOfWeek) {
        return orbitRepository.findByUserAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(user, dayOfWeek);
    }

    public Orbit getOwned(Long id, User user) {
        return orbitRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Orbit save(Orbit orbit, User user) {
        orbit.setUser(user);
        return orbitRepository.save(orbit);
    }

    public void saveForDays(Orbit form, Collection<DayOfWeek> days, User user) {
        Collection<DayOfWeek> selectedDays = days == null || days.isEmpty() ? List.of(form.getDayOfWeek()) : days;
        for (DayOfWeek day : selectedDays) {
            Orbit orbit = new Orbit();
            orbit.setUser(user);
            orbit.setTitle(form.getTitle());
            orbit.setDescription(form.getDescription());
            orbit.setDayOfWeek(day);
            orbit.setStartTime(form.getStartTime());
            orbit.setEndTime(form.getEndTime());
            orbit.setCategory(form.getCategory());
            orbit.setColorKey(form.getColorKey());
            orbit.setActive(form.isActive());
            orbitRepository.save(orbit);
        }
    }

    public void update(Long id, Orbit form, User user) {
        Orbit orbit = getOwned(id, user);
        orbit.setTitle(form.getTitle());
        orbit.setDescription(form.getDescription());
        orbit.setDayOfWeek(form.getDayOfWeek());
        orbit.setStartTime(form.getStartTime());
        orbit.setEndTime(form.getEndTime());
        orbit.setCategory(form.getCategory());
        orbit.setColorKey(form.getColorKey());
        orbit.setActive(form.isActive());
        orbitRepository.save(orbit);
    }

    public void toggle(Long id, User user) {
        Orbit orbit = getOwned(id, user);
        orbit.setActive(!orbit.isActive());
        orbitRepository.save(orbit);
    }

    public void delete(Long id, User user) {
        orbitRepository.delete(getOwned(id, user));
    }
}
