package com.github.thebridsk.bridge.fullserver.test.selenium

class PrivateMethodCaller(x: AnyRef, methodName: String) {
  def apply(_args: Any*): Any = {
    val args = _args.map(_.asInstanceOf[AnyRef])
    import scala.language.existentials
    def _parents: LazyList[Class[_]] =
      LazyList(x.getClass) #::: _parents.map(_.getSuperclass)

    val parents = _parents.takeWhile(_ != null).toList
    val methods = parents.flatMap(_.getDeclaredMethods)
    val method = methods
      .find(_.getName == methodName)
      .getOrElse(
        throw new IllegalArgumentException(
          "Method " + methodName + " not found"
        )
      )
    method.setAccessible(true)
    try {
      method.invoke(x, args: _*)
    } finally {
      method.setAccessible(false)
    }

  }
}

class PrivateMethodExposer(x: AnyRef) {
  def apply(method: scala.Symbol): PrivateMethodCaller =
    new PrivateMethodCaller(x, method.name)
}
