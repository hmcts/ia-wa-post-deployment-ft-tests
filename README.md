# wa-post-deployment-ft-tests

[![Build Status](https://travis-ci.org/hmcts/wa-post-deployment-ft-tests.svg?branch=master)](https://travis-ci.org/hmcts/wa-post-deployment-ft-tests)

#### What does this app do?

This repository contains a set of functional tests which are designed to run periodically or after a helm deployment as a post deployment job to ensure regression.

## Running the post deployment functional tests

### Pre-requisites

- WA environment
- IA environment
- An azure service bus topic where to publish the messages 

Note: The connection string and topic can be configured using these environment variables:

`AZURE_SERVICE_BUS_CONNECTION_STRING` The azure service bus connection string
`AZURE_SERVICE_BUS_TOPIC_NAME` The azure service bus topic name to publish messages 
`AZURE_SERVICE_BUS_MESSAGE_AUTHOR` The author of the message this can be used if you have filters set up in your subscription

### Running in the pipeline

#### When merging to master:
When performing a merge against master the withNightlyPipeline() will be used to run this tests and verify the build this is because this app is not a service that needs to be deployed but rather just a test framework.
The withNightlyPipeline() will perform:

- Dependency check
- Full functional tests run

#### Nightly Builds
The pipeline has also been configured to run every hour in the nightly builds. 
This is specified on the `Jenkinsfile_nightly` file as a cron job `pipelineTriggers([cron('0 * * * *')])`

### Running locally

To run the tests locally ensure you meet the pre-requisites and run with:
```shell
./gradlew clean functional
```
## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
