-- Defesa-in-depth: Sigilo do Grafo Recursal não pode ser reduzido (downgrade) por UPDATE direto.
-- Objetivo: proteger shadow nodes e heranças de sigilo contra adulteração manual no PostgreSQL.

-- Rank determinístico do enum NivelSigilo (Java) em SQL.
CREATE OR REPLACE FUNCTION pjb_sigilo_rank(sigilo TEXT)
RETURNS INT
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    IF sigilo IS NULL THEN
        RETURN 0;
    END IF;

    CASE upper(trim(sigilo))
        WHEN 'PUBLICO' THEN RETURN 0;
        WHEN 'SEGREDO_JUSTICA' THEN RETURN 1;
        WHEN 'SIGILO_N2' THEN RETURN 2;
        WHEN 'SIGILO_N3' THEN RETURN 3;
        WHEN 'SIGILO_N4' THEN RETURN 4;
        WHEN 'SEGREDO_ESTADO' THEN RETURN 5;
        ELSE
            -- desconhecido: assume alto para evitar downgrade acidental
            RETURN 5;
    END CASE;
END;
$$;

CREATE OR REPLACE FUNCTION pjb_case_proceeding_sigilo_no_downgrade()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    old_rank INT;
    new_rank INT;
BEGIN
    old_rank := pjb_sigilo_rank(OLD.secrecy);
    new_rank := pjb_sigilo_rank(NEW.secrecy);

    IF new_rank < old_rank THEN
        RAISE EXCEPTION 'PJB: sigilo do proceeding não pode ser reduzido (old=%, new=%).', OLD.secrecy, NEW.secrecy
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'tr_case_proceeding_sigilo_no_downgrade'
    ) THEN
        CREATE TRIGGER tr_case_proceeding_sigilo_no_downgrade
        BEFORE UPDATE OF secrecy ON tb_case_proceeding
        FOR EACH ROW
        EXECUTE FUNCTION pjb_case_proceeding_sigilo_no_downgrade();
    END IF;
END
$$;
