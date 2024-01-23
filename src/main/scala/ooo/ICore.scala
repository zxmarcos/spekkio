package ooo

import chisel3._
import ooo.decode.DecodeUnit
import ooo.execute.{ALUUnit}
import ooo.fetch.FetchUnit
import ooo.issue.IssueUnit
import ooo.rename.RenameUnit
import ooo.utils.MultiportMemoryLVT


class ICore(implicit p: CoreParams) {
  var fetch: FetchUnit = null
  var decode: DecodeUnit = null
  var rename: RenameUnit = null
  var rat: MultiportMemoryLVT = null
  var prf: MultiportMemoryLVT = null
  var issue: IssueUnit = null
  var alu: ALUUnit = null
}
