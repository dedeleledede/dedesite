package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StarSystemRepository extends JpaRepository<StarSystem, Long> {
    List<StarSystem> findByUserOrderByStatusAscPriorityDescCreatedAtDesc(User user);
    List<StarSystem> findByUserAndStatusNotOrderByPriorityDescCreatedAtDesc(User user, StarSystem.Status status);
    Optional<StarSystem> findByIdAndUser(Long id, User user);
    Optional<StarSystem> findByPublicIdAndUser(UUID publicId, User user);
}
