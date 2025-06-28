<div align="center">
    <a href="https://plugins.jetbrains.com/plugin/23170-jdbi-sql-jump">
        <img src="./src/main/resources/META-INF/pluginIcon.svg" width="280" height="280" alt="logo"/>
    </a>
</div>

<h1 align="center">Intellij Jdbi SQL Jump</h1>

<p align="center">
<a href="https://plugins.jetbrains.com/plugin/23170-jdbi-sql-jump"><img src="https://img.shields.io/jetbrains/plugin/r/stars/23170?style=flat-square"></a>
<a href="https://plugins.jetbrains.com/plugin/23170-jdbi-sql-jump"><img src="https://img.shields.io/jetbrains/plugin/d/23170-jdbi-sql-jump.svg?style=flat-square"></a>
<a href="https://plugins.jetbrains.com/plugin/23170-jdbi-sql-jump"><img src="https://img.shields.io/jetbrains/plugin/v/23170-jdbi-sql-jump.svg?style=flat-square"></a>
</p>

<br>

> Jetbrains Marketplace: https://plugins.jetbrains.com/plugin/23170-jdbi-sql-jump

<b>IntelliJ Jdbi SQL Jump<b/> is a development tool for working with JDBI.
In typical JDBI usage with the `@UseClasspathSqlLocator` annotation, SQL queries are stored in external files. This separation between code and SQL often makes development and navigation less convenient.

This plugin helps bridge that gap by enabling quick navigation from Java code to the corresponding SQL file, making your workflow smoother and more efficient.

There has two way to trigger a jump:
- Select the corresponding code, then click on "Jdbi SQL Jump" from the right-click menu.
  ![image](https://github.com/PinXian53/intellij-jdbi-sql-jump/blob/main/image/code-to-sql.gif)
- Place cursor inside SQL file, then click on "Jdbi SQL Jump" from the right-click menu.
  ![image](https://github.com/PinXian53/intellij-jdbi-sql-jump/blob/main/image/sql-to-code.gif)
