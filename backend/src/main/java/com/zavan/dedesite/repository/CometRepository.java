package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CometRepository extends JpaRepository<Comet, Long> {
    List<Comet> findByUserOrderByDateAscStartTimeAsc(User user);
    List<Comet> findByUserAndDateOrderByStartTimeAsc(User user, LocalDate date);
    List<Comet> findByUserAndDateBetweenOrderByDateAscStartTimeAsc(User user, LocalDate start, LocalDate end);
    List<Comet> findByUserAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(User user, LocalDate date);
    List<Comet> findByUserAndRelatedStarSystemOrderByDateAscStartTimeAsc(User user, StarSystem starSystem);
    List<Comet> findByUserAndRemindAtLessThanEqualOrderByRemindAtAsc(User user, LocalDateTime now);
    Optional<Comet> findByIdAndUser(Long id, User user);
    Optional<Comet> findByPublicIdAndUser(UUID publicId, User user);
}
