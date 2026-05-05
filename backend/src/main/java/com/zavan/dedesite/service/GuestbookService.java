package com.zavan.dedesite.service;

import com.zavan.dedesite.model.GuestbookEntry;
import com.zavan.dedesite.repository.GuestbookEntryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GuestbookService {

    private final GuestbookEntryRepository guestbookEntryRepository;

    public GuestbookService(GuestbookEntryRepository guestbookEntryRepository) {
        this.guestbookEntryRepository = guestbookEntryRepository;
    }

    public List<GuestbookEntry> getEntries() {
        return guestbookEntryRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<GuestbookEntry> getLatestEntry() {
        return guestbookEntryRepository.findFirstByOrderByCreatedAtDesc();
    }

    public void saveEntry(GuestbookEntry entry) {
        entry.setName(entry.getName().trim());
        entry.setMessage(entry.getMessage().trim());
        guestbookEntryRepository.save(entry);
    }

    public void deleteEntryById(Long id) {
        guestbookEntryRepository.deleteById(id);
    }
}
