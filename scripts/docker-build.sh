ver=$(git describe --tags --abbrev=0)
docker build -t "polynote:$ver" -t polynote:latest ..

kind load docker-image polynote:latest --name polynote
kind load docker-image "polynote:$ver" --name polynote

kubectl rollout restart deployment polynote-a
kubectl rollout restart deployment polynote-b
kubectl rollout restart deployment polynote-c