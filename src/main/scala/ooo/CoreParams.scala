package ooo

import chisel3.util.log2Ceil
import ooo.utils.MultiportMemoryConfig

case class CoreParams
(
  prfEntries: Int = 64,
  ratEntries: Int = 16,
  rrfEntries: Int = 16,
  issueEntries: Int = 16,
  robEntries: Int = 32,
  simulation: Boolean = false
){
  val prfSize: Int = log2Ceil(prfEntries)
  val ratSize: Int = log2Ceil(ratEntries)
  val issueSize: Int = log2Ceil(issueEntries)
  val robSize: Int = log2Ceil(robEntries)
  val issueWakeupPorts: Int = 1
  val prfReadPorts: Int = 2
  val prfWritePorts: Int = 1


  val prfMemoryConfig = MultiportMemoryConfig(prfReadPorts, prfWritePorts, prfEntries, 32)
  val ratMemoryConfig = MultiportMemoryConfig(2, 1, ratEntries, prfSize)
  val rrfMemoryConfig = MultiportMemoryConfig(2, 1, rrfEntries, prfSize)


}
