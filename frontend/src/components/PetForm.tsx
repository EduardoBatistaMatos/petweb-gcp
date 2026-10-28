import { useState } from 'react';
import type { PetPayload } from '../services/petService';

interface PetFormProps {
  mode: 'create' | 'view' | 'edit';
  initialData?: Partial<PetPayload & { imagem_url?: string | null }>;
  onSubmit?: (payload: PetPayload, imagem?: File | null) => Promise<void>;
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
  const [imagemUrl] = useState(initialData?.imagem_url || '');
  const [imagem, setImagem] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(initialData?.imagem_url || null);
  const [loading, setLoading] = useState(false);

  const readOnly = mode === 'view';
  const isEdit = mode === 'edit';
  const isCreate = mode === 'create';

  function handleImageChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setImagem(file);
    const url = URL.createObjectURL(file);
    setPreview(url);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!onSubmit) return;
    setLoading(true);
    try {
      const payload: PetPayload = {
        nome,
        especie,
        responsavel,
        raca: raca || null,
        porte: porte || null,
        data_nascimento: dataNascimento || null,
        data_insercao_img: dataInsercaoImg || null,
        detalhes: detalhes || null,
        imagem_url: imagemUrl || null,
      };
      await onSubmit(payload, imagem);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>
      <h1>
        {isCreate && 'Cadastrar Pet'}
        {isEdit && 'Editar Pet'}
        {mode === 'view' && 'Detalhes do Pet'}
      </h1>

      <div style={{ marginBottom: 20 }}>
        {preview && (
          <img
            src={preview}
            alt="Foto do pet"
            style={{ width: '100%', maxWidth: 400, height: 300, objectFit: 'cover', borderRadius: 8 }}
          />
        )}
        {(isEdit || isCreate) && (
          <div style={{ marginTop: 10 }}>
            <input type="file" accept="image/*" onChange={handleImageChange} disabled={loading} />
            {imagem && <button type="button" onClick={() => { setImagem(null); setPreview(imagemUrl || initialData?.imagem_url || null); }}>Remover nova imagem</button>}
          </div>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div>
          <label>Nome *</label>
          <input value={nome} onChange={(e) => setNome(e.target.value)} required readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
        <div>
          <label>Espécie *</label>
          <input value={especie} onChange={(e) => setEspecie(e.target.value)} required readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
        <div>
          <label>Raça</label>
          <input value={raca} onChange={(e) => setRaca(e.target.value)} readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
        <div>
          <label>Porte</label>
          <input value={porte} onChange={(e) => setPorte(e.target.value)} readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
        <div>
          <label>Responsável *</label>
          <input value={responsavel} onChange={(e) => setResponsavel(e.target.value)} required readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
        <div>
          <label>Data de nascimento</label>
          <input type="date" value={dataNascimento} onChange={(e) => setDataNascimento(e.target.value)} readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
        <div>
          <label>Data de inserção da imagem</label>
          <input type="datetime-local" value={dataInsercaoImg} onChange={(e) => setDataInsercaoImg(e.target.value)} readOnly={readOnly} style={{ width: '100%', padding: 8 }} />
        </div>
      </div>

      <div style={{ marginTop: 16 }}>
        <label>Detalhes</label>
        <textarea value={detalhes} onChange={(e) => setDetalhes(e.target.value)} readOnly={readOnly} style={{ width: '100%', padding: 8, minHeight: 80 }} />
      </div>

      <div style={{ marginTop: 24, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        {isCreate && <button type="submit" disabled={loading}>{loading ? 'Salvando...' : 'Cadastrar'}</button>}
        {isEdit && <button type="submit" disabled={loading}>{loading ? 'Salvando...' : 'Salvar'}</button>}
        {mode === 'view' && onEdit && <button type="button" onClick={onEdit}>Editar</button>}
        {mode === 'view' && onDelete && <button type="button" onClick={onDelete} style={{ background: '#d33', color: '#fff' }}>Excluir</button>}
        {onCancel && <button type="button" onClick={onCancel}>Voltar</button>}
      </div>
    </form>
  );
}
