/*
 * Copyright 2015 Johannes Rudolph
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.virtualvoid.sbt.graph

import java.io.File

import sbinary.Format

import scala.collection.mutable.{ HashMap, MultiMap, Set }
import scala.collection.immutable

case class ModuleId(
  organisation: String,
  name:         String,
  version:      String) {
  def idString: String = organisation + ":" + name + ":" + version
}
case class Module(
  id:               ModuleId,
  license:          Option[String] = None,
  extraInfo:        String         = "",
  evictedByVersion: Option[String] = None,
  jarFile:          Option[File]   = None,
  error:            Option[String] = None) {
  def hadError: Boolean = error.isDefined
  def isUsed: Boolean = !isEvicted
  def isEvicted: Boolean = evictedByVersion.isDefined
}

object ModuleGraph {
  val empty = ModuleGraph(Seq.empty, Seq.empty)
}

case class ModuleGraph(nodes: Seq[Module], edges: Seq[Edge]) {
  lazy val modules: Map[ModuleId, Module] =
    nodes.map(n ⇒ (n.id, n)).toMap

  def module(id: ModuleId): Module = modules(id)

  lazy val dependencyMap: Map[ModuleId, Seq[Module]] =
    createMap(identity)

  lazy val reverseDependencyMap: Map[ModuleId, Seq[Module]] =
    createMap { case (a, b) ⇒ (b, a) }

  def createMap(bindingFor: ((ModuleId, ModuleId)) ⇒ (ModuleId, ModuleId)): Map[ModuleId, Seq[Module]] = {
    val m = new HashMap[ModuleId, Set[Module]] with MultiMap[ModuleId, Module]
    edges.foreach { entry ⇒
      val (f, t) = bindingFor(entry)
      m.addBinding(f, module(t))
    }
    m.toMap.mapValues(_.toSeq.sortBy(_.id.idString)).withDefaultValue(Nil)
  }

  def roots: Seq[Module] =
    nodes.filter(n ⇒ !edges.exists(_._2 == n.id)).sortBy(_.id.idString)
}

object ModuleGraphProtocol extends ModuleGraphProtocolCompat {
  import sbinary.DefaultProtocol._

  implicit def seqFormat[T: Format]: Format[Seq[T]] = wrap[Seq[T], List[T]](_.toList, _.toSeq)
  implicit val ModuleIdFormat: Format[ModuleId] = asProduct3(ModuleId)(ModuleId.unapply(_).get)
  implicit val ModuleFormat: Format[Module] = asProduct6(Module)(Module.unapply(_).get)
  implicit val ModuleGraphFormat: Format[ModuleGraph] = asProduct2(ModuleGraph.apply _)(ModuleGraph.unapply(_).get)
}

case class ModuleTreeNode(node: Module, children: immutable.Seq[ModuleTreeNode])

object ModuleTreeNode {
  def apply(module: Module, deps: Map[ModuleId, Seq[Module]]): ModuleTreeNode = {
    val children = deps.getOrElse(module.id, immutable.Seq.empty[Module]).map(m ⇒ apply(m, deps)).toList
    ModuleTreeNode(module, children)
  }
}

case class ModuleTree(roots: immutable.Seq[ModuleTreeNode])

object ModuleTree {
  def apply(graph: ModuleGraph): ModuleTree = {
    val deps = graph.dependencyMap
    ModuleTree(graph.roots.toList.map { root ⇒ ModuleTreeNode(root, deps) })
  }

}
