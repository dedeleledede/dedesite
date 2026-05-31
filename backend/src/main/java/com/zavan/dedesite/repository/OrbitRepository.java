package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.model.StarSystem;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrbitRepository extends JpaRepository<Orbit, Long> {
    List<Orbit> findByUserOrderByDayOfWeekAscStartTimeAsc(User user);
    List<Orbit> findByUserAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(User user);
    List<Orbit> findByUserAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(User user, DayOfWeek dayOfWeek);
    List<Orbit> findByUserAndKindOrderByActiveDescCreatedAtDesc(User user, Orbit.Kind kind);
    List<Orbit> findByUserAndKindAndActiveTrueOrderByCreatedAtDesc(User user, Orbit.Kind kind);
    List<Orbit> findByUserAndStarSystem(User user, StarSystem starSystem);
    List<TimedOrbit> findAllProjectedByUserAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(User user, DayOfWeek dayOfWeek);
    Optional<Orbit> findByIdAndUser(Long id, User user);
    Optional<Orbit> findByPublicIdAndUser(UUID publicId, User user);

    interface TimedOrbit {
        LocalTime getStartTime();
        LocalTime getEndTime();
        Orbit.Kind getKind();
        Orbit.Flexibility getFlexibility();

        default boolean isFixedBlock() {
            return (getKind() == null || getKind() != Orbit.Kind.PULSAR)
                    && (getFlexibility() == null || getFlexibility() == Orbit.Flexibility.FIXED);
        }
    }
}
