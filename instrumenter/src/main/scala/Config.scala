package instrumenter

import org.objectweb.asm.Opcodes

final case class Config(asmVersion: Int, transformedClassesDirName: String)

object Config {
  val current: Config = Config(
    asmVersion = Opcodes.ASM9,
    transformedClassesDirName = "jumbotracer-transformed"
  )
}
