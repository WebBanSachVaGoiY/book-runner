package com.example.bookrunner.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

public class ListToPageUtil {
    public static <T> Page<T> convertListToPage(List<T> list, Pageable pageable) {
        // 1. Tính toán vị trí bắt đầu (Offset)
        int start = (int) pageable.getOffset();

        // Nếu vị trí bắt đầu vượt quá độ dài list, trả về trang trống
        if (start > list.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        }

        // 2. Tính toán vị trí kết thúc
        int end = Math.min((start + pageable.getPageSize()), list.size());

        // 3. Cắt subList tương ứng với trang hiện tại
        List<T> subList = list.subList(start, end);

        // 4. Trả về đối tượng PageImpl (Dữ liệu trang hiện tại, thông tin phân trang, tổng số bản ghi)
        return new PageImpl<>(subList, pageable, list.size());
    }
}
