package com.zavan.dedesite.controller;

import com.zavan.dedesite.dto.PostDTO;
import com.zavan.dedesite.model.Post;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.ImageUploadService;
import com.zavan.dedesite.service.PostService;
import com.zavan.dedesite.service.UserService;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static final Pattern FIRST_IMAGE = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageUploadService imageUploadService;

    @Value("${app.public-url:http://localhost:6969}")
    private String publicUrl;

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
        model.addAttribute("siteTitle", post.getTitle() + " - dedesite");
        model.addAttribute("siteDescription", summarizePost(post));
        firstImage(post).ifPresent(image -> model.addAttribute("siteImage", absoluteUrl(image)));
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

    private String summarizePost(Post post) {
        String html = post.getContentHtml() == null ? post.getContent() : post.getContentHtml();
        String text = html == null ? "" : html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (text.isBlank()) {
            return "A dedesite blog post.";
        }
        return text.length() > 180 ? text.substring(0, 177) + "..." : text;
    }

    private java.util.Optional<String> firstImage(Post post) {
        if (post.getContentHtml() == null) {
            return java.util.Optional.empty();
        }
        Matcher matcher = FIRST_IMAGE.matcher(post.getContentHtml());
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(matcher.group(1));
    }

    private String absoluteUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            return url;
        }
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        String path = url.startsWith("/") ? url : "/" + url;
        return base + path;
    }
}
