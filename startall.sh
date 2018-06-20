PROJECT_NAME=casino-demo
COMMON=--limits='memory=512Mi'
kubectl run axon-casino --image eu.gcr.io/$PROJECT_NAME/axon-casino --env="axoniq.axonhub.servers=axonhub.default.svc.cluster.local" --image-pull-policy=Always --env="spring.profiles.active=gui" $COMMON
kubectl run wallet --image eu.gcr.io/$PROJECT_NAME/axon-casino --env="axoniq.axonhub.servers=axonhub.default.svc.cluster.local" --image-pull-policy=Always --env="spring.profiles.active=wallet" $COMMON
kubectl run game --image eu.gcr.io/$PROJECT_NAME/axon-casino --env="axoniq.axonhub.servers=axonhub.default.svc.cluster.local" --image-pull-policy=Always --env="spring.profiles.active=game" $COMMON
kubectl run query --image eu.gcr.io/$PROJECT_NAME/axon-casino --env="axoniq.axonhub.servers=axonhub.default.svc.cluster.local" --image-pull-policy=Always --env="spring.profiles.active=query" $COMMON
kubectl run kyp --image eu.gcr.io/$PROJECT_NAME/axon-casino --env="axoniq.axonhub.servers=axonhub.default.svc.cluster.local" --image-pull-policy=Always --env="spring.profiles.active=kyp" $COMMON
kubectl run process --image eu.gcr.io/$PROJECT_NAME/axon-casino --env="axoniq.axonhub.servers=axonhub.default.svc.cluster.local" --image-pull-policy=Always --env="spring.profiles.active=process" $COMMON
