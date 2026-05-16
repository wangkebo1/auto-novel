package com.rag.service;

import com.rag.entity.Chapter;
import com.rag.entity.Novel;
import com.rag.repository.ChapterRepository;
import com.rag.repository.NovelRepository;
import com.rag.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubExportService {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final SecurityUtils securityUtils;

    public byte[] exportNovelAsEpub(Long novelId) {
        Long userId = securityUtils.getCurrentUserId();
        Novel novel = novelRepository.findByIdAndUserId(novelId, userId)
                .orElseThrow(() -> new RuntimeException("小说不存在或无权访问"));

        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(baos)) {

            // 1. mimetype (必须第一个，无压缩)
            zip.setLevel(0);
            addZipEntry(zip, "mimetype", "application/epub+zip");

            zip.setLevel(9);

            // 2. META-INF/container.xml
            addZipEntry(zip, "META-INF/container.xml", buildContainer());

            // 3. OEBPS/content.opf
            addZipEntry(zip, "OEBPS/content.opf", buildContentOpf(novel, chapters));

            // 4. OEBPS/toc.ncx
            addZipEntry(zip, "OEBPS/toc.ncx", buildTocNcx(novel, chapters));

            // 5. 章节 HTML
            for (Chapter ch : chapters) {
                if (ch.getContent() != null && !ch.getContent().isBlank()) {
                    addZipEntry(zip, "OEBPS/chapter" + ch.getChapterNumber() + ".html",
                            buildChapterHtml(ch));
                }
            }

            zip.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成 EPUB 失败", e);
            throw new RuntimeException("生成 EPUB 失败: " + e.getMessage());
        }
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String buildContainer() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """;
    }

    private String buildContentOpf(Novel novel, List<Chapter> chapters) {
        StringBuilder manifest = new StringBuilder();
        StringBuilder spine = new StringBuilder();

        for (Chapter ch : chapters) {
            if (ch.getContent() != null && !ch.getContent().isBlank()) {
                String id = "chapter" + ch.getChapterNumber();
                manifest.append("    <item id=\"").append(id).append("\" href=\"").append(id)
                        .append(".html\" media-type=\"application/xhtml+xml\"/>\n");
                spine.append("    <itemref idref=\"").append(id).append("\"/>\n");
            }
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="BookId">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>%s</dc:title>
                    <dc:creator>AI Generated</dc:creator>
                    <dc:language>zh-CN</dc:language>
                    <dc:identifier id="BookId">%s</dc:identifier>
                  </metadata>
                  <manifest>
                %s  </manifest>
                  <spine>
                %s  </spine>
                </package>
                """.formatted(novel.getTitle(), "novel-" + novel.getId(), manifest, spine);
    }

    private String buildTocNcx(Novel novel, List<Chapter> chapters) {
        StringBuilder navPoints = new StringBuilder();
        int playOrder = 1;

        for (Chapter ch : chapters) {
            if (ch.getContent() != null && !ch.getContent().isBlank()) {
                navPoints.append("""
                      <navPoint id="chapter%d" playOrder="%d">
                        <navLabel><text>第%d章 %s</text></navLabel>
                        <content src="chapter%d.html"/>
                      </navPoint>
                    """.formatted(ch.getChapterNumber(), playOrder++, ch.getChapterNumber(),
                        ch.getTitle(), ch.getChapterNumber()));
            }
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                  <head>
                    <meta name="dtb:uid" content="novel-%s"/>
                  </head>
                  <docTitle><text>%s</text></docTitle>
                  <navMap>
                %s  </navMap>
                </ncx>
                """.formatted(novel.getId(), novel.getTitle(), navPoints);
    }

    private String buildChapterHtml(Chapter chapter) {
        String content = chapter.getContent().replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "</p>\n<p>");
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>%s</title></head>
                <body>
                <h2>第%d章 %s</h2>
                <p>%s</p>
                </body>
                </html>
                """.formatted(chapter.getTitle(), chapter.getChapterNumber(),
                        chapter.getTitle(), content);
    }
}
