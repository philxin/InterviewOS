package com.philxin.interviewos.llm;

/**
 * LLM 能力抽象层，屏蔽具体模型厂商差异。
 */
public interface LLMService {

    /**
     * 基于知识点生成一道面试问题。
     *
     * @param knowledgeTitle 知识点标题
     * @param knowledgeContent 知识点内容
     * @return 生成的问题文本
     */
    String generateQuestion(String knowledgeTitle, String knowledgeContent);

    /**
     * 基于知识点和题目生成答题提示。
     *
     * @param knowledgeTitle 知识点标题
     * @param knowledgeContent 知识点内容
     * @param questionText 当前题目
     * @return 简短提示文本
     */
    String generateHint(String knowledgeTitle, String knowledgeContent, String questionText);

    /**
     * 评估用户回答并返回结构化评分结果。
     *
     * @param question 面试问题
     * @param userAnswer 用户回答
     * @return 评分结果 DTO
     */
    EvaluationResult evaluateAnswer(String question, String userAnswer);

    /**
     * 按 V2 反馈契约评估回答。
     *
     * @param request 反馈生成请求
     * @return 反馈结构结果
     */
    FeedbackGenerationResult evaluateAnswer(FeedbackGenerationRequest request);
}
