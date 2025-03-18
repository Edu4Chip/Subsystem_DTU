import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import misc.FormalHelper

class FormalTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  FormalHelper.enableFormalBlocks()

  "InstructionMemory" should "satisfy properties" in {
    verify(new dtu.InstructionMemory(64), Seq(BoundedCheck(10)))
  }

  "PonteDecoder" should "satisfy properties" in {
    verify(new ponte.PonteDecoder, Seq(BoundedCheck(10)))
  }

  "ApbMux" should "satisfy properties" in {
    verify(
      new apb.ApbMux(
        8,
        32,
        Seq(
          BitPat("b0000????"),
          BitPat("b0001????"),
          BitPat("b0010????"),
          BitPat("b0011????")
        )
      ),
      Seq(BoundedCheck(10))
    )
  }

}
