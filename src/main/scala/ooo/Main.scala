package ooo

import chisel3._

object Main extends App {
    (new chisel3.stage.ChiselStage).emitVerilog(
    new Spekkio(CoreParams()),
    Array("--target-dir", "output/",
      "--emission-options", "disableMemRandomization,disableRegisterRandomization"))


}