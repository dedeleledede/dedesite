package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.VisitorIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorIdentityRepository extends JpaRepository<VisitorIdentity, Long> {

    Optional<VisitorIdentity> findByVisitorToken(String visitorToken);

    List<VisitorIdentity> findAllByLastSeenAtAfterOrderByIdAsc(Instant activeSince);
}
