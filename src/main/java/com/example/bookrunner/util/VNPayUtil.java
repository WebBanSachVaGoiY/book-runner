package com.example.bookrunner.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class VNPayUtil {

    private VNPayUtil() {
        // Utility class
    }

    /**
     * Băm chuỗi dữ liệu theo thuật toán HmacSHA512 với khóa bí mật secretKey.
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Khóa hoặc dữ liệu băm không được null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Lỗi khi tính toán HMAC-SHA512: ", ex);
            return "";
        }
    }

    /**
     * Trích xuất địa chỉ IP thực của khách hàng từ HttpServletRequest.
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            // Trường hợp client qua nhiều proxy, lấy IP đầu tiên
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "localhost".equalsIgnoreCase(ipAddress)) {
                ipAddress = "127.0.0.1";
            }
        } catch (Exception e) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    /**
     * Sắp xếp và tạo chuỗi hashData cùng queryString theo định dạng VNPay 2.1.0.
     * Trả về mảng 2 phần tử: [hashData, queryString]
     */
    public static String[] buildHashAndQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                try {
                    String encodedFieldName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString());
                    String encodedFieldValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());

                    // Xây dựng hashData và query
                    hashData.append(fieldName).append('=').append(encodedFieldValue).append('&');
                    query.append(encodedFieldName).append('=').append(encodedFieldValue).append('&');
                } catch (Exception e) {
                    log.error("Lỗi URL encode tham số VNPay: fieldName={}, fieldValue={}", fieldName, fieldValue, e);
                }
            }
        }

        // Loại bỏ dấu & cuối cùng nếu có
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        if (query.length() > 0) {
            query.setLength(query.length() - 1);
        }

        return new String[]{hashData.toString(), query.toString()};
    }

    /**
     * Xác thực chữ ký số HMAC-SHA512 từ các tham số phản hồi của VNPay.
     */
    public static boolean verifySignature(Map<String, String> fields, String secretKey) {
        if (fields == null || !fields.containsKey("vnp_SecureHash") || secretKey == null) {
            return false;
        }
        String vnpSecureHash = fields.get("vnp_SecureHash");

        Map<String, String> fieldsCopy = new HashMap<>(fields);
        fieldsCopy.remove("vnp_SecureHash");
        fieldsCopy.remove("vnp_SecureHashType");

        String[] hashAndQuery = buildHashAndQuery(fieldsCopy);
        String calculatedHash = hmacSHA512(secretKey, hashAndQuery[0]);

        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }
}
