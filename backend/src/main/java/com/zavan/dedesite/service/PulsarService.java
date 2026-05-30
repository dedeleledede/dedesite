package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Pulsar;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.PulsarRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PulsarService {
    private final PulsarRepository pulsarRepository;
    private final CometService cometService;

    public PulsarService(PulsarRepository pulsarRepository, CometService cometService) {
        this.pulsarRepository = pulsarRepository;
        this.cometService = cometService;
    }

    public List<Pulsar> findAll(User user) {
        return pulsarRepository.findByUserOrderByActiveDescCreatedAtDesc(user);
    }

    public List<Pulsar> findActive(User user) {
        return pulsarRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(user);
    }

    public Pulsar getOwned(Long id, User user) {
        return pulsarRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Pulsar save(Pulsar pulsar, Long relatedExamId, User user) {
        pulsar.setUser(user);
        pulsar.setRelatedExam(resolveExam(relatedExamId, user));
        return pulsarRepository.save(pulsar);
    }

    public void update(Long id, Pulsar form, Long relatedExamId, User user) {
        Pulsar pulsar = getOwned(id, user);
        pulsar.setTitle(form.getTitle());
        pulsar.setSubject(form.getSubject());
        pulsar.setFrequency(form.getFrequency());
        pulsar.setTargetMinutesPerWeek(form.getTargetMinutesPerWeek());
        pulsar.setRelatedExam(resolveExam(relatedExamId, user));
        pulsar.setActive(form.isActive());
        pulsarRepository.save(pulsar);
    }

    public void toggle(Long id, User user) {
        Pulsar pulsar = getOwned(id, user);
        pulsar.setActive(!pulsar.isActive());
        pulsarRepository.save(pulsar);
    }

    public void delete(Long id, User user) {
        pulsarRepository.delete(getOwned(id, user));
    }

    private Comet resolveExam(Long relatedExamId, User user) {
        if (relatedExamId == null) {
            return null;
        }
        Comet comet = cometService.getOwned(relatedExamId, user);
        return comet.getType() == Comet.Type.EXAM ? comet : null;
    }
}
