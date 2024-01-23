package ooo.simulator

import chiseltest.simulator.SimulatorContext

import scala.collection.mutable.ArrayBuffer
import scala.swing.{Color, Component, Dimension, Graphics2D, Panel, ScrollPane}

class WaveSignal(val label: String, val name: String, isBoolean: Boolean) {
  val states = new ArrayBuffer[BigInt]
  def registerCurrentState(dut: SimulatorContext): Unit = {
    states.addOne(dut.peek(name))
  }
}

class Waves(dut: SimulatorContext) extends Panel {
  var signals = new ArrayBuffer[WaveSignal]
  var currentState = 0
  val waveHeight = 20
  val waveLabelWidth = 80
  val cycleWidth = 10

  preferredSize = new Dimension(200,200)

  private def updateSize() = {
    minimumSize = new Dimension(waveLabelWidth + (currentState + 1) * cycleWidth, waveHeight * signals.length * 2)
  }
  def addSignal(signal: WaveSignal): Unit = {
    signals.addOne(signal)
    updateSize()
    repaint()
  }

  def tick() = {
    for (signal <- signals) {
      signal.registerCurrentState(dut)
    }
    updateSize()
    repaint()
  }

  override protected def paintComponent(g: Graphics2D): Unit = {
    super.paintComponent (g)

    val waveOffsetX = 80

    for ((s,idx) <- signals.zipWithIndex) {
      val waveOffsetY = (idx + 0) * waveHeight
      g.drawString(s.label, 0, (idx + 1) * waveHeight)


      for ((value,idx) <- s.states.zipWithIndex) {
        val sx = waveOffsetX + cycleWidth * idx
        g.setColor(new Color(0,0,0))
        g.fillRect(sx, waveOffsetY, cycleWidth, waveHeight)


        val high = waveOffsetY + 1
        val low = waveOffsetY + waveHeight - 2

        if (value > 0) {
          g.setColor(new Color(0, 100, 0))
          g.fillRect(sx, high, cycleWidth, waveHeight - 1)
        }
        g.setColor(new Color(0,255,0))

        val sy = if (value == 0) low else high
        g.drawLine(sx, sy , sx + cycleWidth, sy)
        if (idx > 0) {
          val lastState = s.states(idx - 1)
          if (lastState != value) {
            g.drawLine(sx, low, sx, high)
          }
        }

        g.setColor(new Color(0, 0, 0))
      }
    }
  }
}
