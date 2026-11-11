package com.example.bookrunner.dto.request;

import com.example.bookrunner.enums.InteractionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionLogRequest {

    @NotNull(message = "Mã sách (bookId) không được để trống")
    private Long bookId;

    @NotNull(message = "Loại tương tác (interactionType) không được để trống")
    private InteractionType interactionType;
}
