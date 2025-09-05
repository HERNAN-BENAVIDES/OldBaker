package co.edu.uniquindio.oldbaker.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;


@Data
@RequiredArgsConstructor
@Table(name = "verification_codes")
@Builder
@Entity
@AllArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verification_code_seq")
    @SequenceGenerator(name = "verification_code_seq", sequenceName = "VERIFICATION_CODE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "code", nullable = false)
    @NotBlank(message = "El código es obligatorio")
    private String code;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long userId;

}
