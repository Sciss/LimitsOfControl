import sbt._

class LimitsOfControlProject( info: ProjectInfo ) extends DefaultProject( info ) {
   val jfreechart = "jfree" % "jfreechart" % "1.0.13"
   val jbossRepo = "JBoss Repository" at "http://repository.jboss.org" // XXX correct? can't (don't want to) check as jfreechart is in my local cache
}