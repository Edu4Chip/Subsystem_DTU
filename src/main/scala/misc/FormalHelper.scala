package misc

import chisel3._

import chiseltest._
import chiseltest.formal._
import scala.util.DynamicVariable

object FormalHelper {
  def implies(a: Bool, b: Bool): Bool = !a || b
  def eventually(a: Bool, within: Int): Bool = {
    if (within == 0) a
    else past(a, within) || eventually(a, within - 1)
  }
  // if I saw 'a' x clock cycles ago, I should have
  // seen 'b' in one of the last x clock cycles
  def leadsTo(a: Bool, b: Bool, within: Int = 1): Bool =
    implies(past(a, within), eventually(b, within))

  case class BoundedLeadsToBridge(lhs: Bool, within: Int) {
    def |=>(rhs: Bool): Bool = leadsTo(lhs, rhs, within)
  }

  implicit class ImplicationOperator(lhs: Bool) {
    def within(within: Int): BoundedLeadsToBridge =
      BoundedLeadsToBridge(lhs, within)
    def ->(rhs: Bool): Bool = implies(lhs, rhs)
    def |=>(rhs: Bool): Bool = leadsTo(lhs, rhs)
  }

  private var propertiesEnabled = false
  def enableProperties(): Unit = propertiesEnabled = true
  def disableProperties(): Unit = propertiesEnabled = false

  object formalProperties {
    def apply(block: => Any): Unit = if (propertiesEnabled) block
  }
}
