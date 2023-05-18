package com.epfl.systemf.jumbotrace.instrumenter

final case class ClassName(name: String) extends AnyVal {
  override def toString: String = name
}

final case class MethodName(name: String) extends AnyVal {
  override def toString: String = name
}
