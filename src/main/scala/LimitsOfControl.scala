import collection.{JavaConversions, breakOut}
import com.itextpdf.text.pdf.{BaseFont, DefaultFontMapper, PdfWriter}
import com.itextpdf.text.{FontFactory, Document => IDocument, Rectangle => IRectangle}
import io.Source
import java.awt.geom.Rectangle2D
import java.awt.{Graphics2D, Font, BorderLayout, GradientPaint, Color, EventQueue}
import java.io.{FileOutputStream, File}
import java.text.{SimpleDateFormat, DateFormat}
import java.util.{Locale, Calendar, TimeZone, Date}
import javax.swing.{JComponent, JPanel, WindowConstants, JFrame}
import org.jfree.chart.annotations.{CategoryAnnotation, XYTitleAnnotation}
import org.jfree.chart.axis.{ValueAxis, CategoryAxis, CategoryAnchor, SubCategoryAxis}
import org.jfree.chart.block.{RectangleConstraint, LabelBlock, BlockBorder, BorderArrangement, BlockContainer}
import org.jfree.chart.plot.{CategoryPlot, PlotOrientation}
import org.jfree.chart.renderer.category.{StandardBarPainter, GroupedStackedBarRenderer}
import org.jfree.chart.title.LegendTitle
import org.jfree.chart.{LegendItemSource, ChartPanel, JFreeChart, ChartFactory}
import org.jfree.data.category.{CategoryDataset, DefaultCategoryDataset}
import org.jfree.data.KeyToGroupMap
import org.jfree.ui.{RectangleInsets, HorizontalAlignment, RectangleEdge, RectangleAnchor, StandardGradientPaintTransformer, GradientPaintTransformType}

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
//      chart2( ps )
//      collectActivities( ps ).foreach( println )
      chart3( ps )
   }

   def chart3( ps: Seq[ Project ]) {
      val chart = createChart2( createCatSet2( ps ))
//      createPDF( onDesktop( "commitchart.pdf" ), chart, 1050, 400 )
      createPDF( onDesktop( "commitchart.pdf" ), chart, 1050 * 5 / 4, 400 )
//      showChart( chart )
   }

   def chart2( ps: Seq[ Project ]) {
      val chart = createChart1( createCatSet1( filterComposition( ps )))
      createPDF( onDesktop( "chart2.pdf" ), chart, 1000, 500 )
   }

   def chart1( ps: Seq[ Project ]) {
      val chart = createChart1( createCatSet1( ps ))
      // showChart( chart )
      createPDF( onDesktop( "chart1.pdf" ), chart, 1000, 500 )
   }

   def onDesktop( name: String ) : File = {
      new File( new File( System.getProperty( "user.home" ), "Desktop" ), name )
   }

   /**
    * Results:

   Cleanup (irrelevant)
   Composition
   Improved Functionality
   New Functionality
   Experimentation
   Documentation
   Bug fixes
   Runtime Infrastructure
   Dependancy update
   Debug printing

    possible groupings:
        Composition + Experimentation + New Functionality + Improved Functionality
    vs. Cleanup + Bug fixes + Runtime Infrastructure + Dependancy update + Debug printing
    vs. Documentation

    */
//   def collectActivities( ps: Seq[ Project ]) : Set[ String ] = {
//      ps.flatMap( _.commits.flatMap( _.activities.map( _.name )))( breakOut ) : Set[ String ]
//   }

   object ActivityType {
      implicit def apply( str: String ) : ActivityType = map( str )

      private lazy val map : Map[ String, ActivityType ] =
         List( ActivityClean, ActivityCompo,  ActivityImpro, ActivityFunc,   ActivityExperi,
               ActivityDocu,  ActivityBugfix, ActivityInfra, ActivityUpdate, ActivityPrint )
            .map( tpe => tpe.name -> tpe )( breakOut )
   }
   sealed abstract class ActivityType( val name: String )
   case object ActivityClean  extends ActivityType( "Cleanup (irrelevant)" )
   case object ActivityCompo  extends ActivityType( "Composition" )
   case object ActivityImpro  extends ActivityType( "Improved Functionality" )
   case object ActivityFunc   extends ActivityType( "New Functionality" )
   case object ActivityExperi extends ActivityType( "Experimentation" )
   case object ActivityDocu   extends ActivityType( "Documentation" )
   case object ActivityBugfix extends ActivityType( "Bug fixes" )
   case object ActivityInfra  extends ActivityType( "Runtime Infrastructure" )
   case object ActivityUpdate extends ActivityType( "Dependancy update" )
   case object ActivityPrint  extends ActivityType( "Debug printing" )

   lazy val userFontDir : File = new File( new File( System.getProperty( "user.home" ), "Library" ), "Fonts" )
   lazy val systemFontDir = new File( "/Library/Fonts" )
   lazy val x11type1FontDir = new File( "/usr/X11R6/lib/X11/fonts/Type1" )

   lazy val axisFontName   = "Gulim" // "Share-Regular"      // "Gulim"
   lazy val axisFontFile   = new File( userFontDir, "Gulim.ttf" ) // "Share-Regular.otf"  // "Gulim.ttf"
   val axisFontSize        = 15 // 12
   val legendFontSize      = 17 // 13
//   lazy val titleFontName = "l049016t" // "Luxi Serif Bold Regular"
//   lazy val titleFontName = "TimesBold" // "Luxi Serif Bold Regular"
//   lazy val titleFontFile = new File( onDesktop( "font_tmp" ), "TimesBold.ttf" )
//   lazy val titleFontFile = new File( x11type1FontDir, "l049016t.afm" )
//   lazy val titleFontName = "Times-Bold"
//   lazy val titleFontFile  = new File( "/usr/local/texlive/2010/texmf-dist/fonts/afm/adobe/times/ptmb8a.afm" )
   lazy val titleFontName  = "Arial Narrow" // "BellySansCondensed" // "FreeSerif"
   lazy val titleFontFile  = {
//      val res = new File( userFontDir, "FreeSerif.ttf" ) // "Share-Regular.otf"  // "Gulim.ttf"
//      val res = new File( userFontDir, "BellySansCondensed.ttf" ) // "Share-Regular.otf"  // "Gulim.ttf"
      val res = new File( systemFontDir, "Arial Narrow.ttf" ) // "Share-Regular.otf"  // "Gulim.ttf"
      require( res.exists )
      res
   }
   val titleFontSize       = 16

   lazy val fontMapper = {
      val res = new DefaultFontMapper()
      val p1 = new DefaultFontMapper.BaseFontParameters( axisFontFile.getAbsolutePath )
      p1.encoding = BaseFont.IDENTITY_H
      res.putName( axisFontName, p1 )
//      val p2 = new DefaultFontMapper.BaseFontParameters( titleFontFile.getAbsolutePath )
//      p2.encoding = BaseFont.IDENTITY_H // "#simple" // BaseFont.IDENTITY_H
//      res.putName( titleFontName, p2 )
      res
   }

   lazy val axisIFont = {
      fontMapper
      BaseFont.createFont( axisFontFile.getAbsolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED )
   }

   lazy val axisFont    = fontMapper.pdfToAwt( axisIFont, axisFontSize )
   lazy val legendFont  = fontMapper.pdfToAwt( axisIFont, legendFontSize )

//   lazy val titleFont = new Font( "FreeSerif", Font.BOLD, titleFontSize )
   lazy val titleFont = new Font( "SansSerif", Font.BOLD, titleFontSize )

//   lazy val titleFont = {
//      fontMapper
////      fontMapper.
////      new Font( titleFontName, Font.BOLD, titleFontSize )
//
//      // this results in ExceptionConverter: java.io.UnsupportedEncodingException: Identity-H
//      val bf = BaseFont.createFont( titleFontFile.getAbsolutePath, BaseFont.IDENTITY_H /* "#simple" */, BaseFont.EMBEDDED )
////      val bf = new Type1Font( titleFontFile.getAbsolutePath, BaseFont.IDENTITY_H, true, null, null, true )
////      val bf = BaseFont.createFont( "Times-Bold", "#full", false )
////      val f    = FontFactory.getFont( FontFactory.TIMES_BOLD, titleFontSize, Font.BOLD )
////      val bf   = f.getBaseFont
//
//      val awt  = fontMapper.pdfToAwt( bf, titleFontSize )
//      println( "Title got awt : " + awt.getFamily + " / " + awt.getFontName )
//      awt
//   }

   def createPDF( file: File, chart: JFreeChart, width: Int, height: Int ) {
//      val fntMap     = new DefaultFontMapper()
//      val aliases    = fntMap.getAliases()
//      val iter       = aliases.keySet.iterator
//      while( iter.hasNext ) {
//         val key = iter.next
//         println( key + " -> " + aliases.get( key ))
//      }
//      val mapper = fntMap.getMapper
//      val iter       = mapper.keySet.iterator
//      while( iter.hasNext ) {
//         val key = iter.next
//         println( key + " -> " + mapper.get( key ))
//      }
//      fntMap.insertDirectory( userFontDir.getAbsolutePath )
//      val bf = new DefaultFontMapper.BaseFontParameters( new File( userFontDir, fontFile ).getAbsolutePath )
//      bf. = BaseFont.FONT_TYPE_TT

//      val bf = BaseFont.createFont( new File( userFontDir, fontFile ).getAbsolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED )
//      bf.setPostscriptFontName( "Gulim" )
//      fntMap.putName( fontName, bf )
//      bf.

//      val testFont = fntMap.awtToPdf( new Font( fontName, Font.PLAIN, 10 ))
//      import JavaConversions._
//      println( "GOT : " + testFont.getFamilyFontName.toList.flatten )

//      val width      = comp.getWidth
//      val height     = comp.getHeight
      val pageSize	= new IRectangle( 0, 0, width, height )
      val doc		   = new IDocument( pageSize, 0, 0, 0, 0 )
      val stream	   = new FileOutputStream( file )
      val writer	   = PdfWriter.getInstance( doc, stream )
//FontFactory.registerDirectories()
//writer.setAtLeastPdfVersion( PdfWriter.VERSION_1_7 )

      doc.open()
      val cb		   = writer.getDirectContent
      val tp		   = cb.createTemplate( width, height )
      val g2		   = tp.createGraphics( width, height, fontMapper )
//      comp.paint( g2 )
//      comp.print( g2 )
      val r2         = new Rectangle2D.Double( 0, 0, width, height )
      chart.draw( g2, r2 )
      g2.dispose
      cb.addTemplate( tp, 0, 0 )
      doc.close
   }

   def filterByActivity( ps: Seq[ Project ])( fun: ActivityType => Boolean ) : Seq[ Project ] =
      ps.map( p => p.copy( commits = p.commits.map( c => c.copy( activities = c.activities.filter( a => fun( a.tpe ))))))

   def filterComposition( ps: Seq[ Project ]) : Seq[ Project ] = filterByActivity( ps )( tpe => tpe == ActivityCompo || tpe == ActivityExperi )

   def filterNotDocumentation( ps: Seq[ Project ]) : Seq[ Project ] = filterByActivity( ps )( _ != ActivityDocu )

   /**
    * LOC categorized by source and activity type
    *   Composition + Experimentation + New Functionality + Improved Functionality
    *   vs. Cleanup + Bug fixes + Runtime Infrastructure + Dependancy update + Debug printing
    */
   def createCatSet2( ps: Seq[ Project ]) : CategoryDataset = {
      val set  = new DefaultCategoryDataset()
      val p    = filterNotDocumentation( ps ) // .find( _.name == "Dissemination" ).get

//      println( p )

      val cal  = Calendar.getInstance( TimeZone.getTimeZone( "BST" ))
      val grouped = p.flatMap( _.commits ).groupBy( c => {
         cal.setTime( c.date )
         cal.get( Calendar.DAY_OF_YEAR  )
      })
      val act = grouped.mapValues( _.flatMap( _.activities ))

//      println( act.toList.sortBy( _._1 ))

//      val mapped: Map[ Int, Seq[ Edit ]] = grouped.map( tup => tup._1 -> tup._2.flatMap( _.activities.flatMap( _.edits )))( breakOut )
//      val mapped: Map[ Int, Map[ ActivityCateg, Map[ EditType, Int ]]] = grouped.mapValues( _.flatMap( _.activities )
//         .groupBy( _.tpe : ActivityCateg ).mapValues[ Map[ EditType, Int ]]( _.flatMap( _.edits.groupBy( _.tpe ).mapValues( _.map( _.numLines ).sum ))( breakOut )))
//      val mapped: Map[ Int, Map[ ActivityCateg, Map[ EditSourceType, Int ]]] = act.mapValues(
//         _.groupBy( _.tpe : ActivityCateg ).mapValues( _.flatMap( _.edits.groupBy( _.source.tpe ).mapValues( _.map( _.numLines ).sum ))( breakOut )))
      val mapped: Map[ Int, Map[ ActivityCateg, Map[ EditSourceType, Int ]]] = act.mapValues(
         _.groupBy( _.tpe : ActivityCateg ).mapValues( _.flatMap( _.edits )).mapValues( _.groupBy( _.source.tpe )
            .mapValues( _.map( _.numLines ).sum )))

//            _.flatMap( _.edits.groupBy( _.source.tpe ).mapValues( _.map( _.numLines ).sum ))( breakOut )))

      val dfmt = new SimpleDateFormat( "MMM dd", Locale.UK )
      val sorted = mapped.toList.sortBy( _._1 )
//      println( sorted )
      sorted.foreach { case (day, map) =>
         cal.set( Calendar.DAY_OF_YEAR, day )
         List( ActivityCategCompo, ActivityCategOther ).foreach { categ =>
            val map2 = map.getOrElse( categ, Map.empty )
            List( EditSourceNew, EditSourceOther, EditSourceSelf ).foreach { src =>
               val num = map2.getOrElse( src, 0 )
               val v = EditCategory2( src, categ )
               val d = dfmt.format( cal.getTime() )
//               println( "" + d + " (" + cal.get( Calendar.DAY_OF_YEAR ) + "), " + num + ", " + v )
               set.addValue( num, v, d )
            }
         }
      }
      set
   }

   def mapHSBComponent( c: Color, i: Int, fun: Double => Double ) : Color = {
      val arr = new Array[ Float ]( 3 )
      Color.RGBtoHSB( c.getRed(), c.getGreen(), c.getBlue(), arr )
      val f0 = fun( arr( i )).toFloat
      val f  = if( i > 0 ) math.max( 0f, math.min( 1f, f0 )) else f0
      arr( i ) = f
      Color.getHSBColor( arr( 0 ), arr( 1 ), arr( 2 ))
   }

   def mapHue( c: Color, fun: Double => Double ) : Color        = mapHSBComponent( c, 0, fun )
   def mapSaturation( c: Color, fun: Double => Double ) : Color = mapHSBComponent( c, 1, fun )
   def mapBrightness( c: Color, fun: Double => Double ) : Color = mapHSBComponent( c, 2, fun )

   def createChart2( set: CategoryDataset ) : JFreeChart = {
      val chart = ChartFactory.createStackedBarChart(
         "" /* "Commit History" */, "Category", "Lines of Code", set,
         PlotOrientation.VERTICAL, true, false, false )

//      chart.getTitle().setFont( titleFont )
      chart.getTitle().setVisible( false )
//      chart.setPadding( new RectangleInsets( 0, 0, 0, 0 ))
//      chart.setBorderVisible( false )

      val renderer = new GroupedStackedBarRenderer()
      val map = new KeyToGroupMap( "G1" )
      List( ActivityCategCompo, ActivityCategOther ).zipWithIndex.foreach { case (tpe, i) =>
         val group = "G" + (i+1)
         List( EditSourceNew, EditSourceOther, EditSourceSelf ).foreach { src =>
            map.mapKeyToGroup( EditCategory2( src, tpe ), group )
         }
      }
      renderer.setSeriesToGroupMap( map )
//      renderer.setBaseItemLabelFont( titleFont )

      renderer.setItemMargin( 0.10 )
      renderer.setDrawBarOutline( false )
      val p1a = new Color( 0, 0xA0, 0 ) // new GradientPaint( 0.0f, 0.0f, new Color(0x22, 0x22, 0xFF),  0.0f, 0.0f, new Color( 0x88, 0x88, 0xFF ))
      val p1b = Color.blue
      renderer.setSeriesPaint(  0, p1a )
      renderer.setSeriesPaint(  3,  p1b )
//      renderer.setSeriesPaint(  6, p1 )

//      val p2 = new Color( 0xC0, 0xA0, 0 ) // new GradientPaint( 0.0f, 0.0f, new Color( 0x22, 0xFF, 0x22), 0.0f, 0.0f, new Color( 0x88, 0xFF, 0x88 ))
      val p2a = mapHue( mapSaturation( mapBrightness( p1a, _ * 1.6 ), _ * 0.6 ), _ - 0.05 )
      val p2b = mapHue( mapSaturation( mapBrightness( p1b, _ * 2.4 ), _ * 0.35 ), _ - 0.05 )
      renderer.setSeriesPaint(  1, p2a )
      renderer.setSeriesPaint(  4, p2b )
//      renderer.setSeriesPaint(  7, p2 )

      val p3 = Color.red // new GradientPaint( 0.0f, 0.0f, new Color( 0xFF, 0x22, 0x22 ), 0.0f, 0.0f, new Color( 0xFF, 0x88, 0x88 ))
      renderer.setSeriesPaint(  2, p3 )
      renderer.setSeriesPaint(  5, p3 )
//      renderer.setSeriesPaint(  8, p3 )

//      val p4 = Color.yellow // new GradientPaint( 0.0f, 0.0f, new Color( 0xFF, 0xFF, 0x22 ), 0.0f, 0.0f, new Color( 0xFF, 0xFF, 0x88 ))
//      renderer.setSeriesPaint(  3, p4 )
//      renderer.setSeriesPaint(  7, p4 )
//      renderer.setSeriesPaint( 11, p4 )

//      renderer.setGradientPaintTransformer( new StandardGradientPaintTransformer( GradientPaintTransformType.HORIZONTAL ))
//      renderer.setLegendItemLabelGenerator( new SOCRCategorySeriesLabelGenerator() )
      renderer.setBarPainter( new StandardBarPainter() )

//      val domainAxis = new SubCategoryAxis( "XXX" )
//      domainAxis.setCategoryMargin( 0.05 )
//      domainAxis.addSubCategory( "C" ) // EditSourceNew.toString )
//      domainAxis.addSubCategory( "O" ) // EditSourceOther.toString )
////      domainAxis.addSubCategory( "I" ) // EditSourceSelf.toString )
//      domainAxis.setCategoryMargin( 0.2 )
////      domainAxis.setAxisLineVisible( true )
////      domainAxis.setAxisLinePaint( Color.blue )
////      domainAxis.setLabel( "Date" )
////      domainAxis.setTickLabelsVisible( false ) // affects dates
////      domainAxis.setVisible( false ) // affects all
//
      val plot = chart.getPlot().asInstanceOf[ CategoryPlot ]
////      plot.setDomainAxis( domainAxis )
//      plot.setAxisOffset( new RectangleInsets( 0, 0, 0, 0 ))
      plot.setInsets( new RectangleInsets( 0, 0, 0, 0 ))

//      plot.setDomainGridlinesVisible( true )
//      plot.setDomainGridlinePaint( Color.lightGray )
//      plot.setDomainGridlinePosition( CategoryAnchor.START ) // XXX hmmm, not optimal ; could use MIDDLE with custom Stroke?

//      plot.setDomainAxisLocation( AxisLocation.TOP_OR_RIGHT )
      plot.setRenderer( renderer )
//      plot.setFixedLegendItems( createLegendItems() )
      plot.setBackgroundPaint( Color.white )
//      plot.setDomainGridlinePaint( Color.blue )
      plot.setOutlinePaint( Color.gray )
      // plot.setRangeCrosshairPaint( Color.blue )
      plot.setRangeGridlinePaint( Color.lightGray )

//      plot.setAnchorValue( 100 ) // no apparent effect
      val xaxis = new CategoryAxis()
//      axis.setLabelAngle( 90 )    // no apparent effect
//      axis.setFixedDimension( 100 )    // no apparent effect
//      axis.setTickLabelFont( new Font( fontName, Font.PLAIN, 10 ))
      xaxis.setTickLabelFont( axisFont )
      plot.setDomainAxis( xaxis )
//      xaxis.setCategoryMargin( 0.0 )
//      xaxis.setLabelInsets( new RectangleInsets( 0, 0, 0, 0 ))
//      xaxis.setFixedDimension( 0.0 )
      xaxis.setLowerMargin( 0.01 )
      xaxis.setUpperMargin( 0.01 )

      val yaxis = plot.getRangeAxis()
      yaxis.setLabelFont( axisFont )
      yaxis.setTickLabelFont( axisFont )

      val legendOld = chart.getLegend()
      legendOld.setVisible( false )
////      legend.setLegendItemGraphicLocation( RectangleAnchor.TOP_RIGHT )
//      legend.setPosition( RectangleEdge.TOP )

//      val lt = new LegendTitle( plot )
////      lt.setItemFont(new Font("Dialog", Font.PLAIN, 9));
////      lt.setBackgroundPaint(new Color(200, 200, 255, 100));
////      lt.setFrame( new BlockBorder( Color.white ))
//      lt.setPosition( RectangleEdge.BOTTOM )
//      val ta = new CategoryTextAnnotation( 0.98, 0.02, lt, RectangleAnchor.BOTTOM_RIGHT )
//      ta.setMaxWidth( 0.48 )
//      plot.addAnnotation( ta )

      val legend = new LegendTitle( plot )
      legend.setBackgroundPaint( Color.white )
      legend.setPadding( 10, 10, 10, 10 )

//      val lala = legend.getSources
//      val lala2 = new Array[ LegendItemSource ]( lala.size - 1 )
//      System.arraycopy( lala, 0, lala2, 0, lala2.size )
//      legend.setSources( lala2 )

//      val blockcontainer = new BlockContainer( new BorderArrangement() )
//      blockcontainer.setBorder( new BlockBorder( 1.0D, 1.0D, 1.0D, 1.0D ))
      val contItem = legend.getItemContainer()

      contItem.setPadding( 2D, 10D, 5D, 2D )
//      blockcontainer.add( blockcontainer1 )
      legend.setWrapper( contItem )
      legend.setItemFont( legendFont )
//      legend.setPosition( RectangleEdge.RIGHT )
         legend.setHorizontalAlignment( HorizontalAlignment.LEFT )
//      chart.addSubtitle( legend )
//      legend.setFrame( )

      plot.addAnnotation( new CategoryAnnotation {
         def draw( g2: Graphics2D, plot: CategoryPlot, r: Rectangle2D, xaxis: CategoryAxis, yaxis: ValueAxis ) {
//            println( "-------AQUI : " + r )
//            legend.setBounds( new Rectangle2D.Double( r.getX + r.getWidth * 0.5, r.getY, r.getWidth * 0.5, r.getHeight ))
            val w       = 0.25
            val right   = 0.03
            val top     = 0.03
            val h       = 0.31
            legend.arrange( g2, new RectangleConstraint( r.getWidth * w, r.getHeight * h ))
            legend.draw( g2, new Rectangle2D.Double(
               r.getX + r.getWidth * (1.0 - (w + right)), r.getY + r.getHeight * top, r.getWidth * w, r.getHeight * h ))
//            g2.setColor( Color.red )
//            g2.fill( r )
         }
      })

      // setCategorySummary(dataset);

      chart
   }

   object ActivityCateg {
      implicit def fromType( tpe: ActivityType ) : ActivityCateg = tpe match {
         case ActivityCompo  => ActivityCategCompo
         case ActivityExperi => ActivityCategCompo
         case ActivityFunc   => ActivityCategOther // ActivityCategCompo
         case ActivityImpro  => ActivityCategOther // ActivityCategCompo
         case ActivityClean  => ActivityCategOther
         case ActivityInfra  => ActivityCategOther
         case ActivityPrint  => ActivityCategOther
         case ActivityBugfix => ActivityCategOther
         case ActivityDocu   => ActivityCategOther
         case ActivityUpdate => ActivityCategOther
      }
   }
   sealed trait ActivityCateg extends Comparable[ ActivityCateg ] {
      def id: Int
      def compareTo( other: ActivityCateg ) = id.compareTo( other.id )
   }
   case object ActivityCategCompo extends ActivityCateg {
      def id = 0
      override def toString = "Composition"
   }
   case object ActivityCategOther extends ActivityCateg {
      def id = 1
      override def toString = "Infrastructure"
   }

   case class EditCategory2( source: EditSourceType, activity: ActivityCateg ) extends Comparable[ EditCategory2 ] {
      def compareTo( other: EditCategory2 ) = {
         val c1 = source.compareTo( other.source )
         if( c1 != 0 ) c1 else {
            activity.compareTo( other.activity )
         }
      }

      override def toString = activity.toString + " / " + source.toString
   }

   /**
    * LOC categorized by source and edit type
    */
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
         "Commit History", "Category", "LOC", set,
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

//      renderer.setGradientPaintTransformer( new StandardGradientPaintTransformer( GradientPaintTransformType.HORIZONTAL ))
//      renderer.setLegendItemLabelGenerator( new SOCRCategorySeriesLabelGenerator() )
      renderer.setBarPainter( new StandardBarPainter() )

      val domainAxis = new SubCategoryAxis( "XXX" )
      domainAxis.setCategoryMargin( 0.05 )
      domainAxis.addSubCategory( "N" ) // EditSourceNew.toString )
      domainAxis.addSubCategory( "E" ) // EditSourceOther.toString )
      domainAxis.addSubCategory( "I" ) // EditSourceSelf.toString )
      domainAxis.setCategoryMargin( 0.2 )
//      domainAxis.setAxisLineVisible( true )
//      domainAxis.setAxisLinePaint( Color.blue )

      val plot = chart.getPlot().asInstanceOf[ CategoryPlot ]
      plot.setDomainAxis( domainAxis )

      plot.setDomainGridlinesVisible( true )
      plot.setDomainGridlinePaint( Color.lightGray )
      plot.setDomainGridlinePosition( CategoryAnchor.START ) // XXX hmmm, not optimal ; could use MIDDLE with custom Stroke?

//      plot.setDomainAxisLocation( AxisLocation.TOP_OR_RIGHT )
      plot.setRenderer(renderer);
//      plot.setFixedLegendItems( createLegendItems() )
      plot.setBackgroundPaint( Color.white )
//      plot.setDomainGridlinePaint( Color.blue )
      plot.setOutlinePaint( Color.gray )
      // plot.setRangeCrosshairPaint( Color.blue )
      plot.setRangeGridlinePaint( Color.lightGray )

      // setCategorySummary(dataset);

      chart
   }

   def showChart( chart: JFreeChart ) : JPanel = {
      val f = new JFrame( "Chart" )
      val p = new ChartPanel( chart )
      f.getContentPane().add( p, BorderLayout.CENTER )
      f.pack()
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setLocationRelativeTo( null )
      f.setVisible( true )
      p
   }

   sealed trait EditSourceType extends Comparable[ EditSourceType ] {
      def id: Int
      def compareTo( b: EditSourceType ) = id.compareTo( b.id )
   }
   case object EditSourceNew extends EditSourceType {   def id = 0; override def toString = "Newly created" }
   case object EditSourceOther extends EditSourceType { def id = 1; override def toString = "From previous work" }
   case object EditSourceSelf extends EditSourceType {  def id = 2; override def toString = "Self referential" }

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

   case class Activity( tpe: ActivityType, edits: Seq[ Edit ])

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