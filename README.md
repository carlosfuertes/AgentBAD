=== Installing SCALA

* $brew install scala -> deberia ser la version 0.9.2
* setup documentation
    $ cd /usr/local/Cellar/scala/2.9.2/libexec/
    $ mkdir -p scala-devel-docs/api
    $ wget http://www.scala-lang.org/downloads/distrib/files/scala-docs-2.9.2.zip
    $ unzip scala-docs-2.9.2.zip
    $ mv scala-docs-2.9.2/* ./
    $ rm -rf scala-docs-2.9.2
* setup scala en el proyecto intellij -> scala module -> set scala home -> /usr/local/Cellar/scala/2.9.2/libexec


=== Installing sbt

* $brew install sbt

=== Running the project using IntelliJ

* IntelliJ deberia utilizar Maven para resolver las dependencias.
* El proyecto Maven está descrito en el fichero pom.xml
* botón derecho en la clase ejecutable y después 'run NameOfTheFile'

=== Running the project using SBT

* SBT mantiene su configuración en el fichero build.sbt
* para ejecutar el proyecto se puede utilizar $sbt run
e.g:
  antonio@Antonios-MacBook-Pro-4:~/Development/Business/AgentBAD [master] $ sbt run
  [info] Set current project to AgentBAD (in build file:/Users/antonio/Development/Business/AgentBAD/)
  [info] Updating {file:/Users/antonio/Development/Business/AgentBAD/}default-28b7f5...
  [info] Resolving com.typesafe#config;0.3.1 ...
  [info] Done updating.
  [info] Running mmaker.TestMain
  ** RECEIVED hey
  ** RECEIVED ping
  ** TERMINATING
  [success] Total time: 5 s, completed Dec 31, 2012 11:06:09 PM
