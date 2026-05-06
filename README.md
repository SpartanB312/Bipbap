# Bipbap

[![CodeFactor](https://www.codefactor.io/repository/github/spartanb312/bipbap/badge)](https://www.codefactor.io/repository/github/spartanb312/bipbap)

This is a ready-to-use lightweight obfuscator without configuration and dependency requirements. Bipbap aims for those developers without obfuscation experience. It will automatically figure and exclude most in-jar dependencies and mixin classes. And its easy-to-use HardwareID authenticator injector will automatically insert authenticator in your jar.

## Usage

In command lines:

Open UI: java -jar bipbap.jar `-ui`

Generate a config: java -jar bipbap.jar `-genconfig` `config`

- `config` The config JSON file that will be generated

Use your config: java -jar bipbap.jar `config`

- `config` The specified config JSON file that will be used

Use our presets: java -jar bipbap.jar `preset` `input.jar` `output.jar` `threads` `authentication`

- `preset` (Optional) Select one preset in -low -mid -high. If no preset is selected, -low will be used
- `input.jar` (Optional) The jar file that will be processed. If not specified, input.jar will be used
- `output.jar` (Optional) The jar file that will be written. If input is specified but output is not, input-obf.jar will be used
- `threads` (Optional) format: -mt:4. If not specified, one thread will be used. Use -mt or -mt:-1 to use all available processors
- `authentication` (Optional) format: -auth=https://authentication.com

## Presets

| Preset  | Activated features                                                                              |
|---------|-------------------------------------------------------------------------------------------------|
| `-low`  | CodeOptimize, ConstEncrypt, Renamer(LocalVar), HideCode                                         |
| `-mid`  | CodeOptimize, ConstEncrypt, Renamer(LocalVar, Field, Method), InvokeDynamics, HideCode          |
| `-high` | CodeOptimize, ConstEncrypt, Renamer(LocalVar, Field, Method), InvokeDynamics, Crasher, HideCode |

## Features

### Obfuscation

* [X] HWIDAuthenticator
* [X] FieldRename
* [X] MethodRename
* [X] LocalVarRename
* [X] NumberEncrypt
* [X] StringEncrypt
* [X] InvokeDynamics
* [X] CodeHider
* [X] Watermark
* [X] Crasher

### Optimization

* [X] RemoveSource
* [X] RemoveInnerClass
* [X] RemoveDeadCodes
* [X] KotlinOptimize

## License: MIT

This is a free and open source software under MIT license

