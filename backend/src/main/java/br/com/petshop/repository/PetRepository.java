package br.com.petshop.repository;

import br.com.petshop.db.Database;
import br.com.petshop.model.Pet;
import br.com.petshop.model.PetRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso à tabela {@code pet} usando apenas JDBC e PreparedStatement.
 */
public final class PetRepository {

    private static final String COLUNAS = """
            id, nome, especie, raca, porte, responsavel,
            data_nascimento, data_insercao_img, detalhes, imagem_url
            """.replaceAll("\\s+", " ").trim();

    // A capa fica na mesma linha do pet; a consulta não faz JOIN nem duplica linhas.
    private static final String SELECT_BASE = """
            SELECT p.id, p.nome, p.especie, p.raca, p.porte, p.responsavel,
                   p.data_nascimento, p.data_insercao_img, p.detalhes,
                   COALESCE(p.img_principal, p.imagem_url) AS imagem_url
              FROM pet p
            """;
    private static final String SELECT_LEGADO = "SELECT " + COLUNAS + " FROM pet";
    private static final String INSERT = """
            INSERT INTO pet
                (nome, especie, raca, porte, responsavel,
                 data_nascimento, data_insercao_img, detalhes, imagem_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING %s
            """.formatted(COLUNAS);
    private static final String UPDATE = """
            UPDATE pet
               SET nome = ?,
                   especie = ?,
                   raca = ?,
                   porte = ?,
                   responsavel = ?,
                   data_nascimento = ?,
                   data_insercao_img = ?,
                   detalhes = ?,
                   imagem_url = ?
             WHERE id = ?
            RETURNING %s
            """.formatted(COLUNAS);

    private final Database database;

    public PetRepository(Database database) {
        this.database = database;
    }

    public List<Pet> findAll() throws SQLException {
        try {
            return listarComConsulta(SELECT_BASE + " ORDER BY p.id");
        } catch (SQLException e) {
            if (!"42P01".equals(e.getSQLState()) && !"42703".equals(e.getSQLState())) throw e;
            // A tabela de fotos é uma migração opcional para o CRUD legado.
            return listarComConsulta(SELECT_LEGADO + " ORDER BY id");
        }
    }

    public Optional<Pet> findById(int id) throws SQLException {
        try {
            return buscarComConsulta(SELECT_BASE + " WHERE p.id = ?", id);
        } catch (SQLException e) {
            if (!"42P01".equals(e.getSQLState()) && !"42703".equals(e.getSQLState())) throw e;
            return buscarComConsulta(SELECT_LEGADO + " WHERE id = ?", id);
        }
    }

    private List<Pet> listarComConsulta(String sql) throws SQLException {
        List<Pet> pets = new ArrayList<>();
        try (Connection conexao = database.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) pets.add(mapear(rs));
        }
        return pets;
    }

    private Optional<Pet> buscarComConsulta(String sql, int id) throws SQLException {
        try (Connection conexao = database.getConnection(); PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Pet create(PetRequest pet) throws SQLException {
        try (Connection conexao = database.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(INSERT)) {
            preencherDados(stmt, pet);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("O banco de dados não retornou o pet cadastrado.");
                }
                return mapear(rs);
            }
        }
    }

    public Optional<Pet> update(int id, PetRequest pet) throws SQLException {
        try (Connection conexao = database.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(UPDATE)) {
            preencherDados(stmt, pet);
            stmt.setInt(10, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public boolean deleteById(int id) throws SQLException {
        try (Connection conexao = database.getConnection();
             PreparedStatement stmt = conexao.prepareStatement("DELETE FROM pet WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private static void preencherDados(PreparedStatement stmt, PetRequest pet) throws SQLException {
        stmt.setString(1, pet.nome());
        stmt.setString(2, pet.especie());
        stmt.setString(3, pet.raca());
        stmt.setString(4, pet.porte());
        stmt.setString(5, pet.responsavel());

        if (pet.dataNascimento() == null) {
            stmt.setNull(6, Types.DATE);
        } else {
            stmt.setObject(6, pet.dataNascimento(), Types.DATE);
        }

        if (pet.dataInsercaoImg() == null) {
            stmt.setNull(7, Types.TIMESTAMP);
        } else {
            stmt.setObject(7, pet.dataInsercaoImg(), Types.TIMESTAMP);
        }

        stmt.setString(8, pet.detalhes());
        stmt.setString(9, pet.imagemUrl());
    }

    private static Pet mapear(ResultSet rs) throws SQLException {
        return new Pet(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("especie"),
                rs.getString("raca"),
                rs.getString("porte"),
                rs.getString("responsavel"),
                rs.getObject("data_nascimento", LocalDate.class),
                rs.getObject("data_insercao_img", LocalDateTime.class),
                rs.getString("detalhes"),
                rs.getString("imagem_url")
        );
    }
}
