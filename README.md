# BIOfid-Literature Crawler

This crawler was creates as part of the [BIOfid](https://www.biofid.de/en/)-project. It is primarily intended to crawl the [Biodiversity Heritage Library](https://www.biodiversitylibrary.org/) and [Zobodat](https://www.zobodat.at/index.php). However, the crawler was created to be highly extensible for any other text source.

Given a configuration file `config/harvesting.yml`, the crawler downloads all demanded items (i.e. books, monographies, a journal issue) and store them locally. In the configuration file the base output directory is given. Subsequently, all harvesters create their own subdirectory and within these, they create two directories `text` and `metadata`, which store all text files and the metadata as XML, respectively.

## Requirements
The project needs Java 8 and Maven 3.6+ (at least this is what it was build and tested with).

### Building
To build the project simply call `mvn package`.

### Testing
To run all unit tests on a UNIX machine call `mvn test`.
The tests create a temporary directory at `/tmp/test`. This works on UNIX just fine, but the behavior was not tested on Windows machines.

## BHL Harvester
For the BHL Harvester it is mandatory to provide an BHL API key, which you can request [here](https://www.biodiversitylibrary.org/getapikey.aspx). You can provide this key either directly in the configuration file or only give a path to a file containing only the BHL key.

## Custom Harvester
If you want to harvest another source, you can simply create a custom class extending the [Harvester](https://github.com/FID-Biodiversity/LiteratureCrawler/blob/master/src/main/java/de/biofid/services/crawler/Harvester.java) class and integrating the demanded abstract functions. After also giving it a name and a `class` setting in the configuration file, you should be fine.

## Bugs
If you find bugs, please do not hesitate to open an [issue](https://github.com/FID-Biodiversity/LiteratureCrawler/issues)!
