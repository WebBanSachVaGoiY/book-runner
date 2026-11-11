package com.example.bookrunner.service;

/**
 * Service quản lý và vô hiệu hóa (eviction) bộ đệm cho tầng Gợi ý (RecSys).
 * Đảm bảo tính nhất quán dữ liệu giữa CSDL và Redis/RAM Cache khi sách,
 * đơn hàng hoặc đánh giá thay đổi.
 */
public interface RecSysCacheService {

    /**
     * Xóa cache danh sách sách thịnh hành / bán chạy (recsys:trending).
     * Gọi khi: tạo đơn hàng, hủy đơn, hoàn kho, hoặc có đánh giá mới.
     */
    void evictTrending();

    /**
     * Xóa cache sách tương tự (recsys:related) và thường mua cùng (recsys:fbt) của một sách.
     */
    void evictRelatedAndFbt(Long bookId);

    /**
     * Xóa toàn diện khi thông tin sách (giá, tiêu đề, ảnh, mô tả, trạng thái nổi bật) thay đổi.
     */
    void evictAllOnBookChange(Long bookId);

    /**
     * Xóa sạch toàn bộ cache gợi ý khi có sách bị xóa mềm (active = false).
     * Đảm bảo sách bị xóa không còn xuất hiện trong bất kỳ danh sách FOR_YOU, RELATED, FBT hay TRENDING nào.
     */
    void evictAllOnBookDelete();

    /**
     * Xóa sạch 100% tất cả các cache RecSys (FOR_YOU, RELATED, FBT, TRENDING).
     * Gọi khi vừa hoàn tất tái huấn luyện mô hình máy học.
     */
    void clearAllRecSysCaches();
}
