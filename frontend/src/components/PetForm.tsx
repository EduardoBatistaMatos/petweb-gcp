import { useEffect, useState } from 'react';
import type { PetPayload } from '../services/petService';

interface PetFormProps {
  mode: 'create' | 'view' | 'edit';
  initialData?: Partial<PetPayload & { imagem_url?: string | null }>;
  onSubmit?: (payload: PetPayload, imagens?: File[], dataFotos?: string) => Promise<void>;
  onCancel?: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
}

export default function PetForm({ mode, initialData, onSubmit, onCancel, onEdit, onDelete }: PetFormProps) {
  const [nome, setNome] = useState(initialData?.nome || '');
  const [especie, setEspecie] = useState(initialData?.especie || '');
  const [raca, setRaca] = useState(initialData?.raca || '');
  const [porte, setPorte] = useState(initialData?.porte || '');
  const [responsavel, setResponsavel] = useState(initialData?.responsavel || '');
  const [dataNascimento, setDataNascimento] = useState(initialData?.data_nascimento || '');
  const [dataInsercaoImg, setDataInsercaoImg] = useState(initialData?.data_insercao_img || '');
  const [detalhes, setDetalhes] = useState(initialData?.detalhes || '');
  const [imagens, setImagens] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const [dataFotos, setDataFotos] = useState('');
  const [loading, setLoading] = useState(false);

  const readOnly = mode === 'view';
  const isEdit = mode === 'edit';
  const isCreate = mode === 'create';

  useEffect(() => () => previews.forEach(URL.revokeObjectURL), [previews]);

  function handleImageChange(event: React.ChangeEvent<HTMLInputElement>) {
    previews.forEach(URL.revokeObjectURL);
    const files = Array.from(event.target.files || []).slice(0, 10);
    setImagens(files);
    setPreviews(files.map(file => URL.createObjectURL(file)));
    if (files.length && !dataFotos) setDataFotos(new Date().toISOString().slice(0, 10));
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!onSubmit || loading) return;
    setLoading(true);
    try {
      await onSubmit({
        nome, especie, responsavel,
        raca: raca || null,
        porte: porte || null,
        data_nascimento: dataNascimento || null,
        data_insercao_img: dataInsercaoImg || null,
        detalhes: detalhes || null,
        imagem_url: initialData?.imagem_url || null,
      }, imagens, dataFotos);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-shell page-shell-narrow">
      <form className="form-card" onSubmit={handleSubmit}>
        <header className="form-heading">
          <span className="eyebrow">{readOnly ? 'Perfil do pet' : 'Cadastro'}</span>
          <h1 id="pet-form-title">{isCreate ? 'Cadastrar pet' : isEdit ? 'Editar pet' : nome}</h1>
          {!readOnly && <p className="page-lead">Preencha os dados para manter o perfil atualizado.</p>}
        </header>

        {readOnly && initialData?.imagem_url && <img className="pet-detail-cover" src={initialData.imagem_url} alt={`Foto principal de ${nome}`} />}

        <section className="form-section">
          <h2>Dados do pet</h2>
          <div className="field-grid">
            <label className="field"><span>Nome <b>*</b></span><input value={nome} onChange={e => setNome(e.target.value)} required readOnly={readOnly} /></label>
            <label className="field"><span>Espécie <b>*</b></span><input value={especie} onChange={e => setEspecie(e.target.value)} required readOnly={readOnly} /></label>
            <label className="field"><span>Raça</span><input value={raca} onChange={e => setRaca(e.target.value)} readOnly={readOnly} /></label>
            <label className="field"><span>Porte</span><input value={porte} onChange={e => setPorte(e.target.value)} readOnly={readOnly} /></label>
            <label className="field"><span>Responsável <b>*</b></span><input value={responsavel} onChange={e => setResponsavel(e.target.value)} required readOnly={readOnly} /></label>
            <label className="field"><span>Data de nascimento</span><input type="date" value={dataNascimento} onChange={e => setDataNascimento(e.target.value)} readOnly={readOnly} /></label>
            <label className="field"><span>Data de inserção da imagem</span><input type="datetime-local" value={dataInsercaoImg} onChange={e => setDataInsercaoImg(e.target.value)} readOnly={readOnly} /></label>
          </div>
          <label className="field field-full"><span>Detalhes</span><textarea value={detalhes} onChange={e => setDetalhes(e.target.value)} readOnly={readOnly} rows={4} /></label>
        </section>

        {!readOnly && <section className="form-section photo-upload-section">
          <div className="section-heading"><div><h2>Fotos</h2><p>Adicione até 10 imagens por envio e escolha a data do registro.</p></div><span className="section-mark" aria-hidden="true">✦</span></div>
          <label className="field"><span>Data das fotos{imagens.length > 0 && <b> *</b>}</span><input type="date" value={dataFotos} onChange={e => setDataFotos(e.target.value)} disabled={loading} required={imagens.length > 0} /></label>
          <label className="upload-dropzone">
            <input className="visually-hidden" type="file" accept="image/*" multiple onChange={handleImageChange} disabled={loading} />
            <span className="upload-icon" aria-hidden="true">↑</span><strong>Selecionar imagens</strong><small>JPG, PNG ou HEIC · até 10 imagens</small>
          </label>
          {previews.length > 0 && <div className="selected-photos"><div className="selected-photo-heading"><strong>{imagens.length} imagem(ns) selecionada(s)</strong><span>A primeira será a capa se o pet ainda não tiver uma.</span></div><div className="selected-photo-grid">{previews.map((src, index) => <figure className="selected-photo" key={`${imagens[index]?.name}-${index}`}><img src={src} alt={`Prévia ${index + 1}: ${imagens[index]?.name}`} /><figcaption>{index === 0 && <span className="cover-badge">Capa inicial</span>}{imagens[index]?.name}</figcaption></figure>)}</div></div>}
        </section>}

        <footer className="form-actions">
          {isCreate && <button className="button button-primary" type="submit" disabled={loading}>{loading ? 'Salvando…' : 'Cadastrar pet'}</button>}
          {isEdit && <button className="button button-primary" type="submit" disabled={loading}>{loading ? 'Salvando…' : 'Salvar alterações'}</button>}
          {readOnly && onEdit && <button className="button button-primary" type="button" onClick={onEdit}>Editar perfil</button>}
          {readOnly && onDelete && <button className="button button-danger" type="button" onClick={onDelete}>Excluir pet</button>}
          {onCancel && <button className="button button-secondary" type="button" onClick={onCancel}>{readOnly ? 'Voltar à lista' : 'Cancelar'}</button>}
        </footer>
      </form>
    </section>
  );
}
