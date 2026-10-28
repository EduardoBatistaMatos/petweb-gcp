import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PetForm from '../components/PetForm';
import { buscarPet, excluirPet, listarFotos, excluirFoto, definirFotoPrincipal, type Pet, type PetFoto } from '../services/petService';

export default function PetView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [pet, setPet] = useState<Pet | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [fotos, setFotos] = useState<PetFoto[]>([]);
  const [fotoErro, setFotoErro] = useState('');
  const [fotoFeedback, setFotoFeedback] = useState('');
  const [fotoProcessando, setFotoProcessando] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    if (!id) return;
    buscarPet(Number(id)).then(async data => {
      if (!ativo) return;
      setPet(data);
      try {
        const historico = await listarFotos(data.id);
        if (ativo) setFotos(historico);
      } catch (e: any) {
        if (ativo) { setFotos([]); setFotoErro(e.message || 'Não foi possível carregar as fotos.'); }
      }
    }).catch((e: any) => { if (ativo) setErro(e.message || 'Não foi possível carregar o pet.'); })
      .finally(() => { if (ativo) setLoading(false); });
    return () => { ativo = false; };
  }, [id]);

  async function handleDelete() {
    if (!pet || !confirm('Excluir o cadastro deste pet e todas as imagens associadas?')) return;
    try { await excluirPet(pet.id); navigate('/pets', { state: { notice: 'Pet e imagens excluídos com sucesso.' } }); }
    catch (e: any) { window.alert(e.message || 'Não foi possível excluir o pet.'); }
  }

  async function handleDefinirCapa(foto: PetFoto) {
    if (!pet || fotoProcessando) return;
    setFotoProcessando(foto.id); setFotoErro(''); setFotoFeedback('');
    try { await definirFotoPrincipal(pet.id, foto.id); setFotos(await listarFotos(pet.id)); setFotoFeedback('Foto principal atualizada.'); }
    catch (e: any) { setFotoErro(e.message || 'Não foi possível atualizar a capa.'); }
    finally { setFotoProcessando(null); }
  }

  async function handleExcluirFoto(foto: PetFoto) {
    if (!pet || fotoProcessando || !confirm('Excluir esta foto do histórico e do armazenamento?')) return;
    setFotoProcessando(foto.id); setFotoErro(''); setFotoFeedback('');
    try { await excluirFoto(pet.id, foto.id); setFotos(await listarFotos(pet.id)); setFotoFeedback('Foto excluída do histórico e do armazenamento.'); }
    catch (e: any) { setFotoErro(e.message || 'Não foi possível excluir esta foto.'); }
    finally { setFotoProcessando(null); }
  }

  if (!id) return <main className="page-shell"><div className="notice notice-error">Identificador de pet ausente.</div></main>;
  if (loading) return <main className="page-shell"><div className="loading-panel"><span className="spinner" />Carregando perfil do pet…</div></main>;
  if (erro) return <main className="page-shell"><div className="notice notice-error" role="alert"><strong>Não foi possível abrir este pet</strong><p>{erro}</p><button className="button button-secondary" onClick={() => navigate('/pets')}>Voltar à lista</button></div></main>;
  if (!pet) return <main className="page-shell"><section className="empty-state"><h2>Pet não encontrado</h2><p>Confira o endereço ou volte à lista de pets.</p><button className="button button-secondary" onClick={() => navigate('/pets')}>Voltar à lista</button></section></main>;

  const fotosPorData = Object.entries(fotos.reduce<Record<string, PetFoto[]>>((grupos, foto) => {
    (grupos[foto.data_foto] ||= []).push(foto);
    return grupos;
  }, {}));

  return <>
    <PetForm mode="view" initialData={pet} onEdit={() => navigate(`/pets/${pet.id}/editar`)} onDelete={handleDelete} onCancel={() => navigate('/pets')} />
    <section className="page-shell history-shell">
      {location.state?.notice && <div className="notice notice-success" role="status"><span>{location.state.notice}</span><button className="notice-close" onClick={() => navigate(location.pathname, { replace: true, state: null })} aria-label="Dispensar mensagem">×</button></div>}
      <header className="section-heading history-heading"><div><span className="eyebrow">Memórias</span><h2>Histórico de fotos</h2><p className="page-lead">{fotos.length} registro(s), organizados por data.</p></div></header>
      {fotoErro && <div className="notice notice-error" role="alert">{fotoErro}</div>}
      {fotoFeedback && <div className="notice notice-success" role="status">{fotoFeedback}</div>}
      {fotos.length === 0 && !fotoErro ? <section className="empty-state empty-state-compact"><div className="empty-state-icon" aria-hidden="true">▧</div><h3>Nenhuma foto ainda</h3><p>As fotos adicionadas aparecerão aqui por data.</p></section> : <div className="photo-history">{fotosPorData.map(([data, grupo]) => <section className="photo-date-group" key={data}>
        <h3>{new Date(`${data}T12:00:00`).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' })}<span>{grupo.length} foto(s)</span></h3>
        <div className="photo-grid">{grupo.map(foto => <article className={`history-card${foto.img_principal ? ' is-cover' : ''}`} key={foto.id}>
          <div className="history-image-wrap"><img src={foto.url} alt={`Foto de ${pet.nome} em ${new Date(`${foto.data_foto}T12:00:00`).toLocaleDateString('pt-BR')}`} />{foto.img_principal && <span className="cover-badge">Foto principal</span>}</div>
          <div className="history-card-actions">{!foto.img_principal && <button className="button button-secondary button-small" disabled={fotoProcessando !== null} onClick={() => void handleDefinirCapa(foto)}>{fotoProcessando === foto.id ? 'Atualizando…' : 'Definir capa'}</button>}
            <button className="button button-danger-quiet button-small" disabled={fotoProcessando !== null} onClick={() => void handleExcluirFoto(foto)}>{fotoProcessando === foto.id ? 'Excluindo…' : 'Excluir'}</button>
          </div>
        </article>)}</div>
      </section>)}</div>}
    </section>
  </>;
}
