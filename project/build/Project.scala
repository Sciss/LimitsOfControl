import sbt._

class LimitsOfControlProject( info: ProjectInfo ) extends DefaultProject( info ) {
   val jfreechart    = "jfree" % "jfreechart" % "1.0.13"
   val jfreeRepo     = "JBoss Repository" at "http://repository.jboss.org/maven2"
   val itextpdf      = "com.itextpdf" % "itextpdf" % "5.0.4"
   val itextpdfRepo  = "Maven Repository for iText" at "http://maven.itextpdf.com/"
}