// vars/buildAndDeploy.groovy
def call(Map config) {
    // Clone the repository
    git url: config.repositoryUrl, branch: config.branch, credentialsId: config.gitCredentialsId

    // Build Docker image
    sh "docker build -t ${config.dockerImageName}:${config.dockerImageTag} ."

    // Push Docker image to Docker Hub
    withCredentials([usernamePassword(credentialsId: config.dockerHubCredentialsId, usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
        sh "echo ${DOCKER_HUB_PASSWORD} | docker login -u ${DOCKER_HUB_USER} --password-stdin"
        sh "docker push ${config.dockerImageName}:${config.dockerImageTag}"
    }

    // Delete local Docker image
    sh "docker rmi ${config.dockerImageName}:${config.dockerImageTag}"

    // Update Kubernetes deployment
    sh """
        sed -i 's|image: .*|image: ${config.dockerImageName}:${config.dockerImageTag}|g' ${config.kubeDeploymentFile}
    """

    // Deploy to Kubernetes
    sh "kubectl apply -f ${config.kubeDeploymentFile}"
}