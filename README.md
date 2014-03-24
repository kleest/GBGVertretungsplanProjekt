#GBG Vertretungsplan
[![Get it on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=de.stkl.gbgvertretungsplan)

The following information is only available in German because the Android app relates to a German school. However, code and comments are written in English and should be easy to understand.  
Feel free to contact myself (e.g. via the Google Play Store) if you have questions or if you are interested in an English localization of this README.

## Einleitung
Die offizielle App für den Online-Vertretungsplan des Georg-Büchner-Gymnasiums Bad Vilbel. Damit ist ein komfortabler Zugriff auf den Vertretungsplan jederzeit und von überall aus möglich!
Zur Nutzung der App sind Zugangsdaten erforderlich, die allen Schülerinnen und Schülern durch ihre Klassenlehrer bzw. Tutoren mitgeteilt wurden. Sie entsprechen denen für den Zugriff auf den Online-Vertretungsplan über die GBG-Webseite.

Weitere Details: [https://play.google.com/store/apps/details?id=de.stkl.gbgvertretungsplan](https://play.google.com/store/apps/details?id=de.stkl.gbgvertretungsplan)

Der hier verfügbare Quellcode spiegelt nicht den gesamten Quellcode wider, der erforderlich ist, um die finale App bauen zu können. Die Webseite-Kommunikation fehlt (de.stkl.gbgvertretungsplan.priv.GBGCommunication). Diese Klasse implementiert de.stkl.gbgvertretungsplan.networkcommunication.CommunicationInterface. Zur korrekten Funktionsweise der App ist jenes Interface zu implementieren.

## Lizenz
Copyright 2014 Steffen Klee  

Licensed under the Apache License, Version 2.0 (the "License");  
you may not use this file except in compliance with the License.  
You may obtain a copy of the License at  

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software  
distributed under the License is distributed on an "AS IS" BASIS,  
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
See the License for the specific language governing permissions and  
limitations under the License.  

## Open-Source-Lizenzen
[jsoup (1.7.3)](http://jsoup.org/): [MIT License](http://opensource.org/licenses/mit-license.php)  
Copyright (c) 2009, 2010, 2011, 2012, 2013 Jonathan Hedley <jonathan@hedley.net>

[TableFixHeaders](https://github.com/InQBarna/TableFixHeaders): [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)  
Copyright 2012-2013 InQBarna

[Android Support Library v4 + v7 (appcompat)](https://developer.android.com/tools/support-library/index.html): [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)  
Copyright (C) 2012-2013 The Android Open Source Project

## Building
Die App wurde mit Android Studio erstellt, ein Öffnen des Projekts sollte einfach möglich sein. Ebenfalls lässt sich Gradle über eine Shell nutzen.