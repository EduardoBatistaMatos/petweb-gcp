-- Migração manual: mantém todos os metadados em public.pet.
-- A API não executa este arquivo. Faça backup antes de aplicá-lo.
BEGIN;

ALTER TABLE public.pet
    ADD COLUMN IF NOT EXISTS fotos JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS img_principal TEXT;

-- Copia as fotos da tabela antiga para o JSONB antes de removê-la.
DO $migration$
BEGIN
    IF to_regclass('public.pet_foto') IS NOT NULL THEN
        EXECUTE $copy$
            WITH fotos_antigas AS (
                SELECT pet_id,
                       jsonb_agg(jsonb_build_object(
                           'id', id::text,
                           'pet_id', pet_id,
                           'url', url,
                           'data_foto', data_foto,
                           'img_principal', img_principal
                       ) ORDER BY data_foto, id) AS fotos,
                       (array_agg(url ORDER BY id) FILTER (WHERE img_principal))[1] AS capa
                  FROM public.pet_foto
                 GROUP BY pet_id
            )
            UPDATE public.pet p
               SET fotos = COALESCE(p.fotos, '[]'::jsonb) || f.fotos,
                   img_principal = COALESCE(f.capa, p.img_principal)
              FROM fotos_antigas f
             WHERE p.id = f.pet_id
        $copy$;

        EXECUTE 'DROP TABLE public.pet_foto';
    END IF;
END
$migration$;

-- Preserva a imagem única legada como uma foto principal se ela ainda não
-- estiver representada na nova coleção. imagem_url é mantida para o CRUD antigo.
DO $legacy$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema='public' AND table_name='pet' AND column_name='imagem_url'
    ) THEN
        EXECUTE $copy$
            UPDATE public.pet
               SET fotos = jsonb_build_array(jsonb_build_object(
                       'id', 'legacy-' || id::text,
                       'pet_id', id,
                       'url', imagem_url,
                       'data_foto', COALESCE(data_insercao_img::date, CURRENT_DATE),
                       'img_principal', true
                   )),
                   img_principal = COALESCE(img_principal, imagem_url)
             WHERE imagem_url IS NOT NULL
               AND imagem_url <> ''
               AND fotos = '[]'::jsonb
        $copy$;
    END IF;
END
$legacy$;

COMMIT;
