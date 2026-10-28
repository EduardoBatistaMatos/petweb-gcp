package br.com.petshop.model;

import java.time.LocalDate;

public record PetFoto(String id, int petId, String url, LocalDate dataFoto, boolean imgPrincipal) {}
