{:dev
 {:env {:name "development"
        :uri "http://localhost:5678"
        :datomic-db "test"
        :datomic-storage "mem"
        :datomic-storage-ip "127.0.0.1"}}
 :test
 {:env {:name "test"
        :datomic-db "test"
        :datomic-storage "mem"
        :datomic-storage-ip "127.0.0.1"
        :uri "http://localhost:5678"
        :npm "src"}}}
