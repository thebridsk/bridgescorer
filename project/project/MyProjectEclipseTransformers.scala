
import com.typesafe.sbteclipse.core.EclipsePlugin
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseTransformerFactory
import sbt.ProjectRef
import sbt.State
import java.io.File
import scala.language.postfixOps

object MyProjectEclipseTransformers {

// inspired from:
// From  https://groups.google.com/forum/#!topic/scala-js/lh7GrAssOYo
// See here for more info https://gist.github.com/fxthomas/5006558

  /**
   * A transformer that replaces classpathentry elements that match.
   * The classpathentry element must have a "kind" attribute with a value of "src"
   * <code><pre>
   *   EclipseKeys.classpathTransformerFactories ++= Seq(
   *     MyEclipseTransformers.replaceRelativePath("/shared", "/sharedJS")
   *   )
   * </pre></code>
   * @param old the old text in the path attribute
   * @param replace the new text to replace in the path attribute
   */
  def replaceRelativePath(old: String, replace: String) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {
    import scala.xml._
    import scalaz.Scalaz._

//    println("Adding classpathentry transformer for "+old+" -> "+replace )

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = {
//        println("Trying "+n)
        val r = n match {
          case e: Elem if e.label == "classpathentry" &&
                          (e \ "@kind" text) == "src" &&
                          (e \ "@path" text) == old =>
             val x = e % Attribute(null, "path", replace, Null)
//             println("Replacing "+e)
//             println("  With "+x)
             x

          case e => e
        }
        r
      }
    }).success
  }


  /**
   * A transformer that replaces classpathentry elements that match.
   * The classpathentry element must have a "kind" attribute with a value of "src"
   * and the path attribute must end with suffix.
   * Also removes the output attribute.
   * <code><pre>
   *   EclipseKeys.classpathTransformerFactories ++= Seq(
   *     MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-src-main-scala", "shared-src-main-scala")
   *   )
   * </pre></code>
   * @param suffix the suffix in the old name
   * @param replace the new text to replace in the path attribute
   */
  def fixLinkedNameFromClasspath(suffix: String, replace: String) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {
    import scala.xml._
    import scalaz.Scalaz._

//    println("Adding classpath transformer: "+suffix+" -> "+replace )

    // <classpathentry output="C:-git-Scala-ProjectScala-BridgeScorer-shared-src-main-scala" kind="src" path="C:-git-Scala-ProjectScala-BridgeScorer-shared-src-main-scala"/>
    // <classpathentry kind="src" path="C:-git-Scala-ProjectScala-BridgeScorer-shared-src-main-scala"/>
    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = {
        val r = n match {
          case Elem(prefix,label,attrs,scope, /* min, */ children @ _*)
                  if label == "classpathentry" &&
                     attrs.get("kind").map( nodes => nodes.text).getOrElse("") == "src" &&
                     attrs.get("path").map( nodes => nodes.text).getOrElse("").endsWith(suffix)
                =>
            val ch = children
            val x = Elem( prefix,label, attrs.remove("output"), scope, false, ch:_* )
            val x2 = x % Attribute(null, "path", replace, Null)
            x2

          case e => e
        }
        r
      }
    }).success
  }

  /**
   * A transformer that fixes the link name.
   * The name element must have a value of suffix
   * <code><pre>
   *   EclipseKeys.classpathTransformerFactories ++= Seq(
   *     MyEclipseTransformers.fixLinkName("-shared-src-main-scala", "shared-src-main-scala")
   *   )
   * </pre></code>
   * @param suffix the suffix in the old name
   * @param replace the new text to replace in the path attribute
   */
  def fixLinkName(suffix: String, replace: String) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {
    import scala.xml._
    import scalaz.Scalaz._

//    println("Adding project transformer: "+suffix+" -> "+replace )

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = {
        val r = n match {
          case e: Elem if e.label == "name" && e.child.head.text.endsWith(suffix) => {
             val x = <name>{replace}</name>
             x
             }
          case e => e
        }
        r
      }
    }).success
  }

  /**
   * A transformer that removes a classpathentry from the .classpath.
   * The entry has a kind attribute of "src" and the path starts with prefix.
   * @param prefix the prefix that must match the path attribute of the classpathentry to delete.
   */
  def removeRelativePath(prefix: String) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {
    import scala.xml._
    import scalaz.Scalaz._

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = {
        val r = n match {
          case e: Elem if e.label == "classpathentry" &&
                          (e \ "@kind" text) == "src" &&
                          (e \ "@path" text) == prefix
                       =>
            Seq()
          case e => Seq(e)
        }
        r
      }
    }).success
  }

  /**
   * A a linked source, type 2, to the .project file.
   * @linkName the link name, the name as it appears in eclipse
   * @path the File object that represents the target of the link.
   */
  def addLinkedSource(linkName: String, path: File) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {
    import scala.xml._
    import scalaz.Scalaz._

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if e.label == "linkedResources"  => e.copy(child = e.child :+
          <link>
          <name>{linkName}</name>
          <type>2</type>
          <location>{path.getAbsolutePath.replace('\\', '/')}</location>
        </link>
        )
        case e => e
      }
    }).success
  }

  /**
   * add a classpathentry for a project to the classpath in .classpath
   */
  def addDependentProject( projectName: String ) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {

    import scala.xml._
    import scalaz.Scalaz._

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if e.label == "classpath"  => e.copy(child = e.child :+
          <classpathentry combineaccessrules="false" exported="true" kind="src" path={"/"+projectName} />
        )
        case e => e
      }
    }).success
  }

  /**
   * add the project folder as the first entry in the classpath.  The entry only includes scala files.
   * @param folderName
   */
  def addProjectFolderToClasspath() = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {

    import scala.xml._
    import scalaz.Scalaz._

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if e.label == "classpath"  => e.copy(child =
          <classpathentry including="*.scala|com/**/*.scala" kind="src" path=""/> +: e.child
        )
        case e => e
      }
    }).success
  }

  /**
   * add a classpathentry for a folder, the folder should have been added to the .project with addLinkedSouce()
   * @param folderName
   */
  def addDependentRunClassFolder( folderName: String ) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {

    import scala.xml._
    import scalaz.Scalaz._

    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if e.label == "classpath"  => e.copy(child = e.child :+
          <classpathentry kind="lib" path={folderName}/>
        )
        case e => e
      }
    }).success
  }

  /**
   * Remove the output attribute from the classpathentry where the output ends in the specified suffix
   * @param suffix
   */
  def removeOutputFromClasspath(suffix: String) = new EclipseTransformerFactory[scala.xml.transform.RewriteRule] {
    import scala.xml._
    import scalaz.Scalaz._

    // <classpathentry output="C:-git-Scala-ProjectScala-BridgeScorer-shared-src-main-scala" kind="src" path="C:-git-Scala-ProjectScala-BridgeScorer-shared-src-main-scala"/>
    // <classpathentry kind="src" path="C:-git-Scala-ProjectScala-BridgeScorer-shared-src-main-scala"/>
    override def createTransformer(ref: ProjectRef, state: State) = (new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node) = {
        val r = n match {
          case e: Elem if e.label == "classpathentry" &&
                          (e \ "@kind" text) == "src" &&
                          (e \ "@output" text).endsWith(suffix) => {
             val o: String = null
             val x = e % Attribute(null, "output", o, Null)
             x
             }
          case e => e
        }
        r
      }
    }).success
  }

}
