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
package rendering

import net.virtualvoid.sbt.graph.model.ModuleGraph

object DOT {
  val EvictedStyle = "stroke-dasharray: 5,5"

  def dotGraph(graph: ModuleGraph,
               dotHead: String,
               nodeFormation: (String, String, String) ⇒ String,
               labelRendering: HTMLLabelRendering): String = {
    val nodes = {
      for (n ← graph.nodes) yield {
        val style = if (n.isEvicted) EvictedStyle else ""
        val label = nodeFormation(n.id.organisation, n.id.name, n.id.version)
        """    "%s"[%s style="%s"]""".format(n.id.idString,
          labelRendering.renderLabel(label),
          style)
      }
    }.mkString("\n")

    def originWasEvicted(edge: Edge): Boolean = graph.module(edge._1).isEvicted
    def targetWasEvicted(edge: Edge): Boolean = graph.module(edge._2).isEvicted

    // add extra edges from evicted to evicted-by module
    val evictedByEdges: Seq[Edge] =
      graph.nodes.filter(_.isEvicted).map(m ⇒ Edge(m.id, m.id.copy(version = m.evictedByVersion.get)))

    // remove edges to new evicted-by module which is now replaced by a chain
    // dependend -> [evicted] -> dependee
    val evictionTargetEdges =
      graph.edges.filter(targetWasEvicted).map {
        case (from, evicted) ⇒ (from, evicted.copy(version = graph.module(evicted).evictedByVersion.get))
      }.toSet

    val filteredEdges =
      graph.edges
        .filterNot(e ⇒ originWasEvicted(e) || evictionTargetEdges(e)) ++ evictedByEdges

    val edges = {
      for (e ← filteredEdges) yield {
        val extra = if (graph.module(e._1).isEvicted)
          s""" [label="Evicted By" style="$EvictedStyle"]""" else ""
        """    "%s" -> "%s"%s""".format(e._1.idString, e._2.idString, extra)
      }
    }.mkString("\n")

    "%s\n%s\n%s\n}".format(dotHead, nodes, edges)
  }

  sealed trait HTMLLabelRendering {
    def renderLabel(labelText: String): String
  }
  /**
   *  Render HTML labels in Angle brackets as defined at http://graphviz.org/content/node-shapes#html
   */
  case object AngleBrackets extends HTMLLabelRendering {
    def renderLabel(labelText: String): String = s"label=<$labelText>"
  }

  /**
   * Render HTML labels with `labelType="html"` and label content in double quotes as supported by
   * dagre-d3
   */
  case object LabelTypeHtml extends HTMLLabelRendering {
    def renderLabel(labelText: String): String = s"""labelType="html" label="$labelText""""
  }
}
