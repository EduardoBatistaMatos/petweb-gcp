package br.com.petshop.repository;

import br.com.petshop.db.Database;
import br.com.petshop.model.PetFoto;
import br.com.petshop.web.json.Json;
import com.fasterxml.jackson.core.type.TypeReference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Fotos embutidas no JSONB da própria linha de pet. */
public final class PetFotoRepository {
    private static final TypeReference<List<PetFoto>> TIPO_FOTOS = new TypeReference<>() {};
    private final Database database;

    public PetFotoRepository(Database database) { this.database = database; }

    public List<PetFoto> listar(int petId) throws SQLException {
        try (Connection c = database.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT fotos, img_principal FROM pet WHERE id=?")) {
            s.setInt(1, petId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return List.of();
                return ordenar(aplicarPrincipal(lerFotos(r.getString("fotos")), r.getString("img_principal")));
            }
        }
    }

    public List<PetFoto> inserirLote(int petId, List<PetFoto> novas) throws SQLException {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try {
                PetEstado estado = bloquearPet(c, petId);
                if (estado == null) throw new SQLException("Pet não encontrado: " + petId);
                List<PetFoto> fotos = new ArrayList<>(aplicarPrincipal(estado.fotos(), estado.principal()));
                String principal = estado.principal();
                for (PetFoto foto : novas) {
                    boolean capa = principal == null && fotos.isEmpty();
                    PetFoto persistida = new PetFoto(UUID.randomUUID().toString(), petId, foto.url(), foto.dataFoto(), capa);
                    fotos.add(persistida);
                    if (capa) principal = persistida.url();
                }
                salvar(c, petId, fotos, principal);
                c.commit();
                return aplicarPrincipal(fotos, principal);
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException sql) throw sql;
                throw new SQLException("Falha ao persistir fotos do pet.", e);
            }
        }
    }

    public boolean definirPrincipal(int petId, String fotoId) throws SQLException {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try {
                PetEstado estado = bloquearPet(c, petId);
                if (estado == null) { c.rollback(); return false; }
                List<PetFoto> fotos = aplicarPrincipal(estado.fotos(), estado.principal());
                PetFoto selecionada = fotos.stream().filter(f -> f.id().equals(fotoId)).findFirst().orElse(null);
                if (selecionada == null) { c.rollback(); return false; }
                salvar(c, petId, aplicarPrincipal(fotos, selecionada.url()), selecionada.url());
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public Optional<PetFoto> buscar(int petId, String fotoId) throws SQLException {
        return listar(petId).stream().filter(f -> f.id().equals(fotoId)).findFirst();
    }

    public boolean excluir(int petId, String fotoId) throws SQLException {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try {
                PetEstado estado = bloquearPet(c, petId);
                if (estado == null) { c.rollback(); return false; }
                List<PetFoto> fotos = new ArrayList<>(aplicarPrincipal(estado.fotos(), estado.principal()));
                PetFoto removida = fotos.stream().filter(f -> f.id().equals(fotoId)).findFirst().orElse(null);
                if (removida == null) { c.rollback(); return false; }
                fotos.remove(removida);
                String principal = estado.principal();
                if (removida.url().equals(principal)) {
                    principal = fotos.stream().max(Comparator.comparing(PetFoto::dataFoto).thenComparing(PetFoto::id))
                            .map(PetFoto::url).orElse(null);
                }
                salvar(c, petId, aplicarPrincipal(fotos, principal), principal);
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private PetEstado bloquearPet(Connection c, int petId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT fotos, img_principal FROM pet WHERE id=? FOR UPDATE")) {
            s.setInt(1, petId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? new PetEstado(lerFotos(r.getString("fotos")), r.getString("img_principal")) : null;
            }
        }
    }

    private void salvar(Connection c, int petId, List<PetFoto> fotos, String principal) throws SQLException {
        String json;
        try { json = Json.MAPPER.writeValueAsString(fotos); }
        catch (Exception e) { throw new SQLException("Não foi possível serializar as fotos do pet.", e); }
        try (PreparedStatement s = c.prepareStatement("UPDATE pet SET fotos=?::jsonb, img_principal=? WHERE id=?")) {
            s.setString(1, json); s.setString(2, principal); s.setInt(3, petId);
            if (s.executeUpdate() != 1) throw new SQLException("Pet não encontrado: " + petId);
        }
    }

    private static List<PetFoto> lerFotos(String json) throws SQLException {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return Json.MAPPER.readValue(json, TIPO_FOTOS); }
        catch (Exception e) { throw new SQLException("A coluna pet.fotos contém JSON inválido.", e); }
    }

    private static List<PetFoto> aplicarPrincipal(List<PetFoto> fotos, String urlPrincipal) {
        return fotos.stream().map(f -> new PetFoto(f.id(), f.petId(), f.url(), f.dataFoto(), f.url().equals(urlPrincipal))).toList();
    }

    private static List<PetFoto> ordenar(List<PetFoto> fotos) {
        return fotos.stream().sorted(Comparator.comparing(PetFoto::dataFoto).reversed()
                .thenComparing(PetFoto::id, Comparator.reverseOrder())).toList();
    }

    private record PetEstado(List<PetFoto> fotos, String principal) {}
}
