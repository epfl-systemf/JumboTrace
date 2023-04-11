import kotlin.system.exitProcess

enum class ExitCode(val code: Int) {
    CmdArgsError(-2),
    SrcParseError(-3);

    fun exit(): Nothing {
        exitProcess(code)
    }

}
