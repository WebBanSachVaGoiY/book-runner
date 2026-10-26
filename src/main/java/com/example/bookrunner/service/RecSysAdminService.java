package com.example.bookrunner.service;

import java.util.Map;

/**
 * Nghiệp vụ quản trị cho RecSys: trigger training và làm mới cache gợi ý.
 */
public interface RecSysAdminService {

    /**
     * Trigger training trên RecSys Microservice rồi xóa cache gợi ý.
     *
     * @param type loại training (content-based | collaborative | co-purchase | all)
     * @return payload JSON trả về từ FastAPI
     */
    Map<String, Object> train(String type);

    /**
     * Đo lường các chỉ số chất lượng mô hình RecSys (Precision@K, Recall@K, NDCG@K, Coverage).
     */
    Map<String, Object> evaluate(int topK);
}
