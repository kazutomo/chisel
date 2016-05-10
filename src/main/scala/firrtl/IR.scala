/*
Copyright (c) 2014 - 2016 The Regents of the University of
California (Regents). All Rights Reserved.  Redistribution and use in
source and binary forms, with or without modification, are permitted
provided that the following conditions are met:
   * Redistributions of source code must retain the above
     copyright notice, this list of conditions and the following
     two paragraphs of disclaimer.
   * Redistributions in binary form must reproduce the above
     copyright notice, this list of conditions and the following
     two paragraphs of disclaimer in the documentation and/or other materials
     provided with the distribution.
   * Neither the name of the Regents nor the names of its contributors
     may be used to endorse or promote products derived from this
     software without specific prior written permission.
IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
REGENTS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE. THE SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF
ANY, PROVIDED HEREUNDER IS PROVIDED "AS IS". REGENTS HAS NO OBLIGATION
TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
MODIFICATIONS.
*/

package firrtl

import scala.collection.Seq

// Should this be defined elsewhere?
/*
Structure containing source locator information.
Member of most Stmt case classes.
*/
trait Info
case object NoInfo extends Info {
  override def toString(): String = ""
}
case class FileInfo(info: StringLit) extends Info {
  override def toString(): String = " @[" + info.serialize + "]"
}

class FIRRTLException(str: String) extends Exception(str)

trait AST {
  def serialize: String = firrtl.Serialize.serialize(this)
}

trait HasName {
  val name: String
}
trait HasInfo {
  val info: Info
}
trait IsDeclaration extends HasName with HasInfo

case class StringLit(array: Array[Byte]) extends AST

trait PrimOp extends AST
case object ADD_OP extends PrimOp 
case object SUB_OP extends PrimOp
case object MUL_OP extends PrimOp
case object DIV_OP extends PrimOp
case object REM_OP extends PrimOp
case object LESS_OP extends PrimOp
case object LESS_EQ_OP extends PrimOp
case object GREATER_OP extends PrimOp
case object GREATER_EQ_OP extends PrimOp
case object EQUAL_OP extends PrimOp
case object NEQUAL_OP extends PrimOp
case object PAD_OP extends PrimOp
case object AS_UINT_OP extends PrimOp
case object AS_SINT_OP extends PrimOp
case object AS_CLOCK_OP extends PrimOp
case object SHIFT_LEFT_OP extends PrimOp
case object SHIFT_RIGHT_OP extends PrimOp
case object DYN_SHIFT_LEFT_OP extends PrimOp
case object DYN_SHIFT_RIGHT_OP extends PrimOp
case object CONVERT_OP extends PrimOp
case object NEG_OP extends PrimOp
case object NOT_OP extends PrimOp
case object AND_OP extends PrimOp
case object OR_OP extends PrimOp
case object XOR_OP extends PrimOp
case object AND_REDUCE_OP extends PrimOp
case object OR_REDUCE_OP extends PrimOp
case object XOR_REDUCE_OP extends PrimOp
case object CONCAT_OP extends PrimOp
case object BITS_SELECT_OP extends PrimOp
case object HEAD_OP extends PrimOp
case object TAIL_OP extends PrimOp

trait Expression extends AST {
  def tpe: Type
}
case class Ref(name: String, tpe: Type) extends Expression with HasName
case class SubField(exp: Expression, name: String, tpe: Type) extends Expression with HasName
case class SubIndex(exp: Expression, value: Int, tpe: Type) extends Expression
case class SubAccess(exp: Expression, index: Expression, tpe: Type) extends Expression
case class Mux(cond: Expression, tval: Expression, fval: Expression, tpe: Type) extends Expression
case class ValidIf(cond: Expression, value: Expression, tpe: Type) extends Expression
case class UIntValue(value: BigInt, width: Width) extends Expression {
  def tpe = UIntType(width)
}
case class SIntValue(value: BigInt, width: Width) extends Expression {
  def tpe = SIntType(width)
}
case class DoPrim(op: PrimOp, args: Seq[Expression], consts: Seq[BigInt], tpe: Type) extends Expression

abstract class Statement extends AST
case class DefWire(info: Info, name: String, tpe: Type) extends Statement with IsDeclaration
case class DefRegister(
    info: Info,
    name: String,
    tpe: Type,
    clock: Expression,
    reset: Expression,
    init: Expression) extends Statement with IsDeclaration
case class DefInstance(info: Info, name: String, module: String) extends Statement with IsDeclaration
case class DefMemory(
    info: Info,
    name: String,
    dataType: Type,
    depth: Int,
    writeLatency: Int,
    readLatency: Int,
    readers: Seq[String],
    writers: Seq[String],
    readwriters: Seq[String]) extends Statement with IsDeclaration
case class DefNode(info: Info, name: String, value: Expression) extends Statement with IsDeclaration
case class Conditionally(
    info: Info,
    pred: Expression,
    conseq: Statement,
    alt: Statement) extends Statement with HasInfo
case class Begin(stmts: Seq[Statement]) extends Statement
case class PartialConnect(info: Info, loc: Expression, expr: Expression) extends Statement with HasInfo
case class Connect(info: Info, loc: Expression, expr: Expression) extends Statement with HasInfo
case class IsInvalid(info: Info, expr: Expression) extends Statement with HasInfo
case class Stop(info: Info, ret: Int, clk: Expression, en: Expression) extends Statement with HasInfo
case class Print(
    info: Info,
    string: StringLit,
    args: Seq[Expression],
    clk: Expression,
    en: Expression) extends Statement with HasInfo
case object EmptyStmt extends Statement

abstract class Width extends AST {
  def +(x: Width): Width = (this, x) match {
    case (a: IntWidth, b: IntWidth) => IntWidth(a.width + b.width)
    case _ => UnknownWidth
  }
  def -(x: Width): Width = (this, x) match {
    case (a: IntWidth, b: IntWidth) => IntWidth(a.width - b.width)
    case _ => UnknownWidth
  }
  def max(x: Width): Width = (this, x) match {
    case (a: IntWidth, b: IntWidth) => IntWidth(a.width max b.width)
    case _ => UnknownWidth
  }
  def min(x: Width): Width = (this, x) match {
    case (a: IntWidth, b: IntWidth) => IntWidth(a.width min b.width)
    case _ => UnknownWidth
  }
}
/** Positive Integer Bit Width of a [[GroundType]] */
case class IntWidth(width: BigInt) extends Width
case object UnknownWidth extends Width

/** Orientation of [[Field]] */
abstract class Orientation extends AST
case object Default extends Orientation
case object Flip extends Orientation

/** Field of [[BundleType]] */
case class Field(name: String, flip: Orientation, tpe: Type) extends AST with HasName

abstract class Type extends AST
abstract class GroundType extends Type {
  val width: Width
}
abstract class AggregateType extends Type
case class UIntType(width: Width) extends GroundType
case class SIntType(width: Width) extends GroundType
case class BundleType(fields: Seq[Field]) extends AggregateType
case class VectorType(tpe: Type, size: Int) extends AggregateType
case object ClockType extends GroundType {
  val width = IntWidth(1)
}
case object UnknownType extends Type

/** [[Port]] Direction */
abstract class Direction extends AST
case object Input extends Direction
case object Output extends Direction

/** [[DefModule]] Port */
case class Port(info: Info, name: String, direction: Direction, tpe: Type) extends AST with IsDeclaration

/** Base class for modules */
abstract class DefModule extends AST with IsDeclaration {
  val info : Info
  val name : String
  val ports : Seq[Port]
}
/** Internal Module
  *
  * An instantiable hardware block
  */
case class Module(info: Info, name: String, ports: Seq[Port], body: Statement) extends DefModule
/** External Module
  *
  * Generally used for Verilog black boxes
  */
case class ExtModule(info: Info, name: String, ports: Seq[Port]) extends DefModule

case class Circuit(info: Info, modules: Seq[DefModule], main: String) extends AST with HasInfo

