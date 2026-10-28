import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PetForm from '../components/PetForm';
import PetFormModal from '../components/PetFormModal';
import { criarPet, enviarFotos, type PetPayload } from '../services/petService';

export default function PetCreate() {
  const navigate = useNavigate();
  const [erro, setErro] = useState('');

  async function handleSubmit(payload: PetPayload, imagens: File[] = [], dataFotos = '') {
    try {
      setErro('');
      const pet = await criarPet(payload);
      if (imagens.length) await enviarFotos(pet.id, imagens, dataFotos);
      navigate(`/pets/${pet.id}`, { state: { notice: 'Pet cadastrado com sucesso.' } });
    } catch (e: any) {
      setErro(e.message || 'Erro ao cadastrar pet');
    }
  }

  return (
    <PetFormModal title="Cadastrar pet">
      {erro && <div className="modal-error notice notice-error" role="alert">{erro}</div>}
      <PetForm mode="create" onSubmit={handleSubmit} onCancel={() => navigate('/pets')} />
    </PetFormModal>
  );
}
