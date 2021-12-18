

object EnvProps {

  /**
    * Get the property value.
    *
    * The value is obtained either from system properties or environment variables.
    * If both are specified, the system properties is used.
    *
    * @param name
    * @return the value, None if the property is not set as a system property or environment variable.
    */
  def getProp(name: String): Option[String] = {
    sys.props.get(name) match {
      case Some(s) =>
        Some(s)
      case None    =>
        sys.env.get(name)
          .map { s =>
            s
          }.orElse {
            None
          }
    }
  }

  /**
    * Get the property value.
    *
    * The value is obtained either from system properties or environment variables.
    * If both are specified, the system properties is used.
    *
    * @param name
    * @param default
    * @return the value, *default* is returned if the property is not set as a system property or environment variable.
    */
  def getProp(
    name: String,
    default: String
  ): String = {
    getProp(name).getOrElse(default)
  }

  /**
    * Get the property value.
    *
    * The value is obtained either from system properties or environment variables.
    * If both are specified, the system properties is used.
    *
    * @param name
    * @param default
    * @return the boolean value, *default* is returned if the property is not set as a system property or environment variable.
    */
  def getBooleanProp(name: String, default: Boolean): Boolean = {
    getProp(name)
      .map(s => s.equalsIgnoreCase("true") || s.equals("1"))
      .getOrElse(default)
  }

}
