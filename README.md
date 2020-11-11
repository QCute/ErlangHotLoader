# Erlang Hot Loader Plugin
Erlang Hot Loader for IntelliJ IDEs (e.g. IntelliJ IDEA, ...)

Compile and Load Current Module File, See Edit menu or use Shift + Alt + H.

## Configuration
* SDK 
  * Project Structure -> Project -> Project SDK
* Compiler Options
  * Settings -> Build, Execution, Deployment -> Compiler -> Erlang Compiler
* Output Path
  * Default
    * Project Structure -> Project -> Project Compiler Output
  * Custom
    * Project Structure -> Modules (Current Module) -> Paths -> Use Module Compile Output Path (Not For Test)
* Node and Cookie 
  * Run -> Edit Configurations 
    * Erlang Application (flags for 'erl') 
    * Erlang Console (Shell Arguments) 
    * Erlang Rebar (${BASE_PATH}/config/vm.args)
* Target
  * Run -> 
    * Run 
    * Debug (Selected Configuration)

## Install
Use your IDE. Settings/Plugins/Marketplace search for "Erlang Hot Loader".

## Build
* Clone repo ``` git clone http://github.com/QCute/ErlangHotLoader  ```  

* Download [Dependencies Plugin](https://plugins.jetbrains.com/plugin/7083-erlang)  

* Extract jar to lib directory  

* Add jar to SDKs -> IntelliJ IDEA IU-* -> ClassPath  

* Open the project in IntelliJ IDEA.  

* Build -> Build Project -> Prepare Plugin Module 'ErlangHotLoader' Deployment

## Dependencies
* [intellij-erlang](https://github.com/ignatov/intellij-erlang/)

## Helps
* [Plugin Dependencies](https://jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_dependencies.html)
