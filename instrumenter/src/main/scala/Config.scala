package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.Opcodes

final case class Config(asmVersion: Int)

object Config {
  val current: Config = Config(Opcodes.ASM9)
}
