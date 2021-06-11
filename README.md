# BIOfid Literature Crawler

This crawler was created as part of the [BIOfid](https://www.biofid.de/en/)-project. It is primarily intended to crawl the [Biodiversity Heritage Library](https://www.biodiversitylibrary.org/) and [Zobodat](https://www.zobodat.at/index.php). However, the crawler was created to be highly extensible for any other text source. The crawler stores metadata and demanded text files in seperated subdirectories, depending on the file format.

Given a configuration file `config/harvesting.yml`, the crawler downloads all demanded items (i.e. books, monographies, a journal issue) and store them locally. In the configuration file the base output directory is given. Subsequently, all included crawlers create their own subdirectory and within these, they create two directories `text` and `metadata`, which store all text files and the metadata as XML, respectively.

## Requirements
The project needs OpenJDK 11+ and Maven 3.6+. At least the harvesting of items from the Botanical Garden of Madrid (via the BHLHarvester) will not work with Oracle Java 8, because of not available cipher suites for the TLS encryption.

### Building
To build the project simply call `mvn package`. This should give you a file `target/LiteratureCrawler.jar`. This you can run simply with 

`java -jar target/LiteratureCrawler.jar`

and the application will run.

#### Building in Docker
If you want or have to build a Docker image for the BIOfid Literature crawler, you can do this by:

```
docker build --tag literature-crawler:latest .
```

You should configure your harvesters BEFORE building, beause the config files are pushed to the image. However, there are ways to map the host config files to a container, using the `-v` parameter when calling `docker run`.

To run the image in a container, call:

```
mkdir output
mkdir logs
docker run -v "$PWD"/output:/harvesting -v "$PWD"/logs:/usr/src/literature-crawler/logs --user $(id -u):$(id -g) literature-crawler:latest
```

This command will put all the content generated in the container, put into the folder `output` in your current directory.

### Testing
To run all unit tests on a UNIX machine call `mvn test`.
The tests create a temporary directory at `/tmp/test`. This works on UNIX just fine, but the behavior was not tested on Windows machines.

## BHL Harvester
For the BHL Harvester it is mandatory to provide an BHL API key, which you can request [here](https://www.biodiversitylibrary.org/getapikey.aspx). You can provide this key either directly in the configuration file or only give a path to a file containing only the BHL key.

### Configuration
The BHL Harvester differentiates between single `items` and `titles`. Both can be provided as keywords in the configuration file followed by lists (even only with a single element). While `items` are processed "as is", `titles` (i.e. a series of books) are first resolved to their items and then these items are downloaded. 

## Custom Harvester
If you want to harvest another source, you can simply create a custom class extending the [Harvester](https://github.com/FID-Biodiversity/LiteratureCrawler/blob/master/src/main/java/de/biofid/services/crawler/Harvester.java) class and integrating the demanded abstract functions. After also giving it a name and a `class` setting in the configuration file, you should be fine.

## Bugs
If you find bugs, please do not hesitate to open an [issue](https://github.com/FID-Biodiversity/LiteratureCrawler/issues)!
