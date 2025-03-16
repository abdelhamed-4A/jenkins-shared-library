// vars/buildAndDeploy.groovy
def call(Map config) {
    // Clone the repository
    git url: 'https://github.com/abdelhamed-4A/NTI-IVolve-Training.git', branch: 'main', credentialsId: 'github-token'

    // Build Docker image
    sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} Jenkins/Task-3/."

    // Push Docker image to Docker Hub
    withCredentials([usernamePassword(credentialsId: 'DockerHub', usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
        sh "echo ${DOCKER_HUB_PASSWORD} | docker login -u ${DOCKER_HUB_USER} --password-stdin"
        sh "docker push ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
    }

    // Delete local Docker image
    sh "docker rmi ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"

    // Update Kubernetes deployment
    sh """
        kubectl config set-credentials jenkins-user --token=${K8S_TOKEN}
                kubectl config set-cluster minikube --server=https://192.168.49.2:8443 --insecure-skip-tls-verify
                kubectl config set-context jenkins-context --cluster=my-cluster --user=jenkins-user
                kubectl config use-context jenkins-context
                kubectl apply -f Jenkins/Task-3/deployment.yaml
    """

}