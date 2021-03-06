/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.deploy

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.apache.spark.deploy.SparkSubmit._

class SparkSubmitSuite extends FunSuite with ShouldMatchers {
  test("prints usage on empty input") {
    val clArgs = Array[String]()
    // val appArgs = new SparkSubmitArguments(clArgs)
  }

  test("handles YARN cluster mode") {
    val clArgs = Array("thejar.jar", "--deploy-mode", "cluster",
      "--master", "yarn", "--executor-memory", "5g", "--executor-cores", "5",
      "--class", "org.SomeClass", "--jars", "one.jar,two.jar,three.jar",
      "--arg", "arg1", "--arg", "arg2", "--driver-memory", "4g",
      "--queue", "thequeue", "--files", "file1.txt,file2.txt",
      "--archives", "archive1.txt,archive2.txt", "--num-executors", "6")
    val appArgs = new SparkSubmitArguments(clArgs)
    val (childArgs, classpath, sysProps, mainClass) = createLaunchEnv(appArgs)
    val childArgsStr = childArgs.mkString(" ")
    childArgsStr should include ("--jar thejar.jar")
    childArgsStr should include ("--class org.SomeClass")
    childArgsStr should include ("--addJars one.jar,two.jar,three.jar")
    childArgsStr should include ("--executor-memory 5g")
    childArgsStr should include ("--driver-memory 4g")
    childArgsStr should include ("--executor-cores 5")
    childArgsStr should include ("--args arg1 --args arg2")
    childArgsStr should include ("--queue thequeue")
    childArgsStr should include ("--files file1.txt,file2.txt")
    childArgsStr should include ("--archives archive1.txt,archive2.txt")
    childArgsStr should include ("--num-executors 6")
    mainClass should be ("org.apache.spark.deploy.yarn.Client")
    classpath should have length (0)
    sysProps should have size (0)
  }

  test("handles YARN client mode") {
    val clArgs = Array("thejar.jar", "--deploy-mode", "client",
      "--master", "yarn", "--executor-memory", "5g", "--executor-cores", "5",
      "--class", "org.SomeClass", "--jars", "one.jar,two.jar,three.jar",
      "--arg", "arg1", "--arg", "arg2", "--driver-memory", "4g",
      "--queue", "thequeue", "--files", "file1.txt,file2.txt",
      "--archives", "archive1.txt,archive2.txt", "--num-executors", "6")
    val appArgs = new SparkSubmitArguments(clArgs)
    val (childArgs, classpath, sysProps, mainClass) = createLaunchEnv(appArgs)
    childArgs.mkString(" ") should be ("arg1 arg2")
    mainClass should be ("org.SomeClass")
    classpath should contain ("thejar.jar")
    classpath should contain ("one.jar")
    classpath should contain ("two.jar")
    classpath should contain ("three.jar")
    sysProps("spark.executor.memory") should be ("5g")
    sysProps("spark.executor.cores") should be ("5")
    sysProps("spark.yarn.queue") should be ("thequeue")
    sysProps("spark.yarn.dist.files") should be ("file1.txt,file2.txt")
    sysProps("spark.yarn.dist.archives") should be ("archive1.txt,archive2.txt")
    sysProps("spark.executor.instances") should be ("6")
  }

  test("handles standalone cluster mode") {
    val clArgs = Array("thejar.jar", "--deploy-mode", "cluster",
      "--master", "spark://h:p", "--class", "org.SomeClass", "--arg", "arg1", "--arg", "arg2",
      "--supervise", "--driver-memory", "4g", "--driver-cores", "5")
    val appArgs = new SparkSubmitArguments(clArgs)
    val (childArgs, classpath, sysProps, mainClass) = createLaunchEnv(appArgs)
    val childArgsStr = childArgs.mkString(" ")
    print("child args: " + childArgsStr)
    childArgsStr.startsWith("--memory 4g --cores 5 --supervise") should be (true)
    childArgsStr should include ("launch spark://h:p thejar.jar org.SomeClass arg1 arg2")
    mainClass should be ("org.apache.spark.deploy.Client")
    classpath should have length (0)
    sysProps should have size (0)
  }

  test("handles standalone client mode") {
    val clArgs = Array("thejar.jar", "--deploy-mode", "client",
      "--master", "spark://h:p", "--executor-memory", "5g", "--total-executor-cores", "5",
      "--class", "org.SomeClass", "--arg", "arg1", "--arg", "arg2",
      "--driver-memory", "4g")
    val appArgs = new SparkSubmitArguments(clArgs)
    val (childArgs, classpath, sysProps, mainClass) = createLaunchEnv(appArgs)
    childArgs.mkString(" ") should be ("arg1 arg2")
    mainClass should be ("org.SomeClass")
    classpath should contain ("thejar.jar")
    sysProps("spark.executor.memory") should be ("5g")
    sysProps("spark.cores.max") should be ("5")
  }

  test("handles mesos client mode") {
    val clArgs = Array("thejar.jar", "--deploy-mode", "client",
      "--master", "mesos://h:p", "--executor-memory", "5g", "--total-executor-cores", "5",
      "--class", "org.SomeClass", "--arg", "arg1", "--arg", "arg2",
      "--driver-memory", "4g")
    val appArgs = new SparkSubmitArguments(clArgs)
    val (childArgs, classpath, sysProps, mainClass) = createLaunchEnv(appArgs)
    childArgs.mkString(" ") should be ("arg1 arg2")
    mainClass should be ("org.SomeClass")
    classpath should contain ("thejar.jar")
    sysProps("spark.executor.memory") should be ("5g")
    sysProps("spark.cores.max") should be ("5")
  }
}
