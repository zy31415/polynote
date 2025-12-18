export POLYNOTE_DATA_DIR="$HOME/workspace/polynote/data"
envsubst < kind-config.yaml.tmpl > kind-config.yaml
kind create cluster --name polynote --config kind-config.yaml