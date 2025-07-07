package com.aisearch.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextSimilarity {

    private static EmbeddingModel embeddingModel = new BgeSmallZhV15EmbeddingModel();

    public static void main(String[] args) {
        String text1 = "东汉末年及三国时期的政治家、军事家、文学家，魏朝的奠基人";
        String text2 = "三国时期魏国的奠基人，著名的政治家、军事家";
        System.out.println(getSimilarity(text1, text2));

        text1 = "三国时期魏国的奠基人，著名的政治家、军事家，魏朝的奠基人";
        text2 = "三国时期魏国的奠基人，著名的政治家、军事家";
        System.out.println(getSimilarity(text1, text2));
    }

    public static double getSimilarity(String text1, String text2) {

        float[] l = embed(text1);
        float[] r = embed(text2);
        if (l.length != r.length) {
            return 0.0f;
        }
        return CosineSimilarity.cosineSimilarity(l, r);
    }

    private static Cache<String, float[]> vectorCache
            = CacheBuilder.newBuilder().maximumSize(4096).weakKeys().build();


    private static float[] embed(String text) {
        float[] vector = vectorCache.getIfPresent(text);
        if (vector == null) {
            Embedding embedding = embeddingModel.embed(text).content();
            vector = embedding.vector();
            vectorCache.put(text, vector);
        }
        return vector;
    }

}
