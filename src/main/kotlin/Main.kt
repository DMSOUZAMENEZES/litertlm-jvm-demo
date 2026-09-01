import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
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
 *
 * Com um modelo FunctionGemma (nome do arquivo casando com "functiongemma" /
 * "mobile-actions", ou LITERTLM_TOOLS=1), as ferramentas de DemoTools sao
 * registradas e chamadas automaticamente pelo motor (automaticToolCalling).
 */
private const val DEFAULT_SYSTEM_INSTRUCTION =
    "Voce e um assistente de terminal conciso e direto. Responda sempre em " +
        "portugues do Brasil, em no maximo 3 frases, sem rodeios nem listas. " +
        "Quando nao tiver certeza de algo, diga claramente que nao sabe."

/**
 * Ferramentas locais expostas ao modelo via function calling. Cada funcao
 * anotada com @Tool vira uma ferramenta; os parametros usam @ToolParam.
 * Tipos aceitos: String, Int, Boolean, Double, Float, List.
 *
 * Nomes/descricoes em ingles: os modelos FunctionGemma pequenos respondem
 * bem melhor a ferramentas em ingles. Cada chamada e logada em stderr.
 */
class DemoTools : ToolSet {

    @Tool(description = "Get the current local date and time as an ISO-8601 string.")
    fun get_current_time(): String {
        val now = java.time.LocalDateTime.now().withNano(0).toString()
        System.err.println("[tool] get_current_time() -> $now")
        return now
    }

    @Tool(description = "Add two integers and return the sum.")
    fun add_numbers(
        @ToolParam(description = "First integer.") a: Int,
        @ToolParam(description = "Second integer.") b: Int,
    ): Int {
        val r = a + b
        System.err.println("[tool] add_numbers(a=$a, b=$b) -> $r")
        return r
    }

    @Tool(description = "Convert a temperature from Celsius to Fahrenheit.")
    fun celsius_to_fahrenheit(
        @ToolParam(description = "Temperature in degrees Celsius.") celsius: Double,
    ): Double {
        val r = celsius * 9.0 / 5.0 + 32.0
        System.err.println("[tool] celsius_to_fahrenheit(celsius=$celsius) -> $r")
        return r
    }
}

suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: ./gradlew run --args=\"/caminho/absoluto/para/model.litertlm\"")
        println()
        println("Baixe um modelo .litertlm em: https://huggingface.co/litert-community")
        println("Exemplo recomendado para testar: litert-community/Gemma3-1B-IT")
        return
    }

    val modelPath = args[0]

    // Function calling: ligado quando LITERTLM_TOOLS=1, ou automaticamente se o
    // nome do arquivo parecer um modelo FunctionGemma. LITERTLM_TOOLS=0 forca
    // desligar. (Capabilities.supportsFunctionCalling() do .litertlm da
    // litert-community retorna false, entao nao da pra auto-detectar pela API.)
    val toolsEnv = System.getenv("LITERTLM_TOOLS")
    val looksLikeFunctionModel = Regex("functiongemma|function-gemma|mobile.?actions", RegexOption.IGNORE_CASE)
        .containsMatchIn(modelPath)
    val functionCalling = toolsEnv == "1" || (toolsEnv != "0" && looksLikeFunctionModel)
    val tools = if (functionCalling) listOf(tool(DemoTools())) else emptyList()

    // Esconde logs internos verbosos do motor nativo.
    Engine.setNativeMinLogSeverity(LogSeverity.ERROR)

    val engineConfig = EngineConfig(
        modelPath = modelPath,
        backend = Backend.CPU(), // Troque para Backend.GPU() se tiver OpenCL >= 1.2 funcional (nao e o caso deste APU Renoir no Mesa 22.x)
        // Diretorio gravavel opcional; melhora o tempo de carregamento seguinte.
        cacheDir = System.getProperty("java.io.tmpdir"),
    )

    // Personalidade fixa do assistente. LITERTLM_SYSTEM sobrescreve o padrao;
    // LITERTLM_SYSTEM="" (vazio) desliga o systemInstruction. Em modo function
    // calling o padrao NAO e aplicado: o FunctionGemma tem template proprio e a
    // instrucao conversacional atrapalha.
    val systemInstruction = System.getenv("LITERTLM_SYSTEM")
        ?: if (functionCalling) "" else DEFAULT_SYSTEM_INSTRUCTION

    val conversationConfig = if (systemInstruction.isNotBlank()) {
        ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            tools = tools,
            automaticToolCalling = tools.isNotEmpty(),
        )
    } else {
        ConversationConfig(
            tools = tools,
            automaticToolCalling = tools.isNotEmpty(),
        )
    }

    Engine(engineConfig).use { engine ->
        println("Carregando modelo (pode levar ate ~10s)...")
        engine.initialize()
        if (systemInstruction.isNotBlank()) {
            println("System instruction ativo (LITERTLM_SYSTEM para trocar).")
        }
        if (tools.isNotEmpty()) {
            println("Function calling ativo: get_current_time, add_numbers, celsius_to_fahrenheit.")
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
