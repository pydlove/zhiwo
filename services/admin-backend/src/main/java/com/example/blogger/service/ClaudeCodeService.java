package com.example.blogger.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class ClaudeCodeService {

    /**
     * 调用本地 Claude Code CLI (claude -p) 生成内容
     *
     * @param prompt 提示词
     * @return AI 生成的文本内容
     */
    public String callClaude(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt 不能为空");
        }

        // 检查 claude 命令是否可用
        checkClaudeInstalled();

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("claude", "-p");
            pb.redirectErrorStream(true);
            process = pb.start();

            // 写入 prompt 到 stdin
            try (OutputStream os = process.getOutputStream();
                 Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            }

            // 读取 stdout，设置超时 3 分钟
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                long startTime = System.currentTimeMillis();
                long timeoutMillis = 3 * 60 * 1000; // 3 分钟

                while (true) {
                    if (reader.ready()) {
                        int read = reader.read(buffer);
                        if (read == -1) break;
                        output.append(buffer, 0, read);
                    } else {
                        // 检查进程是否已结束
                        if (!process.isAlive()) {
                            // 读取剩余内容
                            int read;
                            while ((read = reader.read(buffer)) != -1) {
                                output.append(buffer, 0, read);
                            }
                            break;
                        }
                        // 检查超时
                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
                            process.destroyForcibly();
                            throw new RuntimeException("Claude Code 执行超时（超过 3 分钟），请检查网络或降低生成复杂度");
                        }
                        Thread.sleep(200);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Claude Code 执行失败，退出码: " + exitCode + "，输出: " + output.toString().trim());
            }

            String result = output.toString().trim();
            if (result.isEmpty()) {
                throw new RuntimeException("Claude Code 返回空内容");
            }
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Claude Code 调用被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("Claude Code IO 错误: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void checkClaudeInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "claude");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                throw new IllegalStateException("未检测到本地 Claude Code CLI。请先安装：npm install -g @anthropic-ai/claude-code，并运行 claude login 登录");
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("检查 Claude Code CLI 安装状态时出错: " + e.getMessage());
        }
    }
}
