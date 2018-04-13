Aetr
===

[![Build Status](https://travis-ci.org/paradoxical-io/aetr.svg?branch=master)](https://travis-ci.org/paradoxical-io/aetr)

API Execution Task Runner

AETR can run async steps as a workflow.  To start, a postgres DB is required. Run `create-db` to initialize the datastore.

The data model is represented by two concepts

1. StepTree
2. Run

StepTree
----
A step tree is a template of what to do. It is the workflow.  Tredbstdbtes are composed of 3 options, either a parent node which has children to be run in 
either sequential or parallel mode, or a child node which is an action.

Currently supported actions are executing POST requests on an API URL.  

URL's POSTed to should response back to the completion API with the passed in RunToken.

Run
-----

A run is an instance of a step tree. It is a distinct workflow.  Run's store the result of each node and parent nodes determine their results 
from children.


Development
---

Run `docker-compose up`

If this is your first time, create the db via:

```
docker run \
    -e DB_JDBC_URL=jdbc:postgresql://db:5432/aetr \
    --network aetr_default \
    paradoxical/aetr:1.0-SNAPSHOT create-db```

```

From then you can now run the server:

```
docker run -it -p 9999:9999 \
  -e DB_JDBC_URL=jdbc:postgresql://db:5432/aetr \
  --network aetr_default \
  paradoxical/aetr:1.0-SNAPSHOT server
```

