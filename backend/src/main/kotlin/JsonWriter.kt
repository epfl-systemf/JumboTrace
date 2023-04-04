import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

object JsonWriter {

    fun write(path: Path, trace: Trace){
        val json = Json { prettyPrint = true }
        Files.createDirectories(path.parent)
        if (!path.exists()){
            Files.createFile(path)
        }
        path.toFile().writeText(json.encodeToString(trace))
    }

}
