package com.carolai.extractor.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carolai.extractor.dto.response.FreeTrainingFieldsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TrainingUtil {

    private static final Logger log = LogManager.getLogger(TrainingUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //Use Test
    public static void logTreino(FreeTrainingFieldsResponse f) {
        String nome = f.nomeTreino() != null ? f.nomeTreino().stringValue() : "n/d";
        String id = f.refTreino() != null ? f.refTreino().stringValue() : "n/d";
        String dataCriacao = f.dataCriacao() != null ? f.dataCriacao().timestampValue() : null;
        String personalId = f.refPersonalMontou() != null ? f.refPersonalMontou().stringValue() : "n/d";
        String nomePersonal = f.nomePersonalMontou() != null ? f.nomePersonalMontou().stringValue() : "n/d";
        // String capa = f.urlCapa() != null ? f.urlCapa().stringValue() : "n/d";
        String nota = f.nota() != null ? f.nota().integerValue() : null;

        log.info("➡ Nome: {}", nome);
        log.info("➡ ID: {}", id);
        log.info("➡ Nota: {}", nota != null ? nota : "n/d");
        log.info("➡ Data criação: {}", dataCriacao != null ? dataCriacao : "n/d");
        log.info("➡ Personal: {}", personalId);
        log.info("➡ Nome personal: {}", nomePersonal);
        // log.info("➡ URL capa: {}", capa);

        JsonNode grupos = objectMapper.valueToTree(f.gruposExercicios());

        if (grupos == null || grupos.isNull()) {
            log.info("❌ Nenhum grupo de exercícios encontrado.");
            return;
        }

        JsonNode gruposLivres = grupos
                .path("mapValue")
                .path("fields")
                .path("gruposLivres")
                .path("arrayValue")
                .path("values");

        if (!gruposLivres.isArray()) {
            log.warn("❌ Estrutura inesperada para gruposLivres");
            return;
        }

        log.info("====================================");
        log.info("📌 GRUPOS DE EXERCÍCIOS");
        log.info("====================================");

        int grupoIndex = 1;

        for (JsonNode grupoNode : gruposLivres) {

            JsonNode fieldsGrupo = grupoNode.path("mapValue").path("fields");

            String refGrupo = getString(fieldsGrupo, "refGrupo");
            String nomeGrupo = getString(fieldsGrupo, "nomeGrupo");

            log.info("➡ Grupo {}: {}  (ref={})", grupoIndex, nomeGrupo, refGrupo);

            // séries
            JsonNode series = fieldsGrupo
                    .path("seriesLivre")
                    .path("arrayValue")
                    .path("values");

            int serieIndex = 1;

            for (JsonNode serieNode : series) {

                JsonNode fieldsSerie = serieNode.path("mapValue").path("fields");

                String refSerie = getString(fieldsSerie, "refSerie");
                log.info("   ▶ Série {} (ref={})", serieIndex, refSerie);

                // exercícios
                JsonNode exercicios = fieldsSerie
                        .path("exerciciosLivre")
                        .path("arrayValue")
                        .path("values");

                int exIndex = 1;

                for (JsonNode exNode : exercicios) {

                    JsonNode fieldsEx = exNode.path("mapValue").path("fields");

                    String nomeExercicio = getString(fieldsEx, "nomeExercicio");
                    String refExercicio = getString(fieldsEx, "refExercicio");
                    // String urlVideo = getString(fieldsEx, "urlVideo");
                    String observacao = getString(fieldsEx, "observacao");

                    log.info("       → Ex {}: {} (ref={})", exIndex, nomeExercicio, refExercicio);
                    // log.info("         🎥 Vídeo: {}", urlVideo);
                    log.info("         📝 Observação: {}", observacao != null ? observacao : "n/d");
                    // --- Campos adicionais da série ---
                    String ordemSerie = getString(fieldsSerie, "ordem");
                    log.info("       Série ordem: {}", ordemSerie != null ? ordemSerie : "n/d");

                    // --- Campos adicionais do exercício ---
                    // String urlCapaEx = getString(fieldsEx, "urlCapa");
                    // String urlPersonalEx = getString(fieldsEx, "urlPersonalMontou");
                    String nomePersonalEx = getString(fieldsEx, "nomePersonalMontou");
                    String livre = getString(fieldsEx, "livre");
                    String emCasa = getString(fieldsEx, "emCasa");
                    String ordemEx = getString(fieldsEx, "ordem");
                    String dataCriado = getString(fieldsEx, "dataCriado");
                    String orientacaoVertical = getString(fieldsEx, "orientacaoVertical");

                    // gruposMusculares (array)
                    JsonNode gruposMuscularesNode = fieldsEx
                            .path("gruposMusculares")
                            .path("arrayValue")
                            .path("values");

                    List<String> gruposMusculares = new ArrayList<>();
                    if (gruposMuscularesNode.isArray()) {
                        for (JsonNode gm : gruposMuscularesNode) {
                            gruposMusculares.add(getString(gm, "stringValue"));
                        }
                    }

                    // modoExecucao (array de objetos)
                    JsonNode modosExecucaoNode = fieldsEx
                            .path("modoExecucao")
                            .path("arrayValue")
                            .path("values");

                    List<String> modosExec = new ArrayList<>();
                    if (modosExecucaoNode.isArray()) {
                        for (JsonNode m : modosExecucaoNode) {
                            JsonNode mFields = m.path("mapValue").path("fields");
                            String tipo = getString(mFields, "tipo");
                            String valor = getString(mFields, "valor");
                            String unidade = getString(mFields, "unidadeMedidaDistancia");
                            modosExec.add(tipo + " = " + valor + " " + (unidade != null ? unidade : ""));
                        }
                    }

                    // imprime tudo
                    // log.info("         🖼 Capa exercício: {}", urlCapaEx);
                    log.info("         🧑‍🏫 Personal montou: {}", nomePersonalEx);
                    // log.info("         🔗 URL Personal: {}", urlPersonalEx);
                    log.info("         🏠 Em casa? {}", emCasa);
                    log.info("         🔓 Livre? {}", livre);
                    log.info("         🔢 Ordem: {}", ordemEx);
                    log.info("         📅 Criado em: {}", dataCriado);
                    log.info("         📐 Vertical? {}", orientacaoVertical);
                    log.info("         💪 Grupos musculares: {}", gruposMusculares);
                    log.info("         ⚙ Modo Execução: {}", modosExec);

                    exIndex++;
                }

                serieIndex++;
            }

            grupoIndex++;
        }

        log.info("====================================");
    }

    private static String getString(JsonNode node, String field) {
        JsonNode v = node.path(field);

        if (v.has("stringValue")) return v.get("stringValue").asText();
        if (v.has("integerValue")) return v.get("integerValue").asText();
        if (v.has("booleanValue")) return String.valueOf(v.get("booleanValue").asBoolean());
        if (v.has("nullValue")) return null;

        return "n/d";
    }
}
