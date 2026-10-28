import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PetForm from '../components/PetForm';
import { atualizarPet, buscarPet, type Pet, type PetPayload } from '../services/petService';

export default function PetEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [pet, setPet] = useState<Pet | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');

  useEffect(() => {
    carregar();
  }, [id]);

  async function carregar() {
    if (!id) return;
    try {
      setLoading(true);
      setErro('');
      const data = await buscarPet(Number(id));
      setPet(data);
    } catch (e: any) {
      setErro(e.message || 'Erro ao carregar pet');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(payload: PetPayload, imagem?: File | null) {
    if (!pet) return;
    try {
      setErro('');
      const atualizado = await atualizarPet(pet.id, payload, imagem);
      navigate(`/pets/${atualizado.id}`);
    } catch (e: any) {
      setErro(e.message || 'Erro ao atualizar pet');
    }
  }

  if (loading) return <p style={{ padding: 20 }}>Carregando...</p>;
  if (erro && !pet) return <p style={{ padding: 20, color: 'red' }}>{erro}</p>;
  if (!pet) return <p style={{ padding: 20 }}>Pet não encontrado</p>;

  return (
    <div>
      {erro && <p style={{ color: 'red', textAlign: 'center' }}>{erro}</p>}
      <PetForm mode="edit" initialData={pet} onSubmit={handleSubmit} onCancel={() => navigate(`/pets/${pet.id}`)} />
    </div>
  );
}
