package com.zavan.dedesite.repository;

import com.zavan.dedesite.model.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Post> findFirstByOrderByCreatedAtDesc();
}
