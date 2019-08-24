## H2 Web Console to check table data

http://localhost:8080/h2

**h2 URL:** jdbc:h2:mem:dataSource:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL

*No password*


## REST services

**Get details visitors of an existing request:** http://localhost:8080/{visitRequestId}

**Exchange contact with visitor:** http://localhost:8080/exchange/{visitRequestId}/{visitorIdToReplace}/{contactIdToExchange}


## Test run

Some data is created automatically during startup, check H2 web console for details

```
Initial data
    VisitRequest ID:1
        Visitor ID: 2
        Visitor ID: 3
    
    ConactPerson ID: 4

#http://localhost:8080/{visitRequestId}
$ curl http://localhost:8080/1
[{"class":"com.test.Visitor","id":2},{"class":"com.test.Visitor","id":3}]

#http://localhost:8080/exchange/{visitRequestId}/{visitorIdToReplace}/{contactIdToExchange}
$ curl http://localhost:8080/exchange/1/2/4
[{"class":"com.test.ContactPerson","id":4,"phoneNumber":"9999999999"},{"class":"com.test.Visitor","id":3}]

#http://localhost:8080/{visitRequestId}
$ curl http://localhost:8080/1
[{"class":"com.test.Visitor","id":3},{"class":"com.test.ContactPerson","id":4,"phoneNumber":"9999999999"}]
```