package com.carolai.extractor.dto.response;

import java.util.Map;

import com.carolai.extractor.dto.response.types.FirestoreBoolean;
import com.carolai.extractor.dto.response.types.FirestoreString;
import com.carolai.extractor.dto.response.types.FirestoreTimestamp;

public record CustomerResponse(
        FirestoreString ref,
        FirestoreString situacao,
        FirestoreString tipoPerfil,
        FirestoreString nome,
        FirestoreString email,
        FirestoreString idClienteApp,

        FirestoreTimestamp dataCadastro,
        FirestoreTimestamp dataAtualizacao,
        FirestoreTimestamp dataNascimento,
        FirestoreTimestamp dataVencimentoPlano,
        FirestoreTimestamp ultimoAcesso,

        Map<String, Object> telefone,
        FirestoreString telefoneCompleto,

        FirestoreString sexo,
        FirestoreString linkDeLogin,
        FirestoreString urlFotoUser,

        FirestoreString ref_plan,
        FirestoreString charge_id,

        FirestoreBoolean active_subscription,
        FirestoreString status_payment,
        FirestoreString method_payment,
        FirestoreString cpf,

        Map<String, Object> objetivos,

        FirestoreString statusPlanoDeTreino,
        FirestoreString topicNotification,
        FirestoreString versaoAppUltimoAcesso,
        FirestoreString dispositivoUltimoAcesso
) {}