package com.carolai.extractor.dto.response;

import java.util.Map;

import com.carolai.extractor.dto.response.types.FirestoreInteger;
import com.carolai.extractor.dto.response.types.FirestoreString;
import com.carolai.extractor.dto.response.types.FirestoreTimestamp;

public record FreeTrainingFieldsResponse(
        FirestoreString urlCapa,
        FirestoreString refTreino,
        FirestoreString observacoes,
        FirestoreString nomeTreino,
        FirestoreInteger nota,
        FirestoreTimestamp dataCriacao,
        FirestoreString refPersonalMontou,
        FirestoreString nomePersonalMontou,
        Map<String, Object> gruposExercicios
) {}