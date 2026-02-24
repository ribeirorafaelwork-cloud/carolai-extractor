package com.carolai.extractor.browser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.carolai.extractor.config.CredentialsProperties;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Keyboard;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

@Component
public class PersonalFitSession {

    private static final Logger log = LogManager.getLogger(PersonalFitSession.class);

    private static final String HOME_URL = "https://web.apppersonalfit.com.br";

    private final CredentialsProperties credentials;

    private final Pattern BIBLIOTECA_PATTERN = Pattern.compile("Biblioteca|Library", Pattern.CASE_INSENSITIVE);

    @Autowired
    public PersonalFitSession(CredentialsProperties credentials) {
        this.credentials = credentials;
    }

    public void login(BrowserContext context) throws IOException {
        Page page = context.newPage();

        page.onResponse(response -> {
            String url = response.url();

            if (!url.contains("google.firestore.v1.Firestore/Listen/channel"))
                return;

            log.info("📡 Firestore Listen → {}", url);

            try {
                byte[] bytes = null;

                try {
                    bytes = response.body();
                } catch (Exception ignore) {
                    return;
                }

                if (bytes == null || bytes.length == 0) {
                    return;
                }

                String body = new String(bytes, StandardCharsets.UTF_8);

                log.info("📨 Payload bruto:\n{}", body);

                if (body.contains("\"documentChange\"")) {
                    log.info("🔥🔥🔥 TREINO DETECTADO!");
                }

            } catch (Exception e) {
                log.error("Erro lendo mensagem do Firestore", e);
            }
        });

        log.info("🌐 Acessando PersonalFit Web...");
        page.navigate(HOME_URL);

        log.info("⏳ Esperando o Flutter carregar a interface...");
        page.waitForTimeout(5000);

        log.info("🟦 Tentando habilitar semantics clicando no placeholder...");
        page.evaluate("""
            () => {
            const el = document.querySelector("flt-semantics-placeholder");
            if (el) {
                console.log("JS: Clique no placeholder!");
                el.click();
            } else {
                console.log("JS: flt-semantics-placeholder NÃO encontrado.");
            }
            }
        """);

        Locator acessarConta = page.locator(
            "flt-semantics[role='button']",
            new Page.LocatorOptions()
                .setHasText(Pattern.compile("Access.*account", Pattern.CASE_INSENSITIVE))
        );

        acessarConta.waitFor(new Locator.WaitForOptions()
                .setTimeout(20000)
                .setState(WaitForSelectorState.VISIBLE));

        log.info("👆 Clicando no botão 'Access my account'...");
        acessarConta.click();

        log.info("✅ Clique realizado com sucesso!");

        log.info("📧 Aguardando botão 'Login by email'...");

        Locator acessarEmail = page.locator(
            "flt-semantics[role='button']",
            new Page.LocatorOptions().setHasText(Pattern.compile("Login by email", Pattern.CASE_INSENSITIVE))
        );

        acessarEmail.waitFor(new Locator.WaitForOptions()
                .setTimeout(15000)
                .setState(WaitForSelectorState.VISIBLE));

        log.info("📧 Clicando em 'Login by email'...");
        acessarEmail.click();

        log.info("📧 Aguardando campo de email aparecer...");

        Locator campoEmail = page.locator("textarea[data-semantics-role='text-field']");

        campoEmail.waitFor(new Locator.WaitForOptions()
                .setTimeout(15000)
                .setState(WaitForSelectorState.VISIBLE));

        log.info("🖱️ Clicando no campo de email");
        campoEmail.click();

        log.info("⌨️ Digitando email...");
        page.keyboard().type(credentials.getEmail(), new Keyboard.TypeOptions().setDelay(40));

        Locator campoSenha = page.locator("input[type='password'][data-semantics-role='text-field']");

        campoSenha.waitFor(new Locator.WaitForOptions()
                .setTimeout(15000)
                .setState(WaitForSelectorState.VISIBLE));

        log.info("🖱️ Clicando no campo de senha");
        campoSenha.click();
        campoSenha.click();
        campoSenha.click();

        log.info("⌨️ Digitando senha...");
        page.keyboard().type(credentials.getPassword(), new Keyboard.TypeOptions().setDelay(40));

        log.info("🔍 Monitorando escrita da senha...");

        page.waitForCondition(() -> {
            String valorDigitado = campoSenha.inputValue();
            boolean igual = credentials.getPassword().equals(valorDigitado);

            log.info("👉 Senha esperada: [" + credentials.getPassword() + "]");
            log.info("👉 Senha no campo: [" + valorDigitado + "]");
            log.info("👉 Iguais? " + igual);

            return igual;
        }, new Page.WaitForConditionOptions().setTimeout(15000));


        log.info("⏳ Aguardando botão 'Continuar' habilitar...");

        Locator botaoContinuar = page.locator(
    "flt-semantics[role='button']",
            new Page.LocatorOptions().setHasText(Pattern.compile("\\b(Continuar|Prosseguir|Confirmar|Enviar|Avançar|Proximo|Próximo|OK|Seguir|"
               + "Continue|Next|Submit|Proceed)\\b",
               Pattern.CASE_INSENSITIVE))
        ).first();

        botaoContinuar.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(15000));

        log.info("⏳ Aguardando botão habilitar (aria-disabled desaparecer)…");

        page.waitForCondition(() -> {
            String disabled = botaoContinuar.getAttribute("aria-disabled");
            return disabled == null;
        }, new Page.WaitForConditionOptions().setTimeout(15000));

        log.info("✅ Botão habilitado! Clicando…");
        botaoContinuar.click();

        // log.info("🔎 Listando todos os <flt-semantics> encontrados na página...");

        // Locator allSemantics = page.locator("flt-semantics");
        // int count = allSemantics.count();

        // log.info("📄 Total de flt-semantics encontrados: " + count);

        // for (int i = 0; i < count; i++) {
        //     Locator element = allSemantics.nth(i);

        //     String html = (String) element.evaluate("el => el.outerHTML");
        //     String text = element.innerText();

        //     log.info("----- <flt-semantics> #" + i + " -----");
        //     log.info("HTML: " + html);
        //     log.info("Texto detectado: '" + text + "'");
        // }


        log.info("🔍 Verificando se login foi concluído…");
        Locator bibliotecaBtn = page.locator(
            "flt-semantics[role='button']",
            new Page.LocatorOptions().setHasText(BIBLIOTECA_PATTERN)
        );

        page.waitForTimeout(10000);

        bibliotecaBtn.waitFor(new Locator.WaitForOptions()
                .setTimeout(15000)
                .setState(WaitForSelectorState.VISIBLE));

        log.info("✅ Login confirmado! Botão 'Biblioteca' encontrado.");

        log.info("📚 Clicando em 'Biblioteca'...");
        bibliotecaBtn.click();
    }
}