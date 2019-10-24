# Clojure Dojo

* Intro - what and why clojure?
* IDE & REPL: emacs / vim / vscode ....
* env: lein / deps.edn / docker
* koans as tests
* databases: jdbc, coersion, honeysql, hugsql etc
* web: httpkit, rest, routing, context
* ui: reagent, re-frame
* tests: back and ui
* deploy: jar, kubernetes


## Start

```sh
make up
make repl
# connect from ide

# for ui
cd ui
make repl

# after that your UI is available by http://localhost:8887/static/index.html
```

## Deploy

### Kubectl

On Mac:

```
brew install kubernetes-cli
```

On Linux see https://kubernetes.io/docs/tasks/tools/install-kubectl/#install-kubectl-on-linux

### Google Cloud SDK

Install Google Cloud SDK locally:

On Mac:
```
brew cask install google-cloud-sdk
```

On Linux see https://cloud.google.com/sdk/docs/quickstarts

Create project in [Google Clound Console](https://console.cloud.google.com/home/dashboard), then login locally with:
```
gcloud init
```

Ask @niqola to give your Google account access, then:
```
gcloud container clusters get-credentials dojo --zone us-central1-a --project dojo-clojure
```

It overwrites your local ` ~/.kube/config`

Check you can access cluster:
```
kubectl get nodes
```

### Build and deploy

To build artifacts, pack them into docker image and deploy to cluster use:
```
make all
```

Just to deploy or update entities in Kubernetes cluster
```
make deploy
```

To get server IP address use:
```
kubectl get ingress -n dojo
```
