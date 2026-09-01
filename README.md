# litertlm-jvm-demo

Projeto Gradle mínimo para rodar um LLM local na sua máquina Linux usando a
API Kotlin/JVM do [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM),
sem precisar do Bazel.

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

## Versões travadas (`build.gradle.kts`)

O `litertlm-jvm` 0.17.0-alpha1 impõe três combinações que precisam bater:

| O quê | Versão | Por quê |
|---|---|---|
| Plugin Kotlin | `2.4.10` | as classes do `litertlm-jvm` têm metadata Kotlin 2.4.x; Kotlin 1.9 não lê |
| JDK (toolchain) | `21` | bytecode Java 21 (class file v65) |
| `kotlinx-coroutines-core` | `1.11.0` | o bytecode chama `SendChannel.close$default` como método **estático de interface**, que só existe a partir do coroutines 1.11.0 (o POM do litertlm pede 1.9.0, que quebra com `NoSuchMethodError` ao terminar a resposta) |

## Estrutura

- `build.gradle.kts` — dependências e configuração do build
- `settings.gradle.kts` — nome do projeto
- `src/main/kotlin/Main.kt` — o chat de terminal em si

## Próximos passos

- Trocar `Backend.CPU()` por `Backend.GPU()` no `Main.kt` se sua máquina tiver
  GPU compatível (OpenCL).
- Adicionar `ConversationConfig` com `systemInstruction` para dar uma
  personalidade fixa ao assistente.
- Registrar `tools` (function calling) se usar um modelo compatível, como o
  FunctionGemma.
