Earthcraft
==========

Earthcraft is a bukkit world generator plugin which renders real-world mapping
data in Minecraft.


Installing
----------

Add earthcraft-<VERSION>.jar to your craftbukkit server's plugin directory.

Building from source
--------------------

Source code can be downloaded from https://github.com/sbliven/earthcraft.

To fetch dependencies and build the jar, execute

    mvn package

This will produce the jar in target/earthcraft-<VERSION>.jar

Data Sources
------------

**MapQuest**The OpenElevationConnector requires a MapQuest API key. You can
register for one for free at https://developer.mapquest.com/.

License
-------

Earthcraft licensed under the GPL.

Additionally, the earthcraft binaries may include the following libraries:

* [Bukkit](http://bukkit.org/) (GPL)
* [GeoTools](http://geotools.org) (LGPL)
* [Apache Commons](http://projects.apache.org/projects/commons_collections.html) (Apache License Version 2.0)
* [Java Topology Suite](http://www.vividsolutions.com/jts/JTSHome.htm) (LGPL)


