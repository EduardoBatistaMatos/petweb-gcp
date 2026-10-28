import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PetForm from '../components/PetForm';
import { criarPet, type PetPayload } from '../services/petService';

export default function PetCreate() {
  const navigate = useNavigate();
  const [erro, setErro] = useState('');

  async function handleSubmit(payload: PetPayload, imagem?: File | null) {
    try {
      setErro('');
      const pet = await criarPet(payload, imagem);
      navigate(`/pets/${pet.id}`);
    } catch (e: any) {
      setErro(e.message || 'Erro ao cadastrar pet');
    }
  }

  return (
    <div>
      {erro && <p style={{ color: 'red', textAlign: 'center' }}>{erro}</p>}
      <PetForm mode="create" onSubmit={handleSubmit} onCancel={() => navigate('/pets')} />
    </div>
  );
}
