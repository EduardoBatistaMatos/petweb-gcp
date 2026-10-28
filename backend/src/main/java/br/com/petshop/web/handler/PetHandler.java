package br.com.petshop.web.handler;

import br.com.petshop.model.Pet;
import br.com.petshop.model.PetRequest;
import br.com.petshop.service.PetService;
import br.com.petshop.storage.GoogleCloudStorageService;
import br.com.petshop.storage.StorageConfig;
import br.com.petshop.web.http.ApiException;
import br.com.petshop.web.http.HttpUtils;
import br.com.petshop.web.http.Multipart;
import br.com.petshop.web.json.Json;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Endpoints de /pets e /pets/{id}.
 */
public final class PetHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(PetHandler.class.getName());
    private static final int TAMANHO_MAXIMO_ARQUIVO = 10 * 1024 * 1024;
    private static final int TAMANHO_MAXIMO_MULTIPART = TAMANHO_MAXIMO_ARQUIVO + 1_048_576;

    private final PetService service;
    private final GoogleCloudStorageService storageService;
    private final PetFotoHandler petFotoHandler;

    public PetHandler(PetService service) {
        this(service, null);
    }

    public PetHandler(PetService service, PetFotoHandler petFotoHandler) {
        this.service = service;
        this.petFotoHandler = petFotoHandler;
        GoogleCloudStorageService svc = null;
        try {
            svc = new GoogleCloudStorageService(StorageConfig.from(null));
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Google Cloud Storage não configurado ou dependências ausentes. Upload de imagens desabilitado: " + e.getClass().getSimpleName());
        }
        this.storageService = svc;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String caminho = normalizarCaminho(exchange.getRequestURI().getPath());
        if (petFotoHandler != null && caminho.matches("^/pets/[^/]+/fotos(?:/.*)?$")) {
            petFotoHandler.handle(exchange);
            return;
        }
        int status = 500;
        long inicio = System.nanoTime();

        try {
            if ("OPTIONS".equals(metodo)) {
                HttpUtils.enviarSemConteudo(exchange, 204);
                status = 204;
                return;
            }

            if (caminho.equals("/pets")) {
                status = tratarColecao(exchange, metodo);
            } else if (caminho.startsWith("/pets/")) {
                status = tratarItem(exchange, metodo, caminho.substring("/pets/".length()));
            } else {
                status = 404;
                HttpUtils.enviarErro(exchange, 404, "Rota não encontrada.", null);
            }
        } catch (ApiException e) {
            status = e.status();
            if (e.allowHeader() != null) {
                exchange.getResponseHeaders().set("Allow", e.allowHeader());
            }
            HttpUtils.enviarErro(exchange, e.status(), e.getMessage(), e.details());
        } catch (JsonProcessingException e) {
            status = 400;
            HttpUtils.enviarErro(exchange, 400, "JSON inválido.",
                    List.of(e.getOriginalMessage()));
        } catch (SQLException e) {
            status = 500;
            LOGGER.log(Level.SEVERE, "Erro de banco de dados ao processar " + metodo + " " + caminho, e);
            HttpUtils.enviarErro(exchange, 500,
                    "Erro interno ao acessar o banco de dados.", null);
        } catch (Exception e) {
            status = 500;
            LOGGER.log(Level.SEVERE, "Erro inesperado ao processar " + metodo + " " + caminho, e);
            HttpUtils.enviarErro(exchange, 500, "Erro interno inesperado.", null);
        } finally {
            long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
            LOGGER.info(metodo + " " + caminho + " -> " + status + " (" + duracaoMs + " ms)");
            exchange.close();
        }
    }

    private int tratarColecao(HttpExchange exchange, String metodo) throws IOException, SQLException {
        return switch (metodo) {
            case "GET" -> {
                List<Pet> pets = service.listarTodos();
                HttpUtils.enviarJson(exchange, 200, pets);
                yield 200;
            }
            case "POST" -> {
                PetRequest requisicao = lerRequisicao(exchange, null);
                Pet pet = service.criar(requisicao);
                exchange.getResponseHeaders().set("Location", "/pets/" + pet.id());
                HttpUtils.enviarJson(exchange, 201, pet);
                yield 201;
            }
            default -> throw ApiException.methodNotAllowed(
                    "Método " + metodo + " não permitido para /pets.",
                    "GET, POST, OPTIONS");
        };
    }

    private int tratarItem(HttpExchange exchange, String metodo, String idTexto)
            throws IOException, SQLException {
        int id = PetService.validarId(idTexto);

        return switch (metodo) {
            case "GET" -> {
                Pet pet = service.buscarPorId(id);
                HttpUtils.enviarJson(exchange, 200, pet);
                yield 200;
            }
            case "PUT" -> {
                Pet petAtual = service.buscarPorId(id);
                PetRequest requisicao = lerRequisicao(exchange, petAtual.imagemUrl());
                Pet pet = service.atualizar(id, requisicao);
                HttpUtils.enviarJson(exchange, 200, pet);
                yield 200;
            }
            case "DELETE" -> {
                Pet pet = service.buscarPorId(id);
                if (petFotoHandler != null) {
                    petFotoHandler.excluirArquivosDoPet(pet);
                } else if (storageService != null && pet.imagemUrl() != null && !pet.imagemUrl().isBlank()) {
                    try {
                        storageService.deleteByUrl(pet.imagemUrl());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Falha ao excluir imagem do bucket: " + e.getMessage());
                    }
                }
                service.excluir(id);
                HttpUtils.enviarSemConteudo(exchange, 204);
                yield 204;
            }
            default -> throw ApiException.methodNotAllowed(
                    "Método " + metodo + " não permitido para /pets/{id}.",
                    "GET, PUT, DELETE, OPTIONS");
        };
    }

    private PetRequest lerRequisicao(HttpExchange exchange, String imagemUrlAtual) throws IOException {
        String contentType = HttpUtils.contentType(exchange).toLowerCase(Locale.ROOT);
        if (contentType.contains("multipart/form-data")) {
            return lerMultipart(exchange, imagemUrlAtual);
        }
        if (contentType.contains("application/json")) {
            return HttpUtils.lerJson(exchange, PetRequest.class);
        }
        throw ApiException.unsupportedMediaType("Content-Type deve ser application/json ou multipart/form-data.");
    }

    private PetRequest lerMultipart(HttpExchange exchange, String imagemUrlAtual) throws IOException {
        String boundary = getBoundary(HttpUtils.contentType(exchange));
        byte[] corpo = HttpUtils.lerCorpo(exchange, TAMANHO_MAXIMO_MULTIPART);
        if (corpo.length > TAMANHO_MAXIMO_MULTIPART) {
            throw ApiException.payloadTooLarge("O corpo da requisição excede o limite de 11 MiB.");
        }
        if (corpo.length == 0) {
            throw ApiException.badRequest("O corpo da requisição é obrigatório.");
        }
        List<Multipart.Part> parts = Multipart.parse(corpo, boundary);
        Map<String, Multipart.Part> map = Multipart.toMap(parts);

        String nome = getValor(map, "nome");
        String especie = getValor(map, "especie");
        String raca = getValor(map, "raca");
        String porte = getValor(map, "porte");
        String responsavel = getValor(map, "responsavel");
        String dataNascimentoStr = getValor(map, "data_nascimento");
        String dataInsercaoImgStr = getValor(map, "data_insercao_img");
        String detalhes = getValor(map, "detalhes");
        String imagemUrlStr = getValor(map, "imagem_url");

        LocalDate dataNascimento = null;
        if (dataNascimentoStr != null && !dataNascimentoStr.isBlank()) {
            try {
                dataNascimento = LocalDate.parse(dataNascimentoStr.trim());
            } catch (Exception e) {
                throw ApiException.badRequest("Data de nascimento inválida.");
            }
        }

        LocalDateTime dataInsercaoImg = null;
        if (dataInsercaoImgStr != null && !dataInsercaoImgStr.isBlank()) {
            try {
                dataInsercaoImg = LocalDateTime.parse(dataInsercaoImgStr.trim());
            } catch (Exception e) {
                throw ApiException.badRequest("Data de inserção da imagem inválida.");
            }
        }

        String novaImagemUrl = imagemUrlStr;
        if (novaImagemUrl != null && novaImagemUrl.isBlank()) {
            novaImagemUrl = null;
        }

        Multipart.Part imagem = map.get("imagem");
        if (imagem == null) {
            imagem = map.get("image");
        }
        if (imagem == null) {
            imagem = map.get("foto");
        }
        if (imagem != null) {
            if (imagem.data() == null || imagem.data().length == 0) {
                throw ApiException.badRequest("Arquivo de imagem vazio.");
            }
            if (imagem.data().length > TAMANHO_MAXIMO_ARQUIVO) {
                throw ApiException.payloadTooLarge("A imagem excede o limite de 10 MiB.");
            }
            String ct = imagem.contentType();
            if (ct == null || (!ct.startsWith("image/"))) {
                throw ApiException.badRequest("Arquivo deve ser uma imagem.");
            }
            if (storageService == null) {
                throw ApiException.badRequest("Google Cloud Storage não configurado.");
            }
            String urlAntiga = imagemUrlAtual;
            try {
                novaImagemUrl = storageService.upload(imagem.inputStream(), imagem.fileName(), ct);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Falha no upload para GCS", e);
                throw ApiException.badRequest("Falha ao enviar imagem para o Google Cloud Storage.");
            }
            if (urlAntiga != null && !urlAntiga.isBlank() && !urlAntiga.equals(novaImagemUrl)) {
                try {
                    storageService.deleteByUrl(urlAntiga);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Falha ao excluir imagem antiga do bucket: " + e.getMessage());
                }
            }
        }

        if (novaImagemUrl == null && imagemUrlAtual != null) {
            novaImagemUrl = imagemUrlAtual;
        }

        return new PetRequest(
                nome,
                especie,
                raca,
                porte,
                responsavel,
                dataNascimento,
                dataInsercaoImg,
                detalhes,
                novaImagemUrl
        );
    }

    private static String getBoundary(String contentType) {
        if (contentType == null) {
            return null;
        }
        for (String part : contentType.split(";")) {
            String p = part.trim();
            if (p.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
                String b = p.substring("boundary=".length()).trim();
                if (b.startsWith("\"") && b.endsWith("\"")) {
                    b = b.substring(1, b.length() - 1);
                }
                return b;
            }
        }
        return null;
    }

    private static String getValor(Map<String, Multipart.Part> map, String chave) {
        Multipart.Part p = map.get(chave);
        if (p == null) {
            return null;
        }
        String s = p.asString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String normalizarCaminho(String caminho) {
        if (caminho == null || caminho.isEmpty()) {
            return "/";
        }
        String normalizado = caminho;
        while (normalizado.length() > 1 && normalizado.endsWith("/")) {
            normalizado = normalizado.substring(0, normalizado.length() - 1);
        }
        return normalizado;
    }
}
