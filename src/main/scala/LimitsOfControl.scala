import io.Source
import java.awt.{BorderLayout, GradientPaint, Color, EventQueue}
import java.text.{SimpleDateFormat, DateFormat}
import java.util.{Locale, Calendar, TimeZone, Date}
import javax.swing.{WindowConstants, JFrame}
import org.jfree.chart.axis.SubCategoryAxis
import org.jfree.chart.plot.{CategoryPlot, PlotOrientation}
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer
import org.jfree.chart.{ChartPanel, JFreeChart, ChartFactory}
import org.jfree.data.category.{CategoryDataset, DefaultCategoryDataset}
import collection.breakOut
import org.jfree.data.KeyToGroupMap
import org.jfree.ui.{StandardGradientPaintTransformer, GradientPaintTransformType}

/**
 * (C)opyright 2011 Hanns Holger Rutz. All rights reserved.
 * All code provided under the GNU General Public License v2.
 */
object LimitsOfControl extends Runnable {
   def main( args: Array[ String ]) {
      EventQueue.invokeLater( this )
   }

   def run {
      val ln   = Source.fromFile( getClass().getResource( "GitActivity.txt" ).toURI, "UTF-8" ).getLines
      val ps   = readAll( ln )
      // p.size
      process( ps )
   }

   def process( ps: Seq[ Project ]) {
      showChart( createChart1( createCatSet1( ps )))
   }

   def createCatSet1( ps: Seq[ Project ]) : CategoryDataset = {
      val set  = new DefaultCategoryDataset()
      val p    = ps.find( _.name == "Dissemination" ).get
      val cal  = Calendar.getInstance( TimeZone.getTimeZone( "BST" ))
      val grouped = p.commits.groupBy( c => {
         cal.setTime( c.date )
         cal.get( Calendar.DAY_OF_YEAR  )
      })
      val mapped: Map[ Int, Seq[ Edit ]] = grouped.map( tup => tup._1 -> tup._2.flatMap( _.activities.flatMap( _.edits )))( breakOut )
      val dfmt = new SimpleDateFormat( "MMM dd", Locale.UK )
      val sorted = mapped.toList.sortBy( _._1 )
      sorted.foreach { case (day, edits) =>
         cal.set( Calendar.DAY_OF_YEAR, day )
         val eg: Map[ EditSourceType, Map[ EditType, Int ]] = edits.groupBy( _.source.tpe ).map( tup => tup._1 -> tup._2.groupBy( _.tpe ).map( tup => tup._1 -> tup._2.foldLeft(0)( (cnt, ed) => cnt + ed.numLines )))
//         eg.foreach { case (src, eg1) =>
//            eg1.foreach { case (tpe, num) =>
//               set.addValue( num, EditCategory( src, tpe ), dfmt.format( cal.getTime() ))
//            }
//         }
         List( EditSourceNew, EditSourceOther, EditSourceSelf ).foreach { src =>
            val m = eg.getOrElse( src, Map.empty )
            List( EditInsert, EditModify, EditDelete ).foreach { tpe =>
               val num = m.getOrElse( tpe, 0 )
               set.addValue( num, EditCategory( src, tpe ), dfmt.format( cal.getTime() ))
            }
         }
      }
      set
   }

   def createChart1( set: CategoryDataset ) : JFreeChart = {
      val chart = ChartFactory.createStackedBarChart(
         "Title", "Category", "LOC", set,
         PlotOrientation.VERTICAL, true, false, false )

      val renderer = new GroupedStackedBarRenderer()
      val map = new KeyToGroupMap( "G1" )
      List( EditSourceNew, EditSourceOther, EditSourceSelf ).zipWithIndex.foreach { case (src, i) =>
         val group = "G" + (i+1)
         List( EditInsert, EditModify, EditDelete ).foreach { tpe =>
            map.mapKeyToGroup( EditCategory( src, tpe ), group )
         }
      }
      renderer.setSeriesToGroupMap( map )

      renderer.setItemMargin( 0.10 )
      renderer.setDrawBarOutline( false )
      val p1 = new Color( 0, 0xA0, 0 ) // new GradientPaint( 0.0f, 0.0f, new Color(0x22, 0x22, 0xFF),  0.0f, 0.0f, new Color( 0x88, 0x88, 0xFF ))
      renderer.setSeriesPaint(  0, p1 )
      renderer.setSeriesPaint(  3, p1 )
      renderer.setSeriesPaint(  6, p1 )

      val p2 = new Color( 0xC0, 0xA0, 0 ) // new GradientPaint( 0.0f, 0.0f, new Color( 0x22, 0xFF, 0x22), 0.0f, 0.0f, new Color( 0x88, 0xFF, 0x88 ))
      renderer.setSeriesPaint(  1, p2 )
      renderer.setSeriesPaint(  4, p2 )
      renderer.setSeriesPaint(  7, p2 )

      val p3 = Color.red // new GradientPaint( 0.0f, 0.0f, new Color( 0xFF, 0x22, 0x22 ), 0.0f, 0.0f, new Color( 0xFF, 0x88, 0x88 ))
      renderer.setSeriesPaint(  2, p3 )
      renderer.setSeriesPaint(  5, p3 )
      renderer.setSeriesPaint(  8, p3 )

//      val p4 = Color.yellow // new GradientPaint( 0.0f, 0.0f, new Color( 0xFF, 0xFF, 0x22 ), 0.0f, 0.0f, new Color( 0xFF, 0xFF, 0x88 ))
//      renderer.setSeriesPaint(  3, p4 )
//      renderer.setSeriesPaint(  7, p4 )
//      renderer.setSeriesPaint( 11, p4 )

      renderer.setGradientPaintTransformer( new StandardGradientPaintTransformer( GradientPaintTransformType.HORIZONTAL ))
//      renderer.setLegendItemLabelGenerator( new SOCRCategorySeriesLabelGenerator() )

      val domainAxis = new SubCategoryAxis( "XXX" )
      domainAxis.setCategoryMargin( 0.05 )
      domainAxis.addSubCategory( EditSourceNew.toString )
      domainAxis.addSubCategory( EditSourceOther.toString )
      domainAxis.addSubCategory( EditSourceSelf.toString )

      val plot = chart.getPlot().asInstanceOf[ CategoryPlot ]
      plot.setDomainAxis( domainAxis )
//      plot.setDomainAxisLocation( AxisLocation.TOP_OR_RIGHT )
      plot.setRenderer(renderer);
//      plot.setFixedLegendItems( createLegendItems() )

      // setCategorySummary(dataset);

      chart
   }

   def showChart( chart: JFreeChart ) {
      val f = new JFrame( "Chart" )
      val p = new ChartPanel( chart )
      f.getContentPane().add( p, BorderLayout.CENTER )
      f.pack()
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setLocationRelativeTo( null )
      f.setVisible( true )
   }

   sealed trait EditSourceType extends Comparable[ EditSourceType ] {
      def id: Int
      def compareTo( b: EditSourceType ) = id.compareTo( b.id )
   }
   case object EditSourceNew extends EditSourceType {   def id = 0; override def toString = "New" }
   case object EditSourceOther extends EditSourceType { def id = 1; override def toString = "Other" }
   case object EditSourceSelf extends EditSourceType {  def id = 2; override def toString = "Self" }

   sealed trait EditSource { def tpe: EditSourceType }
   case object NewSource extends EditSource { def tpe = EditSourceNew }
   case class SelfSource( clazz: String ) extends EditSource { def tpe = EditSourceSelf }
   case object OtherSource extends EditSource { def tpe = EditSourceOther }

   sealed trait EditType extends Comparable[ EditType ] {
      def id: Int
      def compareTo( b: EditType ) = id.compareTo( b.id )
   }
   case object EditInsert extends EditType { def id = 0; override def toString = "Add" }
   case object EditModify extends EditType { def id = 1; override def toString = "Modify" }
   case object EditDelete extends EditType { def id = 2; override def toString = "Delete" }

   case class EditCategory( source: EditSourceType, tpe: EditType ) extends Comparable[ EditCategory ] {
      def compareTo( c: EditCategory ) : Int = {
         val c1 = source.compareTo( c.source )
         if( c1 != 0 ) c1 else tpe.compareTo( c.tpe )
      }
      override def toString = "(" + source.toString + ", " + tpe.toString + ")"
   }

   case class Edit( source: EditSource, tpe: EditType, numLines: Int )

   case class Activity( name: String, edits: Seq[ Edit ])

   case class Commit( id: String, date: Date, msg: String, activities: Seq[ Activity ])

   case class Project( name: String, commits: Seq[ Commit ])

   def strip( l: String ) : String = {
      val cm = l.indexOf( "//" )
      val l0 = if( cm < 0 ) l else l.substring( 0, cm )
      l0.dropWhile( _ == ' ' ).reverse.dropWhile( _ == ' ' ).reverse
   }

   def strip1( l: String ) : String = {
      l.dropWhile( _ == ' ' ).reverse.dropWhile( _ == ' ' ).reverse
   }

   def readActivity( name: String, ln: Iterator[String]) : (String, Activity) = {
       var nextL = strip( ln.next )
       var doneAct = false
       var edits = Seq.empty[ Edit ]
       while( !(nextL.startsWith( "Type:" ) || nextL.isEmpty) ) {
           val i = nextL.indexOf( ' ' )
           val num = nextL.substring( 0, i ).toInt
           val l0 = strip( nextL.substring( i ))
           val tpe = if( l0.contains( "edited" )) EditModify // ( num )
           else if( l0.contains( "added" ))       EditInsert // ( num )
           else if( l0.contains( "deleted" ))     EditDelete // ( num )
           else error( "Illegal edit type " + l0 )
           val src = if( l0.contains( "from other project" )) OtherSource else
           if( l0.contains( "from self" )) SelfSource( strip( l0.substring( l0.indexOf( "from self" ) + 9 )))
           else NewSource
           edits :+= Edit( src, tpe, num )
           nextL = strip( ln.next )
       }
       (nextL, Activity( name, edits ))
   }

   def readCommit( id: String, ln: Iterator[String]) : Commit = {
       var dO = Option.empty[ Date ]
       while( ln.hasNext && dO.isEmpty ) {
           val l = strip( ln.next )
           if( l.startsWith( "Date:" )) dO = Some( new Date( strip( l.substring( 5 ))))
           else require( l.isEmpty, "Illegal line " + l )
       }
       val d = dO.getOrElse( error( "EOF" ))
       var mO = Option.empty[ String ]
       while( ln.hasNext && mO.isEmpty ) {
           val l = strip( ln.next )
           if( l.startsWith( "Msg:" )) mO = Some( strip( l.substring( 4 )))
           else require( l.isEmpty, "Illegal line " + l )
       }
       val m = mO.getOrElse( error( "EOF" ))
       var activities = Seq.empty[ Activity ]
       var doneAct = false
       var nextL = strip( ln.next )
       while( !doneAct ) {
           while( nextL.startsWith( "Type:" )) {
               val (l, act) = readActivity( strip( nextL.substring( 5 )), ln )
               activities :+= act
               nextL = l
           }
           require( nextL.isEmpty, "Illegal line " + nextL )
           doneAct = true
       }
       Commit( id, d, m, activities )
   }

   def readProject( name: String, ln: Iterator[String]) : (String, Project) = {
       var commits = Seq.empty[ Commit ]
       var nextL = strip( ln.next )
       while( ln.hasNext && !nextL.startsWith( ":" )) {
           if( nextL.startsWith( "Commit:" )) {
               commits :+= readCommit( nextL, ln )
           } else {
               require( nextL.isEmpty || nextL.startsWith( ":" ), "Illegal line " + nextL )
           }
           if( ln.hasNext ) nextL = strip( ln.next )
       }
       (nextL, Project( name, commits ))
   }

   def readAll( ln: Iterator[String] ) : Seq[ Project ] = {
       var projects = Seq.empty[ Project ]
       var nextL = strip( ln.next )
       while( ln.hasNext ) {
           if( nextL.startsWith( ":" )) {
               val (l, p) = readProject( strip( nextL.substring( 1 )), ln )
               projects :+= p
               nextL = l
           } else require( nextL.isEmpty, "Illegal line " + nextL )
       }
       projects
   }
}