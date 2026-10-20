FaithfulBlocks 1.6.0-rc2
=======================

Minecraft: 26.3
Modloader: Fabric
Fabric Loader: 0.19.5 oder neuer
Fabric API: 0.161.0+26.3 oder neuer
Java: 25

FaithfulBlocks erweitert das Vanilla-Bausystem um detailreiche, Minecraft-nahe Architekturformen für Burgen, Kirchen, Kathedralen, Türme, Stadtmauern und andere historische oder monumentale Bauwerke.

KERNFUNKTIONEN
--------------
- 46 unterstützte Vanilla-Baumaterialien
- direkte Referenzierung der Vanilla-Texturen für hohe Resource-Pack-Kompatibilität
- eigener Architektur-Arbeitstisch mit Formauswahl und eigener FaithfulBlocks-Textur
- abgerundete Außen- und Innenecken
- abgeschrägte Außen- und Innenecken
- schmale Säule und große Säule, vertikal und horizontal platzierbar
- normaler Sockel und Großsäulen-Sockel als flexible Abschlüsse
- Pilaster / Halbsäule
- Bogen mit automatischen Innen-/Außenecken sowie normaler/umgedrehter Platzierung
- Wandsockel und Wandgesims mit automatischen Eckverbindungen
- Mauerabdeckung und Wandabschluss
- Fassadenplatte / Versatzblock
- Fensterrahmen mit pane-artiger vertikaler Platzierung, automatischer Nachbarausrichtung und direkt einsetzbaren Vanilla-Glasscheiben in allen 16 Farben
- Schräg-/Stufenblock mit treppenartiger Platzierung sowie Innen-/Außenecken

ARCHITEKTUR-ARBEITSTISCH
------------------------
Rezept:

S S S
S C S
P P P

S = Steinziegel
C = Steinmetz
P = beliebige Holzbretter

Ein unterstütztes Basismaterial in den Tisch legen, gewünschte Architekturform auswählen und das Ergebnis entnehmen.

Bereits hergestellte FaithfulBlocks-Architekturblöcke können ebenfalls in den Tisch gelegt werden. Dann erscheint die Option "Ursprungsblock zurück" und ein Element wird wieder in genau einen ursprünglichen Vanilla-Materialblock umgewandelt.

FENSTERRAHMEN
-------------
Eine klare oder gefärbte Vanilla-Glasscheibe halten und ohne Schleichen einen Fensterrahmen rechtsklicken. Genau eine Scheibe wird eingesetzt. Im Survival-Modus wird sie verbraucht; im Creative-Modus nicht.

Beim Abbau droppt ein leerer Rahmen nur den Rahmen. Ein Rahmen mit eingesetzter Scheibe droppt zusätzlich exakt die eingesetzte Scheibe.

BUILD UNTER WINDOWS
-------------------
1. JDK 25 installieren.
2. BUILD-WINDOWS.bat starten.
3. Die fertige JAR liegt anschließend unter:
   build\libs\faithfulblocks-1.6.0-rc2.jar
4. Die alte FaithfulBlocks-Version aus dem mods-Ordner entfernen.
5. faithfulblocks-1.6.0-rc2.jar zusammen mit Fabric API in den mods-Ordner legen.

ENTWICKLUNGSSTAND / QA
----------------------
Der Release 1.5.0 wurde vor dem Final Build über mehrere Release-Candidate-Runden geprüft. Getestet wurden unter anderem:
- alle sichtbaren Architekturformen
- Survival-Crafting und Drops
- Werkzeugzuordnung und Abbauverhalten
- Vanilla- und externe Resource Packs
- Dedicated Server / Multiplayer
- Speichern und erneutes Laden von Blockzuständen
- größere Mengen von FaithfulBlocks-Blöcken

Ein zusätzlicher Offline-Validator prüft Ressourcen, Modelle, Loot-Tabellen, Werkzeugtags, Sprachen und Arbeitstisch-Registrierungen.

KOMPATIBILITÄT
--------------
FaithfulBlocks kopiert für Architekturmaterialien keine Vanilla-Texturen. Die Modelle referenzieren die jeweiligen minecraft:block/... Ressourcen direkt. Resource Packs, die diese Vanilla-Texturen ersetzen, wirken deshalb automatisch auf die entsprechenden FaithfulBlocks-Formen.

LIZENZ
------
All Rights Reserved. Siehe LICENSE.txt.


Projekt-Repository
------------------
https://github.com/MrCage1997/FaithfulBlocks
