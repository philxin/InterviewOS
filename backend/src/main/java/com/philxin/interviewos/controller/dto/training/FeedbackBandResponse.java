package com.philxin.interviewos.controller.dto.training;

import com.philxin.interviewos.entity.FeedbackBand;

/**
 * 对外反馈档位结构。
 */
public class FeedbackBandResponse {
    private String code;
    private String label;
    private String description;

    public static FeedbackBandResponse fromBand(FeedbackBand band) {
        if (band == null) {
            return null;
        }
        FeedbackBandResponse response = new FeedbackBandResponse();
        response.setCode(band.name());
        response.setLabel(band.getLabel());
        response.setDescription(band.getDescription());
        return response;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
