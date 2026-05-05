package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Post;
import com.zavan.dedesite.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MarkdownService markdownService;

    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Post> getLatestPost() {
        return postRepository.findFirstByOrderByCreatedAtDesc();
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public void savePost(Post post) {
        String safeHtml = markdownService.sanitizeHtml(post.getContent());
        String plainText = markdownService.toPlainText(safeHtml);

        post.setContent(plainText.isBlank() ? " " : plainText);
        post.setContentHtml(safeHtml);
        postRepository.save(post);
    }

    public Page<Post> getPostsPaginated(int page, int size) {
        return postRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    public void deletePostById(Long id) {
        postRepository.deleteById(id);
    }
}
