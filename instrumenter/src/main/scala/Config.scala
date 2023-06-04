package instrumenter

import org.objectweb.asm.Opcodes

/**
 * Container for global program configuration
 * @param asmVersion version of the ASM API used in this project
 * @param transformedClassesDirName name of the directory where the instrumented bytecode should be written
 */
final case class Config(asmVersion: Int, transformedClassesDirName: String)

object Config {
  val config: Config = Config(
    asmVersion = Opcodes.ASM9,
    transformedClassesDirName = "jumbotracer-transformed"
  )
}
