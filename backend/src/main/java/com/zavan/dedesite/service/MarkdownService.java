package com.zavan.dedesite.service;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.List;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {

    private static final MutableDataSet OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, List.of(
                    AutolinkExtension.create(),
                    StrikethroughExtension.create(),
                    TablesExtension.create()
            ));

    private final Parser parser = Parser.builder(OPTIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("a", "p", "h1", "h2", "h3", "h4", "h5", "h6",
                    "ul", "ol", "li", "strong", "b", "em", "i", "u", "s", "del",
                    "blockquote", "pre", "code", "img", "hr", "br", "span", "div",
                    "table", "thead", "tbody", "tr", "th", "td")
            .allowUrlProtocols("http", "https", "data")
            .allowAttributes("href", "target").onElements("a")
            .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
            .allowAttributes("class", "id").onElements("span", "div", "pre", "code",
                    "table", "thead", "tbody", "tr", "th", "td", "p", "blockquote")
            .requireRelNofollowOnLinks()
            .toFactory();

    public String toSafeHtml(String markdown) {
        String md = markdown == null ? "" : markdown;
        Node doc = parser.parse(md);
        String rawHtml = renderer.render(doc);
        return sanitizeHtml(rawHtml);
    }

    public String sanitizeHtml(String html) {
        return POLICY.sanitize(html == null ? "" : html);
    }

    public String toPlainText(String html) {
        return sanitizeHtml(html)
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
