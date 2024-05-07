# transaction-manager-server

## Build service

In order to build this service you have to have an installed:

* [Maven](https://maven.apache.org/download.cgi)
* [JDK 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html) (you can a JDK from another provider as well)
* [Docker](https://www.docker.com/products/docker-desktop/) (for testing purposes)

### Building

In order to build this service with testing you should execute following command:

```shell
mvn clean install
```

This will launch incremental build of modules and also running tests. This includes integration tests
which requires a Docker to be up and running (we are using _PostgreSQL_ Docker image for testing).

As a result you will have a JAR file for your service in the `transaction-manager/target/` folder.

### Building without tests

In order to skip testing phase (it will skip compiling and running tests), you can use following command:

```shell
mvn clean install -DskipTests
```

This will compile a JAR file under `transaction-manager/target/` path (should have name something like `transaction-manager-0.0.1.jar`).

## Run the server

In order to run the server you have to create Spring properties file (`transaction-manager/src/main/resources/application-<profile>.yml`). 
Template for the properties file can be found at: `transaction-manager/src/main/resources/application-template.yml`

### Placeholders

You have to define values for placeholders or replace placeholders with relevant properties. These are:

* `POSTGRES_JDBC_URL` - JDBC URL to PostgreSQL server
* `POSTGRES_USER` - username to use for authorization in PostgreSQL
* `POSTGRES_PASSWORD` - password for authorization in PostgreSQL
* `TRANSATRON_URL` - URL to TransaTron server
* `TRANSATRON_API_KEY` - API key for authorization on TransaTron server
* `PAYMENT_DEPOSIT_ADDRESS` - an address for receiving payments from users
* `TRONGRID_PRIVATE_KEY` - private key to use in `ApiWrapper` (does not necessary have to be a wallet with resources, we won't use this wallet for making transactions, it is just that we cannot create `ApiWrapper` instance without it)
* `TRONGRID_API_KEY` - API key for authorization on TronGrid
* `TRON_ENERGY_MARKET_WALLET` - Tron Energy Market wallet address
* `TRON_ENERGY_API_KEY` - API key for authorization on Tron Energy Market

### Run

You can run the server with the following command:

```shell
java -jar -Dspring.profiles.active=<profile> transaction-manager/target/transaction-manager-0.0.1.jar
```

Where `<profile>` is a profile that you have created and placed under `transaction-manager/src/main/resources/` folder

### Private keys

NOTE: authors of this project do not recommend you to store private keys in properties of production systems, this approach is valid only for PoC

In order for service being able to delegate and reclaim resources from user addresses we have to sign those transactions.
In order to sign those transactions we are storing private keys in properties file under, `wallets.private-keys` 
property which expects a mapping of address (Tron58 format) to private key. 

## Recommendations

### Recommended JDK

At the moment of writing it is recommended to use _Correto JDK_ provided by _AWS_, version 17 (LTS version of JDK)

### Recommended JDK options

#### Memory tuning options

It is recommended to explicitly specify amount of memory to use by the application:

* `-Xms` - initial memory allocation pool (which is also a minimal)
* `-Xmx` - maximum memory allocation pool
* `-XX:+UseG1GC` - use G1 garbage collector

#### Service tuning

It is also recommended to use following params which may help you with troubleshooting whenever it happens.

##### JMX options

* `-Dcom.sun.management.jmxremote`
* `-Dcom.sun.management.jmxremote.host=<host-mask>`
* `-Dcom.sun.management.jmxremote.port=<port>`
* `-Dcom.sun.management.jmxremote.rmi.port=<port>`
* `-Dcom.sun.management.jmxremote.local.only=false`
* `-Dcom.sun.management.jmxremote.authenticate=false`
* `-Dcom.sun.management.jmxremote.ssl=false`

##### Error logging

* `-XX:+HeapDumpOnOutOfMemoryError` - indicates that JVM should generate a heap dump when allocation of the Java memory cannot be satisfied
* `-XX:HeapDumpPath=/path/to/directory/transaction-manager-<timestamp>.hprof` - a path to the heap dump file
* `-XX:ErrorFile=/path/to/directory/transaction-manager-<timestamp>_error.log` - a path to fatal error log
