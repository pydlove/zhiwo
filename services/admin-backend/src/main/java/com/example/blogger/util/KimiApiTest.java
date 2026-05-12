package com.example.blogger.util;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static dev.langchain4j.data.message.UserMessage.userMessage;

/**
 * Kimi API 测试工具
 * 用法: java com.example.blogger.util.KimiApiTest <baseUrl> <apiKey> [model]
 * 示例: java com.example.blogger.util.KimiApiTest https://api.kimi.com/coding sk-xxxxxx moonshot-v1-8k
 */
public class KimiApiTest {

    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.kimi.com/coding/v1") // 指向Moonshot API的地址
                .apiKey("sk-kimi-PAVWMaLdRmhoGfVFrytNS94cgV3vJbGva0iOPq6ScdYnjvMQGUyuFLxv1MmErBDs")                // 替换为你的API Key
                .modelName("kimi-k2.6")                // 指定要使用的具体模型
                .logRequests(true)                     // 可选：在控制台打印请求详情
                .logResponses(true)                    // 可选：在控制台打印响应详情
                .build();

        // 1. 简单的对话
        String answer = model.generate("你好，Kimi，请做一个简单的自我介绍。");
        System.out.println(answer);

        // 2. 使用 ChatRequest 对象的更完整的调用方式（推荐）
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage("请用一句话介绍你自己。"))
                .build();

        ChatResponse chatResponse = model.chat(chatRequest);
        System.out.println(chatResponse.aiMessage().text());

    }
}
