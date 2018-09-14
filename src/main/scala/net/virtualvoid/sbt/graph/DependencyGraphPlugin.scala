/*
 * Copyright 2011, 2012 Johannes Rudolph
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

import sbt._
import Keys._

object DependencyGraphPlugin extends AutoPlugin {
  object autoImport extends DependencyGraphKeys

  override def projectSettings: Seq[Def.Setting[_]] = DependencyGraphSettings.graphSettings

  override def trigger: PluginTrigger = AllRequirements

  override def requires: Plugins = plugins.JvmPlugin
}
