package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Constellation;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StarRepository extends JpaRepository<Star, Long> {
    List<Star> findByUserOrderByCreatedAtDesc(User user);
    List<Star> findByUserAndStatusOrderByCreatedAtDesc(User user, Star.Status status);
    List<Star> findByUserAndStatusNotOrderByDueDateAscCreatedAtDesc(User user, Star.Status status);
    List<Star> findByUserAndConstellationOrderByStatusAscDueDateAsc(User user, Constellation constellation);
    List<Star> findByUserAndScheduledStartBetweenOrderByScheduledStartAsc(User user, LocalDateTime start, LocalDateTime end);
    List<Star> findByUserAndDueDateBetweenAndStatusNotOrderByDueDateAsc(User user, LocalDate start, LocalDate end, Star.Status status);
    List<Star> findByUserAndDueDateBeforeAndStatusNotOrderByDueDateAsc(User user, LocalDate date, Star.Status status);
    long countByUserAndConstellationAndStatusNot(User user, Constellation constellation, Star.Status status);
    Optional<Star> findFirstByUserAndConstellationAndStatusNotOrderByDueDateAscCreatedAtAsc(User user, Constellation constellation, Star.Status status);
    Optional<Star> findByIdAndUser(Long id, User user);
}
