import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { listarPets, type Pet } from '../services/petService';

export default function PetList() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [imagensComFalha, setImagensComFalha] = useState<Set<number>>(() => new Set());
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    let ativo = true;
    listarPets().then(data => { if (ativo) setPets(data); })
      .catch((e: any) => { if (ativo) setErro(e.message || 'Não foi possível carregar os pets.'); })
      .finally(() => { if (ativo) setLoading(false); });
    return () => { ativo = false; };
  }, []);

  async function carregar() {
    try {
      setLoading(true);
      setErro('');
      setPets(await listarPets());
    } catch (e: any) {
      setErro(e.message || 'Não foi possível carregar os pets.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page-shell">
      {location.state?.notice && <div className="notice notice-success" role="status"><span>{location.state.notice}</span><button className="notice-close" onClick={() => navigate(location.pathname, { replace: true, state: null })} aria-label="Dispensar mensagem">×</button></div>}
      <header className="page-heading">
        <div>
          <span className="eyebrow">Cuidado e companhia</span>
          <h1>Pets</h1>
          <p className="page-lead">Encontre e acompanhe os pets cadastrados.</p>
        </div>
      </header>

      {loading && <div className="pet-grid" aria-label="Carregando pets">{[1, 2, 3, 4].map(item => <div className="pet-skeleton" key={item} />)}</div>}
      {!loading && erro && <section className="notice notice-error" role="alert"><div><strong>Não foi possível carregar a lista</strong><p>{erro}</p></div><button className="button button-secondary" onClick={() => void carregar()}>Tentar novamente</button></section>}
      {!loading && !erro && pets.length === 0 && <section className="empty-state"><div className="empty-state-icon" aria-hidden="true">🐾</div><h2>Nenhum pet cadastrado</h2><p>Cadastre o primeiro pet para começar o acompanhamento.</p><button className="button button-primary" onClick={() => navigate('/pets/novo')}>Cadastrar primeiro pet</button></section>}

      {!loading && !erro && pets.length > 0 && <div className="pet-grid">
        {pets.map(pet => <button className="pet-card" key={pet.id} onClick={() => navigate(`/pets/${pet.id}`)} aria-label={`Ver detalhes de ${pet.nome}`}>
          <span className="pet-card-image-wrap">
            {pet.imagem_url && !imagensComFalha.has(pet.id) ? <img className="pet-card-image" src={pet.imagem_url} alt={`Foto de ${pet.nome}`} onError={() => setImagensComFalha(current => new Set(current).add(pet.id))} /> : <span className="pet-card-placeholder"><span aria-hidden="true">🐾</span><small>{pet.imagem_url ? 'Imagem indisponível' : 'Sem imagem'}</small></span>}
          </span>
          <span className="pet-card-content">
            <strong className="pet-card-name">{pet.nome}</strong>
            <span className="pet-card-species">{pet.especie}</span>
            <span className="pet-card-meta">{pet.raca || 'Raça não informada'}<span className="meta-dot">·</span>{pet.porte || 'Porte não informado'}</span>
          </span>
          <span className="pet-card-arrow" aria-hidden="true">↗</span>
        </button>)}
      </div>}
    </main>
  );
}
