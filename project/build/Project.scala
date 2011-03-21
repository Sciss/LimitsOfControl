import sbt._

class LimitsOfControlProject( info: ProjectInfo ) extends DefaultProject( info ) {
   val jfreechart    = "jfree" % "jfreechart" % "1.0.13"
//   val jbossRepo   = "JBoss Repository" at "http://repository.jboss.org" // XXX correct? can't (don't want to) check as jfreechart is in my local cache
   val itextpdf      = "com.itextpdf" % "itextpdf" % "5.0.4"
   val itextpdfRepo  = "Maven Repository for iText" at "http://maven.itextpdf.com/"
}