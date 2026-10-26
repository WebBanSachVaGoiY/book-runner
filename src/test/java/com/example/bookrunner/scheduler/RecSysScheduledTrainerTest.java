package com.example.bookrunner.scheduler;

import com.example.bookrunner.config.RecSysProperties;
import com.example.bookrunner.service.RecSysAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecSysScheduledTrainerTest {

    @Mock
    private RecSysAdminService recSysAdminService;

    @Mock
    private RecSysProperties recSysProperties;

    @InjectMocks
    private RecSysScheduledTrainer recSysScheduledTrainer;

    @Test
    @DisplayName("Chạy tự động huấn luyện thành công khi autoTrainingEnabled=true")
    void shouldRetrainSuccessfullyWhenEnabled() {
        when(recSysProperties.isAutoTrainingEnabled()).thenReturn(true);
        when(recSysProperties.getTrainingCron()).thenReturn("0 0 2 * * ?");
        when(recSysAdminService.train("all")).thenReturn(Map.of("status", "success"));

        recSysScheduledTrainer.scheduledRetrain();

        verify(recSysAdminService, times(1)).train("all");
    }

    @Test
    @DisplayName("Bỏ qua huấn luyện khi autoTrainingEnabled=false")
    void shouldSkipRetrainWhenDisabled() {
        when(recSysProperties.isAutoTrainingEnabled()).thenReturn(false);

        recSysScheduledTrainer.scheduledRetrain();

        verify(recSysAdminService, never()).train(anyString());
    }

    @Test
    @DisplayName("Bắt lỗi an toàn và không làm crash khi service FastAPI ném exception")
    void shouldCatchExceptionGracefullyWhenServiceFails() {
        when(recSysProperties.isAutoTrainingEnabled()).thenReturn(true);
        when(recSysProperties.getTrainingCron()).thenReturn("0 0 2 * * ?");
        when(recSysAdminService.train("all")).thenThrow(new RuntimeException("Connection refused"));

        // Không ném lỗi ra ngoài
        recSysScheduledTrainer.scheduledRetrain();

        verify(recSysAdminService, times(1)).train("all");
    }
}
