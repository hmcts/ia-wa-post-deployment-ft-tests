# wa-post-deployment-ft-tests

[![Build Status](https://travis-ci.org/hmcts/wa-post-deployment-ft-tests.svg?branch=master)](https://travis-ci.org/hmcts/wa-post-deployment-ft-tests)

## Purpose
This repository contains a set of functional tests which are designed to run periodically or after a helm deployment as a post deployment job to ensure regression.

## What does this app do?
It creates a case in CCD and publishes a message for this case on the Demo ASB topic.
Then your local Case Event Handler can consume this message using your development subscription.
This way we can test user paths end to end.

## Requirements
* Minikube and your local env has to be up and running.
* Bring up the following services:
  * case event handler
  * workflow-api
  * task-management-api
  * configuration-api
  * case-api
  * documentation-api
  * notification-api

* Configure the following env vars in the application-functional profile:
  * AZURE_SERVICE_BUS_CONNECTION_STRING
  * AZURE_SERVICE_BUS_TOPIC_NAME

* Source your bash so that the Case Event Handler can use those env vars too.
* Finally, set the following env vars in the Case Event Handler:
  * AZURE_SERVICE_BUS_SUBSCRIPTION_NAME to your developer subscription in the demo env.
  * AZURE_SERVICE_BUS_FEATURE_TOGGLE=true

## When merging to master:
When performing a merge against master the withNightlyPipeline() will be used to run this tests and verify the build this is because this app is not a service that needs to be deployed but rather just a test framework.
The withNightlyPipeline() will perform:

- Dependency check
- Full functional tests run

## Nightly Builds
The pipeline has also been configured to run every hour in the nightly builds.
This is specified on the `Jenkinsfile_nightly` file as a cron job `pipelineTriggers([cron('0 * * * *')])`

## Running functional tests
```bash
./gradlew functional
```
### (TODO) You can also target a specific scenario:
```bash
./gradlew functional --tests ScenarioRunnerTest --info -Dscenario=IA-RWA-000-requestRespondentEvidence-with-awaitingRespondentEvidence-postEventState-should-create-a-task
```
### (TODO) or multiple scenarios:
```bash
./gradlew functional --tests ScenarioRunnerTest --info -Dscenario=IA-RWA-000
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
