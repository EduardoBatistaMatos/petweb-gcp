package br.com.petshop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representação de um pet já persistido, incluindo o identificador.
 */
public record Pet(
        int id,
        String nome,
        String especie,
        String raca,
        String porte,
        String responsavel,
        LocalDate dataNascimento,
        LocalDateTime dataInsercaoImg,
        String detalhes,
        String imagemUrl
) {
}
