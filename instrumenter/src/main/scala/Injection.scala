package com.epfl.systemf.jumbotrace.instrumenter

import com.epfl.systemf.jumbotrace.instrumenter.TypeDescriptor as TD

object Injection {

  val jumboTracer: ClassName = ClassName("___JumboTracer___")
  
  val lineVisited: MethodName = MethodName("lineVisited")
  val variableSet: MethodName = MethodName("variableSet")
  val returned: MethodName = MethodName("returned")
  val returnedVoid: MethodName = MethodName("returnedVoid")
  val display: MethodName = MethodName("display")
  val writeJsonTrace: MethodName = MethodName("writeJsonTrace")

  val stringIntToVoid: MethodDescriptor = MethodDescriptor(Seq(TD.String, TD.Int), TD.Void)
  val stringObjToVoid: MethodDescriptor = MethodDescriptor(Seq(TD.String, TD.Object), TD.Void)
  val stringToVoid: MethodDescriptor = MethodDescriptor(Seq(TD.String), TD.Void)

}
