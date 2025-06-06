# ia-wa-post-deployment-ft-tests

[![Build Status](https://travis-ci.org/hmcts/ia-wa-post-deployment-ft-tests.svg?branch=master)](https://travis-ci.org/hmcts/wa-post-deployment-ft-tests)

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
  * case-api
  * documentation-api
  * notification-api

* Configure the following env vars in the application-functional profile:
  * AZURE_SERVICE_BUS_CONNECTION_STRING
  * AZURE_SERVICE_BUS_TOPIC_NAME
  * AZURE_SERVICE_BUS_MESSAGE_AUTHOR (The author of the message this can be used if you have filters set up in your subscription)

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

## Publishing as ACR task
Any commit or merge into master will automatically trigger an Azure ACR task. This task has been manually
created using `./bin/deploy-acr-task.sh`. The task is defined in `acr-build-task.yaml`.

Note: the deployment script relies on a GitHub token (https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line) defined in `infra-vault-prod`, secret `hmcts-github-apikey`. The token is for setting up a webhook so Azure will be notified when a merge or commit happens. Make sure you are a repo admin and select token scope of: `admin:repo_hook  Full control of repository hooks`

More info on ACR tasks can be read here: https://docs.microsoft.com/en-us/azure/container-registry/container-registry-tasks-overview

## Running functional tests
```bash
./gradlew functional
```
### You can also target a specific scenario:
```bash
./gradlew functional --tests ScenarioRunnerTest --info -Dscenario=IA-RWA-000-requestRespondentEvidence-with-awaitingRespondentEvidence-postEventState-should-create-a-task
```
### or multiple scenarios:
```bash
./gradlew functional --tests ScenarioRunnerTest --info -Dscenario=IA-RWA-000
```

## Tips for onboarding teams
if you are onboarding into Task Management, you may clone this repository and customize the following items to setup
your own post deployment tests:
# src/functionalTest/resources/application-functional.yaml
- document_management.url: set your local document store api
- ccd_gateway.url: set your local ccd gateway
- core_case_data.api.url: set your local ccd case data api
- azure.servicebus.connection-string: set your service bus connection string
- azure.servicebus.topic set your service bus topic name

# src/functionalTest/resources/templates
add your case data template file in json format, this template will be used in your scenarios
# src/functionalTest/resources/scenarios
- add your test scenario files in json format
# src/testUtils/java/uk/gov/hmcts/reform/wapostdeploymentfttests/services/AuthorizationHeadersProvider.java
customize this class to setup your authorization headers and user credentials.
# Caveat
This repository is still under development and so changes may occur in the future.
We will update this Readme file anytime we modify the structure or the underlying framework.
If you are unsure about something, please check with the team for clarification.

# Running against Preview
Please replace <PR> with the actual preview PR number (e.g., pr-2500) in the terminal and then execute the functional tests. These tests are expected to user all containers in preview for both CCD and WA.

User profile variables are currently assigned to active AAT user profiles. If you encounter any test failures related to user profiles, it might be because they've been deactivated or removed from AAT. In such cases, please update the variables with new, active profiles.

By adjusting the URLs below, the functional tests can be executed against any desired environment. Make sure user profiles are updated relevant to the environment as well.

export WA_TASK_MANAGEMENT_URL=“https://wa-task-management-api-ia-case-api-<PR>.preview.platform.hmcts.net”
export WA_CASE_EVENT_HANDLER_URL=“https://wa-case-event-handler-ia-case-api-<PR>.preview.platform.hmcts.net”
export WA_TASK_MONITOR_URL=“https://wa-task-monitor-ia-case-api-<PR>.preview.platform.hmcts.net”
export CCD_URL=“https://ccd-data-store-api-ia-case-api-<PR>.preview.platform.hmcts.net”
export WA_POST_DEPLOYMENT_TEST_ENVIRONMENT=local
export AZURE_SERVICE_BUS_CONNECTION_STRING=“Endpoint=sb://ia-sb-preview.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=bo/fiSRbwJ9fjEnklyX90t+WyEEBea+GA+ASbK0idNI=“
export AZURE_SERVICE_BUS_TOPIC_NAME=“ia-case-api-<PR>-ccd-case-events”
export AZURE_SERVICE_BUS_MESSAGE_AUTHOR=<PR>
export DM_STORE_URL=“http://dm-store-aat.service.core-compute-aat.internal”
export CCD_GW_URL=“https://gateway-ia-case-api-<PR>.preview.platform.hmcts.net”

export IA_IDAM_CLIENT_ID=iac
export IA_IDAM_CLIENT_SECRET=NDPH6DFQBIPA9UP7
export OPEN_ID_IDAM_URL=“https://idam-web-public.aat.platform.hmcts.net”
export IA_IDAM_REDIRECT_URI=“https://ia-case-api-aat.service.core-compute-aat.internal/oauth2/callback”
export S2S_URL=“http://rpe-service-auth-provider-aat.service.core-compute-aat.internal”
export S2S_SECRET_TASK_MANAGEMENT_API=UYIEF2FUGBFE666Y
export S2S_NAME_TASK_MANAGEMENT_API=wa_task_management_api
export IDAM_URL=“https://idam-web-public.aat.platform.hmcts.net”
export CAMUNDA_URL=“https://camunda-ia-case-api-<PR>.preview.platform.hmcts.net/engine-rest”
export ROLE_ASSIGNMENT_URL=“https://am-role-assignment-ia-case-api-<PR>.preview.platform.hmcts.net”


export TEST_WA_CASEOFFICER_PUBLIC_A_USERNAME=“CRD_func_test_aat_stcw@justice.gov.uk”
export TEST_WA_CASEOFFICER_PUBLIC_A_PASSWORD=“AldgateT0wer”
export TEST_CTSC_ADMIN_USERNAME=“CRD_func_test_aat_adm66@justice.gov.uk”
export TEST_CTSC_ADMIN_PASSWORD=“AldgateT0wer”
export TEST_ADMINOFFICER_USERNAME=“CRD_func_test_aat_adm66@justice.gov.uk”
export TEST_ADMINOFFICER_PASSWORD=“AldgateT0wer”
export TEST_NBC_ADMIN_USERNAME=“CRD_func_test_aat_adm66@justice.gov.uk”
export TEST_NBC_ADMIN_PASSWORD=“AldgateT0wer”
export TEST_WA_LAW_FIRM_USERNAME=“ialegalreporgcreator12@mailnesia.com”
export TEST_WA_LAW_FIRM_PASSWORD=“Aldg@teT0wer”

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
