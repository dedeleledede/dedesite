package com.zavan.dedesite.controller;

import com.zavan.dedesite.dto.PostDTO;
import com.zavan.dedesite.model.Post;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.ImageUploadService;
import com.zavan.dedesite.service.PostService;
import com.zavan.dedesite.service.UserService;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageUploadService imageUploadService;

    @GetMapping
    public String showBlog(@RequestParam(defaultValue = "0") int page, Model model) {
        int size = 5;
        Page<Post> postPage = postService.getPostsPaginated(page, size);

        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("hasNext", postPage.hasNext());
        return "blog";
    }

    @GetMapping("/{id}")
    public String showPost(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("post", post);
        return "post";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String showNewPostForm(Model model) {
        model.addAttribute("postDto", new PostDTO());
        return "new_post";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String savePost(@ModelAttribute PostDTO postDto,
                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        Post post = new Post();
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setAuthor(user);
        postService.savePost(post);
        return "redirect:/blog";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletePost(@PathVariable Long id) {
        postService.deletePostById(id);
        return "redirect:/blog";
    }

    @PostMapping("/uploads/image")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> uploadPostImage(@RequestParam("file") MultipartFile file) throws IOException {
        return imageUploadService.uploadBlogImage(file);
    }

    @PostMapping("/uploads/image-data")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> uploadPostImageData(@org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) throws IOException {
        return imageUploadService.uploadBlogImageData(payload.get("dataUrl"), payload.get("filename"));
    }
}
