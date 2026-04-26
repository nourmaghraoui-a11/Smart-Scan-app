package com.example.myapplication.Data;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AiRepository {

    // ===== GROK / OPENAI COMPATIBLE REQUEST =====
    public static class GrokRequest {
        @SerializedName("model")
        public String model;
        @SerializedName("messages")
        public List<Message> messages;
        @SerializedName("temperature")
        public double temperature;

        public GrokRequest(String model, List<Message> messages, double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
        }

        public static class Message {
            @SerializedName("role")
            public String role;
            @SerializedName("content")
            public String content;

            public Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }

    // ===== GROK / OPENAI COMPATIBLE RESPONSE =====
    public static class GrokResponse {
        @SerializedName("choices")
        public List<Choice> choices;

        public static class Choice {
            @SerializedName("message")
            public Message message;
        }

        public static class Message {
            @SerializedName("content")
            public String content;
        }
    }
}
