package b2bCompiler

import org.junit.Test
import org.junit.Assert.assertEquals

class MethodSignatureTest {

  // inputs are taken/inspired from https://asm.ow2.io/asm4-guide.pdf, page 12

  @Test def parseMethodSigTest1(): Unit = {
    val input = "(IF)V"
    val actual = MethodSignature.parse(input)
    val exp = MethodSignature(Seq(IntT, FloatT), None)
    assertEquals(exp, actual)
  }

  @Test def parseMethodSigTest2(): Unit = {
    val input = "(Ljava/lang/Object;)I"
    val actual = MethodSignature.parse(input)
    val exp = MethodSignature(Seq(ObjectRefT), Some(IntT))
    assertEquals(exp, actual)
  }

  @Test def parseMethodSigTest3(): Unit = {
    val input = "(ILjava/lang/String;)[I"
    val actual = MethodSignature.parse(input)
    val exp = MethodSignature(Seq(IntT, ObjectRefT), Some(ObjectRefT))
    assertEquals(exp, actual)
  }

  @Test def parseMethodSigTest4(): Unit = {
    val input = "(ZBJJI)[Z"
    val actual = MethodSignature.parse(input)
    val exp = MethodSignature(Seq(BooleanT, ByteT, LongT, LongT, IntT), Some(ObjectRefT))
    assertEquals(exp, actual)
  }

}
