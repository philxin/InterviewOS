package com.philxin.interviewos.service;

import java.util.List;

/**
 * Embedding 抽象，统一隔离向量模型接入细节。
 */
public interface EmbeddingService {

    float[] embed(String text);

    List<float[]> embed(List<String> texts);

    String getModel();

    int getDimensions();
}
