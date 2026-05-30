package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Constellation;
import com.zavan.dedesite.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstellationRepository extends JpaRepository<Constellation, Long> {
    List<Constellation> findByUserOrderByStatusAscPriorityDescNameAsc(User user);
    List<Constellation> findByUserAndStatusNotOrderByPriorityDescNameAsc(User user, Constellation.Status status);
    Optional<Constellation> findByIdAndUser(Long id, User user);
}
