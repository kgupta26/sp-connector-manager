# Streaming Platform Connector Management

This is a POC to demonstrate management of Connect jobs on the Streaming Platform using a state file.

## Motivation
As the streaming platform grows in terms of use cases, so will the number of connectors. The idea is to automate the trivial tasks such as:
    
* Starting Connectors
* Removing Connectors
* Pausing Connectors
* Restarting Connectors
* Updating Connectors

With the help of some conformity (state file), we can control ownership of the connectors and automate notification based on the supplied owner's information when connector is having some issue, for example. Other advantages include automation of RFC when there is a new connector, or perhaps we need to update an existing one. Whatever the case maybe, each should go through a set of approval process, validation steps, and a report should be generated upon acceptance.

## Proof of Concept
This project is a service on top of Kafka Connect that reads a state file and applies changes to the Connect servers. For demonstration purposes we will show the working of following capabilities through this service API:

1. Pause a running connector
2. Resume a paused connector
3. Remove a connector
4. Start a new connector
5. Update an existing connector
6. Validate if the state file is in correct format
7. Show all the Running/Paused/Failed Connectors

## Let's begin

* Fork this project and then clone your fork.

### Start Connect Servers

* Start the confluent connect server (actually all the confluent stuff). Simply run the following command in your terminal. It takes a couple of minutes. (Feel free to get a cup of coffee!)

```bash
./scripts/start.sh
```

* Go `localhost:9021` and confirm 3 connectors running - `replicate-topic`, `elasticsearch-ksqldb`, and `wikipedia-irc`. Credential to control center is `superUser/superUser`.

### Start Connect Manager

* Start the Connect Manager service! You need to supply your github token to the service. This is needed so that it can read the state file `state/connector/state.json` in your fork. You need to export two variable for it to pull the state file correctly - `SOURCE_GITHUB_USER`, and `SOURCE_GITHUB_TOKEN`.

```
$ export SOURCE_GITHUB_USER=<your github username>
$ export SOURCE_GITHUB_TOKEN=<your github token>
``` 

Note: Please refer to [this document](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) to create your token if you don't have one already.

Finally, lets run it!

``` bash
$ sbt run
```

Your output should look like this.
``` bash
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Jan 03, 2021 4:57:49 PM com.twitter.finagle.Init$ $anonfun$once$1
INFO: Finagle version 20.12.0 (rev=762bc8512b820e936283d64f4ab27134969193b0) built at 20201210-195733
```

### Get a list of Running Connectors

* Validate. Let's check if the service is up. The Connect Manager is running on `localhost:8080`. Let's make a request to get a list of running connectors. Open a new terminal and run:

```
curl -X GET -H 'Authorization: open sesame' 'http://localhost:8080/connectors/running'
["replicate-topic","elasticsearch-ksqldb","wikipedia-irc"]
```

### Pause a connector
Now lets make a connector pause. It is important to understand that although the state file is present in this repository ideally it will exist in its own. We want to make sure the service, and the state files are decoupled. Because this is a POC, I thought it would be easier to deal with one repo. After all, one can decide the state file perhaps stays in S3. The idea is to make a pull request, and a deploy makes it available wherever we want. For now lets work with github only.

Open the file `state/connector/state.json` and change `paused` field to true for the connector named `wikipedia-irc`. Commit the file and push it your repository (fork).

```
$ git add state/connector/state.json
$ git commit -m "pause wikipedia-irc connector"
$ git push origin develop
```

Now, run the `sync` api on the connect manager.
```bash
$ curl -X POST -H 'Authorization: open sesame' 'http://localhost:8080/connectors/sync'
```

The response will be a list of connectors that were added, removed, updated, paused, and restarted. We can see `wikipedia-irc` being mentioned under paused. Congrats we successfully deployed a state change! 

To validate lets run another curl command to get a list of paused connectors.

```
$ curl -X GET -H 'Authorization: open sesame' 'http://localhost:8080/connectors/paused'
["wikipedia-irc"]
```

### Resume a Connector
Let's resume the connector we paused above. Open the state file again and set the value from true to false for `paused` field for connector named `wikipedia-irc`.

``` bash
$ git add state/connector/state.json
$ git commit -m "resume wikipedia-irc connector"
$ git push origin develop
```

Run `sync`

``` bash
curl -X POST -H 'Authorization: open sesame' 'http://localhost:8080/connectors/sync'
```

You should see `wikipedia-irc` under resumed list. 

### Remove a Connector
Let's just remove that connector again. Remove ( and paste it in a notepad temporarily...) the JSON structure for connector name `wikipedia-irc`. Commit and push again. Then run the `sync` command again.

```
$ git add state/connector/state.json
$ git commit -m "remove wikipedia-irc connector"
$ git push origin develop
```

``` bash
curl -X POST -H 'Authorization: open sesame' 'http://localhost:8080/connectors/sync'
```

You should see `wikipedia-irc` in the added list. 

### Start a new Connector
Since we have removed the above connector we can add it back in as a new one. Paste back in the configuration into the state file as it was. Commit, push & sync again!

```
$ git add state/connector/state.json
$ git commit -m "start a new connector as wikipedia-irc"
$ git push origin develop
```  

``` bash
curl -X POST -H 'Authorization: open sesame' 'http://localhost:8080/connectors/sync'
```

You should see `wikipedia-irc` in the added section!

### Update a Connector
Let's update the configuration of the `wikipedia-irc` connector. Go to `irc.channels` field and remove the first item - `#en.wikipedia`. Commit, push, & sync!

```
$ git add state/connector/state.json
$ git commit -m "udpate connector as wikipedia-irc"
$ git push origin develop
```  

``` bash
curl -X POST -H 'Authorization: open sesame' 'http://localhost:8080/connectors/sync'
```

Now it should `wikipedia-irc` in the updated section of the response!

## Validation
Prior to making the `sync` calls we might have issues where format of the state file is syntactically wrong. Or perhaps two connectors cannot have the same name. Errors like these can happen. Therefore, we need to be able to catch such things. 

Lets go ahead and make an error! Make a type and instead of `paused`, name it something incorrect `pausedd`. Commit, push & _validate_.

```bash
curl -X GET -H 'Authorization: open sesame' 'http://localhost:8080/validate'
Cannot find field: pausedd in message com.massmutual.streaming.model.SPConnectorDefinition
```

As you can see it can validate the state of the file. With the `validation` api we can extend it do further validation so that all our connectors abide a standard and best practices.

Feel free to play more and maybe change the name of the connectors so that there are multiple of them with the same name. And when we call the `validate` api again, it should return a status of Bad Request and associated error message. 

