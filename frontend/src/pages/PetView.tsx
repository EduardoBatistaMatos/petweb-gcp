import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PetForm from '../components/PetForm';
import { buscarPet, excluirPet, type Pet } from '../services/petService';

export default function PetView() {
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

  async function handleDelete() {
    if (!pet) return;
    if (!confirm('Remover o cadastro deste pet e sua imagem associada?')) return;
    try {
      await excluirPet(pet.id);
      alert('Pet excluído com sucesso');
      navigate('/pets');
    } catch (e: any) {
      alert(e.message || 'Erro ao excluir pet');
    }
  }

  if (loading) return <p style={{ padding: 20 }}>Carregando...</p>;
  if (erro) return <p style={{ padding: 20, color: 'red' }}>{erro}</p>;
  if (!pet) return <p style={{ padding: 20 }}>Pet não encontrado</p>;

  return (
    <PetForm
      mode="view"
      initialData={pet}
      onEdit={() => navigate(`/pets/${pet.id}/editar`)}
      onDelete={handleDelete}
      onCancel={() => navigate('/pets')}
    />
  );
}
