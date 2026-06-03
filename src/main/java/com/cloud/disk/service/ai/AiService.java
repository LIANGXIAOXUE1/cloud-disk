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
import java.util.zip.ZipFile;

/**
 * AI service — calls Tongyi Qianwen API for document summarization.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
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
                  "input": {
                    "messages": [
                      {"role": "system", "content": "你是一个专业的文档分析助手，擅长总结和提取关键信息。请用中文回复。"},
                      {"role": "user", "content": "%s"}
                    ]
                  },
                  "parameters": {
                    "max_tokens": 1000,
                    "temperature": 0.5
                  }
                }
                """, escapeJson(prompt));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("AI API error: {}", response.body());
            throw new RuntimeException("AI 服务返回错误: " + response.statusCode());
        }

        // Parse the response JSON to extract the text content
        String respBody = response.body();
        // Find "text": "..." in the nested JSON
        int textIdx = respBody.indexOf("\"text\":\"");
        if (textIdx < 0) {
            log.error("Unexpected API response: {}", respBody);
            throw new RuntimeException("AI 响应格式异常");
        }
        int start = respBody.indexOf('"', textIdx + 7) + 1;
        int end = respBody.indexOf('"', start);
        String text = respBody.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        return text;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
