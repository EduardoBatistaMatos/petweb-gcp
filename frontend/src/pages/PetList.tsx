import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listarPets, type Pet } from '../services/petService';

export default function PetList() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [imagensComFalha, setImagensComFalha] = useState<Set<number>>(() => new Set());
  const navigate = useNavigate();

  useEffect(() => {
    carregar();
  }, []);

  async function carregar() {
    try {
      setLoading(true);
      setErro('');
      const data = await listarPets();
      setPets(data);
    } catch (e: any) {
      setErro(e.message || 'Erro ao carregar pets');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ padding: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h1>Pets</h1>
        <button onClick={() => navigate('/pets/novo')}>Cadastrar novo pet</button>
      </div>

      {loading && <p>Carregando...</p>}
      {erro && <p style={{ color: 'red' }}>{erro}</p>}
      {!loading && !erro && pets.length === 0 && (
        <div>
          <p>Nenhum pet cadastrado.</p>
          <button onClick={() => navigate('/pets/novo')}>Cadastrar primeiro pet</button>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {pets.map((pet) => (
          <div
            key={pet.id}
            onClick={() => navigate(`/pets/${pet.id}`)}
            style={{ border: '1px solid #ddd', borderRadius: 8, overflow: 'hidden', cursor: 'pointer', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}
          >
            {pet.imagem_url && !imagensComFalha.has(pet.id) ? (
              <img
                src={pet.imagem_url}
                alt={pet.nome}
                onError={() => setImagensComFalha((atuais) => new Set(atuais).add(pet.id))}
                style={{ width: '100%', height: 200, objectFit: 'cover' }}
              />
            ) : (
              <div style={{ width: '100%', height: 200, background: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                {pet.imagem_url ? 'Imagem indisponível' : 'Sem imagem'}
              </div>
            )}
            <div style={{ padding: 12 }}>
              <h3 style={{ margin: '0 0 8px 0' }}>{pet.nome}</h3>
              <p style={{ margin: 0, fontSize: 14 }}>Espécie: {pet.especie}</p>
              {pet.raca && <p style={{ margin: 0, fontSize: 14 }}>Raça: {pet.raca}</p>}
              {pet.porte && <p style={{ margin: 0, fontSize: 14 }}>Porte: {pet.porte}</p>}
            </div>
          </div>
        ))}
      </div>

      <style>{`
        @media (max-width: 1200px) { div[style*="repeat(4, 1fr)"] { grid-template-columns: repeat(3, 1fr) !important; } }
        @media (max-width: 900px) { div[style*="repeat(4, 1fr)"] { grid-template-columns: repeat(2, 1fr) !important; } }
        @media (max-width: 600px) { div[style*="repeat(4, 1fr)"] { grid-template-columns: 1fr !important; } }
      `}</style>
    </div>
  );
}
