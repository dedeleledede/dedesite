package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Pulsar;
import com.zavan.dedesite.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PulsarRepository extends JpaRepository<Pulsar, Long> {
    List<Pulsar> findByUserOrderByActiveDescCreatedAtDesc(User user);
    List<Pulsar> findByUserAndActiveTrueOrderByCreatedAtDesc(User user);
    Optional<Pulsar> findByIdAndUser(Long id, User user);
    Optional<Pulsar> findByPublicIdAndUser(UUID publicId, User user);
}
