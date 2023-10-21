package javaPreprocessor

import org.junit.Test
import org.junit.Assert.{assertEquals, fail}

import scala.collection.mutable

class PreprocessorTests {

  @Test
  def preprocessTest(): Unit = {
    val origCode =
      """import java.util.List;
        |import java.util.ArrayList;   import java.util.LinkedList;
        |
        |public static void main(String[] args){
        |    Precond.require(args.length >= 1);
        |    var lt = args[0];
        |    if (lt == "LL"){ System.out.println(new LinkedList<>()); } else {
        |        var arrL = new ArrayList<Integer>();
        |        for (int i = 0; i < 10; i++){
        |            arrL.add(i); arrL.add(i+1);
        |            arrL.add(i*2);
        |        }
        |        System.out.println(arrL);
        |    }
        |}""".stripMargin
    end origCode
    val expectedCode =
      """import java.util.List;
        |import java.util.ArrayList;
        |import java.util.LinkedList;
        |
        |public static void main(String[] args){
        |Precond.require(args.length >= 1);
        |var lt = args[0];
        |if (lt == "LL"){
        |System.out.println(new LinkedList<>());
        |}
        |else {
        |var arrL = new ArrayList<Integer>();
        |for (int i = 0;
        |i < 10;
        |i++){
        |arrL.add(i);
        |arrL.add(i+1);
        |arrL.add(i*2);
        |}
        |System.out.println(arrL);
        |}
        |}""".stripMargin
    end expectedCode
    val expectedMapping = Map(
      1 -> 1,
      2 -> 2, 3 -> 2,
      4 -> 3,
      5 -> 4,
      6 -> 5,
      7 -> 6,
      8 -> 7, 9 -> 7, 10 -> 7, 11 -> 7,
      12 -> 8,
      13 -> 9, 14 -> 9, 15 -> 9,
      16 -> 10, 17 -> 10,
      18 -> 11,
      19 -> 12,
      20 -> 13,
      21 -> 14,
      22 -> 15
    )
    val linesB = Seq.newBuilder[String]
    origCode.lines().forEach(linesB.addOne)
    val (actualCode, actualMapping) = (new Preprocessor()).preprocess(linesB.result())
    assertEquals(expectedCode, actualCode)
    assertMapEquals(expectedMapping, actualMapping)
  }

  private def assertMapEquals[T, U](expected: Map[T, U], actual: Map[T, U])(using Ordering[T]): Unit = {
    val errors = mutable.ListBuffer.empty[String]
    for (key <- (expected.keys ++ actual.keys).toSeq.sorted){
      val expValOpt = expected.get(key)
      val actValOpt = actual.get(key)
      (expValOpt, actValOpt) match {
        case (Some(expVal), Some(actVal)) if expVal == actVal => ()
        case (None, None) => ()
        case (Some(expVal), Some(actVal)) =>
          errors.addOne(s"value error for key $key: expected '$expVal', was '$actVal'")
        case (Some(expVal), None) =>
          errors.addOne(s"expected '$key' -> '$expVal' but no mapping for this key")
        case (None, Some(actVal)) =>
          errors.addOne(s"expected no mapping for key $key, but it is mapped to '$actVal'")
      }
    }
    if (errors.nonEmpty){
      fail(errors.mkString("\n", "\n", "\n"))
    }
  }

}
