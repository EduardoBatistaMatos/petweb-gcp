package br.com.petshop.service;

import br.com.petshop.model.Pet;
import br.com.petshop.model.PetRequest;
import br.com.petshop.repository.PetRepository;
import br.com.petshop.web.http.ApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio e validação dos dados de pet.
 */
public final class PetService {

    private static final int NOME_MAX = 100;
    private static final int ESPECIE_MAX = 50;
    private static final int RACA_MAX = 100;
    private static final int PORTE_MAX = 30;
    private static final int RESPONSAVEL_MAX = 150;

    private final PetRepository repository;

    public PetService(PetRepository repository) {
        this.repository = repository;
    }

    public List<Pet> listarTodos() throws SQLException {
        return repository.findAll();
    }

    public Pet buscarPorId(int id) throws SQLException {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Pet com id " + id + " não encontrado."));
    }

    public Pet criar(PetRequest requisicao) throws SQLException {
        PetRequest pet = validar(requisicao);
        return repository.create(pet);
    }

    public Pet atualizar(int id, PetRequest requisicao) throws SQLException {
        PetRequest pet = validar(requisicao);
        return repository.update(id, pet)
                .orElseThrow(() -> ApiException.notFound("Pet com id " + id + " não encontrado."));
    }

    public void excluir(int id) throws SQLException {
        if (!repository.deleteById(id)) {
            throw ApiException.notFound("Pet com id " + id + " não encontrado.");
        }
    }

    public static int validarId(String valor) {
        try {
            int id = Integer.parseInt(valor);
            if (id <= 0) {
                throw new NumberFormatException("id deve ser positivo");
            }
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(
                    "Identificador inválido. Informe um número inteiro maior que zero.");
        }
    }

    private static PetRequest validar(PetRequest requisicao) {
        if (requisicao == null) {
            throw ApiException.badRequest("O corpo da requisição é obrigatório.");
        }

        List<String> erros = new ArrayList<>();

        String nome = normalizarObrigatorio(requisicao.nome(), "nome", NOME_MAX, erros);
        String especie = normalizarObrigatorio(requisicao.especie(), "especie", ESPECIE_MAX, erros);
        String raca = normalizarOpcional(requisicao.raca(), "raca", RACA_MAX, erros);
        String porte = normalizarOpcional(requisicao.porte(), "porte", PORTE_MAX, erros);
        String responsavel = normalizarObrigatorio(
                requisicao.responsavel(), "responsavel", RESPONSAVEL_MAX, erros);
        String detalhes = normalizarOpcional(requisicao.detalhes(), "detalhes", -1, erros);
        String imagemUrl = normalizarOpcional(requisicao.imagemUrl(), "imagem_url", -1, erros);

        if (!erros.isEmpty()) {
            throw ApiException.badRequest("Dados inválidos.", erros);
        }

        return new PetRequest(
                nome,
                especie,
                raca,
                porte,
                responsavel,
                requisicao.dataNascimento(),
                requisicao.dataInsercaoImg(),
                detalhes,
                imagemUrl
        );
    }

    private static String normalizarObrigatorio(
            String valor, String campo, int tamanhoMaximo, List<String> erros) {
        String normalizado = valor == null ? null : valor.trim();
        if (normalizado == null || normalizado.isEmpty()) {
            erros.add("O campo '" + campo + "' é obrigatório.");
            return null;
        }
        validarTamanho(normalizado, campo, tamanhoMaximo, erros);
        return normalizado;
    }

    private static String normalizarOpcional(
            String valor, String campo, int tamanhoMaximo, List<String> erros) {
        String normalizado = valor == null ? null : valor.trim();
        if (normalizado == null || normalizado.isEmpty()) {
            return null;
        }
        if (tamanhoMaximo > 0) {
            validarTamanho(normalizado, campo, tamanhoMaximo, erros);
        }
        return normalizado;
    }

    private static void validarTamanho(
            String valor, String campo, int tamanhoMaximo, List<String> erros) {
        int tamanho = valor.codePointCount(0, valor.length());
        if (tamanho > tamanhoMaximo) {
            erros.add("O campo '" + campo + "' deve ter no máximo "
                    + tamanhoMaximo + " caracteres.");
        }
    }
}
