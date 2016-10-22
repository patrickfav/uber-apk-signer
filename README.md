# Uber Apk Signer
A tool that helps signing and zip aligning multiple files with either debug or provided release certificatas

Main features:

* Wildcard support for package names at the end or middle of the filter string: `com.android.*` or `com.android.*e`
* Possible to provide multiple packages to uninstall: `com.android.*,com.google.*,org.wiki*`
* Uninstalling on all connected devices

Basic usage:

    java -jar uber-apk-signer.jar -a /path/to/apks


This should run on any Windows, Mac or Linux machine where Java7+ is installed. 

## Download

[Grab jar from latest Release](https://github.com/patrickfav/uber-apk-signer/releases/latest)

## Command Line Interface

## Build

Use maven (3.1+) to create a jar including all dependencies

    mvn clean package

## Tech Stack

* Java 7
* Maven

# License

Copyright 2016 Patrick Favre-Bulle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
