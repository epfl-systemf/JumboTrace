package instrumenter

import org.junit.Test

import org.junit.Assert.assertEquals

import instrumenter.TypeDescriptor as TD
import instrumenter.MethodDescriptor as MD
import MD.==>

class DescriptorsTests {

  @Test def parseLongTest(): Unit = {
    assertEquals(TD.Long, TD.parse("J"))
  }

  @Test def parseStringTest(): Unit = {
    assertEquals(TD.String, TD.parse("Ljava/lang/String;"))
  }

  @Test def parseCustomTypeTest(): Unit = {
    assertEquals(TD.Class(Seq("foo"), "Bar"), TD.parse("Lfoo/Bar;"))
  }

  @Test def parseArrayTypeTest(): Unit = {
    assertEquals(TD.Array(TD.Int), TD.parse("[I"))
  }

  @Test def parseRefArrayTypeTest(): Unit = {
    assertEquals(TD.Array(TD.Object), TD.parse("[Ljava/lang/Object;"))
  }

  @Test def parseArrayOfArrayTypeTest(): Unit = {
    assertEquals(TD.Array(TD.Array(TD.Int)), TD.parse("[[I"))
  }

  @Test def parseVoidMethodTest(): Unit = {
    assertEquals(Seq.empty ==> TD.Void, MD.parse("()V"))
  }

  @Test def parseArrayToBooleanMethodTest(): Unit = {
    assertEquals(Seq(TD.Array(TD.Int)) ==> TD.Boolean, MD.parse("([I)Z"))
  }

  @Test def parseStringArrayFooToArrayMethodTest(): Unit = {
    assertEquals(
      Seq(TD.String, TD.Array(TD.Double), TD.Class(Seq.empty, "Foo")) ==> TD.Array(TD.String),
      MD.parse("(Ljava/lang/String;[DLFoo;)[Ljava/lang/String;")
    )
  }

  @Test def parseArrayOfArrayToArray1(): Unit = {
    assertEquals(
      Seq(TD.Array(TD.Array(TD.Int))) ==> TD.Array(TD.Int),
      MD.parse("([[I)[I")
    )
  }

  @Test def parseArrayOfArrayToArray2(): Unit = {
    assertEquals(
      Seq(TD.Float, TD.Array(TD.Array(TD.String))) ==> TD.Array(TD.Object),
      MD.parse("(F[[Ljava/lang/String;)[Ljava/lang/Object;")
    )
  }

}
