package ooo

import chisel3.{RawModule, _}
import chisel3.stage._
import chisel3.stage.phases._
import chiseltest._
import chiseltest.simulator.SimulatorContext
import firrtl.annotations.{Annotation, DeletedAnnotation}
import firrtl.options.TargetDirAnnotation
import firrtl.stage.{FirrtlCircuitAnnotation, FirrtlStage}
import firrtl.{AnnotationSeq, EmittedCircuitAnnotation}
import logger.{LogLevelAnnotation, Logger}
import ooo.simulator.{IssueQueueTableModel, PipelineStagesTableModel, RegisterSignal, RegisterTableModel, WaveSignal, Waves}

import javax.swing.table.DefaultTableModel
import scala.swing._

private object Compiler {

  private val elaboratePhase = new Elaborate
  def elaborate[M <: RawModule](gen: () => M, annotationSeq: AnnotationSeq): (firrtl.CircuitState, M) = {
    // run Builder.build(Module(gen()))
    val genAnno = ChiselGeneratorAnnotation(gen)
    val elaborationAnnos = Logger.makeScope(annotationSeq) { elaboratePhase.transform(genAnno +: annotationSeq) }

    // extract elaborated module
    val dut = elaborationAnnos.collectFirst { case DesignAnnotation(d) => d }.get

    // run aspects
    val aspectAnnos = maybeAspects.transform(elaborationAnnos)

    // run Converter.convert(a.circuit) and toFirrtl on all annotations
    val converterAnnos = converter.transform(aspectAnnos)

    // annos to state
    val state = annosToState(converterAnnos)

    (state, dut.asInstanceOf[M])
  }
  def toLowFirrtl(state: firrtl.CircuitState, annos: AnnotationSeq = List()): firrtl.CircuitState = {
    requireTargetDir(state.annotations)
    val inAnnos = annos ++: stateToAnnos(state)
    val res = firrtlStage.execute(Array("-E", "low"), inAnnos)
    annosToState(res)
  }
  def lowFirrtlToSystemVerilog(state: firrtl.CircuitState, annos: AnnotationSeq = List()): firrtl.CircuitState = {
    requireTargetDir(state.annotations)
    val inAnnos = annos ++: stateToAnnos(state)
    val res = firrtlStage.execute(Array("--start-from", "low", "-E", "sverilog"), inAnnos)
    annosToState(res)
  }
  def lowFirrtlToVerilog(state: firrtl.CircuitState, annos: AnnotationSeq = List()): firrtl.CircuitState = {
    requireTargetDir(state.annotations)
    val inAnnos = annos ++: stateToAnnos(state)
    val res = firrtlStage.execute(Array("--start-from", "low", "-E", "verilog"), inAnnos)
    annosToState(res)
  }
  private val maybeAspects = new MaybeAspectPhase
  private val converter = new Convert
  private def stateToAnnos(state: firrtl.CircuitState): AnnotationSeq = {
    val annosWithoutCircuit = state.annotations.filterNot(_.isInstanceOf[FirrtlCircuitAnnotation])
    FirrtlCircuitAnnotation(state.circuit) +: annosWithoutCircuit
  }
  def annosToState(annos: AnnotationSeq): firrtl.CircuitState = {
    val circuit = annos.collectFirst { case FirrtlCircuitAnnotation(c) => c }.get
    val filteredAnnos = annos.filterNot(isInternalAnno)
    firrtl.CircuitState(circuit, filteredAnnos)
  }
  private def isInternalAnno(a: Annotation): Boolean = a match {
    case _: FirrtlCircuitAnnotation | _: DesignAnnotation[_] | _: ChiselCircuitAnnotation | _: DeletedAnnotation |
         _: EmittedCircuitAnnotation[_] | _: LogLevelAnnotation =>
      true
    case _ => false
  }
  private def firrtlStage = new FirrtlStage
  def requireTargetDir(annos: AnnotationSeq): os.Path = {
    val targetDirs = annos.collect { case TargetDirAnnotation(d) => d }.toSet
    require(targetDirs.nonEmpty, "Expected exactly one target directory, got none!")
    require(targetDirs.size == 1, s"Expected exactly one target directory, got multiple: $targetDirs")
    os.pwd / os.RelPath(targetDirs.head)
  }
}

class MemValue(signal: String, addr: Int, dut: SimulatorContext) extends BoxPanel(Orientation.Horizontal) {

  val address = new Label(f"$addr%5s")
  address.size.width = 100
  val value = new Label("0")
  value.size.width = 100

  contents += address
  contents += value

  def refresh(): Unit = {
    val s =dut.peek(signal).asUInt.litValue.toString(16)
    value.text = f"$s%10s"
  }
}

class SignalValue(signal: String, name: String, dut: SimulatorContext) extends BoxPanel(Orientation.Horizontal) {

  val address = new Label(f"$name%5s  ")
  address.size.width = 100
  val value = new Label("0")
  value.size.width = 100

  contents += address
  contents += value

  def refresh(): Unit = {
    val s =dut.peek(signal).asUInt.litValue.toString(16)
    value.text = f"  $s%10s"
  }
}


class SimulatorUI(dut: SimulatorContext) extends MainFrame {

  println(dut.getCoverage())

  title = "Spekkio Simulator"
  preferredSize = new Dimension(320, 240)

  var currentCycle = 0

  val pc = new SignalValue("pc_value", "PC", dut)
  contents = new BoxPanel(Orientation.Vertical) {

    val fetchValid = new CheckBox()
    val fetchInstr = new Label("")

    val decodeValid = new CheckBox()
    val decodeInstr = new Label("")

    val stagesTable = new Table {
      model = new PipelineStagesTableModel(dut)
    }

    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new BoxPanel(Orientation.Vertical) {
        contents += pc
        contents += new Label("Stages")
        contents += new ScrollPane() {
          contents = stagesTable
        }
      }
    }


    val ratTableModel = new RegisterTableModel(dut)
    Seq.tabulate(16) { i =>
      ratTableModel.append(RegisterSignal(i, "x" + i, "rat" + i))
    }
    val prfTableModel = new RegisterTableModel(dut, true)
    Seq.tabulate(64) { i =>
      prfTableModel.append(RegisterSignal(i, "p" + i, "prf" + i, "bprf_" + i))
    }


    val ratTable = new Table {
      title = "RAT"
      model = ratTableModel
    }
    ratTable.size.width = 100
    val prfTable = new Table {
      title = "PRF"
      model = prfTableModel
    }
    prfTable.size.width = 100

    val iqTable = new Table {
      model = new IssueQueueTableModel(dut,16)
    }

    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new BoxPanel(Orientation.Vertical) {
        contents += new Label("RAT")
        contents += new ScrollPane() {
          contents = ratTable
        }
      }
      contents += new BoxPanel(Orientation.Vertical) {
        this.size.width = 100
        contents += new Label("PRF")
        contents += new ScrollPane() {
          contents = prfTable
        }
      }
      contents += new BoxPanel(Orientation.Vertical) {
        contents += new Label("IQ")
        contents += new ScrollPane() {
          contents = iqTable
        }
      }
    }

//    val waves = new Waves(dut)
//    waves.addSignal(new WaveSignal(name="fetch_valid", label="Fetch Valid", isBoolean = true))
//    waves.addSignal(new WaveSignal(name="fetch_stalled", label="Fetch Stall", isBoolean = true))
//
//    waves.addSignal(new WaveSignal(name = "decode_valid", label = "Decode Valid", isBoolean = true))
//    waves.addSignal(new WaveSignal(name = "decode_stalled", label = "Decode Stall", isBoolean = true))
//
//    waves.addSignal(new WaveSignal(name = "rename1_valid", label = "Ren1 Valid", isBoolean = true))
//    waves.addSignal(new WaveSignal(name = "rename1_stalled", label = "Ren1 Stall", isBoolean = true))
//    waves.addSignal(new WaveSignal(name = "rename2_valid", label = "Ren2 Valid", isBoolean = true))
//    waves.addSignal(new WaveSignal(name = "rename2_stalled", label = "Ren2 Stall", isBoolean = true))
//
//    waves.addSignal(new WaveSignal(name = "issue_valid", label = "Issue Valid", isBoolean = true))
//    waves.addSignal(new WaveSignal(name = "issue_stalled", label = "Issue Stall", isBoolean = true))
//
//    waves.tick()
//
//    contents += new ScrollPane() {
//      preferredSize = new Dimension(500, 400)
//      contents = waves
//    }


    contents += new BoxPanel(Orientation.Horizontal) {
      contents += Button("Step") {
        currentCycle+=1;
        println(s"====> Cycle ${currentCycle}")
        dut.step(1)
        stagesTable.repaint()
        ratTable.repaint()
        prfTable.repaint()
        iqTable.repaint()
        pc.refresh()

//        waves.tick()
      }
      contents += Button("Close") {
        sys.exit(0)
      }
    }
  }




}

object Simulator {
  def main(args: Array[String]) {

    println("Compiling FIRRTL...")

    val sim = TreadleBackendAnnotation.getSimulator

    val targetDir = TargetDirAnnotation("test_run_dir/spekkio_benchmark_" + sim.name)

    // elaborate the design and compile to low firrtl
    val (highFirrtl, _) = Compiler.elaborate(() => new SpekkioWrapper(CoreParams(simulation = true)), Seq(targetDir))
    val lowFirrtl = Compiler.toLowFirrtl(highFirrtl, List(WriteVcdAnnotation))

    println(s"Compiled ${lowFirrtl.circuit.main}")
    val dut = sim.createContext(lowFirrtl)

    val ui = new SimulatorUI(dut)
    ui.visible = true

//    RawTester.test(new Spekkio(CoreParams())) { dut =>
//      val ui = new SimulatorUI(dut)
//      ui.visible = true
//
//      println("End of main function")
//    }

  }
}