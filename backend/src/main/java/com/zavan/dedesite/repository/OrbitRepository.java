package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.User;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrbitRepository extends JpaRepository<Orbit, Long> {
    List<Orbit> findByUserOrderByDayOfWeekAscStartTimeAsc(User user);
    List<Orbit> findByUserAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(User user);
    List<Orbit> findByUserAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(User user, DayOfWeek dayOfWeek);
    Optional<Orbit> findByIdAndUser(Long id, User user);
}
