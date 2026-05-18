package com.kpiso.api.modules.task.dto;

import com.kpiso.api.modules.task.RotationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "El título de la tarea es obligatorio")
    private String title;

    private String description;

    @NotNull(message = "Los puntos son obligatorios")
    @Min(value = 1, message = "La tarea debe valer al menos 1 punto")
    private Integer points;

    @NotNull(message = "La casa de destino es obligatoria")
    private UUID houseId;

    @NotNull(message = "El tipo de rotación es obligatorio")
    private RotationType rotationType;

    private UUID assignedToId; // Se usa si el tipo de rotación es FIXED

    private List<UUID> participantIds; // Compañeros que entran en la rueda

    private List<Integer> specificDays; // Ej: [2, 4] para Martes y Jueves

    @NotNull(message = "El número de instancias a proyectar es obligatorio")
    @Min(value = 1, message = "Debes proyectar al menos 1 tarea")
    private Integer occurrencesToProject; // El campo que le faltaba a tu servicio

    private LocalDateTime startDate;
}