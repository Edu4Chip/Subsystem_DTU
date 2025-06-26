package dtu

/** This interface provides control to the DTU subsystem through the following
  * methods:
  *   - resetLerosEnable: Enable the Leros reset signal
  *   - resetLerosDisable: Disable the Leros reset signal
  *   - resetLeros: Enable and then disable reset of the Leros processor
  *   - selectBootRom: Select the boot ROM as the source of instructions
  *   - selectBootRam: Select the programmable instruction memory as the source
  *     of instructions
  *   - uploadProgram: Upload an assembly program to the programmable
  *     instruction memory
  *   - loadInstrMem: Load a sequence of instructions into the programmable
  *     instruction memory
  *   - writeCrossCoreReg: Write a word to a cross-core register
  *   - readCrossCoreReg: Read a word from a cross-core register
  *
  * The methods rely on a send and read method which may be implemented by the
  * APB interface or another layered interface such as the Ponte Uart-to-APB
  * bridge.
  */
trait DtuInterface {

  protected def send(addr: Int, data: Seq[Int]): Unit
  protected def read(addr: Int, words: Int): Seq[Int]

  private def send(addr: Int, data: Int): Unit = send(addr, Seq(data))
  private def read(addr: Int): Int = read(addr, 1).head

  private def setSysCtrlBit(bit: Int) = {
    val status = read(0xc00)
    send(0xc00, status | (1 << bit))
  }

  private def clearSysCtrlBit(bit: Int) = {
    val status = read(0xc00)
    send(0xc00, status & ~(1 << bit))
  }

  /** Enable the Leros reset signal.
    */
  def resetLerosEnable() = {
    setSysCtrlBit(0)
  }

  /** Disable the Leros reset signal.
    */
  def resetLerosDisable() = {
    clearSysCtrlBit(0)
  }

  /** Toggle the Leros reset signal.
    */
  def resetLeros() = {
    resetLerosEnable()
    resetLerosDisable()
  }

  /** Select the boot ROM as the source of instructions.
    */
  def selectBootRom() = {
    clearSysCtrlBit(1)
  }

  /** Select the programmable instruction memory as the source of instructions.
    */
  def selectBootRam() = {
    setSysCtrlBit(1)
  }

  /** Enable UART loopback mode. In this mode, the UART transmit pin is
    * connected to the receive pin, allowing for testing without external
    * connections.
    */
  def enableUartLoopBack() = {
    setSysCtrlBit(2)
  }

  /** Disable UART loopback mode. In this mode, the UART transmit pin is not
    * connected to the receive pin, allowing for normal operation.
    */
  def disableUartLoopBack() = {
    clearSysCtrlBit(2)
  }

  /** Upload an assembly program to the programmable instruction memory. The
    * program is assembled and loaded into the instruction memory.
    * @param path
    *   Path to the assembly file.
    */
  def uploadProgram(path: String) = {
    val blop = leros.util.Assembler.assemble(path)
    val words = blop
      .grouped(2)
      .map {
        case Array(a, b) => (b << 16) | a
        case Array(a)    => a
      }
      .toSeq
    loadInstrMem(words)
  }

  /** Load a sequence of instructions into the programmable instruction memory.
    * @param data
    *   Sequence of instructions to load.
    */
  def loadInstrMem(data: Seq[Int]) = {
    send(0x0000, data)
    read(0x0000, data.length).zip(data).foreach { case (r, w) =>
      assert(r == w, s"Wrong data")
    }
  }

  /** Write a word to a cross-core register.
    * @param reg
    *   Register number.
    * @param data
    *   Data to write.
    */
  def writeCrossCoreReg(reg: Int, data: Int) = {
    send(0x800 | (reg * 4), Seq(data))
  }

  /** Read a word from a cross-core register.
    * @param reg
    *   Register number.
    * @return
    *   Data read from the register.
    */
  def readCrossCoreReg(reg: Int) = {
    read(0x800 | (reg * 4), 1).head
  }
}
