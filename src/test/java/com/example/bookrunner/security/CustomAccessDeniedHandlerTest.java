package com.example.bookrunner.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("handle - Trả về HTTP 403 Forbidden với cấu trúc ApiResponse chuẩn")
    void handle_ShouldReturnForbiddenApiResponse() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getRequestURI()).thenReturn("/api/v1/admin/orders");
        when(response.getWriter()).thenReturn(printWriter);

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access is denied"));

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        String jsonOutput = stringWriter.toString();
        assertTrue(jsonOutput.contains("\"success\":false"));
        assertTrue(jsonOutput.contains("Bạn không có quyền thực hiện thao tác này."));
    }
}
