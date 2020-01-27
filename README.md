# Schema Cartographer

<img width="2290" alt="Cartographer-ScreenShot" src="https://user-images.githubusercontent.com/4416952/71586247-254b8000-2adf-11ea-9599-2ae2102e4e78.png">

*Schema Cartographer* provides a means to visualize, navigate, create, edit and share the relationships that exist in your Datomic schema.

# Table Of Contents
* [Usage CLJ](https://github.com/JarrodCTaylor/schema-cartographer#clj)
* [Usage CLJS](https://github.com/JarrodCTaylor/schema-cartographer#Client)
* [Convention & Schema Annotations](https://github.com/JarrodCTaylor/schema-cartographer#conventions--schema-annotations)
    * [Basic Example](https://github.com/JarrodCTaylor/schema-cartographer#basic-example)
    * [Complete Example Schema](https://github.com/JarrodCTaylor/schema-cartographer#complete-example-schema)
* [License](https://github.com/JarrodCTaylor/schema-cartographer#copyright-and-license)

# CLI

## Output Schema File

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
  -a, --audit          Audit schema annotations and log gaps. Boolean
  -h, --help
```

### Example Usage

    clojure -m cli.core -r "us-east-1" -s "my-system" -d "ice-cream-shop" -o "ice-cream-shop-schema"

The resulting schema file is saved to the `/doc` directory

## Audit Schema Annotations

To ensure your schema is properly annotated run the audit script. This will identify missing namespaces, references, etc.

    clojure -m cli.core -r "us-east-1" -s "my-system" -d "ice-cream-shop" --audit

The results are logged to the console.

## Running tests

    bin/kaocha clj-unit

# Client

A statically hosted version of the application can be found at [https://c-132.com/schema-cartographer](https://c-132.com/schema-cartographer)

## Running locally

* Install application deps: `yarn install`
* Install sass:             `(cd src/sass && yarn install && yarn css)`
* Start application:        `clojure -Acljs-dev`
* App will be running at    `http://localhost:9875/#/`

## Run Tests

    clj -A:cljs-test

Then visit:

    http://localhost:8021/

## Sass Stylesheets (.scss)

Stylesheet source files are located in `/src/sass` and are written in [Sass](http://sass-lang.com/) `.scss` syntax.

Stylesheet Development:

``` sh
# Install dependencies
cd src/sass
yarn install

# Compile CSS once, while in /sass dir
yarn css

# Compile and watch, while in /sass dir
yarn watch:css
```

## Package For Deployment

* `(cd src/sass && yarn css)`
* `clojure -Acljs-min`
* The app will be in `resources/public/`

# Conventions & Schema Annotations

Per the [documentation](https://docs.datomic.com/cloud/best.html#annotate-schema) "Datomic Schema is stored as data, you can and should annotate your schema elements with useful information".
Those useful annotations can enable many things. This particular application uses annotations to build a visualization of the structure and relationships between
schema elements.

By creating idents that are designated as a `namespace`. We can provide documentation related to the intended usage and attributes of the group of entities that
have `:db/ident`'s which share the same namespace. This information can be leveraged to create a very traditional feeling relational data like visualization of the schema.

The annotations used by the application are:

``` clojure
(def annotation-schema-tx [{:db/ident :db.schema/deprecated?
                            :db/valueType :db.type/boolean
                            :db/cardinality :db.cardinality/one
                            :db/doc "DOCUMENTATION ONLY. Boolean flag indicating the field has been deprecated."}
                           {:db/ident :db.schema/replaced-by
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/many
                            :db/doc "DOCUMENTATION ONLY. Used to document when a deprecated field is replaced by another."}
                           {:db/ident :db.schema/references-namespaces
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/many
                            :db/doc "DOCUMENTATION ONLY. Used to indicate which specific :db/idents are intended to be referenced by :db.type/ref"}
                           {:db/ident :db.schema/validates-namespace
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/one
                            :db/doc "DOCUMENTATION ONLY. Used to indicate which specific :db/idents are intended to be validated by :db.type/ref"} ])
```

## Basic Example

### Defining An Entity

By adhering to a convention of assigning a `:db/ident` a value of a keyword with a namespace of `:db.schema.entity.namespace` and a name of `store`.
We can then conclude all elements with a `:db/ident` having a keyword with the namespace `store` will be grouped together in the application.

``` clojure
;; == Create a 'namespace' of 'store'
{:db/ident :db.schema.entity.namespace/store
:db/doc   "An entity representing an individual ice cream store"}
;; == The following elements all have idents with a keyword namespace of 'store' and will be grouped together
{:db/ident      :store/id
:db/valueType   :db.type/uuid
:db/cardinality :db.cardinality/one
:db/unique      :db.unique/identity
:db/doc         "Unique id assigned to each store"}
{:db/ident      :store/address
:db/valueType   :db.type/string
:db/cardinality :db.cardinality/one
:db/doc         "Street address of a specific store location"}
{:db/ident      :store/employees
:db/valueType   :db.type/ref
:db/cardinality :db.cardinality/many
:db/doc         "Employees who may work at a given store"
:db.schema/references-namespaces ["employee"]} ;; Specifies a specific entity that is referenced
```

### Defining An Ident

By adhering to a convention of assigning a `:db/ident` a value of a keyword with a namespace of `:db.schema.ident.namespace` and a name of `ice-cream-flavor`.
We can then conclude all elements with a `:db/ident` having a keyword with the namespace `ice-cream-flavor` will be grouped together in the application.

``` clojure
;; == Create a 'namespace' of 'ice-cream-flavor'
{:db/ident :db.schema.ident.namespace/ice-cream-flavor
:db/doc   "Ice cream flavor options, currently available in store."}
;; == The following ident all have idents with a keyword namespace of 'ice-cream-flavor' and will be grouped together
{:db/ident :ice-cream-flavor/strawberry}
{:db/ident :ice-cream-flavor/chocolate}
{:db/ident :ice-cream-flavor/vanilla}
```

# Complete Example Schema

Create a connection to a new Datomic database and transact the following [example schema](https://github.com/JarrodCTaylor/schema-cartographer/blob/master/resources/complete-example-schema.clj). This will provide a complete fully annotated schema suitable to experiment with the application and for use as a reference.

## Copyright and License

Copyright © 2019 Jarrod Taylor

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
