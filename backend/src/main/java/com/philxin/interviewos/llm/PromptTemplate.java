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
     * 生成答题提示 Prompt，只返回回答方向，不直接给完整答案。
     */
    public static String hintGenerationPrompt(String title, String content, String question) {
        return String.join(
            "\n",
            "你是技术面试辅导教练。请基于知识点和当前问题，生成 1 段简短提示。",
            "",
            "知识点标题：",
            safe(title),
            "",
            "知识点内容：",
            safe(content),
            "",
            "当前问题：",
            safe(question),
            "",
            "输出要求：",
            "1. 只返回提示正文，不要返回 JSON",
            "2. 提示只能给回答角度、结构或关键关键词，不能直接给完整标准答案",
            "3. 长度控制在 1-2 句",
            "4. 如果问题偏项目或场景，应提醒从结论、原理、条件和案例四个层次组织回答"
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

    /**
     * V2 反馈生成 Prompt，要求直接返回结果页需要的核心结构。
     */
    public static String feedbackGenerationPrompt(FeedbackGenerationRequest request) {
        return String.join(
            "\n",
            "你是技术面试反馈专家。请基于题目、知识点和候选人回答，输出可直接用于结果页展示的 JSON。",
            "",
            "知识点标题：",
            safe(request.getKnowledgeTitle()),
            "",
            "知识点内容：",
            safe(request.getKnowledgeContent()),
            "",
            "题目类型：",
            safe(request.getQuestionType()),
            "",
            "难度：",
            safe(request.getDifficulty()),
            "",
            "是否使用提示：",
            Boolean.TRUE.equals(request.getHintUsed()) ? "true" : "false",
            "",
            "问题：",
            safe(request.getQuestionText()),
            "",
            "回答：",
            safe(request.getUserAnswer()),
            "",
            "输出要求：",
            "1. 仅返回 JSON，不要附加说明",
            "2. score 必须是 0-100 的整数",
            "3. majorIssue 必须是一句简短、可读的主要问题总结",
            "4. missingPoints 和 betterAnswerApproach 必须是 1-3 条短句数组",
            "5. naturalExampleAnswer 必须是更自然、接近真实面试表达的参考回答",
            "6. 如果回答较弱，也要给出可执行的改进方向，不要输出空数组",
            "7. JSON 字段必须完整：",
            "{",
            "  \"score\": 0,",
            "  \"majorIssue\": \"\",",
            "  \"missingPoints\": [],",
            "  \"betterAnswerApproach\": [],",
            "  \"naturalExampleAnswer\": \"\"",
            "}"
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
