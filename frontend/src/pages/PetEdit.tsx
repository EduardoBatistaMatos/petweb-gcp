import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PetForm from '../components/PetForm';
import PetFormModal from '../components/PetFormModal';
import { atualizarPet, buscarPet, enviarFotos, type Pet, type PetPayload } from '../services/petService';

export default function PetEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [pet, setPet] = useState<Pet | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let ativo = true;
    if (!id) return;
    buscarPet(Number(id)).then(data => { if (ativo) setPet(data); })
      .catch((e: any) => { if (ativo) setErro(e.message || 'Não foi possível carregar o pet.'); })
      .finally(() => { if (ativo) setLoading(false); });
    return () => { ativo = false; };
  }, [id]);

  async function handleSubmit(payload: PetPayload, imagens: File[] = [], dataFotos = '') {
    if (!pet) return;
    try {
      setErro('');
      const atualizado = await atualizarPet(pet.id, payload);
      if (imagens.length) await enviarFotos(pet.id, imagens, dataFotos);
      navigate(`/pets/${atualizado.id}`, { state: { notice: 'Alterações salvas com sucesso.' } });
    } catch (e: any) { setErro(e.message || 'Não foi possível atualizar o pet.'); }
  }

  if (!id) return <main className="page-shell"><div className="notice notice-error">Identificador de pet ausente.</div></main>;
  if (loading) return <main className="page-shell"><div className="loading-panel"><span className="spinner" />Carregando dados do pet…</div></main>;
  if (!pet) return <main className="page-shell"><div className="notice notice-error" role="alert"><div><strong>Não foi possível carregar o pet</strong><p>{erro || 'Pet não encontrado.'}</p></div><button className="button button-secondary" onClick={() => navigate('/pets')}>Voltar à lista</button></div></main>;

  return <PetFormModal title={`Editar ${pet.nome}`}>
    {erro && <div className="modal-error notice notice-error" role="alert">{erro}</div>}
    <PetForm mode="edit" initialData={pet} onSubmit={handleSubmit} onCancel={() => navigate(`/pets/${pet.id}`)} />
  </PetFormModal>;
}
