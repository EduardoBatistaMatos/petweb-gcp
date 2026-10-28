package br.com.petshop.web.handler;

import br.com.petshop.model.PetFoto;
import br.com.petshop.model.Pet;
import br.com.petshop.repository.PetFotoRepository;
import br.com.petshop.service.PetService;
import br.com.petshop.storage.GoogleCloudStorageService;
import br.com.petshop.storage.StorageConfig;
import br.com.petshop.web.http.ApiException;
import br.com.petshop.web.http.HttpUtils;
import br.com.petshop.web.http.Multipart;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.*;

/** Gerencia a coleção de fotos de um pet. */
public final class PetFotoHandler implements HttpHandler {
    private static final Logger LOG=Logger.getLogger(PetFotoHandler.class.getName());
    private static final int MAX_FOTO=10*1024*1024, MAX_FOTOS=10;
    private final PetFotoRepository repository;
    private final PetService pets;
    private final GoogleCloudStorageService storage;
    public PetFotoHandler(PetFotoRepository repository,PetService pets) {
        this.repository=repository;this.pets=pets;GoogleCloudStorageService s=null;
        try{s=new GoogleCloudStorageService(StorageConfig.from(null));}catch(Exception e){LOG.log(Level.WARNING,"GCS indisponível para fotos",e);} storage=s;
    }
    @Override public void handle(HttpExchange x)throws IOException {
        int status=500; try {
            if(x.getRequestMethod().equalsIgnoreCase("OPTIONS")){HttpUtils.enviarSemConteudo(x,204);return;}
            String[] path=x.getRequestURI().getPath().split("/");
            if(path.length<4||!path[1].equals("pets")||!path[3].equals("fotos"))throw ApiException.notFound("Rota não encontrada.");
            int petId=PetService.validarId(path[2]); pets.buscarPorId(petId);
            String method=x.getRequestMethod().toUpperCase(Locale.ROOT);
            if(path.length==4&&method.equals("GET")){HttpUtils.enviarJson(x,200,repository.listar(petId));status=200;return;}
            if(path.length==4&&method.equals("POST")){status=upload(x,petId);return;}
            if(path.length==6&&path[5].equals("principal")&&method.equals("PUT")){String fotoId=validarFotoId(path[4]);if(!repository.definirPrincipal(petId,fotoId))throw ApiException.notFound("Foto não encontrada para este pet.");HttpUtils.enviarJson(x,200,repository.listar(petId));status=200;return;}
            if(path.length==5&&method.equals("DELETE")){String fotoId=validarFotoId(path[4]);PetFoto foto=repository.buscar(petId,fotoId).orElseThrow(()->ApiException.notFound("Foto não encontrada para este pet."));if(storage==null)throw ApiException.badRequest("Google Cloud Storage não configurado.");if(!storage.deleteByUrl(foto.url()))throw ApiException.badRequest("Não foi possível remover o arquivo do bucket; a foto continua cadastrada.");repository.excluir(petId,fotoId);HttpUtils.enviarSemConteudo(x,204);status=204;return;}
            throw ApiException.methodNotAllowed("Método ou rota não permitido para fotos.","GET, POST, PUT, DELETE, OPTIONS");
        } catch(ApiException e){status=e.status();if(e.allowHeader()!=null)x.getResponseHeaders().set("Allow",e.allowHeader());HttpUtils.enviarErro(x,status,e.getMessage(),e.details());}
          catch(SQLException e){LOG.log(Level.SEVERE,"Erro ao gerenciar fotos",e);HttpUtils.enviarErro(x,500,"Erro interno ao acessar o banco de dados.",null);}
          catch(Exception e){LOG.log(Level.SEVERE,"Erro ao gerenciar fotos",e);HttpUtils.enviarErro(x,500,"Erro interno inesperado.",null);}
        finally{x.close();}
    }
    private static String validarFotoId(String valor) {
        if (valor != null && valor.matches("[A-Za-z0-9-]{1,64}")) return valor;
        throw ApiException.badRequest("Identificador de foto inválido.");
    }

    /** Remove todos os objetos associados antes de permitir a exclusão do pet. */
    public void excluirArquivosDoPet(Pet pet) throws SQLException {
        Set<String> urls = new LinkedHashSet<>();
        for (PetFoto foto : repository.listar(pet.id())) urls.add(foto.url());
        if (pet.imagemUrl() != null && !pet.imagemUrl().isBlank()) urls.add(pet.imagemUrl());
        if (!urls.isEmpty() && storage == null) {
            throw ApiException.badRequest("Google Cloud Storage não configurado; as imagens do pet não foram removidas.");
        }
        for (String url : urls) {
            if (!storage.deleteByUrl(url)) {
                throw ApiException.badRequest("Não foi possível excluir todas as imagens do bucket; o pet continua cadastrado.");
            }
        }
    }
    private int upload(HttpExchange x,int petId)throws Exception {
        if(storage==null)throw ApiException.badRequest("Google Cloud Storage não configurado.");
        String ct=HttpUtils.contentType(x);String boundary=null;for(String p:ct.split(";")){String q=p.trim();if(q.toLowerCase(Locale.ROOT).startsWith("boundary="))boundary=q.substring(9).replaceAll("^\"|\"$","");}
        byte[] body=HttpUtils.lerCorpo(x,(MAX_FOTO*MAX_FOTOS)+1_048_576);if(body.length>(MAX_FOTO*MAX_FOTOS)+1_048_576)throw ApiException.payloadTooLarge("Lote excede 100 MiB.");
        List<Multipart.Part> parts=Multipart.parse(body,boundary);String date=null;List<Multipart.Part> files=new ArrayList<>();
        for(Multipart.Part p:parts){if(p.name().equals("data_foto"))date=p.asString().trim();else if(p.name().equals("fotos")||p.name().equals("foto"))files.add(p);}
        if(files.isEmpty()||files.size()>MAX_FOTOS)throw ApiException.badRequest("Envie de 1 a 10 arquivos no campo fotos.");
        LocalDate data;try{data=LocalDate.parse(date);}catch(Exception e){throw ApiException.badRequest("Informe data_foto no formato YYYY-MM-DD.");}
        for(Multipart.Part f:files)if(f.data().length==0||f.data().length>MAX_FOTO||f.contentType()==null||!f.contentType().toLowerCase(Locale.ROOT).startsWith("image/"))throw ApiException.badRequest("Cada arquivo deve ser uma imagem de até 10 MiB.");
        List<String> urls=new ArrayList<>();try{
            for(Multipart.Part f:files)urls.add(storage.upload(f.inputStream(),f.fileName(),f.contentType()));
            List<PetFoto> metadata=new ArrayList<>();
            for(String url:urls)metadata.add(new PetFoto("",petId,url,data,false));
            HttpUtils.enviarJson(x,201,repository.inserirLote(petId,metadata));return 201;
        }catch(Exception e){for(String url:urls)storage.deleteByUrl(url);throw e;}
    }
}
