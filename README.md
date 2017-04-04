# osi-clj

A clj lib to ease webapp and API dev using datomic, ring and compojure

## Logger

Logging support using https://github.com/pyr/unilog

## DB

DB macros to make working with Datomic easy

### defent

Usage

    (defent exr
      :db exrs
      :schema [uuid :uuid :unique-identity]
              [name :string :indexed])
              
Defines the following fns in the namespace:
* db-uri: returns the datomic DB URI based on environ vars (datomic-db, datomic-storage and datomic-storage-ip)
* db-conn: returns a datomic connection for transactions
* db: returns a DB value for queries
* squuid: returns a new sequential UUID
* q: helper fn to run datomic query without having to pass a DB in
* qf: like q but wraps return value in (map first %)
* qff: like qf but calls first on its return value
* pull: helper fn to pull entity by id or lookup ref. without having to pass a DB in
* pull-many: like pull but for multiple ids or lookup refs
* tx: helper fn to transact datums and return solved entities, also assigns tempids if missing
* rm: helper fn to retract entity by id or lookup ref

## HTTP

HTTP client and handler helpers

## Test

DB and HTTP test helpers

## Deploy

Docker deploy helpers

## License

Copyright © 2016-2018 OptimisCorp & contributors.

Released under the MIT license.
