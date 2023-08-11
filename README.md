# riddlr-server

### Developer Notes

[Install](https://developers.generativeai.google/tutorials/text_java_quickstart#install_the_api_client) the PaLM API Java client locally.

To build and deploy the api (frontend) service:
```
./gradlew clean build frontend:appengineDeploy
```

To update dispatch rules for relevant services:
```
  gcloud init
  gcloud app deploy appengine/dispatch.yaml
```
