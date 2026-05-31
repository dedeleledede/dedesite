package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StarRepository extends JpaRepository<Star, Long> {
    List<Star> findByUserOrderByCreatedAtDesc(User user);
    List<Star> findByUserAndStatusOrderByCreatedAtDesc(User user, Star.Status status);
    List<Star> findByUserAndStatusNotOrderByDueDateAscCreatedAtDesc(User user, Star.Status status);
    List<Star> findByUserAndStarSystemOrderByStatusAscDueDateAsc(User user, StarSystem starSystem);
    List<Star> findByUserAndScheduledStartBetweenOrderByScheduledStartAsc(User user, LocalDateTime start, LocalDateTime end);
    List<ScheduledBlock> findAllProjectedByUserAndScheduledStartBetweenOrderByScheduledStartAsc(User user, LocalDateTime start, LocalDateTime end);
    List<Star> findByUserAndScheduledStartIsNullAndScheduledEndIsNullAndStatusNotOrderByDueDateAscCreatedAtAsc(User user, Star.Status status);
    List<Star> findByUserAndDueDateBetweenAndStatusNotOrderByDueDateAsc(User user, LocalDate start, LocalDate end, Star.Status status);
    List<Star> findByUserAndDueDateBeforeAndStatusNotOrderByDueDateAsc(User user, LocalDate date, Star.Status status);
    long countByUserAndStarSystem(User user, StarSystem starSystem);
    long countByUserAndStarSystemAndStatusNot(User user, StarSystem starSystem, Star.Status status);
    Optional<Star> findFirstByUserAndStarSystemAndStatusNotOrderByDueDateAscCreatedAtAsc(User user, StarSystem starSystem, Star.Status status);
    Optional<Star> findByIdAndUser(Long id, User user);
    Optional<Star> findByPublicIdAndUser(UUID publicId, User user);

    interface ScheduledBlock {
        LocalDateTime getScheduledStart();
        LocalDateTime getScheduledEnd();
    }
}
