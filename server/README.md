# Server

## Running Server Locally

The following will start a local server running on port `9876`

``` sh
clojure -Alocal-server
```

## Output Schema File

Although it provides additional information and metrics the client does not need to rely on a local server to provide the schema.
To generate a schema file loadable by the application, the following script is provided:

``` sh
clojure -m server.core -h
Schema Cartographer Server:
Usage: clojure -Alocal-server

Schema Cartographer Schema Export:
Usage: clojure -m server.core [options]

Options:
  -r, --region REGION  Region where Datomic cloud is located
  -s, --system SYSTEM  Datomic cloud system name
  -d, --db DATABASE    Database Name
  -o, --output FILE    Write schema edn to FILE
  -h, --help
```

### Example Usage

`clojure -m server.core -r "us-east-1" -s "my-system" -d "ice-cream-shop" -o "ice-cream-shop-schema"`

The resulting schema file is saved to the `/doc` directory

## Running tests

``` sh
bin/kaocha
```
