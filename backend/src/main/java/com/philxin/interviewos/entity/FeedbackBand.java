package com.philxin.interviewos.entity;

/**
 * 反馈档位，由服务端根据分数确定。
 */
public enum FeedbackBand {
    UNCLEAR("表达不清晰", "答案结构混乱、难以判断理解程度。"),
    INCOMPLETE("回答不完整", "方向基本正确，但缺少关键细节。"),
    BASIC("基础尚可", "方向正确，但深度不足。"),
    GOOD("回答较完整", "结构较清晰，关键点基本覆盖。"),
    STRONG("回答扎实", "内容完整、表达自然、深度较好。");

    private final String label;
    private final String description;

    FeedbackBand(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static FeedbackBand fromScore(int score) {
        if (score <= 29) {
            return UNCLEAR;
        }
        if (score <= 49) {
            return INCOMPLETE;
        }
        if (score <= 69) {
            return BASIC;
        }
        if (score <= 84) {
            return GOOD;
        }
        return STRONG;
    }
}
