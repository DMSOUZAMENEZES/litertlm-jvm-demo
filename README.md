# litertlm-jvm-demo

Chat de terminal em Kotlin/JVM que roda um LLM local (`.litertlm`) na CPU
usando a API do [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM),
sem precisar do Bazel. Projeto Gradle mínimo, feito para Linux (funciona
também em macOS/Windows).

## Pré-requisitos

- **JDK 21+** instalado (`java -version` para conferir). O `litertlm-jvm`
  0.17.0-alpha1 é compilado para Java 21 (class file v65); JDK 17 dá
  `UnsupportedClassVersionError`. Via SDKMAN: `sdk install java 21.0.5-tem`.
- Não precisa instalar Gradle manualmente: o wrapper (`./gradlew`) baixa tudo
  sozinho na primeira execução, desde que você tenha internet.
- O Gradle acha o JDK 21 automaticamente se ele foi instalado pelo SDKMAN. Se
  estiver em outro lugar, ajuste `org.gradle.java.installations.paths` no
  `gradle.properties`.

## 1. Baixe um modelo `.litertlm`

Modelos prontos estão no
[Hugging Face LiteRT Community](https://huggingface.co/litert-community).
Para testar rápido, recomendo o
[Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT)
(pequeno, roda bem em CPU).

Baixe o arquivo `.litertlm` e anote o caminho absoluto dele.

## 2. Gere o Gradle Wrapper (uma vez só)

Este projeto não inclui os arquivos binários do wrapper (`gradle-wrapper.jar`).
Se você já tem o Gradle instalado localmente, gere o wrapper assim:

```bash
cd litertlm-jvm-demo
gradle wrapper --gradle-version 8.7
```

Se não tiver o Gradle instalado, instale via [SDKMAN](https://sdkman.io/):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.14.4
sdk install java 21.0.5-tem
```

O wrapper deste projeto já está fixado em Gradle 8.14.4 (mínimo exigido pelo
plugin Kotlin 2.4).

## 3. Rode o chat

```bash
./gradlew run --args="/caminho/absoluto/para/model.litertlm"
```

Ou use o atalho, que aponta para `models/gemma3-1b-it-int4.litertlm` por padrão:

```bash
./run.sh                                  # usa o modelo em models/
./run.sh /caminho/para/outro.litertlm     # ou informe outro
```

A primeira execução demora mais porque o Gradle baixa as dependências
(incluindo `litertlm-jvm` do Google Maven). Depois disso, o carregamento do
modelo em si leva alguns segundos.

## Personalidade do assistente (`systemInstruction`)

O chat cria a conversa com um `ConversationConfig` que carrega um
`systemInstruction` fixo (ver `DEFAULT_SYSTEM_INSTRUCTION` em `Main.kt`):
respostas curtas, em português, admitindo quando não sabe.

Para trocar sem editar o código, use a variável de ambiente `LITERTLM_SYSTEM`:

```bash
LITERTLM_SYSTEM="Responda sempre como um pirata mal-humorado." ./run.sh
LITERTLM_SYSTEM="" ./run.sh          # desliga o systemInstruction
```

Modelos pequenos (Gemma3-1B) seguem instruções de sistema de forma frouxa —
o efeito fica mais evidente em modelos maiores.

## Function calling (`tools`)

`Main.kt` tem uma classe `DemoTools : ToolSet` com três funções `@Tool`
(`get_current_time`, `add_numbers`, `celsius_to_fahrenheit`). Quando o modo
está ativo, elas entram no `ConversationConfig` com `automaticToolCalling = true`
— o motor detecta a chamada, executa a função Kotlin e devolve o resultado ao
modelo sozinho. Cada execução é logada em `stderr` como `[tool] ...`.

O modo liga quando o nome do arquivo do modelo casa com `functiongemma` /
`mobile-actions`, ou com `LITERTLM_TOOLS=1` (e `LITERTLM_TOOLS=0` força
desligar). `Capabilities.supportsFunctionCalling()` retorna `false` nos
`.litertlm` da litert-community, então não dá para auto-detectar pela API.

Precisa de um modelo treinado para isso, por exemplo
[functiongemma-270m-ft-mobile-actions](https://huggingface.co/litert-community/functiongemma-270m-ft-mobile-actions)
(arquivo `mobile_actions_q8_ekv1024.litertlm`, ~289 MB, gated pela licença Gemma):

```bash
./run.sh models/functiongemma-270m-mobile-actions.litertlm
>>> Convert 100 Celsius to Fahrenheit.
[tool] celsius_to_fahrenheit(celsius=100.0) -> 212.0
The temperature 100 Celsius has been converted to Fahrenheit.
```

Esse modelo de 270M é fraco em casar pergunta↔ferramenta (foi afinado para
ações de celular); a integração funciona, a precisão vem do modelo.

## Versões travadas (`build.gradle.kts`)

O `litertlm-jvm` 0.17.0-alpha1 impõe três combinações que precisam bater:

| O quê | Versão | Por quê |
|---|---|---|
| Plugin Kotlin | `2.4.10` | as classes do `litertlm-jvm` têm metadata Kotlin 2.4.x; Kotlin 1.9 não lê |
| JDK (toolchain) | `21` | bytecode Java 21 (class file v65) |
| `kotlinx-coroutines-core` | `1.11.0` | o bytecode chama `SendChannel.close$default` como método **estático de interface**, que só existe a partir do coroutines 1.11.0 (o POM do litertlm pede 1.9.0, que quebra com `NoSuchMethodError` ao terminar a resposta) |
| `kotlin-reflect` | `2.4.10` | `tool()` usa `kotlin.reflect.full.KClasses` para achar as funções `@Tool` do `ToolSet` em runtime |

## Estrutura

- `build.gradle.kts` — dependências e configuração do build
- `settings.gradle.kts` — nome do projeto
- `src/main/kotlin/Main.kt` — o chat de terminal em si

## Próximos passos

- Trocar `Backend.CPU()` por `Backend.GPU()` no `Main.kt` se sua máquina tiver
  GPU compatível (OpenCL).
- Expor `SamplerConfig` (temperature / top-k / top-p) via env ou args.
- Empacotar como distribuível (`installDist` / fat jar) para rodar sem Gradle.
