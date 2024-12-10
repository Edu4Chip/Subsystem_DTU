import chisel3._
import chisel3.util._
import leros._
import config._
import leros.DataMemIO
import io._

class LerosMemorySpace() extends Module {
  
  val io = IO(new Bundle {
    val lerosMemIntf = new DataMemIO(LEROS_CONFIG.DMEM_ADDR_WIDTH)
    
    val dmem = Flipped(new DataMemIO(LEROS_CONFIG.DMEM_ADDR_WIDTH))
    val periphRegs = new SingleRegPort(IO_REG_MAP.COUNT, IO_REG_MAP.WIDTH)
    val lerosCCR = new DualRegPort(APB_CONFIG.N_READ_REGS, APB_CONFIG.N_WRITE_REGS, APB_CONFIG.REGS_WIDTH)
  })

  io.lerosMemIntf.rdData := 0.U

  io.dmem.rdAddr := io.lerosMemIntf.rdAddr
  io.dmem.wrAddr := io.lerosMemIntf.wrAddr
  io.dmem.wr := false.B
  io.dmem.wrData := io.lerosMemIntf.wrData
  io.dmem.wrMask := io.lerosMemIntf.wrMask

  io.periphRegs.index := io.lerosMemIntf.rdAddr - LEROS_ADDR_SPACE.IO_START.U
  io.periphRegs.wr := false.B
  io.periphRegs.wrData := io.lerosMemIntf.wrData
  
  io.lerosCCR.rdIndex := io.lerosMemIntf.rdAddr - LEROS_ADDR_SPACE.READ_CCR_START.U
  io.lerosCCR.wrIndex := io.lerosMemIntf.wrAddr - LEROS_ADDR_SPACE.WRITE_CCR_START.U
  io.lerosCCR.wr := false.B
  io.lerosCCR.wrData := io.lerosMemIntf.wrData
  
  when(io.lerosMemIntf.rdAddr >= LEROS_ADDR_SPACE.DMEM_START.U && io.lerosMemIntf.rdAddr <= LEROS_ADDR_SPACE.DMEM_END.U) {
    io.lerosMemIntf.rdData := io.dmem.rdData
  }
  . elsewhen(io.lerosMemIntf.rdAddr >= LEROS_ADDR_SPACE.IO_START.U && io.lerosMemIntf.rdAddr <= LEROS_ADDR_SPACE.IO_END.U) {
    io.lerosMemIntf.rdData := io.periphRegs.rdData
  }
  . elsewhen(io.lerosMemIntf.rdAddr >= LEROS_ADDR_SPACE.READ_CCR_START.U && io.lerosMemIntf.rdAddr <= LEROS_ADDR_SPACE.READ_CCR_END.U) {
    io.lerosMemIntf.rdData := io.lerosCCR.rdData
  }
  . otherwise {
    io.lerosMemIntf.rdData := 0.U
  }
  
  when(io.lerosMemIntf.wr) {
    when(io.lerosMemIntf.wrAddr >= LEROS_ADDR_SPACE.DMEM_START.U && io.lerosMemIntf.wrAddr <= LEROS_ADDR_SPACE.DMEM_END.U) {
      io.dmem.wr := true.B
    }
    . elsewhen(io.lerosMemIntf.wrAddr >= LEROS_ADDR_SPACE.IO_START.U && io.lerosMemIntf.wrAddr <= LEROS_ADDR_SPACE.IO_END.U) {
      io.periphRegs.wr := true.B
    }
    . elsewhen(io.lerosMemIntf.wrAddr >= LEROS_ADDR_SPACE.WRITE_CCR_START.U && io.lerosMemIntf.wrAddr <= LEROS_ADDR_SPACE.WRITE_CCR_END.U) {
      io.lerosCCR.wr := true.B
    }
    . otherwise {
      io.dmem.wr := false.B
      io.periphRegs.wr := false.B
      io.lerosCCR.wr := false.B
    }
  }

}