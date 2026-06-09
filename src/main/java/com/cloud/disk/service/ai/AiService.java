package com.cloud.disk.service.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.ZipFile;

/**
 * AI service — calls Tongyi Qianwen API for document summarization.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String API_KEY = "sk-15431b4b0d4948f6a7f08ab002e3ec7f";
    private static final int MAX_CHARS = 5000;

    /**
     * Generate a summary for the file at the given path.
     * @param filePath disk path to the file
     * @param fileType file extension
     * @return markdown summary
     */
    public String summarize(String filePath, String fileType) throws Exception {
        String text = extractText(filePath, fileType);
        if (text == null || text.trim().isEmpty()) {
            return "无法提取文件内容，请确认文件格式正确。";
        }

        String prompt = buildPrompt(text, fileType);
        return callApi(prompt);
    }

    /**
     * Extract plain text from file based on its type.
     */
    private String extractText(String filePath, String fileType) throws Exception {
        String ext = fileType != null ? fileType.toLowerCase() : "";
        File file = new File(filePath);
        if (!file.exists()) return null;

        return switch (ext) {
            case "txt", "md", "json", "xml", "csv", "log", "yaml", "yml", "html", "htm",
                 "java", "py", "js", "ts", "css", "sql", "sh", "bat" -> readTextFile(file);
            case "pdf" -> extractPdfText(file);
            case "docx" -> extractDocxText(file);
            default -> readTextFile(file); // try as text
        };
    }

    private String readTextFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (content.length() > MAX_CHARS * 2) {
            content = content.substring(0, MAX_CHARS * 2);
        }
        return content;
    }

    private String extractPdfText(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            if (text.length() > MAX_CHARS * 2) {
                text = text.substring(0, MAX_CHARS * 2);
            }
            return text;
        }
    }

    private String extractDocxText(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            var entry = zip.getEntry("word/document.xml");
            if (entry == null) return null;
            try (InputStream is = zip.getInputStream(entry)) {
                String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                // Strip XML tags
                String text = xml.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                if (text.length() > MAX_CHARS * 2) {
                    text = text.substring(0, MAX_CHARS * 2);
                }
                return text;
            }
        }
    }

    /**
     * Build the prompt for the AI.
     */
    private String buildPrompt(String content, String fileType) {
        // Truncate to max chars
        String truncated = content.length() > MAX_CHARS ? content.substring(0, MAX_CHARS) : content;
        String hint = content.length() > MAX_CHARS ? "\n（注：原文过长，已截取前 " + MAX_CHARS + " 字）" : "";

        return String.format("""
                请对以下文档内容进行总结，输出格式为 Markdown，包含：
                1. **文档概述**：一段话概括文档主要内容
                2. **关键要点**：3-5 个要点，用列表格式
                
                文档内容：
                %s
                %s
                """, truncated, hint);
    }

    /**
     * Call Tongyi Qianwen API.
     */
    private String callApi(String prompt) throws Exception {
        String body = String.format("""
                {
                  "model": "qwen-turbo",
                  "messages": [
                    {"role": "system", "content": "You are a professional document analyst. Summarize key points in Chinese."},
                    {"role": "user", "content": "%s"}
                  ],
                  "max_tokens": 1000
                }
                """, escapeJson(prompt));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("AI API status: {}, body length: {}", response.statusCode(), response.body().length());

        if (response.statusCode() != 200) {
            log.error("AI API error body: {}", response.body());
            throw new RuntimeException("AI API error: " + response.statusCode());
        }

        // Parse OpenAI-compatible response
        String respBody = response.body();
        int contentIdx = respBody.indexOf("\"content\":\"");
        if (contentIdx < 0) {
            log.error("Response missing content: {}", respBody);
            throw new RuntimeException("AI response format error");
        }
        int start = respBody.indexOf('"', contentIdx + 10) + 1;
        int end = respBody.indexOf('"', start);
        String text = respBody.substring(start, end)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        return text;
    }

    /**
     * Describe an image using multimodal AI (qwen-vl-plus).
     * @return AI-generated description/OCR text
     */
    public String describeImage(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) return "文件不存在";

        byte[] bytes = Files.readAllBytes(file.toPath());
        String b64 = Base64.getEncoder().encodeToString(bytes);
        String ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        String mime = switch (ext) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg";
        };

        String body = String.format("""
                {
                  "model": "qwen-vl-plus",
                  "messages": [
                    {"role": "user", "content": [
                      {"type": "text", "text": "请描述这张图片的内容。如果图片中有文字，请提取所有文字。"},
                      {"type": "image_url", "image_url": {"url": "data:%s;base64,%s"}}
                    ]}
                  ],
                  "max_tokens": 800
                }
                """, mime, b64);

        return callVisionApi(body);
    }

    /**
     * Save an AI note linked to a file.
     */
    public void saveNote(String originalPath, String content) throws IOException {
        String notePath = getNotePath(originalPath);
        Files.writeString(Path.of(notePath), content, StandardCharsets.UTF_8);
    }

    /**
     * Get an AI note for a file.
     * @return note content or null
     */
    public String getNote(String originalPath) {
        String notePath = getNotePath(originalPath);
        File f = new File(notePath);
        if (!f.exists()) return null;
        try {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String getNotePath(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return dot > 0 ? filePath.substring(0, dot) + ".note.md" : filePath + ".note.md";
    }

    /**
     * Analyze video/audio file — transcribe speech then summarize.
     * For video files, attempts to extract audio via ffmpeg first.
     * @return markdown summary with transcript
     */
    public String analyzeMedia(String filePath, String fileType) throws Exception {
        String ext = fileType != null ? fileType.toLowerCase() : "";
        File file = new File(filePath);
        if (!file.exists()) return "文件不存在";

        // Determine if it's video or audio
        boolean isVideo = List.of("mp4", "webm", "mov", "avi", "mkv", "flv", "wmv").contains(ext);
        boolean isAudio = List.of("mp3", "wav", "ogg", "oga", "flac", "aac", "m4a", "wma").contains(ext);

        if (!isVideo && !isAudio) return "不支持的文件格式，请上传音频或视频文件。";

        File audioFile = file;
        boolean tempAudio = false;

        // Extract audio from video using ffmpeg
        if (isVideo) {
            try {
                audioFile = extractAudio(file);
                tempAudio = true;
            } catch (Exception e) {
                return "视频音频提取失败。请用 ffmpeg 手动提取：ffmpeg -i video.mp4 -vn audio.wav";
            }
        }

        // Limit to 10MB for base64
        if (audioFile.length() > 10 * 1024 * 1024) {
            if (tempAudio) audioFile.delete();
            return "音频文件过大（超过10MB），暂不支持分析。";
        }

        byte[] bytes = Files.readAllBytes(audioFile.toPath());
        String b64 = Base64.getEncoder().encodeToString(bytes);
        String mime = "audio/" + (ext.equals("mp3") ? "mpeg" : ext.equals("wav") ? "wav" : "x-wav");

        String body = String.format("""
                {
                  "model": "qwen-audio-turbo",
                  "messages": [
                    {"role": "user", "content": [
                      {"type": "text", "text": "请转录音频内容，然后用中文总结关键要点和主要内容。输出格式：先输出转录文字，然后输出要点总结。"},
                      {"type": "audio_url", "audio_url": {"url": "data:%s;base64,%s"}}
                    ]}
                  ],
                  "max_tokens": 1500
                }
                """, mime, b64);

        String result = callVisionApi(body);

        if (tempAudio) audioFile.delete();
        return result;
    }

    /**
     * Extract audio from video using ffmpeg.
     */
    private File extractAudio(File videoFile) throws Exception {
        File audioFile = File.createTempFile("extracted_", ".wav");
        audioFile.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoFile.getAbsolutePath(),
                "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                "-y", audioFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            audioFile.delete();
            throw new RuntimeException("ffmpeg exit code: " + exitCode);
        }
        return audioFile;
    }

    private String callVisionApi(String body) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Vision API status: {}", response.statusCode());

        if (response.statusCode() != 200) {
            log.error("Vision API error: {}", response.body());
            throw new RuntimeException("AI image API error: " + response.statusCode());
        }

        String respBody = response.body();
        int contentIdx = respBody.indexOf("\"content\":\"");
        if (contentIdx < 0) {
            // Content might be in array format
            int textIdx = respBody.indexOf("\"text\":\"");
            if (textIdx < 0) throw new RuntimeException("AI vision response format error");
            int start = respBody.indexOf('"', textIdx + 7) + 1;
            int end = respBody.indexOf('"', start);
            return respBody.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
        }
        int start = respBody.indexOf('"', contentIdx + 10) + 1;
        int end = respBody.indexOf('"', start);
        return respBody.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
