package b2bCompiler

import java.util.concurrent.atomic.AtomicLong

final class AbsIntValue private(val descr: String, val tpe: TypeSignature, stamp: Long){

  override def toString: String = s"$descr ($tpe, stamp=$stamp)"

}

object AbsIntValue {

  final class Creator {
    private val stampGen = new AtomicLong(0)

    def newInstance(descr: String, tpe: RegularTypeSignature): AbsIntValue = {
      AbsIntValue(descr, tpe, stampGen.incrementAndGet())
    }

  }

}
