// vars/dockerKubePipeline.groovy
def call(Map config) {
    pipeline {
        agent any

        environment {
            DOCKER_IMAGE_NAME = config.dockerImageName
            DOCKER_IMAGE_TAG  = config.dockerImageTag
            KUBE_DEPLOYMENT_FILE = config.kubeDeploymentFile
        }

        stages {
            stage('Clone Repository') {
                steps {
                    git url: config.repositoryUrl, branch: config.branch, credentialsId: config.gitCredentialsId
                }
            }

            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} Jenkins/Task-3/."
                }
            }

            stage('Push Docker Image') {
                steps {
                    withCredentials([usernamePassword(
                        credentialsId: config.dockerHubCredentialsId,
                        usernameVariable: 'DOCKER_HUB_USER',
                        passwordVariable: 'DOCKER_HUB_PASSWORD'
                    )]) {
                        sh "echo ${DOCKER_HUB_PASSWORD} | docker login -u ${DOCKER_HUB_USER} --password-stdin"
                        sh "docker push ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                    }
                }
            }

            stage('Delete Local Image') {
                steps {
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                }
            }

            stage('Update K8s Deployment') {
                steps {
                    sh """
                        sed -i 's|image:.*|image: ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}|g' ${KUBE_DEPLOYMENT_FILE}
                    """
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    withCredentials([file(
                        credentialsId: config.k8sTokenCredentialsId,
                        variable: 'K8S_TOKEN'
                    )]) {
                        script {
                            sh """
                                kubectl config set-credentials jenkins-user --token=\$(cat ${K8S_TOKEN})
                                kubectl config set-cluster my-cluster --server=https://192.168.49.2:8443 --insecure-skip-tls-verify
                                kubectl config set-context jenkins-context --cluster=my-cluster --user=jenkins-user
                                kubectl config use-context jenkins-context
                                kubectl apply -f ${KUBE_DEPLOYMENT_FILE}
                            """
                        }
                    }
                }
            }
        }

        post {
            always {
                echo 'Pipeline completed.'
            }
        }
    }
}