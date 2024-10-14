package pe.edu.cibertec.patitas_frontend_wc.dto;

import java.time.LocalDateTime;

public record LogoutResponseDTO(boolean resultado, LocalDateTime fecha, String mensajeError) {
}
