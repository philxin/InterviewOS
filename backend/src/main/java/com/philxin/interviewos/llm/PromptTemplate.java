package com.philxin.interviewos.llm;

/**
 * Prompt 模板工具，统一管理 LLM 输入格式。
 */
public final class PromptTemplate {

    private PromptTemplate() {
    }

    /**
     * 生成问题 Prompt。
     */
    public static String questionGenerationPrompt(String title, String content) {
        return String.join(
            "\n",
            "你是资深技术面试官。请基于下面知识点，生成 1 道高质量面试题。",
            "",
            "知识点标题：",
            safe(title),
            "",
            "知识点内容：",
            safe(content),
            "",
            "约束：",
            "1. 只返回问题文本，不要解释",
            "2. 问题应聚焦核心原理或实战场景",
            "3. 问题长度控制在 1-3 句"
        );
    }

    /**
     * 评估回答 Prompt，要求严格返回 JSON。
     */
    public static String answerEvaluationPrompt(String question, String answer) {
        return String.join(
            "\n",
            "你是技术面试评估专家。请评估候选人的回答质量，并返回 JSON。",
            "",
            "问题：",
            safe(question),
            "",
            "回答：",
            safe(answer),
            "",
            "输出要求：",
            "1. 仅返回 JSON，不要附加说明",
            "2. 所有分数范围 0-100（整数）",
            "3. JSON 字段必须完整：",
            "{",
            "  \"accuracy\": 0,",
            "  \"depth\": 0,",
            "  \"clarity\": 0,",
            "  \"overall\": 0,",
            "  \"strengths\": \"\",",
            "  \"weaknesses\": \"\",",
            "  \"suggestions\": [],",
            "  \"exampleAnswer\": \"\"",
            "}"
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
