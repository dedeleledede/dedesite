package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.GuestbookEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestbookEntryRepository extends JpaRepository<GuestbookEntry, Long> {
    List<GuestbookEntry> findAllByOrderByCreatedAtDesc();

    Optional<GuestbookEntry> findFirstByOrderByCreatedAtDesc();
}
