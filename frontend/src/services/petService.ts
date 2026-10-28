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

export async function listarPets(): Promise<Pet[]> {
  const res = await fetch(`${API_BASE_URL}/pets`);
  if (!res.ok) throw new Error('Erro ao listar pets');
  return res.json();
}

export async function buscarPet(id: number): Promise<Pet> {
  const res = await fetch(`${API_BASE_URL}/pets/${id}`);
  if (!res.ok) throw new Error('Erro ao buscar pet');
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
