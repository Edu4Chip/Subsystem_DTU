package basys3

import chisel3._
import chisel3.util.HasBlackBoxInline
import chisel3.experimental.Analog

class Tristate(w: Int) extends BlackBox(Map("W" -> w)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val driveBus = Input(UInt(w.W))
    val busReadValue = Output(UInt(w.W))
    val busDriveValue = Input(UInt(w.W))
    val bus = Analog(w.W)
  })

  setInline(
    s"Tristate.sv",
    s"""
       |module Tristate #(parameter W = 1) (
       |    output  logic [W-1:0] busReadValue,
       |    input   logic [W-1:0] driveBus,
       |    input   logic [W-1:0] busDriveValue,
       |    inout   wire  [W-1:0] bus
       |);
       |
       |    genvar i;
       |    generate
       |        for (i = 0; i < W; i = i + 1) begin : tristate
       |            assign busReadValue[i] = bus[i];
       |            assign bus[i] = driveBus[i] ? 1'bz : busDriveValue[i];
       |        end
       |    endgenerate
       |
       |endmodule
       |""".stripMargin
  )
}
