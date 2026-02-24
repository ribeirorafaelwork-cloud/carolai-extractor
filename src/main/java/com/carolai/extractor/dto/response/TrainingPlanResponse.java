package com.carolai.extractor.dto.response;

import java.util.Map;

import com.carolai.extractor.dto.response.types.FirestoreBoolean;
import com.carolai.extractor.dto.response.types.FirestoreInteger;
import com.carolai.extractor.dto.response.types.FirestoreString;

public record TrainingPlanResponse(
        FirestoreString refPlanoTreino,
        FirestoreString nomePlano, 
        FirestoreString refAluno,
        FirestoreInteger execucaoSemana,
        FirestoreInteger dataInicioLong,
        FirestoreInteger dataTerminoLong,
        FirestoreInteger treinosPrevistos,
        FirestoreBoolean planoTreinoAtivo,
        Map<String, Object> objetivos
) {}