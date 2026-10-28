package br.com.petshop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dados recebidos nas operações de criação e atualização.
 * O identificador é sempre definido pelo banco de dados.
 */
public record PetRequest(
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
