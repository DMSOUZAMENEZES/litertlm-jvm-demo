import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.flow.collect

/**
 * Chat simples de terminal usando o LiteRT-LM (Kotlin/JVM) rodando 100% local
 * na sua maquina Linux (ou macOS/Windows), sem precisar de servidor/internet
 * depois que o modelo estiver baixado.
 *
 * Uso:
 *   ./gradlew run --args="/caminho/absoluto/para/model.litertlm"
 *
 * A "personalidade" fixa do assistente vem do systemInstruction abaixo. Para
 * trocar sem editar o codigo, defina a variavel de ambiente LITERTLM_SYSTEM.
 */
private const val DEFAULT_SYSTEM_INSTRUCTION =
    "Voce e um assistente de terminal conciso e direto. Responda sempre em " +
        "portugues do Brasil, em no maximo 3 frases, sem rodeios nem listas. " +
        "Quando nao tiver certeza de algo, diga claramente que nao sabe."

suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: ./gradlew run --args=\"/caminho/absoluto/para/model.litertlm\"")
        println()
        println("Baixe um modelo .litertlm em: https://huggingface.co/litert-community")
        println("Exemplo recomendado para testar: litert-community/Gemma3-1B-IT")
        return
    }

    val modelPath = args[0]

    // Esconde logs internos verbosos do motor nativo.
    Engine.setNativeMinLogSeverity(LogSeverity.ERROR)

    val engineConfig = EngineConfig(
        modelPath = modelPath,
        backend = Backend.CPU(), // Troque para Backend.GPU() se tiver OpenCL >= 1.2 funcional (nao e o caso deste APU Renoir no Mesa 22.x)
        // Diretorio gravavel opcional; melhora o tempo de carregamento seguinte.
        cacheDir = System.getProperty("java.io.tmpdir"),
    )

    // Personalidade fixa do assistente. LITERTLM_SYSTEM sobrescreve o padrao;
    // LITERTLM_SYSTEM="" (vazio) desliga o systemInstruction.
    val systemInstruction = System.getenv("LITERTLM_SYSTEM") ?: DEFAULT_SYSTEM_INSTRUCTION
    val conversationConfig = if (systemInstruction.isNotBlank()) {
        ConversationConfig(systemInstruction = Contents.of(systemInstruction))
    } else {
        ConversationConfig()
    }

    Engine(engineConfig).use { engine ->
        println("Carregando modelo (pode levar ate ~10s)...")
        engine.initialize()
        if (systemInstruction.isNotBlank()) {
            println("System instruction ativo (LITERTLM_SYSTEM para trocar).")
        }
        println("Pronto! Digite sua mensagem (Ctrl+C para sair).")

        engine.createConversation(conversationConfig).use { conversation ->
            while (true) {
                print("\n>>> ")
                val input = readlnOrNull() ?: break
                if (input.isBlank()) continue

                conversation.sendMessageAsync(input).collect { print(it) }
            }
        }
    }
}
