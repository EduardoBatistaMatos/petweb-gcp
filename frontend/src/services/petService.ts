import API_BASE_URL from '../config/api';

export interface Pet {
  id: number;
  nome: string;
  especie: string;
  raca: string | null;
  porte: string | null;
  responsavel: string;
  data_nascimento: string | null;
  data_insercao_img: string | null;
  detalhes: string | null;
  imagem_url: string | null;
}

export interface PetPayload {
  nome: string;
  especie: string;
  raca?: string | null;
  porte?: string | null;
  responsavel: string;
  data_nascimento?: string | null;
  data_insercao_img?: string | null;
  detalhes?: string | null;
  imagem_url?: string | null;
}

export interface PetFoto { id: string; pet_id: number; url: string; data_foto: string; img_principal: boolean }

async function responseError(res: Response, fallback: string) {
  if (res.ok) return;
  const body = await res.text();
  let message: string | undefined;
  try {
    const parsed = JSON.parse(body);
    message = parsed.message;
  } catch { /* resposta não JSON: usa o corpo bruto abaixo */ }
  throw new Error(message || body || `${fallback} (HTTP ${res.status})`);
}

export async function listarFotos(petId: number): Promise<PetFoto[]> {
  const res = await fetch(`${API_BASE_URL}/pets/${petId}/fotos`);
  await responseError(res, 'Erro ao carregar fotos'); return res.json();
}
export async function enviarFotos(petId: number, files: File[], data: string): Promise<PetFoto[]> {
  const form = new FormData(); files.forEach(file => form.append('fotos', file)); form.append('data_foto', data);
  const res = await fetch(`${API_BASE_URL}/pets/${petId}/fotos`, { method: 'POST', body: form });
  await responseError(res, 'Erro ao enviar fotos'); return res.json();
}
export async function definirFotoPrincipal(petId: number, fotoId: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/pets/${petId}/fotos/${fotoId}/principal`, { method: 'PUT' });
  await responseError(res, 'Erro ao definir foto principal');
}
export async function excluirFoto(petId: number, fotoId: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/pets/${petId}/fotos/${fotoId}`, { method: 'DELETE' });
  await responseError(res, 'Erro ao excluir foto');
}

export async function listarPets(): Promise<Pet[]> {
  const res = await fetch(`${API_BASE_URL}/pets`);
  await responseError(res, 'Erro ao listar pets');
  return res.json();
}

export async function buscarPet(id: number): Promise<Pet> {
  const res = await fetch(`${API_BASE_URL}/pets/${id}`);
  await responseError(res, 'Erro ao buscar pet');
  return res.json();
}

export async function criarPet(payload: PetPayload, imagem?: File | null): Promise<Pet> {
  if (imagem) {
    const formData = new FormData();
    formData.append('nome', payload.nome);
    formData.append('especie', payload.especie);
    formData.append('responsavel', payload.responsavel);
    if (payload.raca) formData.append('raca', payload.raca);
    if (payload.porte) formData.append('porte', payload.porte);
    if (payload.data_nascimento) formData.append('data_nascimento', payload.data_nascimento);
    if (payload.data_insercao_img) formData.append('data_insercao_img', payload.data_insercao_img);
    if (payload.detalhes) formData.append('detalhes', payload.detalhes);
    if (payload.imagem_url) formData.append('imagem_url', payload.imagem_url);
    formData.append('imagem', imagem);
    const res = await fetch(`${API_BASE_URL}/pets`, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Erro ao criar pet');
    }
    return res.json();
  }
  const res = await fetch(`${API_BASE_URL}/pets`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || 'Erro ao criar pet');
  }
  return res.json();
}

export async function atualizarPet(id: number, payload: PetPayload, imagem?: File | null): Promise<Pet> {
  if (imagem) {
    const formData = new FormData();
    formData.append('nome', payload.nome);
    formData.append('especie', payload.especie);
    formData.append('responsavel', payload.responsavel);
    if (payload.raca) formData.append('raca', payload.raca);
    if (payload.porte) formData.append('porte', payload.porte);
    if (payload.data_nascimento) formData.append('data_nascimento', payload.data_nascimento);
    if (payload.data_insercao_img) formData.append('data_insercao_img', payload.data_insercao_img);
    if (payload.detalhes) formData.append('detalhes', payload.detalhes);
    if (payload.imagem_url) formData.append('imagem_url', payload.imagem_url);
    formData.append('imagem', imagem);
    const res = await fetch(`${API_BASE_URL}/pets/${id}`, {
      method: 'PUT',
      body: formData,
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Erro ao atualizar pet');
    }
    return res.json();
  }
  const res = await fetch(`${API_BASE_URL}/pets/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || 'Erro ao atualizar pet');
  }
  return res.json();
}

export async function excluirPet(id: number): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/pets/${id}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error('Erro ao excluir pet');
}
